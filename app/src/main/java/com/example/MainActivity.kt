package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessMatchRepository
import com.example.chess.ui.AppScreen
import com.example.chess.ui.ChessAppViewModel
import com.example.chess.ui.screens.ActiveMatchScreen
import com.example.chess.ui.screens.ArenaHomeScreen
import com.example.chess.ui.screens.HistoryArchiveScreen
import com.example.chess.ui.screens.PostGameAnalysisScreen
import com.example.ui.theme.GrandmasterGoldBright
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TournamentEmerald
import com.example.ui.theme.TournamentEmeraldContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GrandmasterSlateApp()
            }
        }
    }
}

@Composable
fun GrandmasterSlateApp() {
    val context = LocalContext.current
    val repository = remember(context) {
        val db = ChessDatabase.getInstance(context)
        ChessMatchRepository(db.matchRecordDao())
    }
    val viewModel: ChessAppViewModel = viewModel(
        factory = ChessAppViewModel.provideFactory(repository)
    )

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val setupState by viewModel.setupState.collectAsStateWithLifecycle()
    val matchState by viewModel.matchState.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val streakStats by viewModel.streakStats.collectAsStateWithLifecycle()
    val outcomeFilter by viewModel.historyOutcomeFilter.collectAsStateWithLifecycle()
    val difficultyFilter by viewModel.historyDifficultyFilter.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                tonalElevation = 8.dp
            ) {
                val isHome = currentScreen == AppScreen.ARENA_HOME
                NavigationBarItem(
                    selected = isHome,
                    onClick = { viewModel.navigateTo(AppScreen.ARENA_HOME) },
                    icon = {
                        Icon(
                            imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = stringResource(R.string.nav_play)
                        )
                    },
                    label = { Text(stringResource(R.string.nav_play)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TournamentEmerald,
                        selectedTextColor = TournamentEmerald,
                        indicatorColor = TournamentEmeraldContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_arena_tab")
                )

                val isBoard = currentScreen == AppScreen.ACTIVE_MATCH
                NavigationBarItem(
                    selected = isBoard,
                    onClick = {
                        if (!matchState.hasStartedMatch) {
                            viewModel.startNewMatch()
                        } else {
                            viewModel.navigateTo(AppScreen.ACTIVE_MATCH)
                        }
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (matchState.hasStartedMatch && !matchState.gameStatus.isFinished) {
                                    Badge(containerColor = TournamentEmerald)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isBoard) Icons.Filled.GridOn else Icons.Outlined.GridOn,
                                contentDescription = stringResource(R.string.nav_active_game)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.nav_active_game)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TournamentEmerald,
                        selectedTextColor = TournamentEmerald,
                        indicatorColor = TournamentEmeraldContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_board_tab")
                )

                val isArchive = currentScreen == AppScreen.HISTORY_ARCHIVE ||
                    currentScreen == AppScreen.POST_GAME_ANALYSIS
                NavigationBarItem(
                    selected = isArchive,
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY_ARCHIVE) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allMatches.isNotEmpty()) {
                                    Badge(
                                        containerColor = GrandmasterGoldBright,
                                        contentColor = ObsidianBg
                                    ) {
                                        Text("${allMatches.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isArchive) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                contentDescription = stringResource(R.string.nav_history)
                            )
                        }
                    },
                    label = { Text(stringResource(R.string.nav_history)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TournamentEmerald,
                        selectedTextColor = TournamentEmerald,
                        indicatorColor = TournamentEmeraldContainer,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_archive_tab")
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.ARENA_HOME -> {
                    ArenaHomeScreen(
                        setupState = setupState,
                        streakStats = streakStats,
                        activeMatchState = matchState,
                        onSelectDifficulty = viewModel::updateDifficulty,
                        onSelectTimeControl = viewModel::updateTimeControl,
                        onSelectSideChoice = viewModel::updateSideChoice,
                        onStartNewMatch = { viewModel.startNewMatch() },
                        onResumeActiveMatch = { viewModel.navigateTo(AppScreen.ACTIVE_MATCH) },
                        onOpenArchive = { viewModel.navigateTo(AppScreen.HISTORY_ARCHIVE) }
                    )
                }

                AppScreen.ACTIVE_MATCH -> {
                    ActiveMatchScreen(
                        matchState = matchState,
                        currentWinStreak = streakStats.currentWinStreak,
                        onSquareTap = viewModel::onSquareTapped,
                        onPromotionSelect = viewModel::onPromotionPieceSelected,
                        onDismissPromotion = viewModel::dismissPromotionPicker,
                        onRequestHint = viewModel::requestBestMoveHint,
                        onFlipBoard = viewModel::flipActiveBoard,
                        onResignMatch = viewModel::resignActiveMatch,
                        onNewSetup = { viewModel.navigateTo(AppScreen.ARENA_HOME) },
                        onPlayAgainSameSettings = {
                            viewModel.startNewMatch(
                                difficulty = matchState.aiDifficulty,
                                timeControl = matchState.timeControl
                            )
                        },
                        onAnalyzeCompletedMatch = viewModel::openCompletedActiveMatchAnalysis,
                        onDismissGameOverDialog = viewModel::dismissGameOverDialog,
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }

                AppScreen.HISTORY_ARCHIVE -> {
                    HistoryArchiveScreen(
                        matches = allMatches,
                        streakStats = streakStats,
                        selectedOutcomeFilter = outcomeFilter,
                        selectedDifficultyFilter = difficultyFilter,
                        onSelectOutcomeFilter = viewModel::setHistoryOutcomeFilter,
                        onSelectDifficultyFilter = viewModel::setHistoryDifficultyFilter,
                        onOpenMatchAnalysis = viewModel::openMatchAnalysis,
                        onDeleteMatch = viewModel::deleteArchivedMatch,
                        onLoadShowcaseMatch = viewModel::loadTacticalShowcaseMatch,
                        onStartNewMatch = { viewModel.navigateTo(AppScreen.ARENA_HOME) },
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }

                AppScreen.POST_GAME_ANALYSIS -> {
                    PostGameAnalysisScreen(
                        analysisState = analysisState,
                        onSetPly = viewModel::setAnalysisPly,
                        onStepPly = viewModel::stepAnalysisPly,
                        onToggleAutoPlay = viewModel::toggleAutoReplay,
                        onFlipBoard = viewModel::toggleAnalysisBoardFlip,
                        onNavigateBack = { viewModel.navigateBack() }
                    )
                }
            }
        }
    }
}
