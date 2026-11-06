package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TournamentSlateColorScheme = darkColorScheme(
    primary = TournamentEmerald,
    onPrimary = Color(0xFF042F24),
    primaryContainer = TournamentEmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = GrandmasterGold,
    onSecondary = Color(0xFF281900),
    secondaryContainer = GrandmasterGoldContainer,
    onSecondaryContainer = OnGoldContainer,
    tertiary = EvalGoodSky,
    onTertiary = Color(0xFF00263A),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = EvalBlunderCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TournamentSlateColorScheme,
        typography = Typography,
        content = content
    )
}
