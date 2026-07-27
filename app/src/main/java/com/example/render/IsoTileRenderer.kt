package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.TileType
import com.example.ui.theme.*

object IsoTileRenderer {

    fun drawTile(
        drawScope: DrawScope,
        drawPath: Path,
        isoPos: Offset,
        tileType: TileType,
        halfW: Float,
        halfH: Float,
        isExplored: Boolean,
        zLevel: Int
    ) {
        if (!isExplored) return

        drawPath.reset()
        drawPath.moveTo(isoPos.x, isoPos.y - halfH)
        drawPath.lineTo(isoPos.x + halfW, isoPos.y)
        drawPath.lineTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(isoPos.x - halfW, isoPos.y)
        drawPath.close()

        val baseColor = when (tileType) {
            TileType.FLOOR -> if (zLevel == 0) Color(0xFF131F1C) else Color(0xFF1B2329)
            TileType.GRID_ROAD -> Color(0xFF0D2D33)
            TileType.LADDER_UP -> Color(0xFF331D00)
            TileType.LADDER_DOWN -> Color(0xFF2B1600)
            TileType.BARREL_EXPLOSIVE -> Color(0xFF3B1212)
            TileType.TERMINAL -> Color(0xFF0B2B2B)
            TileType.LASER_GRID -> Color(0xFF3B0B1A)
            TileType.EXIT_PORTAL -> Color(0xFF083B1A)
            else -> Color.Transparent
        }

        if (baseColor != Color.Transparent) {
            drawScope.drawPath(drawPath, color = baseColor, style = Fill)
            
            val gridColor = when (tileType) {
                TileType.GRID_ROAD -> ImmersiveCyan.copy(alpha = 0.4f)
                TileType.TERMINAL -> ImmersiveCyan.copy(alpha = 0.5f)
                TileType.EXIT_PORTAL -> ImmersiveGreen.copy(alpha = 0.6f)
                else -> Color(0x1AFFFFFF)
            }
            drawScope.drawPath(drawPath, color = gridColor, style = Stroke(width = 1f))
        }

        when (tileType) {
            TileType.LASER_GRID -> {
                drawScope.drawLine(
                    color = ImmersiveRed,
                    start = Offset(isoPos.x - halfW * 0.7f, isoPos.y),
                    end = Offset(isoPos.x + halfW * 0.7f, isoPos.y),
                    strokeWidth = 3f
                )
                drawScope.drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(isoPos.x - halfW * 0.7f, isoPos.y),
                    end = Offset(isoPos.x + halfW * 0.7f, isoPos.y),
                    strokeWidth = 1f
                )
            }
            TileType.LADDER_UP, TileType.LADDER_DOWN -> {
                val ladderColor = if (tileType == TileType.LADDER_UP) ImmersiveAmber else Color(0xFFFF6600)
                drawScope.drawCircle(color = ladderColor, radius = halfH * 0.5f, center = isoPos, style = Stroke(width = 2f))
                drawScope.drawLine(color = ladderColor, start = isoPos - Offset(0f, 6f), end = isoPos + Offset(0f, 6f), strokeWidth = 2f)
                drawScope.drawLine(color = ladderColor, start = isoPos - Offset(6f, 0f), end = isoPos + Offset(6f, 0f), strokeWidth = 2f)
            }
            TileType.TERMINAL -> {
                drawScope.drawCircle(color = ImmersiveCyan, radius = halfH * 0.4f, center = isoPos, style = Fill)
            }
            TileType.BARREL_EXPLOSIVE -> {
                drawScope.drawCircle(color = ImmersiveRed, radius = halfH * 0.45f, center = isoPos, style = Fill)
                drawScope.drawCircle(color = Color.Yellow, radius = halfH * 0.45f, center = isoPos, style = Stroke(width = 1.5f))
            }
            TileType.EXIT_PORTAL -> {
                drawScope.drawCircle(color = ImmersiveGreen, radius = halfH * 0.6f, center = isoPos, style = Fill)
                drawScope.drawCircle(color = Color.White, radius = halfH * 0.8f, center = isoPos, style = Stroke(width = 2f))
            }
            else -> {}
        }
    }
}
