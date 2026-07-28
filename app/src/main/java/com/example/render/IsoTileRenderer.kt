package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.GbcGraphicsSettings
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
        zLevel: Int,
        gbcSettings: GbcGraphicsSettings = GbcGraphicsSettings()
    ) {
        if (!isExplored) return

        val palette = gbcSettings.palette

        drawPath.reset()
        drawPath.moveTo(isoPos.x, isoPos.y - halfH)
        drawPath.lineTo(isoPos.x + halfW, isoPos.y)
        drawPath.lineTo(isoPos.x, isoPos.y + halfH)
        drawPath.lineTo(isoPos.x - halfW, isoPos.y)
        drawPath.close()

        val baseColor = when (tileType) {
            TileType.FLOOR -> if (zLevel == 0) palette.floorPrimary else palette.floorSecondary
            TileType.GRID_ROAD -> palette.wallPrimary
            TileType.LADDER_UP, TileType.LADDER_DOWN -> palette.floorSecondary
            TileType.BARREL_EXPLOSIVE -> palette.enemyAlert.copy(alpha = 0.85f)
            TileType.TERMINAL -> palette.wallTop
            TileType.LASER_GRID -> palette.enemyAlert.copy(alpha = 0.35f)
            TileType.EXIT_PORTAL -> palette.enemyPatrol.copy(alpha = 0.35f)
            else -> Color.Transparent
        }

        if (baseColor != Color.Transparent) {
            drawScope.drawPath(drawPath, color = baseColor, style = Fill)

            // 8-Bit Pixel Dither Shading Pattern on Tile Surface
            if (gbcSettings.isPixelDitherEnabled && tileType == TileType.FLOOR) {
                val ditherColor = palette.gridOutline.copy(alpha = 0.25f)
                val dotSize = 2f
                // Render 4-point isometric dither matrix
                drawScope.drawCircle(color = ditherColor, radius = dotSize, center = Offset(isoPos.x, isoPos.y - halfH * 0.4f))
                drawScope.drawCircle(color = ditherColor, radius = dotSize, center = Offset(isoPos.x + halfW * 0.35f, isoPos.y))
                drawScope.drawCircle(color = ditherColor, radius = dotSize, center = Offset(isoPos.x, isoPos.y + halfH * 0.4f))
                drawScope.drawCircle(color = ditherColor, radius = dotSize, center = Offset(isoPos.x - halfW * 0.35f, isoPos.y))
            }

            // Crisp 8-Bit Pixel Grid Stroke
            val strokeColor = if (gbcSettings.isPixelOutlineEnabled) {
                when (tileType) {
                    TileType.GRID_ROAD -> palette.wallAccent.copy(alpha = 0.8f)
                    TileType.TERMINAL -> palette.terminalColor.copy(alpha = 0.9f)
                    TileType.EXIT_PORTAL -> palette.enemyPatrol.copy(alpha = 0.9f)
                    else -> palette.gridOutline.copy(alpha = 0.6f)
                }
            } else {
                Color(0x1AFFFFFF)
            }

            val strokeWidth = if (gbcSettings.isPixelOutlineEnabled) 1.5f else 1f
            drawScope.drawPath(drawPath, color = strokeColor, style = Stroke(width = strokeWidth))
        }

        when (tileType) {
            TileType.LASER_GRID -> {
                val laserCol = palette.enemyAlert
                drawScope.drawLine(
                    color = laserCol,
                    start = Offset(isoPos.x - halfW * 0.7f, isoPos.y),
                    end = Offset(isoPos.x + halfW * 0.7f, isoPos.y),
                    strokeWidth = 3.5f
                )
                drawScope.drawLine(
                    color = Color.White,
                    start = Offset(isoPos.x - halfW * 0.7f, isoPos.y),
                    end = Offset(isoPos.x + halfW * 0.7f, isoPos.y),
                    strokeWidth = 1f
                )
            }
            TileType.LADDER_UP, TileType.LADDER_DOWN -> {
                val ladderColor = if (tileType == TileType.LADDER_UP) palette.terminalColor else palette.playerBody
                drawScope.drawCircle(color = ladderColor, radius = halfH * 0.5f, center = isoPos, style = Stroke(width = 2f))
                drawScope.drawLine(color = ladderColor, start = isoPos - Offset(0f, 6f), end = isoPos + Offset(0f, 6f), strokeWidth = 2f)
                drawScope.drawLine(color = ladderColor, start = isoPos - Offset(6f, 0f), end = isoPos + Offset(6f, 0f), strokeWidth = 2f)
            }
            TileType.TERMINAL -> {
                drawScope.drawCircle(color = palette.terminalColor, radius = halfH * 0.45f, center = isoPos, style = Fill)
                drawScope.drawCircle(color = palette.gridOutline, radius = halfH * 0.45f, center = isoPos, style = Stroke(width = 1.5f))
            }
            TileType.BARREL_EXPLOSIVE -> {
                drawScope.drawCircle(color = palette.enemyAlert, radius = halfH * 0.45f, center = isoPos, style = Fill)
                drawScope.drawCircle(color = palette.terminalColor, radius = halfH * 0.45f, center = isoPos, style = Stroke(width = 1.5f))
            }
            TileType.EXIT_PORTAL -> {
                drawScope.drawCircle(color = palette.enemyPatrol, radius = halfH * 0.6f, center = isoPos, style = Fill)
                drawScope.drawCircle(color = Color.White, radius = halfH * 0.8f, center = isoPos, style = Stroke(width = 2f))
            }
            else -> {}
        }
    }
}

