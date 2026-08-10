package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.floor

/**
 * Configuration settings for the Cel-Shading (Toon/Comic Non-Photorealistic) Graphic System.
 */
data class CelShadingSettings(
    val isEnabled: Boolean = true,
    val bands: Int = 3, // 2-Band Retro, 3-Band Classic Anime, 4-Band Detailed Comic
    val inkOutlineThickness: Float = 2.2f, // Ink stroke outline width
    val inkOutlineColor: Color = Color(0xFF03030D), // Deep dark navy ink
    val specularThreshold: Float = 0.85f, // High contrast specular highlight
    val rimLightingEnabled: Boolean = true, // Stylized rim/edge highlights
    val shadowAmbientFloor: Float = 0.30f // Minimum ambient lighting floor for dark bands
)

/**
 * Core Cel-Shading Engine providing quantization, ink outlines, and color banding helpers
 * for both 2D Compose Canvas and OpenGL ES rendering pipelines.
 */
object CelShadingEngine {

    /**
     * Quantizes continuous luminance into discrete lighting bands.
     */
    fun quantizeLuminance(luminance: Float, bands: Int = 3, ambientFloor: Float = 0.30f): Float {
        if (bands <= 1) return luminance.coerceAtLeast(ambientFloor)
        val clampedLuma = luminance.coerceIn(0f, 1f)
        val step = floor(clampedLuma * bands) / (bands - 1)
        return step.coerceAtLeast(ambientFloor).coerceAtMost(1.0f)
    }

    /**
     * Applies cel-shading quantization to an input Color given a light angle factor.
     */
    fun applyCelShading(
        baseColor: Color,
        lightFactor: Float = 1.0f,
        settings: CelShadingSettings = CelShadingSettings()
    ): Color {
        if (!settings.isEnabled) return baseColor

        val r = baseColor.red
        val g = baseColor.green
        val b = baseColor.blue

        // Calculate perceived brightness
        val luma = (0.299f * r + 0.587f * g + 0.114f * b) * lightFactor
        val quantizedLuma = quantizeLuminance(luma, settings.bands, settings.shadowAmbientFloor)

        if (luma < 0.01f) return baseColor.copy(alpha = baseColor.alpha)

        val scale = quantizedLuma / luma.coerceAtLeast(0.01f)
        val celR = (r * scale * 1.05f).coerceIn(0f, 1f)
        val celG = (g * scale * 1.05f).coerceIn(0f, 1f)
        val celB = (b * scale * 1.05f).coerceIn(0f, 1f)

        return Color(red = celR, green = celG, blue = celB, alpha = baseColor.alpha)
    }

    /**
     * Draws a bold comic ink stroke around a vector Path in Compose DrawScope.
     */
    fun drawInkOutline(
        drawScope: DrawScope,
        path: Path,
        settings: CelShadingSettings = CelShadingSettings(),
        customColor: Color? = null,
        customWidth: Float? = null
    ) {
        if (!settings.isEnabled && customWidth == null) return

        val outlineCol = customColor ?: settings.inkOutlineColor
        val strokeWidth = customWidth ?: settings.inkOutlineThickness

        drawScope.drawPath(
            path = path,
            color = outlineCol,
            style = Stroke(width = strokeWidth)
        )
    }

    /**
     * Draws a stylized rim lighting highlight line along top/facing edges.
     */
    fun drawRimHighlight(
        drawScope: DrawScope,
        start: Offset,
        end: Offset,
        highlightColor: Color = Color.White.copy(alpha = 0.7f),
        strokeWidth: Float = 1.5f
    ) {
        drawScope.drawLine(
            color = highlightColor,
            start = start,
            end = end,
            strokeWidth = strokeWidth
        )
    }
}
