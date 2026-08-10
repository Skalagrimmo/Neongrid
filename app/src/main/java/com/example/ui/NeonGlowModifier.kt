package com.example.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberNeonCyan

/**
 * Custom Compose Modifier that renders a customizable neon glow effect
 * behind UI elements (buttons, cards, active controls).
 */
fun Modifier.neonGlow(
    glowColor: Color = CyberNeonCyan,
    glowRadius: Dp = 12.dp,
    cornerRadius: Dp = 8.dp,
    alpha: Float = 0.6f,
    spread: Dp = 2.dp
): Modifier = drawBehind {
    val radiusPx = glowRadius.toPx()
    val cornerPx = cornerRadius.toPx()
    val spreadPx = spread.toPx()

    if (radiusPx <= 0f) return@drawBehind

    val paint = Paint().apply {
        color = glowColor.copy(alpha = alpha)
        asFrameworkPaint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(radiusPx, BlurMaskFilter.Blur.NORMAL)
        }
    }

    drawIntoCanvas { canvas ->
        val rect = androidx.compose.ui.geometry.Rect(
            left = -spreadPx,
            top = -spreadPx,
            right = size.width + spreadPx,
            bottom = size.height + spreadPx
        )
        canvas.drawRoundRect(
            left = rect.left,
            top = rect.top,
            right = rect.right,
            bottom = rect.bottom,
            radiusX = cornerPx,
            radiusY = cornerPx,
            paint = paint
        )
    }
}

/**
 * Applies a sharp border combined with a diffused outer neon glow halo.
 */
fun Modifier.neonGlowBorder(
    borderWidth: Dp = 1.5.dp,
    glowColor: Color = CyberNeonCyan,
    glowRadius: Dp = 10.dp,
    cornerRadius: Dp = 8.dp,
    glowAlpha: Float = 0.5f,
    shape: Shape = RoundedCornerShape(cornerRadius)
): Modifier = this
    .neonGlow(
        glowColor = glowColor,
        glowRadius = glowRadius,
        cornerRadius = cornerRadius,
        alpha = glowAlpha
    )
    .border(
        width = borderWidth,
        color = glowColor,
        shape = shape
    )

/**
 * Animated pulsating neon glow modifier for active UI elements or call-to-action buttons.
 */
fun Modifier.pulsatingNeonGlow(
    glowColor: Color = CyberNeonCyan,
    minGlowRadius: Dp = 6.dp,
    maxGlowRadius: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
    durationMs: Int = 1200
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlowPulse")
    val animatedRadius by infiniteTransition.animateValue(
        initialValue = minGlowRadius,
        targetValue = maxGlowRadius,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadiusAnimation"
    )

    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimation"
    )

    this.neonGlow(
        glowColor = glowColor,
        glowRadius = animatedRadius,
        cornerRadius = cornerRadius,
        alpha = animatedAlpha
    )
}
