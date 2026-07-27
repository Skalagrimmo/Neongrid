package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.CoverType
import com.example.model.ElevationTileNode
import com.example.model.ElevationType
import com.example.model.IsoMultiLevelGrid
import com.example.model.TileType
import com.example.ui.theme.*

object IsoElevationGridRenderer {

    // Converts grid coordinate (x, y, z) + continuous elevation height into 2D isometric screen space
    fun projectToIsoWithElevation(
        x: Float,
        y: Float,
        zLevel: Float,
        elevationHeight: Float,
        halfW: Float = 36f,
        halfH: Float = 20f,
        zScalePx: Float = 45f
    ): Offset {
        val isoX = (x - y) * halfW
        val isoY = (x + y) * halfH - (zLevel + elevationHeight) * zScalePx
        return Offset(isoX, isoY)
    }

    // Draws a single elevated tile diamond along with 3D side walls/cliffs and tactical cover overlays
    fun drawElevationTile(
        drawScope: DrawScope,
        drawPath: Path,
        isoCenter: Offset,
        node: ElevationTileNode,
        halfW: Float,
        halfH: Float,
        zScalePx: Float = 45f,
        isLowSpec: Boolean = false
    ) {
        if (!node.isExplored) return

        val heightOffsetPx = node.elevationHeight * zScalePx

        // Top face diamond vertices
        val pTop = Offset(isoCenter.x, isoCenter.y - halfH)
        val pRight = Offset(isoCenter.x + halfW, isoCenter.y)
        val pBottom = Offset(isoCenter.x, isoCenter.y + halfH)
        val pLeft = Offset(isoCenter.x - halfW, isoCenter.y)

        // 1. Draw 3D Side Walls / Cliff Faces if elevated (elevationHeight > 0)
        if (node.elevationHeight > 0.05f && !isLowSpec) {
            val wallDropY = heightOffsetPx.coerceAtMost(zScalePx * 2.0f)

            // Left Wall Face
            drawPath.reset()
            drawPath.moveTo(pLeft.x, pLeft.y)
            drawPath.lineTo(pBottom.x, pBottom.y)
            drawPath.lineTo(pBottom.x, pBottom.y + wallDropY)
            drawPath.lineTo(pLeft.x, pLeft.y + wallDropY)
            drawPath.close()

            val leftWallColor = when (node.elevationType) {
                ElevationType.ELEVATED_PLATFORM -> Color(0xFF1B2A38)
                ElevationType.LEDGE -> Color(0xFF261D38)
                else -> Color(0xFF161E24)
            }
            drawScope.drawPath(drawPath, color = leftWallColor, style = Fill)
            drawScope.drawPath(drawPath, color = Color(0x3300FFCC), style = Stroke(width = 1f))

            // Right Wall Face
            drawPath.reset()
            drawPath.moveTo(pBottom.x, pBottom.y)
            drawPath.lineTo(pRight.x, pRight.y)
            drawPath.lineTo(pRight.x, pRight.y + wallDropY)
            drawPath.lineTo(pBottom.x, pBottom.y + wallDropY)
            drawPath.close()

            val rightWallColor = when (node.elevationType) {
                ElevationType.ELEVATED_PLATFORM -> Color(0xFF121D28)
                ElevationType.LEDGE -> Color(0xFF1A1328)
                else -> Color(0xFF0F151A)
            }
            drawScope.drawPath(drawPath, color = rightWallColor, style = Fill)
            drawScope.drawPath(drawPath, color = Color(0x2200FFCC), style = Stroke(width = 1f))
        }

        // 2. Draw Main Top Surface Diamond
        drawPath.reset()
        drawPath.moveTo(pTop.x, pTop.y)
        drawPath.lineTo(pRight.x, pRight.y)
        drawPath.lineTo(pBottom.x, pBottom.y)
        drawPath.lineTo(pLeft.x, pLeft.y)
        drawPath.close()

        val baseColor = when (node.elevationType) {
            ElevationType.ELEVATED_PLATFORM -> Color(0xFF1D2E3C)
            ElevationType.LEDGE -> Color(0xFF2B203E)
            ElevationType.STAIRS_UP -> Color(0xFF322614)
            ElevationType.RAMP_NORTH, ElevationType.RAMP_SOUTH,
            ElevationType.RAMP_EAST, ElevationType.RAMP_WEST -> Color(0xFF183332)
            else -> when (node.tileType) {
                TileType.FLOOR -> Color(0xFF1A2228)
                TileType.GRID_ROAD -> Color(0xFF0F2F38)
                TileType.TERMINAL -> Color(0xFF0D3333)
                TileType.LADDER_UP, TileType.LADDER_DOWN -> Color(0xFF33200D)
                else -> Color(0xFF14191E)
            }
        }

        drawScope.drawPath(drawPath, color = baseColor, style = Fill)

        // Grid Outline Stroke
        val strokeColor = when {
            node.elevationType == ElevationType.LEDGE -> ImmersiveLavender.copy(alpha = 0.5f)
            node.elevationHeight > 0f -> ImmersiveCyan.copy(alpha = 0.4f)
            node.tileType == TileType.GRID_ROAD -> ImmersiveCyan.copy(alpha = 0.3f)
            else -> Color(0x1AFFFFFF)
        }
        drawScope.drawPath(drawPath, color = strokeColor, style = Stroke(width = 1f))

        // 3. Draw Ramps & Stairs Visual Indicators
        when (node.elevationType) {
            ElevationType.RAMP_NORTH, ElevationType.RAMP_SOUTH,
            ElevationType.RAMP_EAST, ElevationType.RAMP_WEST -> {
                drawScope.drawLine(
                    color = ImmersiveCyan.copy(alpha = 0.7f),
                    start = isoCenter,
                    end = Offset(
                        isoCenter.x + (if (node.elevationType == ElevationType.RAMP_EAST) 12f else if (node.elevationType == ElevationType.RAMP_WEST) -12f else 0f),
                        isoCenter.y + (if (node.elevationType == ElevationType.RAMP_SOUTH) 8f else if (node.elevationType == ElevationType.RAMP_NORTH) -8f else 0f)
                    ),
                    strokeWidth = 2.5f
                )
            }
            ElevationType.STAIRS_UP -> {
                // Draw mini stair steps inside diamond
                for (step in -2..2) {
                    val stepY = isoCenter.y + (step * 3f)
                    drawScope.drawLine(
                        color = ImmersiveAmber.copy(alpha = 0.8f),
                        start = Offset(isoCenter.x - halfW * 0.4f, stepY),
                        end = Offset(isoCenter.x + halfW * 0.4f, stepY),
                        strokeWidth = 1.5f
                    )
                }
            }
            else -> {}
        }

        // 4. Tactical Cover Icons & High Ground Markers
        if (node.cover != CoverType.NONE) {
            val shieldRadius = halfH * 0.35f
            val shieldCenter = Offset(isoCenter.x, isoCenter.y - halfH * 0.2f)
            val coverColor = if (node.cover == CoverType.FULL_COVER) ImmersiveGreen else ImmersiveAmber

            drawScope.drawCircle(
                color = coverColor.copy(alpha = 0.8f),
                radius = shieldRadius,
                center = shieldCenter,
                style = Fill
            )
            drawScope.drawCircle(
                color = Color.White,
                radius = shieldRadius,
                center = shieldCenter,
                style = Stroke(width = 1.2f)
            )
        }

        // Tactical High Ground Glow Marker
        if (node.tacticalHighGroundBonus > 0.2f) {
            drawScope.drawCircle(
                color = ImmersiveLavender.copy(alpha = 0.4f),
                radius = 4f,
                center = Offset(isoCenter.x, isoCenter.y + halfH * 0.3f)
            )
        }
    }

    // Multi-level depth-sorted rendering loop for complete IsoMultiLevelGrid
    fun drawMultiLevelGridPass(
        drawScope: DrawScope,
        drawPath: Path,
        multiGrid: IsoMultiLevelGrid,
        activeZLevel: Int,
        halfW: Float = 36f,
        halfH: Float = 20f,
        zScalePx: Float = 45f,
        canvasWidth: Float = 0f,
        canvasHeight: Float = 0f,
        centerOffsetX: Float = 0f,
        centerOffsetY: Float = 0f,
        isLowSpec: Boolean = false
    ) {
        fun isVisible(screenPt: Offset, margin: Float = 160f): Boolean {
            if (canvasWidth <= 0f || canvasHeight <= 0f) return true
            val sx = screenPt.x + centerOffsetX
            val sy = screenPt.y + centerOffsetY
            return sx >= -margin && sx <= canvasWidth + margin && sy >= -margin && sy <= canvasHeight + margin
        }

        // Depth sorted rendering across Z levels up to active Z level
        val maxZ = activeZLevel.coerceIn(0, multiGrid.maxZLevels - 1)

        for (z in 0..maxZ) {
            val isLowerLevel = z < activeZLevel

            for (sum in 0 until (multiGrid.width + multiGrid.height)) {
                for (x in 0..sum) {
                    val y = sum - x
                    if (x in 0 until multiGrid.width && y in 0 until multiGrid.height) {
                        val node = multiGrid.getTileNode(x, y, z) ?: continue
                        if (node.tileType == TileType.EMPTY && node.elevationHeight == 0f) continue

                        val isoPos = projectToIsoWithElevation(
                            x = x.toFloat(),
                            y = y.toFloat(),
                            zLevel = z.toFloat(),
                            elevationHeight = node.elevationHeight,
                            halfW = halfW,
                            halfH = halfH,
                            zScalePx = zScalePx
                        )

                        if (isVisible(isoPos)) {
                            drawElevationTile(
                                drawScope = drawScope,
                                drawPath = drawPath,
                                isoCenter = isoPos,
                                node = if (isLowerLevel) node.copy(elevationHeight = node.elevationHeight * 0.8f) else node,
                                halfW = halfW,
                                halfH = halfH,
                                zScalePx = zScalePx,
                                isLowSpec = isLowSpec || isLowerLevel
                            )
                        }
                    }
                }
            }
        }
    }
}
