package com.example.chess.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.ChessAiEngine
import com.example.ui.theme.BoardLightSquare
import com.example.ui.theme.GrandmasterGoldBright
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PieceBlackFill
import com.example.ui.theme.SlateBorder
import kotlin.math.tanh

/**
 * Horizontal and vertical centipawn evaluation bar for live & post-game analysis.
 * Maps centipawn score (-1000..+1000) smoothly using hyperbolic tangent to White's bar share (0.05..0.95).
 */
@Composable
fun HorizontalEvaluationBar(
    evalWhiteCp: Int,
    modifier: Modifier = Modifier
) {
    val targetWhiteFraction = (0.5f + 0.45f * tanh(evalWhiteCp / 380.0).toFloat()).coerceIn(0.05f, 0.95f)
    val animatedWhiteFraction by animateFloatAsState(
        targetValue = targetWhiteFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "evalBarFraction"
    )
    val evalText = ChessAiEngine.formatEvaluationText(evalWhiteCp)
    val whiteAhead = evalWhiteCp >= 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PieceBlackFill)
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .testTag("evaluation_bar")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedWhiteFraction)
                    .background(BoardLightSquare)
            )
        }

        // Center equilibrium marker
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .padding(vertical = 3.dp)
                .background(GrandmasterGoldBright.copy(alpha = 0.65f))
                .padding(horizontal = 1.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (whiteAhead) "WHITE $evalText" else "WHITE",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (animatedWhiteFraction > 0.22f) ObsidianBg else BoardLightSquare
            )
            Text(
                text = if (!whiteAhead) "BLACK $evalText" else "BLACK",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (animatedWhiteFraction < 0.78f) BoardLightSquare else ObsidianBg
            )
        }
    }
}
