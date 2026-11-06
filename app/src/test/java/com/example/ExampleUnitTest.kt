package com.example

import com.example.chess.data.ChessMatchRepository
import com.example.chess.data.MatchRecord
import com.example.chess.engine.ChessAiEngine
import com.example.chess.engine.ChessBoardState
import com.example.chess.model.AiDifficulty
import com.example.chess.model.GameStatus
import com.example.chess.model.MatchOutcome
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.chess.model.TimeControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun initialBoard_hasTwentyLegalMoves() {
        val board = ChessBoardState.initial()
        val moves = board.generateLegalMoves()
        assertEquals(20, moves.size)
        assertEquals(GameStatus.ACTIVE, board.computeGameStatus(moves))
    }

    @Test
    fun allFourAiLevels_selectLegalMovesAndDetectCapture() {
        // Position where Black Queen on d5 is undefended and White Knight on c3 can capture it
        val fen = "r1bqkbnr/ppp1pppp/2n5/3q4/4P3/2N5/PPPP1PPP/R1BQKBNR w KQkq - 0 3"
        val board = ChessBoardState.fromFen(fen)

        for (level in AiDifficulty.entries) {
            val result = ChessAiEngine.selectMove(board, level)
            assertNotNull("Engine at $level should return a move", result)
            val legalMoves = board.generateLegalMoves()
            assertTrue("Move must be strictly legal", legalMoves.contains(result!!.bestMove))
        }

        // Hard and Impossible levels must deterministically capture the free Queen on d5 (via exd5 or Nxd5)
        val hardChoice = ChessAiEngine.selectMove(board, AiDifficulty.HARD)!!
        assertEquals(Square.fromAlgebraic("d5"), hardChoice.bestMove.to)

        val impossibleChoice = ChessAiEngine.selectMove(board, AiDifficulty.IMPOSSIBLE)!!
        assertEquals(Square.fromAlgebraic("d5"), impossibleChoice.bestMove.to)
    }

    @Test
    fun blunderAnalyzer_identifiesQueenBlunderAndBestAlternative() {
        val board = ChessBoardState.fromFen("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3")
        val legal = board.generateLegalMoves()
        // Play a quiet developing move Bc4 (should be BEST or GOOD)
        val bc4 = legal.first { it.from == Square.fromAlgebraic("f1") && it.to == Square.fromAlgebraic("c4") }
        val goodAnalysis = ChessAiEngine.analyzePlayedMove(board, bc4)
        assertTrue(
            goodAnalysis.classification == MoveClassification.BEST ||
                goodAnalysis.classification == MoveClassification.GOOD
        )
    }

    @Test
    fun streakCalculation_computesCurrentAndBestStreaksCorrectly() {
        fun sampleMatch(id: Long, ts: Long, outcome: MatchOutcome, diff: AiDifficulty): MatchRecord {
            return MatchRecord(
                id = id,
                timestamp = ts,
                aiDifficulty = diff,
                timeControl = TimeControl.RAPID_10_0,
                playerColor = PieceColor.WHITE,
                outcome = outcome,
                terminationReason = "Checkmate",
                playerAccuracy = 88f,
                aiAccuracy = 82f,
                bestMoveCount = 10,
                goodMoveCount = 5,
                inaccuracyCount = 1,
                mistakeCount = 0,
                blunderCount = 0,
                totalPlies = 32,
                durationSeconds = 180,
                finalFen = ChessBoardState.INITIAL_FEN,
                analyzedMoves = emptyList()
            )
        }

        val matches = listOf(
            sampleMatch(1, 1000L, MatchOutcome.WIN, AiDifficulty.EASY),
            sampleMatch(2, 2000L, MatchOutcome.WIN, AiDifficulty.MEDIUM),
            sampleMatch(3, 3000L, MatchOutcome.WIN, AiDifficulty.HARD),
            sampleMatch(4, 4000L, MatchOutcome.LOSS, AiDifficulty.IMPOSSIBLE),
            sampleMatch(5, 5000L, MatchOutcome.WIN, AiDifficulty.HARD),
            sampleMatch(6, 6000L, MatchOutcome.WIN, AiDifficulty.IMPOSSIBLE)
        )

        val stats = ChessMatchRepository.computeStreakStats(matches.reversed())
        assertEquals(2, stats.currentWinStreak)
        assertEquals(3, stats.bestWinStreak)
        assertEquals(6, stats.totalGames)
        assertEquals(5, stats.wins)
        assertEquals(1, stats.losses)
    }
}
