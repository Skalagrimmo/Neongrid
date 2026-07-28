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
        val topIso = Offset(isoPos.x, isoPos.y - wallHeight)

        // Left Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x - halfW, isoPos.y)
        drawPath.lineTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()

        drawScope.drawPath(drawPath, color = palette.wallPrimary, style = Fill)
        val outlineColor = if (gbcSettings.isPixelOutlineEnabled) palette.gridOutline else palette.wallAccent.copy(alpha = 0.3f)
        drawScope.drawPath(drawPath, color = outlineColor, style = Stroke(width = 1.5f))

        // Right Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(isoPos.x + halfW, isoPos.y)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.close()

        drawScope.drawPath(drawPath, color = palette.wallPrimary.copy(alpha = 0.85f), style = Fill)
        drawScope.drawPath(drawPath, color = outlineColor, style = Stroke(width = 1.5f))

        // Top Roof Facet
        drawPath.reset()
        drawPath.moveTo(topIso.x, topIso.y - halfH)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()

        drawScope.drawPath(drawPath, color = palette.wallTop, style = Fill)
        drawScope.drawPath(drawPath, color = palette.wallAccent, style = Stroke(width = 1.5f))

        // 8-Bit Pixel Dither Pattern on Top Face
        if (gbcSettings.isPixelDitherEnabled) {
            val dotCol = palette.wallAccent.copy(alpha = 0.4f)
            drawScope.drawCircle(color = dotCol, radius = 2f, center = topIso)
        }
    }
}

