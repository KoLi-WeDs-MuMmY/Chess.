package com.example.chess.engine

import com.example.chess.model.AiDifficulty
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.ChessMove
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class EngineSearchResult(
    val bestMove: ChessMove,
    val evalWhiteCp: Int,
    val nodesSearched: Int
)

data class MoveAnalysisResult(
    val bestMove: ChessMove,
    val bestMoveSan: String,
    val evalBeforeWhiteCp: Int,
    val evalAfterWhiteCp: Int,
    val cpLossForMover: Int,
    val classification: MoveClassification
)

object ChessAiEngine {

    private const val MATE_SCORE = 29000
    private const val INF = 32000

    // Piece-Square Tables from White's perspective (row 0 = rank 8, row 7 = rank 1)
    private val PAWN_PST = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        12, 12, 22, 32, 32, 22, 12, 12,
        6,  6, 12, 27, 27, 12,  6,  6,
        0,  0,  4, 22, 22,  4,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-22,-22, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_PST = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 12, 16, 16, 12,  0,-30,
        -30,  6, 16, 22, 22, 16,  6,-30,
        -30,  0, 16, 22, 22, 16,  0,-30,
        -30,  6, 12, 16, 16, 12,  6,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_PST = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  8, 12, 12,  8,  0,-10,
        -10,  6,  6, 12, 12,  6,  6,-10,
        -10,  0, 12, 12, 12, 12,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  6,  0,  0,  0,  0,  6,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_PST = intArrayOf(
        0,  0,  0,  4,  4,  0,  0,  0,
        6, 12, 12, 12, 12, 12, 12,  6,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  2,  8,  8,  4,  0,  0
    )

    private val QUEEN_PST = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  6,  6,  6,  6,  0,-10,
        -5,  0,  6,  8,  8,  6,  0, -5,
        0,  0,  6,  8,  8,  6,  0, -5,
        -10,  6,  6,  6,  6,  6,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_MIDDLEGAME_PST = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 32, 12,  0,  0, 12, 32, 20
    )

    private val KING_ENDGAME_PST = intArrayOf(
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    )

    /**
     * Returns static + positional evaluation from White's perspective in centipawns.
     * Positive = White advantage, Negative = Black advantage.
     */
    fun evaluatePositionWhiteCp(state: ChessBoardState): Int {
        var whiteScore = 0
        var blackScore = 0
        var whiteBishops = 0
        var blackBishops = 0
        var nonPawnMaterial = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.pieceAt(r, c) ?: continue
                if (p.type != PieceType.PAWN && p.type != PieceType.KING) {
                    nonPawnMaterial += p.type.valueCp
                }
            }
        }
        val isEndgame = nonPawnMaterial <= 2600

        for (r in 0..7) {
            for (c in 0..7) {
                val p = state.pieceAt(r, c) ?: continue
                val tableIndex = if (p.color == PieceColor.WHITE) {
                    r * 8 + c
                } else {
                    (7 - r) * 8 + c
                }
                val pstBonus = when (p.type) {
                    PieceType.PAWN -> PAWN_PST[tableIndex]
                    PieceType.KNIGHT -> KNIGHT_PST[tableIndex]
                    PieceType.BISHOP -> {
                        if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                        BISHOP_PST[tableIndex]
                    }
                    PieceType.ROOK -> ROOK_PST[tableIndex]
                    PieceType.QUEEN -> QUEEN_PST[tableIndex]
                    PieceType.KING -> if (isEndgame) KING_ENDGAME_PST[tableIndex] else KING_MIDDLEGAME_PST[tableIndex]
                }

                // Passed pawn bonus
                var passedPawnBonus = 0
                if (p.type == PieceType.PAWN) {
                    if (isPassedPawn(state, r, c, p.color)) {
                        val advancement = if (p.color == PieceColor.WHITE) (6 - r) else (r - 1)
                        passedPawnBonus = advancement * 14
                    }
                }

                val totalPieceVal = p.type.valueCp + pstBonus + passedPawnBonus
                if (p.color == PieceColor.WHITE) {
                    whiteScore += totalPieceVal
                } else {
                    blackScore += totalPieceVal
                }
            }
        }

        if (whiteBishops >= 2) whiteScore += 35
        if (blackBishops >= 2) blackScore += 35

        if (state.isKingInCheck(PieceColor.WHITE)) whiteScore -= 25
        if (state.isKingInCheck(PieceColor.BLACK)) blackScore -= 25

        return whiteScore - blackScore
    }

    private fun isPassedPawn(state: ChessBoardState, row: Int, col: Int, color: PieceColor): Boolean {
        val enemy = color.opposite()
        val rowRange = if (color == PieceColor.WHITE) 0 until row else (row + 1)..7
        for (r in rowRange) {
            for (c in max(0, col - 1)..min(7, col + 1)) {
                val p = state.pieceAt(r, c)
                if (p != null && p.color == enemy && p.type == PieceType.PAWN) {
                    return false
                }
            }
        }
        return true
    }

    fun selectMove(
        state: ChessBoardState,
        difficulty: AiDifficulty,
        random: Random = Random.Default
    ): EngineSearchResult? {
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) return null
        if (legalMoves.size == 1) {
            val next = state.applyMove(legalMoves[0])
            return EngineSearchResult(legalMoves[0], evaluatePositionWhiteCp(next), 1)
        }

        val orderedMoves = orderMoves(legalMoves)
        val maximizingWhite = state.activeColor == PieceColor.WHITE
        var nodes = 0

        data class ScoredCandidate(val move: ChessMove, val rawWhiteCp: Int, val noisyMoverScore: Int)

        val candidates = ArrayList<ScoredCandidate>(orderedMoves.size)
        val useQuiescence = difficulty == AiDifficulty.HARD || difficulty == AiDifficulty.IMPOSSIBLE
        val transpositionTable = if (difficulty == AiDifficulty.IMPOSSIBLE) HashMap<String, Int>(2048) else null

        for (move in orderedMoves) {
            val childState = state.applyMove(move)
            val evalWhite = alphaBetaWhite(
                state = childState,
                depth = difficulty.searchDepth - 1,
                alphaInit = -INF,
                betaInit = INF,
                maximizingWhite = childState.activeColor == PieceColor.WHITE,
                useQuiescence = useQuiescence,
                tt = transpositionTable,
                onNode = { nodes++ }
            )
            val moverPerspective = if (maximizingWhite) evalWhite else -evalWhite
            val jitter = if (difficulty.noiseCp > 0) {
                random.nextInt(-difficulty.noiseCp, difficulty.noiseCp + 1)
            } else {
                0
            }
            candidates.add(
                ScoredCandidate(
                    move = move,
                    rawWhiteCp = evalWhite,
                    noisyMoverScore = moverPerspective + jitter
                )
            )
        }

        candidates.sortByDescending { it.noisyMoverScore }
        val chosen = if (difficulty == AiDifficulty.EASY && candidates.size >= 3 && random.nextFloat() < 0.35f) {
            candidates[1]
        } else {
            candidates.first()
        }

        return EngineSearchResult(
            bestMove = chosen.move,
            evalWhiteCp = chosen.rawWhiteCp,
            nodesSearched = max(1, nodes)
        )
    }

    /**
     * Evaluates a played move against the engine's best tactical move at that position.
     */
    fun analyzePlayedMove(
        stateBefore: ChessBoardState,
        playedMove: ChessMove
    ): MoveAnalysisResult {
        val legalMoves = stateBefore.generateLegalMoves()
        val mover = stateBefore.activeColor

        // Fast 2-ply tactical search across legal moves to find true best move and evalBefore
        val ordered = orderMoves(legalMoves)
        var bestMove = playedMove
        var bestEvalWhite = if (mover == PieceColor.WHITE) -INF else INF
        var playedEvalWhite: Int? = null

        for (candidate in ordered) {
            val nextState = stateBefore.applyMove(candidate)
            val scoreWhite = alphaBetaWhite(
                state = nextState,
                depth = 1,
                alphaInit = -INF,
                betaInit = INF,
                maximizingWhite = nextState.activeColor == PieceColor.WHITE,
                useQuiescence = true,
                tt = null,
                onNode = {}
            )
            if (candidate.from == playedMove.from &&
                candidate.to == playedMove.to &&
                candidate.promotion == playedMove.promotion
            ) {
                playedEvalWhite = scoreWhite
            }

            if (mover == PieceColor.WHITE) {
                if (scoreWhite > bestEvalWhite) {
                    bestEvalWhite = scoreWhite
                    bestMove = candidate
                }
            } else {
                if (scoreWhite < bestEvalWhite) {
                    bestEvalWhite = scoreWhite
                    bestMove = candidate
                }
            }
        }

        val finalPlayedEvalWhite = playedEvalWhite ?: evaluatePositionWhiteCp(stateBefore.applyMove(playedMove))
        val bestMoverScore = if (mover == PieceColor.WHITE) bestEvalWhite else -bestEvalWhite
        val playedMoverScore = if (mover == PieceColor.WHITE) finalPlayedEvalWhite else -finalPlayedEvalWhite

        // Clamp mate scores when calculating centipawn loss so accuracy remains well-scaled
        val clampedBest = bestMoverScore.coerceIn(-1200, 1200)
        val clampedPlayed = playedMoverScore.coerceIn(-1200, 1200)
        val cpLoss = max(0, clampedBest - clampedPlayed)

        val isExactBest = playedMove.from == bestMove.from &&
            playedMove.to == bestMove.to &&
            playedMove.promotion == bestMove.promotion

        val classification = when {
            isExactBest || cpLoss <= 18 -> MoveClassification.BEST
            cpLoss <= 55 -> MoveClassification.GOOD
            cpLoss <= 115 -> MoveClassification.INACCURACY
            cpLoss <= 230 -> MoveClassification.MISTAKE
            else -> MoveClassification.BLUNDER
        }

        val bestSan = stateBefore.toSan(bestMove, legalMoves)

        return MoveAnalysisResult(
            bestMove = bestMove,
            bestMoveSan = bestSan,
            evalBeforeWhiteCp = bestEvalWhite,
            evalAfterWhiteCp = finalPlayedEvalWhite,
            cpLossForMover = cpLoss,
            classification = classification
        )
    }

    fun calculateSideAccuracy(moves: List<AnalyzedMove>, color: PieceColor): Float {
        val sideMoves = moves.filter { it.color == color }
        if (sideMoves.isEmpty()) return 100f
        var totalMoveAccuracy = 0.0
        for (m in sideMoves) {
            // Standard exponential decay based on centipawn loss
            val acc = 100.0 * exp(-0.0038 * m.cpLossForMover.toDouble())
            totalMoveAccuracy += acc.coerceIn(0.0, 100.0)
        }
        return (totalMoveAccuracy / sideMoves.size).toFloat().coerceIn(0f, 100f)
    }

    private fun alphaBetaWhite(
        state: ChessBoardState,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        maximizingWhite: Boolean,
        useQuiescence: Boolean,
        tt: MutableMap<String, Int>?,
        onNode: () -> Unit
    ): Int {
        onNode()
        val legalMoves = state.generateLegalMoves()
        if (legalMoves.isEmpty()) {
            return if (state.isKingInCheck(state.activeColor)) {
                if (state.activeColor == PieceColor.WHITE) {
                    -MATE_SCORE - depth * 10
                } else {
                    MATE_SCORE + depth * 10
                }
            } else {
                0 // Stalemate
            }
        }
        if (state.halfMoveClock >= 100) return 0

        if (depth <= 0) {
            return if (useQuiescence) {
                quiescenceWhite(state, alphaInit, betaInit, maximizingWhite, 2, onNode)
            } else {
                evaluatePositionWhiteCp(state)
            }
        }

        val ttKey = if (tt != null && depth >= 2) "${state.toFen()}:$depth" else null
        if (ttKey != null) {
            tt?.get(ttKey)?.let { return it }
        }

        var alpha = alphaInit
        var beta = betaInit
        val ordered = orderMoves(legalMoves)

        val result = if (maximizingWhite) {
            var maxEval = -INF
            for (move in ordered) {
                val eval = alphaBetaWhite(
                    state = state.applyMove(move),
                    depth = depth - 1,
                    alphaInit = alpha,
                    betaInit = beta,
                    maximizingWhite = false,
                    useQuiescence = useQuiescence,
                    tt = tt,
                    onNode = onNode
                )
                if (eval > maxEval) maxEval = eval
                if (eval > alpha) alpha = eval
                if (beta <= alpha) break
            }
            maxEval
        } else {
            var minEval = INF
            for (move in ordered) {
                val eval = alphaBetaWhite(
                    state = state.applyMove(move),
                    depth = depth - 1,
                    alphaInit = alpha,
                    betaInit = beta,
                    maximizingWhite = true,
                    useQuiescence = useQuiescence,
                    tt = tt,
                    onNode = onNode
                )
                if (eval < minEval) minEval = eval
                if (eval < beta) beta = eval
                if (beta <= alpha) break
            }
            minEval
        }

        if (ttKey != null && tt != null && tt.size < 8192) {
            tt[ttKey] = result
        }
        return result
    }

    private fun quiescenceWhite(
        state: ChessBoardState,
        alphaInit: Int,
        betaInit: Int,
        maximizingWhite: Boolean,
        qDepth: Int,
        onNode: () -> Unit
    ): Int {
        onNode()
        val standPat = evaluatePositionWhiteCp(state)
        if (qDepth <= 0) return standPat

        var alpha = alphaInit
        var beta = betaInit

        if (maximizingWhite) {
            if (standPat >= beta) return beta
            if (standPat > alpha) alpha = standPat
        } else {
            if (standPat <= alpha) return alpha
            if (standPat < beta) beta = standPat
        }

        val tacticalMoves = state.generateLegalMoves()
            .filter { it.capturedPiece != null || it.promotion != null }
        if (tacticalMoves.isEmpty()) return standPat

        val ordered = orderMoves(tacticalMoves)
        if (maximizingWhite) {
            var best = standPat
            for (move in ordered) {
                val score = quiescenceWhite(
                    state = state.applyMove(move),
                    alphaInit = alpha,
                    betaInit = beta,
                    maximizingWhite = false,
                    qDepth = qDepth - 1,
                    onNode = onNode
                )
                if (score > best) best = score
                if (score > alpha) alpha = score
                if (beta <= alpha) break
            }
            return best
        } else {
            var best = standPat
            for (move in ordered) {
                val score = quiescenceWhite(
                    state = state.applyMove(move),
                    alphaInit = alpha,
                    betaInit = beta,
                    maximizingWhite = true,
                    qDepth = qDepth - 1,
                    onNode = onNode
                )
                if (score < best) best = score
                if (score < beta) beta = score
                if (beta <= alpha) break
            }
            return best
        }
    }

    private fun orderMoves(moves: List<ChessMove>): List<ChessMove> {
        return moves.sortedByDescending { move ->
            var score = 0
            if (move.capturedPiece != null) {
                // MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
                score += 10 * move.capturedPiece.type.valueCp - (move.piece.type.valueCp / 10)
            }
            if (move.promotion != null) {
                score += move.promotion.valueCp
            }
            if (move.isCastlingKingside || move.isCastlingQueenside) {
                score += 90
            }
            // Center control preference
            if (move.to.row in 2..5 && move.to.col in 2..5) {
                score += 15
            }
            score
        }
    }

    fun formatEvaluationText(evalWhiteCp: Int): String {
        if (abs(evalWhiteCp) >= MATE_SCORE - 500) {
            val sign = if (evalWhiteCp > 0) "+" else "-"
            return "${sign}M"
        }
        val pawns = evalWhiteCp / 100.0
        return if (pawns >= 0) {
            String.format("+%.1f", pawns)
        } else {
            String.format("%.1f", pawns)
        }
    }
}
