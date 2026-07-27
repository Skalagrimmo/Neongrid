package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

object IsoStructureRenderer {

    fun drawWallBlock(
        drawScope: DrawScope,
        drawPath: Path,
        isoPos: Offset,
        halfW: Float,
        halfH: Float,
        wallHeight: Float = 40f,
        isExplored: Boolean
    ) {
        if (!isExplored) return

        val topIso = Offset(isoPos.x, isoPos.y - wallHeight)

        // Left Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x - halfW, isoPos.y)
        drawPath.lineTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()
        drawScope.drawPath(drawPath, color = Color(0xFF13181C), style = Fill)
        drawScope.drawPath(drawPath, color = Color(0x3300FFCC), style = Stroke(width = 1f))

        // Right Facet
        drawPath.reset()
        drawPath.moveTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(isoPos.x + halfW, isoPos.y)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.close()
        drawScope.drawPath(drawPath, color = Color(0xFF1A2227), style = Fill)
        drawScope.drawPath(drawPath, color = Color(0x3300FFCC), style = Stroke(width = 1f))

        // Top Roof Facet
        drawPath.reset()
        drawPath.moveTo(topIso.x, topIso.y - halfH)
        drawPath.lineTo(topIso.x + halfW, topIso.y)
        drawPath.lineTo(topIso.x, topIso.y + halfH)
        drawPath.lineTo(topIso.x - halfW, topIso.y)
        drawPath.close()
        drawScope.drawPath(drawPath, color = Color(0xFF232D34), style = Fill)
        drawScope.drawPath(drawPath, color = Color(0x6600FFCC), style = Stroke(width = 1f))
    }
}
