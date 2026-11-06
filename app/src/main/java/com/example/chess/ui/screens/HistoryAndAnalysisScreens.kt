package com.example.chess.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.MatchRecord
import com.example.chess.data.StreakStats
import com.example.chess.engine.ChessAiEngine
import com.example.chess.engine.ChessBoardState
import com.example.chess.model.AiDifficulty
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.MatchOutcome
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.Square
import com.example.chess.ui.AnalysisUiState
import com.example.chess.ui.components.BoardArrow
import com.example.chess.ui.components.ChessBoardCanvas
import com.example.chess.ui.components.HorizontalEvaluationBar
import com.example.ui.theme.EvalBestEmerald
import com.example.ui.theme.EvalBlunderCrimson
import com.example.ui.theme.EvalGoodSky
import com.example.ui.theme.EvalInaccuracyAmber
import com.example.ui.theme.EvalMistakeOrange
import com.example.ui.theme.GrandmasterGoldBright
import com.example.ui.theme.GrandmasterGoldContainer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryArchiveScreen(
    matches: List<MatchRecord>,
    streakStats: StreakStats,
    selectedOutcomeFilter: MatchOutcome?,
    selectedDifficultyFilter: AiDifficulty?,
    onSelectOutcomeFilter: (MatchOutcome?) -> Unit,
    onSelectDifficultyFilter: (AiDifficulty?) -> Unit,
    onOpenMatchAnalysis: (MatchRecord) -> Unit,
    onDeleteMatch: (Long) -> Unit,
    onLoadShowcaseMatch: () -> Unit,
    onStartNewMatch: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val filteredMatches = matches.filter { record ->
        val matchesOutcome = selectedOutcomeFilter == null || record.outcome == selectedOutcomeFilter
        val matchesDiff = selectedDifficultyFilter == null || record.aiDifficulty == selectedDifficultyFilter
        matchesOutcome && matchesDiff
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("history_archive_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Archive Header & Streak Summary
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Offline Match Archive",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap any past match to inspect centipawn telemetry & blunders",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = GrandmasterGoldContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GrandmasterGoldBright.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = GrandmasterGoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Best: ${streakStats.bestWinStreak}W",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = GrandmasterGoldBright
                            )
                        }
                    }
                }

                // Filter Strip: Outcome
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipPill(
                            label = "All Results (${matches.size})",
                            selected = selectedOutcomeFilter == null,
                            onClick = { onSelectOutcomeFilter(null) }
                        )
                    }
                    items(MatchOutcome.entries) { outcome ->
                        FilterChipPill(
                            label = outcome.label,
                            selected = selectedOutcomeFilter == outcome,
                            onClick = {
                                onSelectOutcomeFilter(if (selectedOutcomeFilter == outcome) null else outcome)
                            }
                        )
                    }
                }

                // Filter Strip: Difficulty Tier
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChipPill(
                            label = "All AI Tiers",
                            selected = selectedDifficultyFilter == null,
                            onClick = { onSelectDifficultyFilter(null) }
                        )
                    }
                    items(AiDifficulty.entries) { diff ->
                        FilterChipPill(
                            label = diff.displayName,
                            selected = selectedDifficultyFilter == diff,
                            onClick = {
                                onSelectDifficultyFilter(if (selectedDifficultyFilter == diff) null else diff)
                            }
                        )
                    }
                }
            }
        }

        // 2. Empty State or Filtered Match List
        if (filteredMatches.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SlateSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = TournamentEmerald,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onLoadShowcaseMatch,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("load_showcase_match_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Tactical Demo", fontSize = 12.sp, color = GrandmasterGoldBright)
                            }

                            Button(
                                onClick = onStartNewMatch,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("empty_state_play_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TournamentEmerald,
                                    contentColor = Color(0xFF032219)
                                )
                            ) {
                                Text("Play Match", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredMatches, key = { it.id }) { match ->
                ArchivedMatchCard(
                    match = match,
                    onAnalyze = { onOpenMatchAnalysis(match) },
                    onDelete = { onDeleteMatch(match.id) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (selected) TournamentEmeraldContainer else SlateSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) TournamentEmerald else SlateBorder
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) TournamentEmerald else TextSecondary
        )
    }
}

@Composable
private fun ArchivedMatchCard(
    match: MatchRecord,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit
) {
    val outcomeColor = when (match.outcome) {
        MatchOutcome.WIN -> TournamentEmerald
        MatchOutcome.LOSS -> EvalBlunderCrimson
        MatchOutcome.DRAW -> GrandmasterGoldBright
    }
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onAnalyze() }
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
            .testTag("archived_match_card_${match.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = outcomeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, outcomeColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = match.outcome.label.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = outcomeColor
                        )
                    }

                    Column {
                        Text(
                            text = "vs ${match.aiDifficulty.displayName} AI (${match.aiDifficulty.eloBadge})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${match.terminationReason} • ${match.timeControl.label} • ${match.totalFullMoves} moves • ${dateFormat.format(Date(match.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete archived match",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Accuracy & Move Classification Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStatPill("Accuracy", String.format("%.1f%%", match.playerAccuracy), TournamentEmerald)
                    MiniStatPill("Best ★", "${match.bestMoveCount}", EvalBestEmerald)
                    MiniStatPill("Mistakes ?", "${match.mistakeCount}", EvalMistakeOrange)
                    MiniStatPill("Blunders ??", "${match.blunderCount}", EvalBlunderCrimson)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = GrandmasterGoldBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Inspect",
                        style = MaterialTheme.typography.labelLarge,
                        color = GrandmasterGoldBright
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatPill(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateSurfaceElevated)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = TextMuted)
        Text(value, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostGameAnalysisScreen(
    analysisState: AnalysisUiState,
    onSetPly: (Int) -> Unit,
    onStepPly: (Int) -> Unit,
    onToggleAutoPlay: () -> Unit,
    onFlipBoard: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val record = analysisState.matchRecord
    if (record == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text("No match selected for analysis.", color = TextSecondary)
        }
        return
    }

    val moves = record.analyzedMoves
    val currentPly = analysisState.currentPly.coerceIn(0, moves.size)
    val selectedMove: AnalyzedMove? = if (currentPly in 1..moves.size) moves[currentPly - 1] else null

    val currentFen = when {
        currentPly == 0 -> ChessBoardState.INITIAL_FEN
        selectedMove != null -> selectedMove.fenAfter
        else -> record.finalFen
    }
    val boardAtPly = ChessBoardState.fromFen(currentFen)
    val evalWhiteCp = selectedMove?.evalAfterWhiteCp ?: 0

    // Construct board arrows: played move arrow + gold best-move arrow if suboptimal
    val arrows = buildList {
        if (selectedMove != null) {
            val pFrom = Square.fromAlgebraic(selectedMove.uciPlayed.take(2))
            val pTo = Square.fromAlgebraic(selectedMove.uciPlayed.drop(2).take(2))
            if (pFrom != null && pTo != null) {
                add(
                    BoardArrow(
                        from = pFrom,
                        to = pTo,
                        color = classificationColor(selectedMove.classification)
                    )
                )
            }
            if (selectedMove.classification in listOf(
                    MoveClassification.INACCURACY,
                    MoveClassification.MISTAKE,
                    MoveClassification.BLUNDER
                ) && selectedMove.uciBestMove != selectedMove.uciPlayed
            ) {
                val bFrom = Square.fromAlgebraic(selectedMove.uciBestMove.take(2))
                val bTo = Square.fromAlgebraic(selectedMove.uciBestMove.drop(2).take(2))
                if (bFrom != null && bTo != null) {
                    add(
                        BoardArrow(
                            from = bFrom,
                            to = bTo,
                            color = GrandmasterGoldBright
                        )
                    )
                }
            }
        }
    }

    val swingMoves = moves.filter {
        it.classification == MoveClassification.BLUNDER ||
            it.classification == MoveClassification.MISTAKE ||
            it.classification == MoveClassification.INACCURACY
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("post_game_analysis_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Analysis Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("analysis_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Archive",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Match Analysis vs ${record.aiDifficulty.displayName} AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${record.outcome.label} (${record.terminationReason}) • Ply $currentPly/${moves.size}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            IconButton(
                onClick = onFlipBoard,
                modifier = Modifier.testTag("analysis_flip_board_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Analysis Board",
                    tint = GrandmasterGoldBright
                )
            }
        }

        // 2. Live Centipawn Evaluation Bar
        HorizontalEvaluationBar(evalWhiteCp = evalWhiteCp)

        // 3. Chessboard with Blunder & Gold Best-Move Arrows
        ChessBoardCanvas(
            boardState = boardAtPly,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            isFlipped = analysisState.isBoardFlipped,
            arrows = arrows,
            onSquareTap = {}
        )

        // 4. Move Telemetry & Best-Move Comparison Card
        MoveInsightCard(selectedMove = selectedMove)

        // 5. Scrubber Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SlateSurface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSetPly(0) },
                    enabled = currentPly > 0,
                    modifier = Modifier.testTag("scrubber_first_button")
                ) {
                    Icon(
                        Icons.Default.FastRewind,
                        contentDescription = "Start of Game",
                        tint = if (currentPly > 0) TextPrimary else TextMuted
                    )
                }
                IconButton(
                    onClick = { onStepPly(-1) },
                    enabled = currentPly > 0,
                    modifier = Modifier.testTag("scrubber_prev_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Move",
                        tint = if (currentPly > 0) TextPrimary else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleAutoPlay() }
                        .testTag("scrubber_autoplay_button"),
                    color = TournamentEmeraldContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TournamentEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (analysisState.isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Replay",
                            tint = TournamentEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (analysisState.isAutoPlaying) "Pause" else "Replay",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TournamentEmerald
                        )
                    }
                }
                IconButton(
                    onClick = { onStepPly(1) },
                    enabled = currentPly < moves.size,
                    modifier = Modifier.testTag("scrubber_next_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Move",
                        tint = if (currentPly < moves.size) TextPrimary else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = { onSetPly(moves.size) },
                    enabled = currentPly < moves.size,
                    modifier = Modifier.testTag("scrubber_last_button")
                ) {
                    Icon(
                        Icons.Default.FastForward,
                        contentDescription = "Final Move",
                        tint = if (currentPly < moves.size) TextPrimary else TextMuted
                    )
                }
            }
        }

        // 6. Key Blunders & Mistakes Quick-Jump Strip
        if (swingMoves.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = EvalBlunderCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Key Swing Moments (Tap to Jump & Show Gold Best-Move Arrow)",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(swingMoves, key = { it.ply }) { swing ->
                            val cColor = classificationColor(swing.classification)
                            val isSelected = swing.ply == currentPly
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSetPly(swing.ply) }
                                    .testTag("swing_moment_ply_${swing.ply}"),
                                color = if (isSelected) cColor.copy(alpha = 0.24f) else SlateSurfaceElevated,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = cColor
                                )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "${swing.moveNumber}${if (swing.color == PieceColor.WHITE) "." else "..."} ${swing.san} ${swing.classification.badgeSymbol}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = cColor
                                    )
                                    Text(
                                        text = "Best: ${swing.bestMoveSan}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = GrandmasterGoldBright
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Accuracy & Classification Breakdown Card
        AccuracyBreakdownCard(record = record)

        // 8. Interactive Algebraic Move Grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Complete Move Sheet",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    moves.forEach { m ->
                        val active = m.ply == currentPly
                        val cColor = classificationColor(m.classification)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetPly(m.ply) }
                                .testTag("analysis_ply_chip_${m.ply}"),
                            color = if (active) TournamentEmeraldContainer else SlateSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) TournamentEmerald else SlateBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (m.color == PieceColor.WHITE) {
                                    Text(
                                        text = "${m.moveNumber}.",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                                Text(
                                    text = m.san,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (active) TournamentEmerald else TextPrimary
                                )
                                Text(
                                    text = m.classification.badgeSymbol,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = cColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveInsightCard(selectedMove: AnalyzedMove?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SlateSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        if (selectedMove == null) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = TournamentEmerald)
                Text(
                    text = "Starting position (0.0). Use the replay controls or tap any move below to inspect centipawn swings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        } else {
            val badgeColor = classificationColor(selectedMove.classification)
            val beforeStr = ChessAiEngine.formatEvaluationText(selectedMove.evalBeforeWhiteCp)
            val afterStr = ChessAiEngine.formatEvaluationText(selectedMove.evalAfterWhiteCp)
            val sideName = if (selectedMove.color == PieceColor.WHITE) "White" else "Black"

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor)
                        ) {
                            Text(
                                text = "${selectedMove.classification.label.uppercase()} ${selectedMove.classification.badgeSymbol}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = badgeColor
                            )
                        }
                        Text(
                            text = "$sideName played ${selectedMove.moveNumber}${if (selectedMove.color == PieceColor.WHITE) "." else "..."} ${selectedMove.san}",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "$beforeStr → $afterStr",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GrandmasterGoldBright
                    )
                }

                Text(
                    text = selectedMove.classification.coachSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (selectedMove.classification != MoveClassification.BEST &&
                    selectedMove.bestMoveSan != selectedMove.san
                ) {
                    Text(
                        text = "Engine Recommendation: ${selectedMove.bestMoveSan} (shown in Gold Arrow on board)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = GrandmasterGoldBright
                    )
                }
            }
        }
    }
}

@Composable
private fun AccuracyBreakdownCard(record: MatchRecord) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Player vs Engine Accuracy",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = String.format("You: %.1f%%  •  AI: %.1f%%", record.playerAccuracy, record.aiAccuracy),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TournamentEmerald
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClassificationCountBox("Best ★", record.bestMoveCount, EvalBestEmerald, Modifier.weight(1f))
                ClassificationCountBox("Good ✓", record.goodMoveCount, EvalGoodSky, Modifier.weight(1f))
                ClassificationCountBox("Inacc ?!", record.inaccuracyCount, EvalInaccuracyAmber, Modifier.weight(1f))
                ClassificationCountBox("Mistake ?", record.mistakeCount, EvalMistakeOrange, Modifier.weight(1f))
                ClassificationCountBox("Blunder ??", record.blunderCount, EvalBlunderCrimson, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ClassificationCountBox(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateSurfaceElevated)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = TextSecondary
        )
    }
}
