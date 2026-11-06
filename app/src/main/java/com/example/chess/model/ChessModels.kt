package com.example.chess.model

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val valueCp: Int, val sanLetter: String, val displayName: String) {
    PAWN(100, "", "Pawn"),
    KNIGHT(320, "N", "Knight"),
    BISHOP(330, "B", "Bishop"),
    ROOK(500, "R", "Rook"),
    QUEEN(900, "Q", "Queen"),
    KING(20000, "K", "King")
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    fun fenChar(): Char {
        val base = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) base.uppercaseChar() else base
    }

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }
    }
}

/**
 * row: 0 is rank 8 (top for White), 7 is rank 1 (bottom for White)
 * col: 0 is file 'a', 7 is file 'h'
 */
data class Square(val row: Int, val col: Int) {
    val isValid: Boolean get() = row in 0..7 && col in 0..7

    val isLightSquare: Boolean get() = (row + col) % 2 == 0

    fun toAlgebraic(): String {
        val file = ('a' + col)
        val rank = (8 - row)
        return "$file$rank"
    }

    companion object {
        fun fromAlgebraic(algebraic: String): Square? {
            if (algebraic.length < 2) return null
            val col = algebraic[0].lowercaseChar() - 'a'
            val rank = algebraic[1].digitToIntOrNull() ?: return null
            val row = 8 - rank
            val sq = Square(row, col)
            return if (sq.isValid) sq else null
        }
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastlingKingside: Boolean = false,
    val isCastlingQueenside: Boolean = false
) {
    fun toUci(): String {
        val promoSuffix = promotion?.let {
            when (it) {
                PieceType.QUEEN -> "q"
                PieceType.ROOK -> "r"
                PieceType.BISHOP -> "b"
                PieceType.KNIGHT -> "n"
                else -> ""
            }
        } ?: ""
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promoSuffix"
    }
}

enum class AiDifficulty(
    val displayName: String,
    val eloBadge: String,
    val tagline: String,
    val description: String,
    val searchDepth: Int,
    val noiseCp: Int,
    val allowHints: Boolean
) {
    EASY(
        displayName = "Easy",
        eloBadge = "800 ELO",
        tagline = "Casual Club Novice",
        description = "Shallow lookahead with natural positional inaccuracies. Great for building confidence and practicing openings.",
        searchDepth = 1,
        noiseCp = 175,
        allowHints = true
    ),
    MEDIUM(
        displayName = "Medium",
        eloBadge = "1350 ELO",
        tagline = "Tactical Contender",
        description = "Punishes hanging pieces and basic forks with 2-ply tactical calculation and piece-square awareness.",
        searchDepth = 2,
        noiseCp = 45,
        allowHints = true
    ),
    HARD(
        displayName = "Hard",
        eloBadge = "1850 ELO",
        tagline = "Tournament Candidate",
        description = "Alpha-beta pruning with quiescence capture extension, king safety evaluation, and disciplined endgame play.",
        searchDepth = 3,
        noiseCp = 10,
        allowHints = true
    ),
    IMPOSSIBLE(
        displayName = "Impossible",
        eloBadge = "2450+ ELO",
        tagline = "Grandmaster Slate Engine",
        description = "Maximum-depth iterative search with transposition caching, killer move ordering, and zero tactical mercy.",
        searchDepth = 4,
        noiseCp = 0,
        allowHints = false
    )
}

enum class TimeControl(
    val label: String,
    val category: String,
    val initialTimeMs: Long,
    val incrementMs: Long
) {
    UNTIMED("Untimed", "Casual", 0L, 0L),
    BLITZ_3_2("3 + 2", "Blitz", 3 * 60_000L, 2_000L),
    BLITZ_5_0("5 + 0", "Blitz", 5 * 60_000L, 0L),
    RAPID_10_0("10 + 0", "Rapid", 10 * 60_000L, 0L),
    RAPID_15_10("15 + 10", "Rapid", 15 * 60_000L, 10_000L);

    val isTimed: Boolean get() = initialTimeMs > 0L
}

enum class PlayerSideChoice(val label: String) {
    WHITE("White"),
    RANDOM("Random"),
    BLACK("Black")
}

enum class MoveClassification(
    val label: String,
    val badgeSymbol: String,
    val coachSummary: String
) {
    BEST("Best Move", "★", "Engine's top choice maintaining maximum positional & tactical edge."),
    GOOD("Solid Move", "✓", "Accurate continuation keeping the evaluation steady."),
    INACCURACY("Inaccuracy", "?!", "Slight concession allowing the opponent improved activity."),
    MISTAKE("Mistake", "?", "Significant loss of evaluation overlooking a stronger tactical line."),
    BLUNDER("Blunder", "??", "Critical error swinging the evaluation or dropping material.")
}

data class AnalyzedMove(
    val ply: Int,
    val moveNumber: Int,
    val color: PieceColor,
    val san: String,
    val uciPlayed: String,
    val uciBestMove: String,
    val bestMoveSan: String,
    val evalBeforeWhiteCp: Int,
    val evalAfterWhiteCp: Int,
    val cpLossForMover: Int,
    val classification: MoveClassification,
    val fenBefore: String,
    val fenAfter: String,
    val capturedPieceName: String? = null
)

enum class GameStatus {
    ACTIVE,
    CHECKMATE_WHITE_WINS,
    CHECKMATE_BLACK_WINS,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_INSUFFICIENT_MATERIAL,
    TIMEOUT_WHITE_WINS,
    TIMEOUT_BLACK_WINS,
    RESIGNED_WHITE_WINS,
    RESIGNED_BLACK_WINS,
    DRAW_AGREED;

    val isFinished: Boolean get() = this != ACTIVE

    fun winnerColor(): PieceColor? = when (this) {
        CHECKMATE_WHITE_WINS, TIMEOUT_WHITE_WINS, RESIGNED_WHITE_WINS -> PieceColor.WHITE
        CHECKMATE_BLACK_WINS, TIMEOUT_BLACK_WINS, RESIGNED_BLACK_WINS -> PieceColor.BLACK
        else -> null
    }

    fun terminationTitle(): String = when (this) {
        ACTIVE -> "In Progress"
        CHECKMATE_WHITE_WINS, CHECKMATE_BLACK_WINS -> "Checkmate"
        STALEMATE -> "Stalemate"
        DRAW_FIFTY_MOVE -> "50-Move Rule Draw"
        DRAW_INSUFFICIENT_MATERIAL -> "Insufficient Material"
        TIMEOUT_WHITE_WINS, TIMEOUT_BLACK_WINS -> "Time Out"
        RESIGNED_WHITE_WINS, RESIGNED_BLACK_WINS -> "Resignation"
        DRAW_AGREED -> "Draw by Agreement"
    }
}

enum class MatchOutcome(val label: String) {
    WIN("Victory"),
    LOSS("Defeat"),
    DRAW("Draw")
}
