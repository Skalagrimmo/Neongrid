package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.GbcGraphicsSettings

object IsoStructureRenderer {

    fun drawWallBlock(
        drawScope: DrawScope,
        drawPath: Path,
        isoPos: Offset,
        halfW: Float,
        halfH: Float,
        wallHeight: Float = 40f,
        isExplored: Boolean,
        gbcSettings: GbcGraphicsSettings = GbcGraphicsSettings()
    ) {
        if (!isExplored) return

        val palette = gbcSettings.palette
        val celSettings = gbcSettings.celShadingSettings
        val isCel = gbcSettings.isCelShadingEnabled && celSettings.isEnabled
        val topIso = Offset(isoPos.x, isoPos.y - wallHeight)

        // Calculate cel-shaded quantized face colors
        val leftColor = if (isCel) {
            CelShadingEngine.applyCelShading(palette.wallPrimary, lightFactor = 0.55f, settings = celSettings)
        } else {
            palette.wallPrimary
        }

        val rightColor = if (isCel) {
            CelShadingEngine.applyCelShading(palette.wallPrimary, lightFactor = 0.80f, settings = celSettings)
        } else {
            palette.wallPrimary.copy(alpha = 0.85f)
        }

        val topColor = if (isCel) {
            CelShadingEngine.applyCelShading(palette.wallTop, lightFactor = 1.15f, settings = celSettings)
        } else {
            palette.wallTop
        }

        val outlineColor = if (isCel) {
            celSettings.inkOutlineColor
        } else if (gbcSettings.isPixelOutlineEnabled) {
            palette.gridOutline
        } else {
            palette.wallAccent.copy(alpha = 0.3f)
        }

        val strokeWidth = if (isCel) celSettings.inkOutlineThickness else 1.5f

        // Left Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x - halfW, isoPos.y)
        drawPath.lineTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()

        drawScope.drawPath(drawPath, color = leftColor, style = Fill)
        drawScope.drawPath(drawPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Right Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(isoPos.x + halfW, isoPos.y)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.close()

        drawScope.drawPath(drawPath, color = rightColor, style = Fill)
        drawScope.drawPath(drawPath, color = outlineColor, style = Stroke(width = strokeWidth))

        // Top Roof Facet
        drawPath.reset()
        drawPath.moveTo(topIso.x, topIso.y - halfH)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()

        drawScope.drawPath(drawPath, color = topColor, style = Fill)
        val roofStrokeCol = if (isCel) palette.wallAccent else palette.wallAccent
        drawScope.drawPath(drawPath, color = roofStrokeCol, style = Stroke(width = strokeWidth))

        // Cel-Shaded Rim Highlight along top border
        if (isCel && celSettings.rimLightingEnabled) {
            CelShadingEngine.drawRimHighlight(
                drawScope = drawScope,
                start = Offset(topIso.x - halfW, topIso.y),
                end = Offset(topIso.x, topIso.y - halfH),
                highlightColor = palette.wallAccent.copy(alpha = 0.9f),
                strokeWidth = 2.0f
            )
        }

        // 8-Bit Pixel Dither Pattern on Top Face
        if (gbcSettings.isPixelDitherEnabled) {
            val dotCol = palette.wallAccent.copy(alpha = 0.4f)
            drawScope.drawCircle(color = dotCol, radius = 2f, center = topIso)
        }
    }
}

