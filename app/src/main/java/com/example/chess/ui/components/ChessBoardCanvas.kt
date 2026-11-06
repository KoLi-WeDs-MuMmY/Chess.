package com.example.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.ChessBoardState
import com.example.chess.model.ChessMove
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.ui.theme.BoardDarkSquare
import com.example.ui.theme.BoardFrameSlate
import com.example.ui.theme.BoardKingCheckAlert
import com.example.ui.theme.BoardLastMoveDark
import com.example.ui.theme.BoardLastMoveLight
import com.example.ui.theme.BoardLightSquare
import com.example.ui.theme.BoardSelectedSquare
import com.example.ui.theme.GrandmasterGoldBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PieceBlackFill
import com.example.ui.theme.PieceBlackInner
import com.example.ui.theme.PieceBlackStroke
import com.example.ui.theme.PieceWhiteAccent
import com.example.ui.theme.PieceWhiteFill
import com.example.ui.theme.PieceWhiteStroke
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TournamentEmerald
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class BoardArrow(
    val from: Square,
    val to: Square,
    val color: Color,
    val isDashedOrSecondary: Boolean = false
)

@Composable
fun ChessBoardCanvas(
    boardState: ChessBoardState,
    selectedSquare: Square?,
    legalMovesForSelected: List<ChessMove>,
    isFlipped: Boolean,
    arrows: List<BoardArrow> = emptyList(),
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(BoardFrameSlate)
            .border(1.5.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(6.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag("chess_board_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped, boardState, selectedSquare) {
                    detectTapGestures { offset ->
                        val sqSize = size.width / 8f
                        if (sqSize <= 0f) return@detectTapGestures
                        val visualCol = (offset.x / sqSize).toInt().coerceIn(0, 7)
                        val visualRow = (offset.y / sqSize).toInt().coerceIn(0, 7)
                        val boardRow = if (isFlipped) 7 - visualRow else visualRow
                        val boardCol = if (isFlipped) 7 - visualCol else visualCol
                        onSquareTap(Square(boardRow, boardCol))
                    }
                }
        ) {
            val sqSize = size.width / 8f
            val lastMove = boardState.lastMove
            val whiteKingInCheck = boardState.isKingInCheck(PieceColor.WHITE)
            val blackKingInCheck = boardState.isKingInCheck(PieceColor.BLACK)
            val checkedKingSquare = when {
                whiteKingInCheck -> boardState.findKingSquare(PieceColor.WHITE)
                blackKingInCheck -> boardState.findKingSquare(PieceColor.BLACK)
                else -> null
            }

            // 1. Draw 64 squares & highlights
            for (vRow in 0..7) {
                for (vCol in 0..7) {
                    val bRow = if (isFlipped) 7 - vRow else vRow
                    val bCol = if (isFlipped) 7 - vCol else vCol
                    val sq = Square(bRow, bCol)
                    val topLeft = Offset(vCol * sqSize, vRow * sqSize)

                    val baseColor = if (sq.isLightSquare) BoardLightSquare else BoardDarkSquare
                    drawRect(
                        color = baseColor,
                        topLeft = topLeft,
                        size = Size(sqSize, sqSize)
                    )

                    if (lastMove != null && (lastMove.from == sq || lastMove.to == sq)) {
                        drawRect(
                            color = if (sq.isLightSquare) BoardLastMoveLight else BoardLastMoveDark,
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    if (selectedSquare == sq) {
                        drawRect(
                            color = BoardSelectedSquare,
                            topLeft = topLeft,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    if (checkedKingSquare == sq) {
                        val center = Offset(topLeft.x + sqSize / 2f, topLeft.y + sqSize / 2f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    BoardKingCheckAlert.copy(alpha = 0.9f),
                                    BoardKingCheckAlert.copy(alpha = 0.25f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = sqSize * 0.65f
                            ),
                            radius = sqSize * 0.65f,
                            center = center
                        )
                    }

                    // Coordinate labels on outer ranks/files
                    val labelColor = if (sq.isLightSquare) BoardDarkSquare else BoardLightSquare
                    if (vCol == 0) {
                        val rankText = (8 - bRow).toString()
                        val layout = textMeasurer.measure(
                            text = rankText,
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelColor.copy(alpha = 0.85f)
                            )
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(topLeft.x + 3.dp.toPx(), topLeft.y + 2.dp.toPx())
                        )
                    }
                    if (vRow == 7) {
                        val fileText = ('a' + bCol).toString()
                        val layout = textMeasurer.measure(
                            text = fileText,
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = labelColor.copy(alpha = 0.85f)
                            )
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                topLeft.x + sqSize - layout.size.width - 3.dp.toPx(),
                                topLeft.y + sqSize - layout.size.height - 1.dp.toPx()
                            )
                        )
                    }
                }
            }

            // 2. Draw pieces on all 64 squares
            for (vRow in 0..7) {
                for (vCol in 0..7) {
                    val bRow = if (isFlipped) 7 - vRow else vRow
                    val bCol = if (isFlipped) 7 - vCol else vCol
                    val piece = boardState.pieceAt(bRow, bCol) ?: continue
                    val center = Offset((vCol + 0.5f) * sqSize, (vRow + 0.5f) * sqSize)
                    drawChessPiece(piece = piece, center = center, sqSize = sqSize)
                }
            }

            // 3. Draw legal move target indicators
            for (move in legalMovesForSelected) {
                val vRow = if (isFlipped) 7 - move.to.row else move.to.row
                val vCol = if (isFlipped) 7 - move.to.col else move.to.col
                val center = Offset((vCol + 0.5f) * sqSize, (vRow + 0.5f) * sqSize)
                val isCapture = move.capturedPiece != null || move.isEnPassant

                if (isCapture) {
                    drawCircle(
                        color = TournamentEmerald.copy(alpha = 0.85f),
                        radius = sqSize * 0.41f,
                        center = center,
                        style = Stroke(width = sqSize * 0.09f)
                    )
                } else {
                    drawCircle(
                        color = Color(0xAA0B0F14),
                        radius = sqSize * 0.16f,
                        center = center
                    )
                    drawCircle(
                        color = TournamentEmerald,
                        radius = sqSize * 0.12f,
                        center = center
                    )
                }
            }

            // 4. Draw tactical / analysis arrows
            for (arrow in arrows) {
                drawMoveArrow(arrow, sqSize, isFlipped)
            }
        }
    }
}

private fun DrawScope.drawMoveArrow(
    arrow: BoardArrow,
    sqSize: Float,
    isFlipped: Boolean
) {
    val fromVRow = if (isFlipped) 7 - arrow.from.row else arrow.from.row
    val fromVCol = if (isFlipped) 7 - arrow.from.col else arrow.from.col
    val toVRow = if (isFlipped) 7 - arrow.to.row else arrow.to.row
    val toVCol = if (isFlipped) 7 - arrow.to.col else arrow.to.col

    val start = Offset((fromVCol + 0.5f) * sqSize, (fromVRow + 0.5f) * sqSize)
    val end = Offset((toVCol + 0.5f) * sqSize, (toVRow + 0.5f) * sqSize)

    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLength = sqSize * 0.36f
    val shaftEnd = Offset(
        end.x - cos(angle) * (headLength * 0.65f),
        end.y - sin(angle) * (headLength * 0.65f)
    )

    // Outer shadow for contrast
    drawLine(
        color = Color.Black.copy(alpha = 0.45f),
        start = start,
        end = shaftEnd,
        strokeWidth = sqSize * 0.18f,
        cap = StrokeCap.Round
    )

    // Main arrow shaft
    drawLine(
        color = arrow.color.copy(alpha = 0.9f),
        start = start,
        end = shaftEnd,
        strokeWidth = sqSize * 0.13f,
        cap = StrokeCap.Round
    )

    // Arrowhead triangle
    val wingAngle = 0.55f
    val p1 = Offset(
        end.x - headLength * cos(angle - wingAngle),
        end.y - headLength * sin(angle - wingAngle)
    )
    val p2 = Offset(
        end.x - headLength * cos(angle + wingAngle),
        end.y - headLength * sin(angle + wingAngle)
    )

    val headPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(headPath, color = arrow.color.copy(alpha = 0.95f), style = Fill)
}

private fun DrawScope.drawChessPiece(
    piece: Piece,
    center: Offset,
    sqSize: Float
) {
    val isWhite = piece.color == PieceColor.WHITE
    val fill = if (isWhite) PieceWhiteFill else PieceBlackFill
    val stroke = if (isWhite) PieceWhiteStroke else PieceBlackStroke
    val innerAccent = if (isWhite) PieceWhiteAccent else PieceBlackInner
    val strokeW = sqSize * 0.048f

    // Subtle drop shadow under piece
    drawOval(
        color = Color.Black.copy(alpha = 0.32f),
        topLeft = Offset(center.x - sqSize * 0.34f, center.y + sqSize * 0.25f),
        size = Size(sqSize * 0.68f, sqSize * 0.14f)
    )

    // Pedestal base shared by all pieces
    val baseTop = center.y + sqSize * 0.22f
    val baseWidth = sqSize * 0.64f
    val baseHeight = sqSize * 0.11f
    drawRoundRect(
        color = fill,
        topLeft = Offset(center.x - baseWidth / 2f, baseTop),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(sqSize * 0.04f, sqSize * 0.04f)
    )
    drawRoundRect(
        color = stroke,
        topLeft = Offset(center.x - baseWidth / 2f, baseTop),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(sqSize * 0.04f, sqSize * 0.04f),
        style = Stroke(width = strokeW)
    )

    when (piece.type) {
        PieceType.PAWN -> {
            val bodyPath = Path().apply {
                moveTo(center.x - sqSize * 0.22f, baseTop)
                quadraticTo(
                    center.x - sqSize * 0.12f, center.y + sqSize * 0.02f,
                    center.x - sqSize * 0.14f, center.y - sqSize * 0.08f
                )
                lineTo(center.x + sqSize * 0.14f, center.y - sqSize * 0.08f)
                quadraticTo(
                    center.x + sqSize * 0.12f, center.y + sqSize * 0.02f,
                    center.x + sqSize * 0.22f, baseTop
                )
                close()
            }
            drawPath(bodyPath, color = fill, style = Fill)
            drawPath(bodyPath, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))

            val headCenter = Offset(center.x, center.y - sqSize * 0.18f)
            drawCircle(color = fill, radius = sqSize * 0.14f, center = headCenter)
            drawCircle(color = stroke, radius = sqSize * 0.14f, center = headCenter, style = Stroke(width = strokeW))
            drawCircle(
                color = innerAccent,
                radius = sqSize * 0.05f,
                center = Offset(headCenter.x - sqSize * 0.04f, headCenter.y - sqSize * 0.04f)
            )
        }

        PieceType.ROOK -> {
            val path = Path().apply {
                val topY = center.y - sqSize * 0.28f
                val battlementBottom = center.y - sqSize * 0.14f
                val left = center.x - sqSize * 0.24f
                val right = center.x + sqSize * 0.24f
                val step = (right - left) / 5f

                moveTo(center.x - sqSize * 0.22f, baseTop)
                lineTo(center.x - sqSize * 0.18f, battlementBottom)
                lineTo(left, battlementBottom)
                lineTo(left, topY)
                lineTo(left + step, topY)
                lineTo(left + step, topY + sqSize * 0.07f)
                lineTo(left + step * 2f, topY + sqSize * 0.07f)
                lineTo(left + step * 2f, topY)
                lineTo(left + step * 3f, topY)
                lineTo(left + step * 3f, topY + sqSize * 0.07f)
                lineTo(left + step * 4f, topY + sqSize * 0.07f)
                lineTo(left + step * 4f, topY)
                lineTo(right, topY)
                lineTo(right, battlementBottom)
                lineTo(center.x + sqSize * 0.18f, battlementBottom)
                lineTo(center.x + sqSize * 0.22f, baseTop)
                close()
            }
            drawPath(path, color = fill, style = Fill)
            drawPath(path, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))
        }

        PieceType.KNIGHT -> {
            val path = Path().apply {
                moveTo(center.x - sqSize * 0.22f, baseTop)
                quadraticTo(
                    center.x - sqSize * 0.20f, center.y + sqSize * 0.02f,
                    center.x - sqSize * 0.06f, center.y - sqSize * 0.04f
                )
                lineTo(center.x - sqSize * 0.26f, center.y - sqSize * 0.08f)
                lineTo(center.x - sqSize * 0.22f, center.y - sqSize * 0.18f)
                lineTo(center.x - sqSize * 0.05f, center.y - sqSize * 0.28f)
                lineTo(center.x + sqSize * 0.02f, center.y - sqSize * 0.36f)
                lineTo(center.x + sqSize * 0.10f, center.y - sqSize * 0.27f)
                quadraticTo(
                    center.x + sqSize * 0.27f, center.y - sqSize * 0.14f,
                    center.x + sqSize * 0.22f, baseTop
                )
                close()
            }
            drawPath(path, color = fill, style = Fill)
            drawPath(path, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))
            // Knight eye
            drawCircle(
                color = if (isWhite) stroke else GrandmasterGoldBright,
                radius = sqSize * 0.032f,
                center = Offset(center.x - sqSize * 0.08f, center.y - sqSize * 0.18f)
            )
        }

        PieceType.BISHOP -> {
            val path = Path().apply {
                moveTo(center.x - sqSize * 0.22f, baseTop)
                quadraticTo(
                    center.x - sqSize * 0.24f, center.y - sqSize * 0.08f,
                    center.x, center.y - sqSize * 0.30f
                )
                quadraticTo(
                    center.x + sqSize * 0.24f, center.y - sqSize * 0.08f,
                    center.x + sqSize * 0.22f, baseTop
                )
                close()
            }
            drawPath(path, color = fill, style = Fill)
            drawPath(path, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))
            // Top finial orb
            val orbCenter = Offset(center.x, center.y - sqSize * 0.34f)
            drawCircle(color = fill, radius = sqSize * 0.05f, center = orbCenter)
            drawCircle(color = stroke, radius = sqSize * 0.05f, center = orbCenter, style = Stroke(width = strokeW * 0.85f))
            // Mitre cross slash
            drawLine(
                color = stroke,
                start = Offset(center.x - sqSize * 0.07f, center.y - sqSize * 0.12f),
                end = Offset(center.x + sqSize * 0.07f, center.y - sqSize * 0.12f),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = stroke,
                start = Offset(center.x, center.y - sqSize * 0.19f),
                end = Offset(center.x, center.y - sqSize * 0.05f),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
        }

        PieceType.QUEEN -> {
            val p1 = Offset(center.x - sqSize * 0.28f, center.y - sqSize * 0.22f)
            val p2 = Offset(center.x - sqSize * 0.14f, center.y - sqSize * 0.31f)
            val p3 = Offset(center.x, center.y - sqSize * 0.35f)
            val p4 = Offset(center.x + sqSize * 0.14f, center.y - sqSize * 0.31f)
            val p5 = Offset(center.x + sqSize * 0.28f, center.y - sqSize * 0.22f)

            val path = Path().apply {
                moveTo(center.x - sqSize * 0.23f, baseTop)
                lineTo(p1.x, p1.y)
                lineTo(center.x - sqSize * 0.14f, center.y - sqSize * 0.06f)
                lineTo(p2.x, p2.y)
                lineTo(center.x - sqSize * 0.05f, center.y - sqSize * 0.08f)
                lineTo(p3.x, p3.y)
                lineTo(center.x + sqSize * 0.05f, center.y - sqSize * 0.08f)
                lineTo(p4.x, p4.y)
                lineTo(center.x + sqSize * 0.14f, center.y - sqSize * 0.06f)
                lineTo(p5.x, p5.y)
                lineTo(center.x + sqSize * 0.23f, baseTop)
                close()
            }
            drawPath(path, color = fill, style = Fill)
            drawPath(path, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))

            for (pt in listOf(p1, p2, p3, p4, p5)) {
                drawCircle(color = GrandmasterGoldBright, radius = sqSize * 0.036f, center = pt)
                drawCircle(color = stroke, radius = sqSize * 0.036f, center = pt, style = Stroke(width = strokeW * 0.7f))
            }
        }

        PieceType.KING -> {
            // Royal cross on top
            val crossTop = center.y - sqSize * 0.41f
            val crossBottom = center.y - sqSize * 0.25f
            drawLine(
                color = stroke,
                start = Offset(center.x, crossTop),
                end = Offset(center.x, crossBottom),
                strokeWidth = strokeW * 1.35f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = stroke,
                start = Offset(center.x - sqSize * 0.08f, crossTop + sqSize * 0.07f),
                end = Offset(center.x + sqSize * 0.08f, crossTop + sqSize * 0.07f),
                strokeWidth = strokeW * 1.35f,
                cap = StrokeCap.Round
            )

            val path = Path().apply {
                moveTo(center.x - sqSize * 0.24f, baseTop)
                quadraticTo(
                    center.x - sqSize * 0.30f, center.y - sqSize * 0.18f,
                    center.x - sqSize * 0.10f, center.y - sqSize * 0.24f
                )
                lineTo(center.x, center.y - sqSize * 0.27f)
                lineTo(center.x + sqSize * 0.10f, center.y - sqSize * 0.24f)
                quadraticTo(
                    center.x + sqSize * 0.30f, center.y - sqSize * 0.18f,
                    center.x + sqSize * 0.24f, baseTop
                )
                close()
            }
            drawPath(path, color = fill, style = Fill)
            drawPath(path, color = stroke, style = Stroke(width = strokeW, join = StrokeJoin.Round))

            // Jewel band inside King crown
            drawCircle(
                color = GrandmasterGoldBright,
                radius = sqSize * 0.045f,
                center = Offset(center.x, center.y - sqSize * 0.06f)
            )
        }
    }
}
