package com.example.chess.engine

import com.example.chess.model.CastlingRights
import com.example.chess.model.ChessMove
import com.example.chess.model.GameStatus
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import kotlin.math.abs

class ChessBoardState(
    private val squares: Array<Array<Piece?>>,
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1,
    val lastMove: ChessMove? = null
) {
    fun pieceAt(square: Square): Piece? {
        if (!square.isValid) return null
        return squares[square.row][square.col]
    }

    fun pieceAt(row: Int, col: Int): Piece? {
        if (row !in 0..7 || col !in 0..7) return null
        return squares[row][col]
    }

    fun findKingSquare(color: PieceColor): Square? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = squares[r][c]
                if (p != null && p.color == color && p.type == PieceType.KING) {
                    return Square(r, c)
                }
            }
        }
        return null
    }

    fun isKingInCheck(color: PieceColor): Boolean {
        val kingSq = findKingSquare(color) ?: return false
        return isSquareAttacked(kingSq, color.opposite())
    }

    fun isSquareAttacked(target: Square, byColor: PieceColor): Boolean {
        // 1. Pawn attacks
        val pawnAttackRow = if (byColor == PieceColor.WHITE) target.row + 1 else target.row - 1
        for (dc in intArrayOf(-1, 1)) {
            val p = pieceAt(pawnAttackRow, target.col + dc)
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }

        // 2. Knight attacks
        val knightOffsets = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )
        for (offset in knightOffsets) {
            val p = pieceAt(target.row + offset[0], target.col + offset[1])
            if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
        }

        // 3. King attacks
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val p = pieceAt(target.row + dr, target.col + dc)
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // 4. Orthogonal sliding (Rook / Queen)
        val orthoDirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (dir in orthoDirs) {
            var r = target.row + dir[0]
            var c = target.col + dir[1]
            while (r in 0..7 && c in 0..7) {
                val p = squares[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        // 5. Diagonal sliding (Bishop / Queen)
        val diagDirs = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        for (dir in diagDirs) {
            var r = target.row + dir[0]
            var c = target.col + dir[1]
            while (r in 0..7 && c in 0..7) {
                val p = squares[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }

        return false
    }

    fun generateLegalMoves(): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves(activeColor)
        val legal = ArrayList<ChessMove>(pseudoMoves.size)
        for (move in pseudoMoves) {
            val nextState = applyMove(move)
            if (!nextState.isKingInCheck(activeColor)) {
                legal.add(move)
            }
        }
        return legal
    }

    fun generateLegalMovesForSquare(square: Square): List<ChessMove> {
        val piece = pieceAt(square) ?: return emptyList()
        if (piece.color != activeColor) return emptyList()
        return generateLegalMoves().filter { it.from == square }
    }

    private fun generatePseudoLegalMoves(color: PieceColor): List<ChessMove> {
        val moves = ArrayList<ChessMove>(48)
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = squares[r][c] ?: continue
                if (piece.color != color) continue
                val from = Square(r, c)
                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(from, piece, moves)
                    PieceType.KNIGHT -> generateKnightMoves(from, piece, moves)
                    PieceType.BISHOP -> generateSlidingMoves(
                        from, piece,
                        arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1)),
                        moves
                    )
                    PieceType.ROOK -> generateSlidingMoves(
                        from, piece,
                        arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)),
                        moves
                    )
                    PieceType.QUEEN -> generateSlidingMoves(
                        from, piece,
                        arrayOf(
                            intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1),
                            intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)
                        ),
                        moves
                    )
                    PieceType.KING -> generateKingMoves(from, piece, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Square, piece: Piece, out: MutableList<ChessMove>) {
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
        val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7
        val promotionTypes = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)

        val oneStep = Square(from.row + dir, from.col)
        if (oneStep.isValid && pieceAt(oneStep) == null) {
            if (oneStep.row == promoRow) {
                for (promo in promotionTypes) {
                    out.add(ChessMove(from, oneStep, piece, promotion = promo))
                }
            } else {
                out.add(ChessMove(from, oneStep, piece))
                val twoStep = Square(from.row + dir * 2, from.col)
                if (from.row == startRow && pieceAt(twoStep) == null) {
                    out.add(ChessMove(from, twoStep, piece))
                }
            }
        }

        for (dc in intArrayOf(-1, 1)) {
            val capSq = Square(from.row + dir, from.col + dc)
            if (!capSq.isValid) continue
            val targetPiece = pieceAt(capSq)
            if (targetPiece != null && targetPiece.color != piece.color) {
                if (capSq.row == promoRow) {
                    for (promo in promotionTypes) {
                        out.add(ChessMove(from, capSq, piece, capturedPiece = targetPiece, promotion = promo))
                    }
                } else {
                    out.add(ChessMove(from, capSq, piece, capturedPiece = targetPiece))
                }
            } else if (enPassantTarget != null && capSq == enPassantTarget) {
                val epCapturedPawn = Piece(PieceType.PAWN, piece.color.opposite())
                out.add(
                    ChessMove(
                        from = from,
                        to = capSq,
                        piece = piece,
                        capturedPiece = epCapturedPawn,
                        isEnPassant = true
                    )
                )
            }
        }
    }

    private fun generateKnightMoves(from: Square, piece: Piece, out: MutableList<ChessMove>) {
        val offsets = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )
        for (off in offsets) {
            val to = Square(from.row + off[0], from.col + off[1])
            if (!to.isValid) continue
            val target = pieceAt(to)
            if (target == null || target.color != piece.color) {
                out.add(ChessMove(from, to, piece, capturedPiece = target))
            }
        }
    }

    private fun generateSlidingMoves(
        from: Square,
        piece: Piece,
        dirs: Array<IntArray>,
        out: MutableList<ChessMove>
    ) {
        for (dir in dirs) {
            var r = from.row + dir[0]
            var c = from.col + dir[1]
            while (r in 0..7 && c in 0..7) {
                val to = Square(r, c)
                val target = squares[r][c]
                if (target == null) {
                    out.add(ChessMove(from, to, piece))
                } else {
                    if (target.color != piece.color) {
                        out.add(ChessMove(from, to, piece, capturedPiece = target))
                    }
                    break
                }
                r += dir[0]
                c += dir[1]
            }
        }
    }

    private fun generateKingMoves(from: Square, piece: Piece, out: MutableList<ChessMove>) {
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val to = Square(from.row + dr, from.col + dc)
                if (!to.isValid) continue
                val target = pieceAt(to)
                if (target == null || target.color != piece.color) {
                    out.add(ChessMove(from, to, piece, capturedPiece = target))
                }
            }
        }

        // Castling
        val enemy = piece.color.opposite()
        val homeRow = if (piece.color == PieceColor.WHITE) 7 else 0
        if (from.row == homeRow && from.col == 4 && !isSquareAttacked(from, enemy)) {
            val canKingside = if (piece.color == PieceColor.WHITE) {
                castlingRights.whiteKingside
            } else {
                castlingRights.blackKingside
            }
            if (canKingside &&
                pieceAt(homeRow, 5) == null &&
                pieceAt(homeRow, 6) == null &&
                pieceAt(homeRow, 7) == Piece(PieceType.ROOK, piece.color) &&
                !isSquareAttacked(Square(homeRow, 5), enemy) &&
                !isSquareAttacked(Square(homeRow, 6), enemy)
            ) {
                out.add(
                    ChessMove(
                        from = from,
                        to = Square(homeRow, 6),
                        piece = piece,
                        isCastlingKingside = true
                    )
                )
            }

            val canQueenside = if (piece.color == PieceColor.WHITE) {
                castlingRights.whiteQueenside
            } else {
                castlingRights.blackQueenside
            }
            if (canQueenside &&
                pieceAt(homeRow, 3) == null &&
                pieceAt(homeRow, 2) == null &&
                pieceAt(homeRow, 1) == null &&
                pieceAt(homeRow, 0) == Piece(PieceType.ROOK, piece.color) &&
                !isSquareAttacked(Square(homeRow, 3), enemy) &&
                !isSquareAttacked(Square(homeRow, 2), enemy)
            ) {
                out.add(
                    ChessMove(
                        from = from,
                        to = Square(homeRow, 2),
                        piece = piece,
                        isCastlingQueenside = true
                    )
                )
            }
        }
    }

    fun applyMove(move: ChessMove): ChessBoardState {
        val nextSquares = Array(8) { r -> Array(8) { c -> squares[r][c] } }
        nextSquares[move.from.row][move.from.col] = null

        if (move.isEnPassant) {
            val capturedPawnRow = move.from.row
            nextSquares[capturedPawnRow][move.to.col] = null
        }

        if (move.isCastlingKingside) {
            val row = move.from.row
            val rook = nextSquares[row][7]
            nextSquares[row][7] = null
            nextSquares[row][5] = rook
        } else if (move.isCastlingQueenside) {
            val row = move.from.row
            val rook = nextSquares[row][0]
            nextSquares[row][0] = null
            nextSquares[row][3] = rook
        }

        val placedPiece = if (move.promotion != null) {
            Piece(move.promotion, move.piece.color)
        } else {
            move.piece
        }
        nextSquares[move.to.row][move.to.col] = placedPiece

        // Update castling rights
        var wK = castlingRights.whiteKingside
        var wQ = castlingRights.whiteQueenside
        var bK = castlingRights.blackKingside
        var bQ = castlingRights.blackQueenside

        if (move.piece.type == PieceType.KING) {
            if (move.piece.color == PieceColor.WHITE) {
                wK = false
                wQ = false
            } else {
                bK = false
                bQ = false
            }
        }
        if (move.from == Square(7, 7) || move.to == Square(7, 7)) wK = false
        if (move.from == Square(7, 0) || move.to == Square(7, 0)) wQ = false
        if (move.from == Square(0, 7) || move.to == Square(0, 7)) bK = false
        if (move.from == Square(0, 0) || move.to == Square(0, 0)) bQ = false

        val nextEp = if (move.piece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            Square((move.from.row + move.to.row) / 2, move.from.col)
        } else {
            null
        }

        val nextHalfMove = if (move.piece.type == PieceType.PAWN || move.capturedPiece != null) {
            0
        } else {
            halfMoveClock + 1
        }

        val nextFullMove = if (activeColor == PieceColor.BLACK) fullMoveNumber + 1 else fullMoveNumber

        return ChessBoardState(
            squares = nextSquares,
            activeColor = activeColor.opposite(),
            castlingRights = CastlingRights(wK, wQ, bK, bQ),
            enPassantTarget = nextEp,
            halfMoveClock = nextHalfMove,
            fullMoveNumber = nextFullMove,
            lastMove = move
        )
    }

    fun toSan(move: ChessMove, legalMoves: List<ChessMove> = generateLegalMoves()): String {
        val nextState = applyMove(move)
        val nextLegal = nextState.generateLegalMoves()
        val inCheck = nextState.isKingInCheck(nextState.activeColor)
        val checkSuffix = when {
            inCheck && nextLegal.isEmpty() -> "#"
            inCheck -> "+"
            else -> ""
        }

        if (move.isCastlingKingside) return "O-O$checkSuffix"
        if (move.isCastlingQueenside) return "O-O-O$checkSuffix"

        val sb = StringBuilder()
        if (move.piece.type == PieceType.PAWN) {
            if (move.capturedPiece != null) {
                sb.append('a' + move.from.col)
                sb.append('x')
            }
            sb.append(move.to.toAlgebraic())
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.sanLetter)
            }
        } else {
            sb.append(move.piece.type.sanLetter)
            val ambiguous = legalMoves.filter {
                it != move &&
                    it.piece.type == move.piece.type &&
                    it.to == move.to
            }
            if (ambiguous.isNotEmpty()) {
                val sameFile = ambiguous.any { it.from.col == move.from.col }
                val sameRank = ambiguous.any { it.from.row == move.from.row }
                when {
                    !sameFile -> sb.append('a' + move.from.col)
                    !sameRank -> sb.append(8 - move.from.row)
                    else -> sb.append(move.from.toAlgebraic())
                }
            }
            if (move.capturedPiece != null) {
                sb.append('x')
            }
            sb.append(move.to.toAlgebraic())
        }
        sb.append(checkSuffix)
        return sb.toString()
    }

    fun computeGameStatus(legalMoves: List<ChessMove> = generateLegalMoves()): GameStatus {
        if (legalMoves.isEmpty()) {
            return if (isKingInCheck(activeColor)) {
                if (activeColor == PieceColor.WHITE) {
                    GameStatus.CHECKMATE_BLACK_WINS
                } else {
                    GameStatus.CHECKMATE_WHITE_WINS
                }
            } else {
                GameStatus.STALEMATE
            }
        }
        if (halfMoveClock >= 100) {
            return GameStatus.DRAW_FIFTY_MOVE
        }
        if (hasInsufficientMaterial()) {
            return GameStatus.DRAW_INSUFFICIENT_MATERIAL
        }
        return GameStatus.ACTIVE
    }

    private fun hasInsufficientMaterial(): Boolean {
        val pieces = ArrayList<Piece>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = squares[r][c] ?: continue
                if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                    return false
                }
                if (p.type != PieceType.KING) {
                    pieces.add(p)
                }
            }
        }
        // K vs K, or K+B vs K, or K+N vs K
        return pieces.size <= 1
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val p = squares[r][c]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p.fenChar())
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (r < 7) sb.append('/')
        }
        val activeChar = if (activeColor == PieceColor.WHITE) "w" else "b"
        val epStr = enPassantTarget?.toAlgebraic() ?: "-"
        sb.append(" $activeChar ${castlingRights.toFen()} $epStr $halfMoveClock $fullMoveNumber")
        return sb.toString()
    }

    fun computeMaterialSummary(): MaterialSummary {
        val initialCounts = mapOf(
            PieceType.QUEEN to 1,
            PieceType.ROOK to 2,
            PieceType.BISHOP to 2,
            PieceType.KNIGHT to 2,
            PieceType.PAWN to 8
        )
        val whiteRemaining = mutableMapOf<PieceType, Int>()
        val blackRemaining = mutableMapOf<PieceType, Int>()
        var whiteScore = 0
        var blackScore = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val p = squares[r][c] ?: continue
                if (p.type == PieceType.KING) continue
                val pts = when (p.type) {
                    PieceType.PAWN -> 1
                    PieceType.KNIGHT, PieceType.BISHOP -> 3
                    PieceType.ROOK -> 5
                    PieceType.QUEEN -> 9
                    PieceType.KING -> 0
                }
                if (p.color == PieceColor.WHITE) {
                    whiteRemaining[p.type] = (whiteRemaining[p.type] ?: 0) + 1
                    whiteScore += pts
                } else {
                    blackRemaining[p.type] = (blackRemaining[p.type] ?: 0) + 1
                    blackScore += pts
                }
            }
        }

        val capturedByWhite = mutableListOf<PieceType>() // Black pieces captured by White
        val capturedByBlack = mutableListOf<PieceType>() // White pieces captured by Black

        for ((type, startCount) in initialCounts) {
            val missingBlack = (startCount - (blackRemaining[type] ?: 0)).coerceAtLeast(0)
            repeat(missingBlack) { capturedByWhite.add(type) }

            val missingWhite = (startCount - (whiteRemaining[type] ?: 0)).coerceAtLeast(0)
            repeat(missingWhite) { capturedByBlack.add(type) }
        }

        return MaterialSummary(
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack,
            whiteMaterialAdvantage = whiteScore - blackScore
        )
    }

    companion object {
        const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun initial(): ChessBoardState = fromFen(INITIAL_FEN)

        fun fromFen(fen: String): ChessBoardState {
            val parts = fen.trim().split(Regex("\\s+"))
            val boardPart = parts.getOrNull(0) ?: INITIAL_FEN.substringBefore(' ')
            val squares = Array(8) { arrayOfNulls<Piece>(8) }
            val ranks = boardPart.split('/')
            for (r in 0 until minOf(8, ranks.size)) {
                var c = 0
                for (ch in ranks[r]) {
                    if (c >= 8) break
                    if (ch.isDigit()) {
                        c += ch.digitToInt()
                    } else {
                        squares[r][c] = Piece.fromFenChar(ch)
                        c++
                    }
                }
            }

            val activeColor = if (parts.getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE
            val castlingStr = parts.getOrNull(2) ?: "KQkq"
            val castlingRights = CastlingRights(
                whiteKingside = castlingStr.contains('K'),
                whiteQueenside = castlingStr.contains('Q'),
                blackKingside = castlingStr.contains('k'),
                blackQueenside = castlingStr.contains('q')
            )
            val epSquare = parts.getOrNull(3)?.let {
                if (it == "-") null else Square.fromAlgebraic(it)
            }
            val halfMove = parts.getOrNull(4)?.toIntOrNull() ?: 0
            val fullMove = parts.getOrNull(5)?.toIntOrNull() ?: 1

            return ChessBoardState(
                squares = squares,
                activeColor = activeColor,
                castlingRights = castlingRights,
                enPassantTarget = epSquare,
                halfMoveClock = halfMove,
                fullMoveNumber = fullMove
            )
        }
    }
}

data class MaterialSummary(
    val capturedByWhite: List<PieceType>,
    val capturedByBlack: List<PieceType>,
    val whiteMaterialAdvantage: Int
)
