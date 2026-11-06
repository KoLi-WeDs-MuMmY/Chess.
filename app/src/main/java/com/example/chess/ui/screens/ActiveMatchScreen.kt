package com.example.chess.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.engine.ChessAiEngine
import com.example.chess.engine.MaterialSummary
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.MatchOutcome
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.model.Square
import com.example.chess.ui.ActiveMatchUiState
import com.example.chess.ui.components.BoardArrow
import com.example.chess.ui.components.ChessBoardCanvas
import com.example.chess.ui.components.HorizontalEvaluationBar
import com.example.ui.theme.EvalBestEmerald
import com.example.ui.theme.EvalBlunderCrimson
import com.example.ui.theme.EvalGoodSky
import com.example.ui.theme.EvalInaccuracyAmber
import com.example.ui.theme.EvalMistakeOrange
import com.example.ui.theme.GrandmasterGoldBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TournamentEmerald
import com.example.ui.theme.TournamentEmeraldContainer

@Composable
fun ActiveMatchScreen(
    matchState: ActiveMatchUiState,
    currentWinStreak: Int,
    onSquareTap: (Square) -> Unit,
    onPromotionSelect: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onRequestHint: () -> Unit,
    onFlipBoard: () -> Unit,
    onResignMatch: () -> Unit,
    onNewSetup: () -> Unit,
    onPlayAgainSameSettings: () -> Unit,
    onAnalyzeCompletedMatch: () -> Unit,
    onDismissGameOverDialog: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val material = matchState.boardState.computeMaterialSummary()
    val aiColor = matchState.playerColor.opposite()
    val isWhitePlayer = matchState.playerColor == PieceColor.WHITE

    val aiTimeMs = if (aiColor == PieceColor.WHITE) matchState.whiteTimeRemainingMs else matchState.blackTimeRemainingMs
    val playerTimeMs = if (isWhitePlayer) matchState.whiteTimeRemainingMs else matchState.blackTimeRemainingMs

    val hintArrows = matchState.hintMove?.let {
        listOf(BoardArrow(from = it.from, to = it.to, color = TournamentEmerald))
    } ?: emptyList()

    val moveListState = rememberLazyListState()
    LaunchedEffect(matchState.analyzedMoves.size) {
        if (matchState.analyzedMoves.isNotEmpty()) {
            moveListState.animateScrollToItem(matchState.analyzedMoves.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("active_match_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Opponent (AI) Bar
        CompetitorBanner(
            title = "${matchState.aiDifficulty.displayName} Engine",
            subtitle = matchState.aiDifficulty.eloBadge,
            pieceColor = aiColor,
            isTurn = !matchState.gameStatus.isFinished && matchState.boardState.activeColor == aiColor,
            statusPill = if (matchState.isAiThinking) "Calculating..." else null,
            capturedPieces = if (aiColor == PieceColor.WHITE) material.capturedByWhite else material.capturedByBlack,
            materialDelta = if (aiColor == PieceColor.WHITE) material.whiteMaterialAdvantage else -material.whiteMaterialAdvantage,
            isTimed = matchState.timeControl.isTimed,
            timeRemainingMs = aiTimeMs
        )

        // 2. Live Centipawn Evaluation Bar
        HorizontalEvaluationBar(evalWhiteCp = matchState.liveEvalWhiteCp)

        // 3. Interactive Chessboard Canvas
        ChessBoardCanvas(
            boardState = matchState.boardState,
            selectedSquare = matchState.selectedSquare,
            legalMovesForSelected = matchState.legalMovesForSelected,
            isFlipped = matchState.isBoardFlipped,
            arrows = hintArrows,
            onSquareTap = onSquareTap
        )

        // 4. Player Bar
        CompetitorBanner(
            title = "You (${matchState.playerColor.name.lowercase().replaceFirstChar { it.uppercase() }})",
            subtitle = "Streak: $currentWinStreak W",
            pieceColor = matchState.playerColor,
            isTurn = !matchState.gameStatus.isFinished && matchState.boardState.activeColor == matchState.playerColor,
            statusPill = if (matchState.boardState.isKingInCheck(matchState.playerColor)) "IN CHECK" else null,
            capturedPieces = if (isWhitePlayer) material.capturedByWhite else material.capturedByBlack,
            materialDelta = if (isWhitePlayer) material.whiteMaterialAdvantage else -material.whiteMaterialAdvantage,
            isTimed = matchState.timeControl.isTimed,
            timeRemainingMs = playerTimeMs
        )

        // 5. Algebraic Move Tape with Classification Badges
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SlateSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            if (matchState.analyzedMoves.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Tap any ${matchState.playerColor.name.lowercase()} piece to begin the match...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                LazyRow(
                    state = moveListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(matchState.analyzedMoves, key = { it.ply }) { move ->
                        MoveTapeChip(move = move)
                    }
                }
            }
        }

        // 6. Tactical Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionControlButton(
                icon = Icons.Default.Lightbulb,
                label = if (matchState.aiDifficulty.allowHints) stringResource(R.string.hint_action) else "Locked",
                enabled = matchState.aiDifficulty.allowHints && !matchState.gameStatus.isFinished && !matchState.isAiThinking,
                onClick = onRequestHint,
                testTag = "hint_button",
                modifier = Modifier.weight(1f)
            )
            ActionControlButton(
                icon = Icons.Default.FlipCameraAndroid,
                label = stringResource(R.string.flip_board_action),
                enabled = true,
                onClick = onFlipBoard,
                testTag = "flip_board_button",
                modifier = Modifier.weight(1f)
            )
            if (!matchState.gameStatus.isFinished) {
                ActionControlButton(
                    icon = Icons.Default.Flag,
                    label = stringResource(R.string.resign_action),
                    enabled = true,
                    onClick = onResignMatch,
                    testTag = "resign_button",
                    isDestructive = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ActionControlButton(
                    icon = Icons.Default.Analytics,
                    label = "Analyze",
                    enabled = true,
                    onClick = onAnalyzeCompletedMatch,
                    testTag = "inline_analyze_button",
                    modifier = Modifier.weight(1f)
                )
            }
            ActionControlButton(
                icon = Icons.Default.Refresh,
                label = stringResource(R.string.new_game_action),
                enabled = true,
                onClick = onNewSetup,
                testTag = "new_setup_button",
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Pawn Promotion Modal
    if (matchState.pendingPromotionMoves.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismissPromotion,
            containerColor = SlateSurface,
            titleContentColor = TextPrimary,
            title = {
                Text(
                    text = "Promote Pawn",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select the piece to promote your pawn to:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PieceType.QUEEN,
                            PieceType.ROOK,
                            PieceType.BISHOP,
                            PieceType.KNIGHT
                        ).forEach { type ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onPromotionSelect(type) }
                                    .testTag("promote_${type.name.lowercase()}"),
                                color = SlateSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, TournamentEmerald)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = type.sanLetter,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = GrandmasterGoldBright
                                    )
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissPromotion) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Game Over Summary Dialog
    if (matchState.showGameOverDialog && matchState.gameStatus.isFinished) {
        val winner = matchState.gameStatus.winnerColor()
        val outcome = when (winner) {
            null -> MatchOutcome.DRAW
            matchState.playerColor -> MatchOutcome.WIN
            else -> MatchOutcome.LOSS
        }
        val playerAcc = ChessAiEngine.calculateSideAccuracy(matchState.analyzedMoves, matchState.playerColor)
        val aiAcc = ChessAiEngine.calculateSideAccuracy(matchState.analyzedMoves, matchState.playerColor.opposite())
        val blunders = matchState.analyzedMoves.count {
            it.color == matchState.playerColor && it.classification == MoveClassification.BLUNDER
        }

        val outcomeAccent = when (outcome) {
            MatchOutcome.WIN -> TournamentEmerald
            MatchOutcome.LOSS -> EvalBlunderCrimson
            MatchOutcome.DRAW -> GrandmasterGoldBright
        }

        AlertDialog(
            onDismissRequest = onDismissGameOverDialog,
            containerColor = SlateSurface,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = outcome.label.uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = outcomeAccent
                    )
                    Text(
                        text = "${matchState.gameStatus.terminationTitle()} vs ${matchState.aiDifficulty.displayName} AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = SlateSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = GrandmasterGoldBright
                                )
                                Text(
                                    text = "Current Win Streak",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "$currentWinStreak W",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GrandmasterGoldBright
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Text("YOUR ACCURACY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format("%.1f%%", playerAcc),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TournamentEmerald
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Text("AI ACCURACY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = String.format("%.1f%%", aiAcc),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EvalGoodSky
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateSurfaceElevated)
                                .padding(10.dp)
                        ) {
                            Text("BLUNDERS", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = "$blunders",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (blunders == 0) TournamentEmerald else EvalBlunderCrimson
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onAnalyzeCompletedMatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TournamentEmerald,
                        contentColor = Color(0xFF032219)
                    ),
                    modifier = Modifier.testTag("analyze_completed_match_button")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.analyze_game_cta), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onPlayAgainSameSettings,
                    modifier = Modifier.testTag("play_again_button")
                ) {
                    Text("Rematch", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun CompetitorBanner(
    title: String,
    subtitle: String,
    pieceColor: PieceColor,
    isTurn: Boolean,
    statusPill: String?,
    capturedPieces: List<PieceType>,
    materialDelta: Int,
    isTimed: Boolean,
    timeRemainingMs: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isTurn) 1.5.dp else 1.dp,
            color = if (isTurn) TournamentEmerald else SlateBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Piece Side Badge
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (pieceColor == PieceColor.WHITE) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                        .border(
                            1.5.dp,
                            if (pieceColor == PieceColor.WHITE) Color(0xFF475569) else GrandmasterGoldBright,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (pieceColor == PieceColor.WHITE) "W" else "B",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (pieceColor == PieceColor.WHITE) Color(0xFF0F172A) else GrandmasterGoldBright
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        if (statusPill != null) {
                            Surface(
                                color = if (statusPill == "IN CHECK") EvalBlunderCrimson.copy(alpha = 0.2f) else TournamentEmeraldContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = statusPill,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (statusPill == "IN CHECK") EvalBlunderCrimson else TournamentEmerald
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = subtitle,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        if (capturedPieces.isNotEmpty() || materialDelta > 0) {
                            val capSummary = capturedPieces
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .joinToString(" ") { (t, count) ->
                                    val sym = if (t == PieceType.PAWN) "P" else t.sanLetter
                                    if (count > 1) "${count}$sym" else sym
                                }
                            val advText = if (materialDelta > 0) " +$materialDelta" else ""
                            Text(
                                text = "• $capSummary$advText",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TournamentEmerald
                            )
                        }
                    }
                }
            }

            // Chess Clock
            Surface(
                color = if (isTurn) TournamentEmeraldContainer else SlateSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isTurn) TournamentEmerald else SlateBorder
                )
            ) {
                Text(
                    text = if (isTimed) formatClockMillis(timeRemainingMs) else "∞",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isTurn) TournamentEmerald else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MoveTapeChip(move: AnalyzedMove) {
    val badgeColor = classificationColor(move.classification)
    val prefix = if (move.color == PieceColor.WHITE) "${move.moveNumber}." else "${move.moveNumber}..."

    Surface(
        color = SlateSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = prefix,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextMuted
            )
            Text(
                text = move.san,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TextPrimary
            )
            Text(
                text = move.classification.badgeSymbol,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun ActionControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val contentColor = when {
        !enabled -> TextMuted
        isDestructive -> EvalBlunderCrimson
        else -> TextPrimary
    }
    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag),
        color = SlateSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = contentColor
            )
        }
    }
}

fun classificationColor(classification: MoveClassification): Color = when (classification) {
    MoveClassification.BEST -> EvalBestEmerald
    MoveClassification.GOOD -> EvalGoodSky
    MoveClassification.INACCURACY -> EvalInaccuracyAmber
    MoveClassification.MISTAKE -> EvalMistakeOrange
    MoveClassification.BLUNDER -> EvalBlunderCrimson
}

fun formatClockMillis(ms: Long): String {
    val clamped = ms.coerceAtLeast(0L)
    val totalSeconds = clamped / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val tenths = (clamped % 1000L) / 100L
    return if (minutes == 0L && seconds < 20L) {
        String.format("%01d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
