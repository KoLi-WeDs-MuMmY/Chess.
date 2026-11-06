package com.example.chess.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.StreakStats
import com.example.chess.model.AiDifficulty
import com.example.chess.model.PlayerSideChoice
import com.example.chess.model.TimeControl
import com.example.chess.ui.ActiveMatchUiState
import com.example.chess.ui.MatchSetupState
import com.example.ui.theme.EvalBlunderCrimson
import com.example.ui.theme.EvalGoodSky
import com.example.ui.theme.EvalMistakeOrange
import com.example.ui.theme.GrandmasterGold
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArenaHomeScreen(
    setupState: MatchSetupState,
    streakStats: StreakStats,
    activeMatchState: ActiveMatchUiState,
    onSelectDifficulty: (AiDifficulty) -> Unit,
    onSelectTimeControl: (TimeControl) -> Unit,
    onSelectSideChoice: (PlayerSideChoice) -> Unit,
    onStartNewMatch: () -> Unit,
    onResumeActiveMatch: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("arena_home_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Tournament Hero Banner
        item {
            HeroBannerHeader(
                hasActiveMatch = activeMatchState.hasStartedMatch && !activeMatchState.gameStatus.isFinished,
                activeDifficulty = activeMatchState.aiDifficulty,
                onResumeActiveMatch = onResumeActiveMatch
            )
        }

        // 2. Win Streak & Telemetry Dashboard
        item {
            StreakTelemetryCard(
                streakStats = streakStats,
                onOpenArchive = onOpenArchive,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. 4-Level AI Difficulty Picker
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = TournamentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.select_ai_tier),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                AiDifficulty.entries.forEach { difficulty ->
                    val diffRecord = streakStats.byDifficulty[difficulty]
                    AiDifficultyCard(
                        difficulty = difficulty,
                        isSelected = setupState.selectedDifficulty == difficulty,
                        wins = diffRecord?.wins ?: 0,
                        losses = diffRecord?.losses ?: 0,
                        streak = diffRecord?.currentStreak ?: 0,
                        onClick = { onSelectDifficulty(difficulty) }
                    )
                }
            }
        }

        // 4. Time Control Presets
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = GrandmasterGoldBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.select_time_control),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeControl.entries.forEach { tc ->
                        val selected = setupState.selectedTimeControl == tc
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectTimeControl(tc) }
                                .testTag("time_control_${tc.name.lowercase()}"),
                            color = if (selected) TournamentEmeraldContainer else SlateSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) TournamentEmerald else SlateBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tc.label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selected) TournamentEmerald else TextPrimary
                                )
                                Text(
                                    text = tc.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (selected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Player Piece Color & Start Match CTA
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EvalGoodSky,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.select_player_side),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlayerSideChoice.entries.forEach { side ->
                        val selected = setupState.selectedSideChoice == side
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSideChoice(side) }
                                .testTag("side_choice_${side.name.lowercase()}"),
                            color = if (selected) SlateSurfaceElevated else SlateSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) GrandmasterGoldBright else SlateBorder
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = side.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) GrandmasterGoldBright else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onStartNewMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_match_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TournamentEmerald,
                        contentColor = Color(0xFF032219)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.start_match_cta),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroBannerHeader(
    hasActiveMatch: Boolean,
    activeDifficulty: AiDifficulty,
    onResumeActiveMatch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_chess_hero_banner_1793962977331),
            contentDescription = "Grandmaster Slate Chessboard Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ObsidianBg.copy(alpha = 0.35f),
                            ObsidianBg.copy(alpha = 0.78f),
                            ObsidianBg
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = GrandmasterGoldContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GrandmasterGold.copy(alpha = 0.6f))
            ) {
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrandmasterGoldBright
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )

            if (hasActiveMatch) {
                Surface(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onResumeActiveMatch() }
                        .testTag("resume_active_match_banner"),
                    color = TournamentEmeraldContainer.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TournamentEmerald)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TournamentEmerald)
                        )
                        Text(
                            text = "Match in Progress vs ${activeDifficulty.displayName} AI — Tap to Resume",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakTelemetryCard(
    streakStats: StreakStats,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrandmasterGoldContainer)
                            .border(1.dp, GrandmasterGold.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Win Streak",
                            tint = GrandmasterGoldBright,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.current_streak_label),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${streakStats.currentWinStreak}",
                                style = MaterialTheme.typography.displayMedium,
                                color = GrandmasterGoldBright,
                                modifier = Modifier.testTag("current_win_streak_value")
                            )
                            Text(
                                text = if (streakStats.currentWinStreak == 1) "WIN" else "WINS",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenArchive() }
                        .testTag("open_archive_from_streak_card"),
                    color = SlateSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TournamentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Analyze Past (${streakStats.totalGames})",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 3 Telemetry Pillars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryMetricPill(
                    label = stringResource(R.string.best_streak_label),
                    value = "${streakStats.bestWinStreak} W",
                    accentColor = GrandmasterGoldBright,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricPill(
                    label = stringResource(R.string.win_rate_label),
                    value = if (streakStats.totalGames > 0) {
                        String.format("%.0f%% (%dW/%dL)", streakStats.winRatePercent, streakStats.wins, streakStats.losses)
                    } else {
                        "0% (0W/0L)"
                    },
                    accentColor = TournamentEmerald,
                    modifier = Modifier.weight(1f)
                )
                TelemetryMetricPill(
                    label = stringResource(R.string.avg_accuracy_label),
                    value = if (streakStats.totalGames > 0) {
                        String.format("%.1f%%", streakStats.averageAccuracy)
                    } else {
                        "—"
                    },
                    accentColor = EvalGoodSky,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SlateSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = accentColor
        )
    }
}

@Composable
private fun AiDifficultyCard(
    difficulty: AiDifficulty,
    isSelected: Boolean,
    wins: Int,
    losses: Int,
    streak: Int,
    onClick: () -> Unit
) {
    val tierAccent = when (difficulty) {
        AiDifficulty.EASY -> TournamentEmerald
        AiDifficulty.MEDIUM -> EvalGoodSky
        AiDifficulty.HARD -> EvalMistakeOrange
        AiDifficulty.IMPOSSIBLE -> GrandmasterGoldBright
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) tierAccent else SlateBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("difficulty_card_${difficulty.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SlateSurfaceElevated else SlateSurface
        )
    ) {
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
                    Icon(
                        imageVector = if (difficulty == AiDifficulty.IMPOSSIBLE) {
                            Icons.Default.EmojiEvents
                        } else {
                            Icons.Default.Bolt
                        },
                        contentDescription = null,
                        tint = tierAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = difficulty.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "• ${difficulty.tagline}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    color = tierAccent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tierAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = difficulty.eloBadge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = tierAccent
                    )
                }
            }

            Text(
                text = difficulty.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Record: ${wins}W - ${losses}L  •  Streak: $streak",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = if (streak > 0) GrandmasterGoldBright else TextMuted
                )

                Text(
                    text = if (difficulty.allowHints) "Hints Enabled" else "No Hints • Master Mode",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = if (difficulty.allowHints) TournamentEmerald else EvalBlunderCrimson
                )
            }
        }
    }
}
