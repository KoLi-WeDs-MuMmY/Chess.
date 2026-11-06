package com.example.chess.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chess.data.ChessMatchRepository
import com.example.chess.data.MatchRecord
import com.example.chess.data.StreakStats
import com.example.chess.engine.ChessAiEngine
import com.example.chess.engine.ChessBoardState
import com.example.chess.model.AiDifficulty
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.ChessMove
import com.example.chess.model.GameStatus
import com.example.chess.model.MatchOutcome
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.PlayerSideChoice
import com.example.chess.model.Square
import com.example.chess.model.TimeControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

enum class AppScreen {
    ARENA_HOME,
    ACTIVE_MATCH,
    HISTORY_ARCHIVE,
    POST_GAME_ANALYSIS
}

data class ActiveMatchUiState(
    val hasStartedMatch: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val timeControl: TimeControl = TimeControl.RAPID_10_0,
    val playerColor: PieceColor = PieceColor.WHITE,
    val boardState: ChessBoardState = ChessBoardState.initial(),
    val isBoardFlipped: Boolean = false,
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val pendingPromotionMoves: List<ChessMove> = emptyList(),
    val hintMove: ChessMove? = null,
    val isAiThinking: Boolean = false,
    val liveEvalWhiteCp: Int = 0,
    val analyzedMoves: List<AnalyzedMove> = emptyList(),
    val whiteTimeRemainingMs: Long = TimeControl.RAPID_10_0.initialTimeMs,
    val blackTimeRemainingMs: Long = TimeControl.RAPID_10_0.initialTimeMs,
    val gameStatus: GameStatus = GameStatus.ACTIVE,
    val matchStartEpochMs: Long = System.currentTimeMillis(),
    val savedMatchRecordId: Long? = null,
    val showGameOverDialog: Boolean = false
)

data class MatchSetupState(
    val selectedDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val selectedTimeControl: TimeControl = TimeControl.RAPID_10_0,
    val selectedSideChoice: PlayerSideChoice = PlayerSideChoice.WHITE
)

data class AnalysisUiState(
    val matchRecord: MatchRecord? = null,
    val currentPly: Int = 0,
    val isBoardFlipped: Boolean = false,
    val isAutoPlaying: Boolean = false
)

class ChessAppViewModel(
    private val repository: ChessMatchRepository
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.ARENA_HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _setupState = MutableStateFlow(MatchSetupState())
    val setupState: StateFlow<MatchSetupState> = _setupState.asStateFlow()

    private val _matchState = MutableStateFlow(ActiveMatchUiState())
    val matchState: StateFlow<ActiveMatchUiState> = _matchState.asStateFlow()

    private val _analysisState = MutableStateFlow(AnalysisUiState())
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _historyOutcomeFilter = MutableStateFlow<MatchOutcome?>(null)
    val historyOutcomeFilter: StateFlow<MatchOutcome?> = _historyOutcomeFilter.asStateFlow()

    private val _historyDifficultyFilter = MutableStateFlow<AiDifficulty?>(null)
    val historyDifficultyFilter: StateFlow<AiDifficulty?> = _historyDifficultyFilter.asStateFlow()

    val allMatches: StateFlow<List<MatchRecord>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val streakStats: StateFlow<StreakStats> = repository.streakStats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakStats()
    )

    private var clockJob: Job? = null
    private var aiTurnJob: Job? = null
    private var autoReplayJob: Job? = null

    fun navigateTo(screen: AppScreen) {
        if (screen != AppScreen.POST_GAME_ANALYSIS) {
            stopAutoReplay()
        }
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        return when (_currentScreen.value) {
            AppScreen.POST_GAME_ANALYSIS -> {
                stopAutoReplay()
                _currentScreen.value = AppScreen.HISTORY_ARCHIVE
                true
            }
            AppScreen.ACTIVE_MATCH, AppScreen.HISTORY_ARCHIVE -> {
                _currentScreen.value = AppScreen.ARENA_HOME
                true
            }
            AppScreen.ARENA_HOME -> false
        }
    }

    fun updateDifficulty(difficulty: AiDifficulty) {
        _setupState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun updateTimeControl(timeControl: TimeControl) {
        _setupState.update { it.copy(selectedTimeControl = timeControl) }
    }

    fun updateSideChoice(sideChoice: PlayerSideChoice) {
        _setupState.update { it.copy(selectedSideChoice = sideChoice) }
    }

    fun startNewMatch(
        difficulty: AiDifficulty = _setupState.value.selectedDifficulty,
        timeControl: TimeControl = _setupState.value.selectedTimeControl,
        sideChoice: PlayerSideChoice = _setupState.value.selectedSideChoice
    ) {
        aiTurnJob?.cancel()
        clockJob?.cancel()

        val resolvedPlayerColor = when (sideChoice) {
            PlayerSideChoice.WHITE -> PieceColor.WHITE
            PlayerSideChoice.BLACK -> PieceColor.BLACK
            PlayerSideChoice.RANDOM -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }

        val initialBoard = ChessBoardState.initial()
        _matchState.value = ActiveMatchUiState(
            hasStartedMatch = true,
            aiDifficulty = difficulty,
            timeControl = timeControl,
            playerColor = resolvedPlayerColor,
            boardState = initialBoard,
            isBoardFlipped = resolvedPlayerColor == PieceColor.BLACK,
            whiteTimeRemainingMs = timeControl.initialTimeMs,
            blackTimeRemainingMs = timeControl.initialTimeMs,
            gameStatus = GameStatus.ACTIVE,
            matchStartEpochMs = System.currentTimeMillis()
        )
        _currentScreen.value = AppScreen.ACTIVE_MATCH

        if (timeControl.isTimed) {
            startClockTicker()
        }

        if (resolvedPlayerColor == PieceColor.BLACK) {
            triggerAiTurnIfNeeded()
        }
    }

    private fun startClockTicker() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val current = _matchState.value
                if (!current.hasStartedMatch ||
                    current.gameStatus.isFinished ||
                    !current.timeControl.isTimed ||
                    _currentScreen.value != AppScreen.ACTIVE_MATCH
                ) {
                    if (current.gameStatus.isFinished) break
                    continue
                }

                val activeSide = current.boardState.activeColor
                if (activeSide == PieceColor.WHITE) {
                    val nextTime = max(0L, current.whiteTimeRemainingMs - 100L)
                    _matchState.update { it.copy(whiteTimeRemainingMs = nextTime) }
                    if (nextTime == 0L) {
                        concludeMatch(GameStatus.TIMEOUT_BLACK_WINS)
                        break
                    }
                } else {
                    val nextTime = max(0L, current.blackTimeRemainingMs - 100L)
                    _matchState.update { it.copy(blackTimeRemainingMs = nextTime) }
                    if (nextTime == 0L) {
                        concludeMatch(GameStatus.TIMEOUT_WHITE_WINS)
                        break
                    }
                }
            }
        }
    }

    fun onSquareTapped(square: Square) {
        val state = _matchState.value
        if (state.gameStatus.isFinished || state.isAiThinking) return
        if (state.boardState.activeColor != state.playerColor) return

        // Check if tapped square is a destination for currently selected square
        val matchingMoves = state.legalMovesForSelected.filter { it.to == square }
        if (matchingMoves.isNotEmpty()) {
            if (matchingMoves.size > 1 && matchingMoves.any { it.promotion != null }) {
                // Ask player which promotion piece they want
                _matchState.update { it.copy(pendingPromotionMoves = matchingMoves) }
            } else {
                executeMove(matchingMoves.first())
            }
            return
        }

        // Otherwise check if tapped square has a friendly piece
        val tappedPiece = state.boardState.pieceAt(square)
        if (tappedPiece != null && tappedPiece.color == state.playerColor) {
            if (state.selectedSquare == square) {
                _matchState.update {
                    it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
                }
            } else {
                val legalForSq = state.boardState.generateLegalMovesForSquare(square)
                _matchState.update {
                    it.copy(
                        selectedSquare = square,
                        legalMovesForSelected = legalForSq
                    )
                }
            }
        } else {
            _matchState.update {
                it.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    fun onPromotionPieceSelected(pieceType: PieceType) {
        val pending = _matchState.value.pendingPromotionMoves
        val chosen = pending.firstOrNull { it.promotion == pieceType } ?: pending.firstOrNull()
        _matchState.update { it.copy(pendingPromotionMoves = emptyList()) }
        if (chosen != null) {
            executeMove(chosen)
        }
    }

    fun dismissPromotionPicker() {
        _matchState.update { it.copy(pendingPromotionMoves = emptyList()) }
    }

    fun flipActiveBoard() {
        _matchState.update { it.copy(isBoardFlipped = !it.isBoardFlipped) }
    }

    fun requestBestMoveHint() {
        val state = _matchState.value
        if (state.gameStatus.isFinished || state.isAiThinking) return
        if (!state.aiDifficulty.allowHints) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                ChessAiEngine.selectMove(state.boardState, AiDifficulty.HARD)
            }
            if (result != null) {
                _matchState.update {
                    it.copy(
                        hintMove = result.bestMove,
                        selectedSquare = result.bestMove.from,
                        legalMovesForSelected = state.boardState.generateLegalMovesForSquare(result.bestMove.from)
                    )
                }
            }
        }
    }

    fun resignActiveMatch() {
        val state = _matchState.value
        if (state.gameStatus.isFinished) return
        val status = if (state.playerColor == PieceColor.WHITE) {
            GameStatus.RESIGNED_BLACK_WINS
        } else {
            GameStatus.RESIGNED_WHITE_WINS
        }
        concludeMatch(status)
    }

    fun dismissGameOverDialog() {
        _matchState.update { it.copy(showGameOverDialog = false) }
    }

    private fun executeMove(move: ChessMove) {
        val current = _matchState.value
        val boardBefore = current.boardState
        val legalBefore = boardBefore.generateLegalMoves()
        val san = boardBefore.toSan(move, legalBefore)
        val fenBefore = boardBefore.toFen()
        val nextBoard = boardBefore.applyMove(move)
        val fenAfter = nextBoard.toFen()

        // Add time increment if timed
        val inc = current.timeControl.incrementMs
        val updatedWhiteTime = if (boardBefore.activeColor == PieceColor.WHITE && current.timeControl.isTimed) {
            current.whiteTimeRemainingMs + inc
        } else {
            current.whiteTimeRemainingMs
        }
        val updatedBlackTime = if (boardBefore.activeColor == PieceColor.BLACK && current.timeControl.isTimed) {
            current.blackTimeRemainingMs + inc
        } else {
            current.blackTimeRemainingMs
        }

        val nextLegalMoves = nextBoard.generateLegalMoves()
        val nextStatus = nextBoard.computeGameStatus(nextLegalMoves)
        val newPlyNumber = current.analyzedMoves.size + 1

        viewModelScope.launch {
            // Compute move accuracy and classification on Default dispatcher
            val analysis = withContext(Dispatchers.Default) {
                ChessAiEngine.analyzePlayedMove(boardBefore, move)
            }

            val analyzedMove = AnalyzedMove(
                ply = newPlyNumber,
                moveNumber = boardBefore.fullMoveNumber,
                color = boardBefore.activeColor,
                san = san,
                uciPlayed = move.toUci(),
                uciBestMove = analysis.bestMove.toUci(),
                bestMoveSan = analysis.bestMoveSan,
                evalBeforeWhiteCp = analysis.evalBeforeWhiteCp,
                evalAfterWhiteCp = analysis.evalAfterWhiteCp,
                cpLossForMover = analysis.cpLossForMover,
                classification = analysis.classification,
                fenBefore = fenBefore,
                fenAfter = fenAfter,
                capturedPieceName = move.capturedPiece?.type?.displayName
            )

            _matchState.update {
                it.copy(
                    boardState = nextBoard,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList(),
                    pendingPromotionMoves = emptyList(),
                    hintMove = null,
                    liveEvalWhiteCp = analysis.evalAfterWhiteCp,
                    analyzedMoves = it.analyzedMoves + analyzedMove,
                    whiteTimeRemainingMs = updatedWhiteTime,
                    blackTimeRemainingMs = updatedBlackTime
                )
            }

            if (nextStatus.isFinished) {
                concludeMatch(nextStatus)
            } else {
                triggerAiTurnIfNeeded()
            }
        }
    }

    private fun triggerAiTurnIfNeeded() {
        val state = _matchState.value
        if (state.gameStatus.isFinished) return
        if (state.boardState.activeColor == state.playerColor) return

        aiTurnJob?.cancel()
        aiTurnJob = viewModelScope.launch {
            _matchState.update { it.copy(isAiThinking = true) }
            // Brief natural pacing so player sees their own move land before AI responds
            delay(260L)
            val currentBoard = _matchState.value.boardState
            val difficulty = _matchState.value.aiDifficulty

            val searchResult = withContext(Dispatchers.Default) {
                ChessAiEngine.selectMove(currentBoard, difficulty)
            }

            _matchState.update { it.copy(isAiThinking = false) }
            if (searchResult != null && !_matchState.value.gameStatus.isFinished) {
                executeMove(searchResult.bestMove)
            }
        }
    }

    private fun concludeMatch(finalStatus: GameStatus) {
        aiTurnJob?.cancel()
        clockJob?.cancel()

        val state = _matchState.value
        if (state.gameStatus.isFinished) return

        val winner = finalStatus.winnerColor()
        val outcome = when (winner) {
            null -> MatchOutcome.DRAW
            state.playerColor -> MatchOutcome.WIN
            else -> MatchOutcome.LOSS
        }

        val durationSec = max(1, ((System.currentTimeMillis() - state.matchStartEpochMs) / 1000L).toInt())

        _matchState.update {
            it.copy(
                gameStatus = finalStatus,
                isAiThinking = false,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                showGameOverDialog = true
            )
        }

        viewModelScope.launch {
            val savedId = repository.saveCompletedMatch(
                aiDifficulty = state.aiDifficulty,
                timeControl = state.timeControl,
                playerColor = state.playerColor,
                outcome = outcome,
                terminationReason = finalStatus.terminationTitle(),
                analyzedMoves = _matchState.value.analyzedMoves,
                durationSeconds = durationSec,
                finalFen = state.boardState.toFen()
            )
            _matchState.update { it.copy(savedMatchRecordId = savedId) }
        }
    }

    // History & Post-Game Analysis Controls

    fun setHistoryOutcomeFilter(outcome: MatchOutcome?) {
        _historyOutcomeFilter.value = outcome
    }

    fun setHistoryDifficultyFilter(difficulty: AiDifficulty?) {
        _historyDifficultyFilter.value = difficulty
    }

    fun openMatchAnalysis(matchRecord: MatchRecord) {
        stopAutoReplay()
        // Default to first blunder or mistake if one exists, otherwise end of game or ply 1
        val firstCriticalPly = matchRecord.analyzedMoves.firstOrNull {
            it.color == matchRecord.playerColor &&
                (it.classification.name == "BLUNDER" || it.classification.name == "MISTAKE")
        }?.ply ?: matchRecord.analyzedMoves.size

        _analysisState.value = AnalysisUiState(
            matchRecord = matchRecord,
            currentPly = firstCriticalPly,
            isBoardFlipped = matchRecord.playerColor == PieceColor.BLACK,
            isAutoPlaying = false
        )
        _currentScreen.value = AppScreen.POST_GAME_ANALYSIS
    }

    fun openCompletedActiveMatchAnalysis() {
        val current = _matchState.value
        val savedId = current.savedMatchRecordId
        val fromRepo = allMatches.value.firstOrNull { it.id == savedId }
        if (fromRepo != null) {
            _matchState.update { it.copy(showGameOverDialog = false) }
            openMatchAnalysis(fromRepo)
            return
        }

        // Construct immediate in-memory record if flow hasn't emitted yet
        val winner = current.gameStatus.winnerColor()
        val outcome = when (winner) {
            null -> MatchOutcome.DRAW
            current.playerColor -> MatchOutcome.WIN
            else -> MatchOutcome.LOSS
        }
        val playerMoves = current.analyzedMoves.filter { it.color == current.playerColor }
        val tempRecord = MatchRecord(
            id = savedId ?: 0L,
            timestamp = System.currentTimeMillis(),
            aiDifficulty = current.aiDifficulty,
            timeControl = current.timeControl,
            playerColor = current.playerColor,
            outcome = outcome,
            terminationReason = current.gameStatus.terminationTitle(),
            playerAccuracy = ChessAiEngine.calculateSideAccuracy(current.analyzedMoves, current.playerColor),
            aiAccuracy = ChessAiEngine.calculateSideAccuracy(current.analyzedMoves, current.playerColor.opposite()),
            bestMoveCount = playerMoves.count { it.classification.name == "BEST" },
            goodMoveCount = playerMoves.count { it.classification.name == "GOOD" },
            inaccuracyCount = playerMoves.count { it.classification.name == "INACCURACY" },
            mistakeCount = playerMoves.count { it.classification.name == "MISTAKE" },
            blunderCount = playerMoves.count { it.classification.name == "BLUNDER" },
            totalPlies = current.analyzedMoves.size,
            durationSeconds = max(1, ((System.currentTimeMillis() - current.matchStartEpochMs) / 1000L).toInt()),
            finalFen = current.boardState.toFen(),
            analyzedMoves = current.analyzedMoves
        )
        _matchState.update { it.copy(showGameOverDialog = false) }
        openMatchAnalysis(tempRecord)
    }

    fun setAnalysisPly(ply: Int) {
        val record = _analysisState.value.matchRecord ?: return
        val clamped = ply.coerceIn(0, record.analyzedMoves.size)
        _analysisState.update { it.copy(currentPly = clamped) }
    }

    fun stepAnalysisPly(delta: Int) {
        stopAutoReplay()
        val current = _analysisState.value
        val record = current.matchRecord ?: return
        val next = (current.currentPly + delta).coerceIn(0, record.analyzedMoves.size)
        _analysisState.update { it.copy(currentPly = next) }
    }

    fun toggleAnalysisBoardFlip() {
        _analysisState.update { it.copy(isBoardFlipped = !it.isBoardFlipped) }
    }

    fun toggleAutoReplay() {
        val current = _analysisState.value
        if (current.isAutoPlaying) {
            stopAutoReplay()
            return
        }
        val record = current.matchRecord ?: return
        if (record.analyzedMoves.isEmpty()) return

        if (current.currentPly >= record.analyzedMoves.size) {
            _analysisState.update { it.copy(currentPly = 0) }
        }
        _analysisState.update { it.copy(isAutoPlaying = true) }

        autoReplayJob?.cancel()
        autoReplayJob = viewModelScope.launch {
            while (_analysisState.value.isAutoPlaying) {
                delay(950L)
                val st = _analysisState.value
                val maxPly = st.matchRecord?.analyzedMoves?.size ?: 0
                if (st.currentPly < maxPly) {
                    _analysisState.update { it.copy(currentPly = it.currentPly + 1) }
                } else {
                    _analysisState.update { it.copy(isAutoPlaying = false) }
                    break
                }
            }
        }
    }

    private fun stopAutoReplay() {
        autoReplayJob?.cancel()
        _analysisState.update { it.copy(isAutoPlaying = false) }
    }

    fun deleteArchivedMatch(id: Long) {
        viewModelScope.launch {
            repository.deleteMatch(id)
        }
    }

    fun loadTacticalShowcaseMatch() {
        viewModelScope.launch {
            repository.insertTacticalShowcaseMatch()
        }
    }

    companion object {
        fun provideFactory(repository: ChessMatchRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChessAppViewModel(repository) as T
                }
            }
        }
    }
}
