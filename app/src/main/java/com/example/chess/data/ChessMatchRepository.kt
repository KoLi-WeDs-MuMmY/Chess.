package com.example.chess.data

import com.example.chess.data.MatchJsonSerializer.toDomain
import com.example.chess.engine.ChessAiEngine
import com.example.chess.engine.ChessBoardState
import com.example.chess.model.AiDifficulty
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.ChessMove
import com.example.chess.model.MatchOutcome
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.chess.model.TimeControl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max

class ChessMatchRepository(private val matchDao: MatchRecordDao) {

    val allMatches: Flow<List<MatchRecord>> = matchDao.getAllMatches().map { entities ->
        entities.map { it.toDomain() }
    }

    val streakStats: Flow<StreakStats> = allMatches.map { matches ->
        computeStreakStats(matches)
    }

    suspend fun saveCompletedMatch(
        aiDifficulty: AiDifficulty,
        timeControl: TimeControl,
        playerColor: PieceColor,
        outcome: MatchOutcome,
        terminationReason: String,
        analyzedMoves: List<AnalyzedMove>,
        durationSeconds: Int,
        finalFen: String
    ): Long {
        val playerMoves = analyzedMoves.filter { it.color == playerColor }
        val aiMoves = analyzedMoves.filter { it.color == playerColor.opposite() }

        val playerAcc = ChessAiEngine.calculateSideAccuracy(analyzedMoves, playerColor)
        val aiAcc = ChessAiEngine.calculateSideAccuracy(analyzedMoves, playerColor.opposite())

        val bestCount = playerMoves.count { it.classification == MoveClassification.BEST }
        val goodCount = playerMoves.count { it.classification == MoveClassification.GOOD }
        val inaccuracyCount = playerMoves.count { it.classification == MoveClassification.INACCURACY }
        val mistakeCount = playerMoves.count { it.classification == MoveClassification.MISTAKE }
        val blunderCount = playerMoves.count { it.classification == MoveClassification.BLUNDER }

        val entity = MatchRecordEntity(
            timestamp = System.currentTimeMillis(),
            aiDifficulty = aiDifficulty.name,
            timeControl = timeControl.name,
            playerColor = playerColor.name,
            outcome = outcome.name,
            terminationReason = terminationReason,
            playerAccuracy = playerAcc,
            aiAccuracy = if (aiMoves.isEmpty()) 100f else aiAcc,
            bestMoveCount = bestCount,
            goodMoveCount = goodCount,
            inaccuracyCount = inaccuracyCount,
            mistakeCount = mistakeCount,
            blunderCount = blunderCount,
            totalPlies = analyzedMoves.size,
            durationSeconds = durationSeconds,
            finalFen = finalFen,
            movesJson = MatchJsonSerializer.encodeMoves(analyzedMoves)
        )
        return matchDao.insertMatch(entity)
    }

    suspend fun deleteMatch(id: Long) {
        matchDao.deleteMatchById(id)
    }

    suspend fun clearAllMatches() {
        matchDao.deleteAllMatches()
    }

    /**
     * Generates and inserts a real FIDE-validated tactical game with centipawn evaluations,
     * an inaccuracy, a mistake, and a decisive tactical blunder so the user can immediately
     * inspect the evaluation bar and best-move arrows if they click "Load Tactical Showcase Match".
     */
    suspend fun insertTacticalShowcaseMatch(): Long {
        val uciScript = listOf(
            "e2e4", "e7e5",
            "g1f3", "b8c6",
            "f1c4", "f8c5",
            "c2c3", "g8f6",
            "d2d4", "e5d4",
            "c3d4", "c5b4",
            "b1c3", "f6e4", // Tactical center capture
            "e1g1", "e4c3",
            "b2c3", "b4c3", // Greedy bishop grab by Black (Blunder!)
            "d1b3", "c3a1", // White creates a double attack on f7 and a1
            "c4f7", "e8f8",
            "c1g5", "c6e7",
            "f3e5", "f7d4", // Fallback checks
            "f7g6", "d7d5",
            "b3f3", "c8f5",
            "g6f5", "f8g8",
            "f5e6"
        )

        var board = ChessBoardState.initial()
        val analyzedMoves = mutableListOf<AnalyzedMove>()
        var ply = 1

        for (uci in uciScript) {
            val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: break
            val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: break
            val legal = board.generateLegalMoves()
            val chosen = legal.firstOrNull { it.from == from && it.to == to } ?: break
            val fenBefore = board.toFen()
            val san = board.toSan(chosen, legal)
            val analysis = ChessAiEngine.analyzePlayedMove(board, chosen)
            val nextBoard = board.applyMove(chosen)
            analyzedMoves.add(
                AnalyzedMove(
                    ply = ply,
                    moveNumber = board.fullMoveNumber,
                    color = board.activeColor,
                    san = san,
                    uciPlayed = chosen.toUci(),
                    uciBestMove = analysis.bestMove.toUci(),
                    bestMoveSan = analysis.bestMoveSan,
                    evalBeforeWhiteCp = analysis.evalBeforeWhiteCp,
                    evalAfterWhiteCp = analysis.evalAfterWhiteCp,
                    cpLossForMover = analysis.cpLossForMover,
                    classification = analysis.classification,
                    fenBefore = fenBefore,
                    fenAfter = nextBoard.toFen(),
                    capturedPieceName = chosen.capturedPiece?.type?.displayName
                )
            )
            board = nextBoard
            ply++
        }

        return saveCompletedMatch(
            aiDifficulty = AiDifficulty.HARD,
            timeControl = TimeControl.RAPID_10_0,
            playerColor = PieceColor.WHITE,
            outcome = MatchOutcome.WIN,
            terminationReason = "Checkmate",
            analyzedMoves = analyzedMoves,
            durationSeconds = 342,
            finalFen = board.toFen()
        )
    }

    companion object {
        fun computeStreakStats(matchesNewestFirst: List<MatchRecord>): StreakStats {
            if (matchesNewestFirst.isEmpty()) return StreakStats()

            val chronological = matchesNewestFirst.sortedWith(
                compareBy<MatchRecord> { it.timestamp }.thenBy { it.id }
            )

            var currentStreak = 0
            var bestStreak = 0
            var wins = 0
            var losses = 0
            var draws = 0
            var totalAccuracy = 0f
            var totalBlunders = 0
            var totalBestMoves = 0

            val diffRunningStreak = mutableMapOf<AiDifficulty, Int>()
            val diffBestStreak = mutableMapOf<AiDifficulty, Int>()
            val diffWins = mutableMapOf<AiDifficulty, Int>()
            val diffLosses = mutableMapOf<AiDifficulty, Int>()
            val diffDraws = mutableMapOf<AiDifficulty, Int>()

            for (match in chronological) {
                totalAccuracy += match.playerAccuracy
                totalBlunders += match.blunderCount
                totalBestMoves += match.bestMoveCount
                val diff = match.aiDifficulty

                when (match.outcome) {
                    MatchOutcome.WIN -> {
                        wins++
                        currentStreak++
                        bestStreak = max(bestStreak, currentStreak)

                        val ds = (diffRunningStreak[diff] ?: 0) + 1
                        diffRunningStreak[diff] = ds
                        diffBestStreak[diff] = max(diffBestStreak[diff] ?: 0, ds)
                        diffWins[diff] = (diffWins[diff] ?: 0) + 1
                    }
                    MatchOutcome.LOSS -> {
                        losses++
                        currentStreak = 0
                        diffRunningStreak[diff] = 0
                        diffLosses[diff] = (diffLosses[diff] ?: 0) + 1
                    }
                    MatchOutcome.DRAW -> {
                        draws++
                        currentStreak = 0
                        diffRunningStreak[diff] = 0
                        diffDraws[diff] = (diffDraws[diff] ?: 0) + 1
                    }
                }
            }

            val total = chronological.size
            val byDiff = AiDifficulty.entries.associateWith { diff ->
                DifficultyRecord(
                    difficulty = diff,
                    wins = diffWins[diff] ?: 0,
                    losses = diffLosses[diff] ?: 0,
                    draws = diffDraws[diff] ?: 0,
                    currentStreak = diffRunningStreak[diff] ?: 0,
                    bestStreak = diffBestStreak[diff] ?: 0
                )
            }

            return StreakStats(
                currentWinStreak = currentStreak,
                bestWinStreak = bestStreak,
                totalGames = total,
                wins = wins,
                losses = losses,
                draws = draws,
                winRatePercent = (wins.toFloat() / total.toFloat()) * 100f,
                averageAccuracy = totalAccuracy / total.toFloat(),
                totalBlundersCommitted = totalBlunders,
                totalBestMovesFound = totalBestMoves,
                byDifficulty = byDiff
            )
        }
    }
}
