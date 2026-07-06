package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun GameCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onMovePlayer: (Float, Float) -> Unit = { _, _ -> }
) {
    val player = viewModel.player
    val enemies = viewModel.enemies
    val levelMap = viewModel.gameLevels[viewModel.currentZLevel]
    val noiseRipples = viewModel.noiseRipples
    val projectiles = viewModel.activeProjectiles

    // Isometric Projection Configuration
    val tileWidth = 72f
    val tileHeight = tileWidth / 1.8f
    val tileHalfWidth = tileWidth / 2f
    val tileHalfHeight = tileHeight / 2f
    val zHeightOffset = 50f // Visual height lift between Z levels in 3D projection

    // Reusable Path to avoid allocation in critical render path
    val drawPath = remember { Path() }

    // Helper projection function
    fun toIso(x: Float, y: Float, z: Float): Offset {
        val screenX = (x - y) * tileHalfWidth
        val screenY = (x + y) * tileHalfHeight - z * zHeightOffset
        return Offset(screenX, screenY)
    }

    val isDetected = enemies.any { !it.isDead && it.pos.z.toInt() == viewModel.currentZLevel && isPlayerInVision(it, player) }

    Box(modifier = modifier.fillMaxSize()) {
        val boxScope = this
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(ImmersiveBgDark)
                .pointerInput(Unit) {
                    // Drag input to slide player (movement stick emulation direct on screen)
                    detectDragGestures(
                        onDragStart = {},
                        onDragEnd = { onMovePlayer(0f, 0f) },
                        onDragCancel = { onMovePlayer(0f, 0f) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Normalize drag amount to directional vectors
                            val len = sqrt(dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y)
                            if (len > 0.5f) {
                                // Translate screen coordinates to isometric grid inputs
                                // Isometric X-axis is down-right, Y-axis is down-left on screen
                                val dxGrid = (dragAmount.x / tileHalfWidth + dragAmount.y / tileHalfHeight) / 2f
                                val dyGrid = (-dragAmount.x / tileHalfWidth + dragAmount.y / tileHalfHeight) / 2f
                                // Scale up microscopic frame deltas for responsive drag movement
                                onMovePlayer(dxGrid * 15f, dyGrid * 15f)
                            }
                        }
                    )
                }
        ) {
        val tick = viewModel.gameTick // Read state inside DrawScope to trigger ONLY draw-phase invalidation
        if (levelMap == null) return@Canvas

        // Camera Lock on Player Position
        val cameraCenter = toIso(player.pos.x, player.pos.y, player.pos.z)
        val viewportOffsetX = size.width / 2f - cameraCenter.x
        val viewportOffsetY = size.height / 2f - cameraCenter.y

        withTransform({
            translate(viewportOffsetX, viewportOffsetY)
        }) {
            // 1. Draw Grid Ground and Tiles
            val width = levelMap.width
            val height = levelMap.height

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val tile = levelMap.getTile(x, y)
                    val isoPos = toIso(x.toFloat(), y.toFloat(), viewModel.currentZLevel.toFloat())

                    // Draw floor/grid tile
                    if (tile != TileType.EMPTY) {
                        drawIsoTile(drawPath, isoPos, tileHalfWidth, tileHalfHeight, tile)
                    }

                    // Draw special vertical structures (Walls, Lasers, Terminals, Barrels)
                    drawIsoStructures(drawPath, isoPos, tileHalfWidth, tileHalfHeight, zHeightOffset, tile)
                }
            }

            // 2. Draw Active Hacking Terminal indicator
            if (viewModel.isHackingActive && viewModel.hackTerminalPos != null) {
                val hPos = viewModel.hackTerminalPos!!
                if (hPos.z == viewModel.currentZLevel) {
                    val hIso = toIso(hPos.x.toFloat(), hPos.y.toFloat(), hPos.z.toFloat())
                    drawCircle(
                        color = Color(0xFF00FFCC),
                        radius = 28f * (1f + sin(System.currentTimeMillis() / 200f) * 0.15f),
                        center = hIso,
                        style = Stroke(width = 3f)
                    )
                }
            }

            // 3. Draw Enemy Vision Cones (Rendered flat on floor beneath characters)
            val playerIso = toIso(player.pos.x, player.pos.y, player.pos.z)
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != viewModel.currentZLevel) continue

                val enemyIso = toIso(enemy.pos.x, enemy.pos.y, enemy.pos.z)
                drawVisionCone(drawPath, enemyIso, enemy, player, playerIso, tileHalfWidth, tileHalfHeight)
            }

            // 3b. Draw Player Stealth Signature Radius Ring on Floor
            drawPlayerStealthRadius(drawPath, playerIso, player, tileHalfWidth, tileHalfHeight, isDetected)

            // 4. Draw Sound Noise Ripples
            for (ripple in noiseRipples) {
                if (ripple.pos.z.toInt() == viewModel.currentZLevel) {
                    val rippleIso = toIso(ripple.pos.x, ripple.pos.y, ripple.pos.z)
                    // Isometric circle skewing (Width: radius * tileHalfWidth, Height: radius * tileHalfHeight)
                    drawIsoRing(drawPath, rippleIso, ripple.radius * tileHalfWidth, ripple.radius * tileHalfHeight, Color(0x7700FFCC))
                }
            }

            // 5. Draw Enemies (Characters)
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != viewModel.currentZLevel) continue

                val enemyIso = toIso(enemy.pos.x, enemy.pos.y, enemy.pos.z)
                drawEnemyCharacter(enemyIso, enemy, zHeightOffset)
            }

            // 6. Draw Player Avatar
            drawPlayerCharacter(drawPath, playerIso, player, zHeightOffset)

            // 7. Draw Ranged Projectiles
            for (p in projectiles) {
                if (p.first.z.toInt() == viewModel.currentZLevel) {
                    val projIso = toIso(p.first.x, p.first.y, p.first.z)
                    // Floating orb
                    drawCircle(
                        color = Color(0xFFFF3366),
                        radius = 12f,
                        center = projIso.copy(y = projIso.y - 12f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = projIso.copy(y = projIso.y - 12f)
                    )
                }
            }
        }

        // 8. Draw Level Schematic Stack Overlay on Right Corner (Inspired by Z0-Z10 map on reference image)
        drawSchematicZStack(drawPath, viewModel)
    }

    // Blinking detection warning panel
    if (isDetected) {
        val pulseAlpha = (sin(System.currentTimeMillis() / 80f) + 1f) / 2f * 0.35f + 0.65f
        Box(
            modifier = with(boxScope) {
                Modifier.align(Alignment.TopCenter)
            }
                .padding(top = 16.dp)
                .background(Color(0xE60D0E10), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFFF3355), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(pulseAlpha)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFFF3355), RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WARNING: VISUAL SIGNATURE COMPROMISED",
                    color = Color(0xFFFF3355),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    } else {
        // Cool normal status badge
        val sigText = if (player.isInvisible) "GHOST MODE" else if (player.isSneaking) "SNEAK ACTIVE" else "STANDARD EMISSION"
        val sigColor = if (player.isInvisible) Color(0xFF00FFFF) else if (player.isSneaking) Color(0xFF00FFCC) else Color(0xFFBD93F9)
        Box(
            modifier = with(boxScope) {
                Modifier.align(Alignment.TopCenter)
            }
                .padding(top = 16.dp)
                .background(Color(0xB30D0E10), RoundedCornerShape(6.dp))
                .border(1.dp, sigColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(sigColor, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIGNATURE: $sigText",
                    color = sigColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
}

// Function to draw isometric base tiles
private fun DrawScope.drawIsoTile(
    path: Path,
    isoPos: Offset,
    halfW: Float,
    halfH: Float,
    tile: TileType
) {
    path.reset()
    path.moveTo(isoPos.x, isoPos.y - halfH)
    path.lineTo(isoPos.x + halfW, isoPos.y)
    path.lineTo(isoPos.x, isoPos.y + halfH)
    path.lineTo(isoPos.x - halfW, isoPos.y)
    path.close()

    val tileColor = when (tile) {
        TileType.GRID_ROAD -> Color(0xFF132B32) // Cyan neon road base
        TileType.LADDER_UP, TileType.LADDER_DOWN -> Color(0xFF261D15) // Amber industrial shaft
        TileType.EXIT_PORTAL -> Color(0xFF142E20) // Glowing green portal floor
        else -> Color(0xFF11171C) // Carbon dark blocks
    }

    val strokeColor = when (tile) {
        TileType.GRID_ROAD -> Color(0xFF00FFCC)
        TileType.EXIT_PORTAL -> Color(0xFF00FF66)
        else -> Color(0xFF1D272F) // Normal grid lines
    }

    drawPath(path = path, color = tileColor)
    drawPath(path = path, color = strokeColor, style = Stroke(width = 1.5f))

    // If Grid Road, draw custom neon wire line patterns
    if (tile == TileType.GRID_ROAD) {
        drawLine(
            color = Color(0xFF00F0FF),
            start = Offset(isoPos.x - halfW * 0.5f, isoPos.y - halfH * 0.5f),
            end = Offset(isoPos.x + halfW * 0.5f, isoPos.y + halfH * 0.5f),
            strokeWidth = 2f
        )
    }
}

// Function to draw vertical walls and other 3D elements in isometric space
private fun DrawScope.drawIsoStructures(
    path: Path,
    isoPos: Offset,
    halfW: Float,
    halfH: Float,
    zHeight: Float,
    tile: TileType
) {
    val wallHeight = 55f

    when (tile) {
        TileType.WALL -> {
            // Render 3D isometric box
            val topCenter = Offset(isoPos.x, isoPos.y - wallHeight)

            path.reset()
            path.moveTo(isoPos.x - halfW, isoPos.y)
            path.lineTo(isoPos.x, isoPos.y + halfH)
            path.lineTo(isoPos.x, isoPos.y + halfH - wallHeight)
            path.lineTo(isoPos.x - halfW, isoPos.y - wallHeight)
            path.close()
            // Draw left side (shadowed)
            drawPath(path = path, color = Color(0xFF151B1F))

            path.reset()
            path.moveTo(isoPos.x, isoPos.y + halfH)
            path.lineTo(isoPos.x + halfW, isoPos.y)
            path.lineTo(isoPos.x + halfW, isoPos.y - wallHeight)
            path.lineTo(isoPos.x, isoPos.y + halfH - wallHeight)
            path.close()
            // Draw right side (shaded)
            drawPath(path = path, color = Color(0xFF1B2329))

            path.reset()
            path.moveTo(topCenter.x, topCenter.y - halfH)
            path.lineTo(topCenter.x + halfW, topCenter.y)
            path.lineTo(topCenter.x, topCenter.y + halfH)
            path.lineTo(topCenter.x - halfW, topCenter.y)
            path.close()
            // Draw top face
            drawPath(path = path, color = Color(0xFF242E36))
            // Draw highlights
            drawPath(path = path, color = Color(0xFF33424E), style = Stroke(width = 1.5f))
        }

        TileType.BARREL_EXPLOSIVE -> {
            // Draw simple 3D cylindrical container
            val cy = isoPos.y - 12f
            drawRect(
                color = Color(0xFFFF3333),
                topLeft = Offset(isoPos.x - 14f, cy - 25f),
                size = Size(28f, 32f)
            )
            // Glowing radioactive core ring
            drawRect(
                color = Color(0xFFFFCC00),
                topLeft = Offset(isoPos.x - 14f, cy - 14f),
                size = Size(28f, 8f)
            )
            // Metal rim rings
            drawLine(Color.DarkGray, Offset(isoPos.x - 14f, cy - 25f), Offset(isoPos.x + 14f, cy - 25f), strokeWidth = 3f)
            drawLine(Color.DarkGray, Offset(isoPos.x - 14f, cy + 7f), Offset(isoPos.x + 14f, cy + 7f), strokeWidth = 3f)
        }

        TileType.TERMINAL -> {
            // Drawing holographic server console
            val baseCenter = Offset(isoPos.x, isoPos.y - 10f)
            // Cyber pillar console pedestal
            drawRect(
                color = Color(0xFF26333D),
                topLeft = Offset(baseCenter.x - 10f, baseCenter.y - 20f),
                size = Size(20f, 30f)
            )
            // Pulsing holographic screen float
            val pulse = sin(System.currentTimeMillis() / 150f) * 4f
            val hY = baseCenter.y - 30f + pulse
            
            path.reset()
            path.moveTo(baseCenter.x, hY - 14f)
            path.lineTo(baseCenter.x + 16f, hY)
            path.lineTo(baseCenter.x, hY + 14f)
            path.lineTo(baseCenter.x - 16f, hY)
            path.close()
            drawPath(path, Color(0x3300FFCC))
            drawPath(path, Color(0xFF00FFCC), style = Stroke(width = 2f))
        }

        TileType.LASER_GRID -> {
            // Draw metal gate posts on either side
            drawLine(Color(0xFF2E3D48), Offset(isoPos.x - halfW, isoPos.y), Offset(isoPos.x - halfW, isoPos.y - 45f), strokeWidth = 6f)
            drawLine(Color(0xFF2E3D48), Offset(isoPos.x + halfW, isoPos.y), Offset(isoPos.x + halfW, isoPos.y - 45f), strokeWidth = 6f)

            // Dynamic pulsing laser beams
            val beamAlpha = 0.5f + abs(sin(System.currentTimeMillis() / 100f) * 0.4f)
            drawLine(
                color = Color(0xFFFF0055).copy(alpha = beamAlpha),
                start = Offset(isoPos.x - halfW, isoPos.y - 15f),
                end = Offset(isoPos.x + halfW, isoPos.y - 15f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFFF0055).copy(alpha = beamAlpha),
                start = Offset(isoPos.x - halfW, isoPos.y - 35f),
                end = Offset(isoPos.x + halfW, isoPos.y - 35f),
                strokeWidth = 3f
            )
        }

        TileType.LADDER_UP -> {
            // Vertical ladder scaffold ascending out of level
            val topY = isoPos.y - 65f
            drawLine(Color(0xFFE65C00), Offset(isoPos.x - 8f, isoPos.y + 10f), Offset(isoPos.x - 8f, topY), strokeWidth = 3f)
            drawLine(Color(0xFFE65C00), Offset(isoPos.x + 8f, isoPos.y + 10f), Offset(isoPos.x + 8f, topY), strokeWidth = 3f)
            // Rungs
            for (stepY in isoPos.y.toInt() + 10 downTo topY.toInt() step 12) {
                drawLine(Color(0xFFFF9933), Offset(isoPos.x - 8f, stepY.toFloat()), Offset(isoPos.x + 8f, stepY.toFloat()), strokeWidth = 2.5f)
            }
        }

        TileType.LADDER_DOWN -> {
            // Drawing floor trapdoor grate
            path.reset()
            path.moveTo(isoPos.x, isoPos.y - halfH * 0.6f)
            path.lineTo(isoPos.x + halfW * 0.6f, isoPos.y)
            path.lineTo(isoPos.x, isoPos.y + halfH * 0.6f)
            path.lineTo(isoPos.x - halfW * 0.6f, isoPos.y)
            path.close()
            drawPath(path, Color(0xFFB34700), style = Stroke(width = 3f))
            drawLine(Color(0xFFB34700), Offset(isoPos.x - 6f, isoPos.y), Offset(isoPos.x + 6f, isoPos.y), strokeWidth = 3f)
        }

        else -> {}
    }
}

// Draw Commandos style translucent vision cones with concentric range grids & target locking
private fun DrawScope.drawVisionCone(
    path: Path,
    origin: Offset,
    enemy: Enemy,
    player: Player,
    playerIso: Offset,
    halfW: Float,
    halfH: Float
) {
    val isTracking = isPlayerInVision(enemy, player)
    
    val baseColor = when (enemy.alertState) {
        AlertState.PATROLLING -> Color(0x2200FF66)  // Translucent green
        AlertState.SUSPICIOUS -> Color(0x33FFCC00) // Translucent yellow
        AlertState.ALERTED -> Color(0x44FF0055)     // Translucent red
    }
    
    val coneColor = if (isTracking) Color(0x55FF0033) else baseColor

    val rangePixels = enemy.getVisionRange() * halfW * 1.5f
    val fov = enemy.getVisionConeAngle()
    val baseAngle = enemy.directionAngle

    val startAngle = baseAngle - fov / 2f
    val endAngle = baseAngle + fov / 2f
    val samples = 12

    // 1. Draw Concentric radar grids inside the cone (33%, 66%, 100%)
    for (pct in listOf(0.33f, 0.66f, 1.0f)) {
        val currRange = rangePixels * pct
        path.reset()
        for (i in 0..samples) {
            val ratio = i.toFloat() / samples
            val angle = startAngle + (endAngle - startAngle) * ratio
            val dx = cos(angle) * currRange
            val dy = sin(angle) * currRange
            val pIsoX = origin.x + (dx - dy) * 0.5f
            val pIsoY = origin.y + (dx + dy) * 0.25f
            if (i == 0) path.moveTo(pIsoX, pIsoY) else path.lineTo(pIsoX, pIsoY)
        }
        
        val strokeColor = if (isTracking) Color(0xFFFF3355) else coneColor.copy(alpha = 0.8f)
        val strokeW = if (pct == 1.0f) (if (isTracking) 2.5f else 1.5f) else 1.0f
        
        drawPath(
            path = path,
            color = strokeColor.copy(alpha = if (pct == 1.0f) 0.8f else 0.25f),
            style = Stroke(
                width = strokeW,
                pathEffect = if (pct < 1.0f) PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) else null
            )
        )
    }

    // 2. Draw Solid fill of the complete cone
    path.reset()
    path.moveTo(origin.x, origin.y)
    for (i in 0..samples) {
        val ratio = i.toFloat() / samples
        val angle = startAngle + (endAngle - startAngle) * ratio
        val dx = cos(angle) * rangePixels
        val dy = sin(angle) * rangePixels
        val pIsoX = origin.x + (dx - dy) * 0.5f
        val pIsoY = origin.y + (dx + dy) * 0.25f
        path.lineTo(pIsoX, pIsoY)
    }
    path.close()
    drawPath(path = path, color = coneColor)

    // 3. Draw high-tension red threat laser line when player is target locked
    if (isTracking) {
        val headCenterY = origin.y - 32f
        val playerCenterY = playerIso.y - 18f
        
        // Neon warning laser line
        drawLine(
            color = Color(0xFFFF0055),
            start = Offset(origin.x, headCenterY),
            end = Offset(playerIso.x, playerCenterY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), System.currentTimeMillis() / -12f)
        )
        
        // Locked Reticle rings on player
        val lockPulse = sin(System.currentTimeMillis() / 80f) * 4f
        drawCircle(
            color = Color(0xFFFF0055),
            radius = 12f + lockPulse,
            center = Offset(playerIso.x, playerCenterY),
            style = Stroke(width = 1.5f)
        )
    }
}

// Isometric ellipse/oval ring drawing for noise ripples
private fun DrawScope.drawIsoRing(
    path: Path,
    center: Offset,
    radiusX: Float,
    radiusY: Float,
    color: Color
) {
    path.reset()
    path.addOval(androidx.compose.ui.geometry.Rect(
        left = center.x - radiusX,
        top = center.y - radiusY,
        right = center.x + radiusX,
        bottom = center.y + radiusY
    ))
    drawPath(path = path, color = color, style = Stroke(width = 3.5f))
}

// Draw Enemy Character
private fun DrawScope.drawEnemyCharacter(
    isoPos: Offset,
    enemy: Enemy,
    zHeight: Float
) {
    val headCenter = Offset(isoPos.x, isoPos.y - 32f)

    // Base Shadow
    drawOval(
        color = Color(0x77000000),
        topLeft = Offset(isoPos.x - 14f, isoPos.y - 7f),
        size = Size(28f, 14f)
    )

    // Cyber Body Cylinder
    drawRect(
        color = Color(0xFF3B4854),
        topLeft = Offset(isoPos.x - 10f, isoPos.y - 28f),
        size = Size(20f, 26f)
    )

    // Red glow sensory face/eye visor
    val visorColor = if (enemy.alertState == AlertState.ALERTED) Color(0xFFFF0033) else Color(0xFFFFCC00)
    drawCircle(
        color = visorColor,
        radius = 5f + sin(System.currentTimeMillis() / 100f) * 1.5f,
        center = Offset(isoPos.x + cos(enemy.directionAngle) * 8f, headCenter.y + 4f + sin(enemy.directionAngle) * 4f)
    )

    // HP Bar
    val barW = 32f
    val barH = 4f
    val hpPct = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
    drawRect(
        color = Color.DarkGray,
        topLeft = Offset(headCenter.x - barW / 2f, headCenter.y - 12f),
        size = Size(barW, barH)
    )
    drawRect(
        color = Color(0xFFFF3355),
        topLeft = Offset(headCenter.x - barW / 2f, headCenter.y - 12f),
        size = Size(barW * hpPct, barH)
    )
}

// Draw Player Character
private fun DrawScope.drawPlayerCharacter(
    path: Path,
    isoPos: Offset,
    player: Player,
    zHeight: Float
) {
    val headCenter = Offset(isoPos.x, isoPos.y - 34f)

    // Shadow
    drawOval(
        color = Color(0xAA000000),
        topLeft = Offset(isoPos.x - 15f, isoPos.y - 8f),
        size = Size(30f, 16f)
    )

    // Main Cloak/Armor Base
    val playerColor = when {
        player.isInvisible -> Color(0x3300FFFF) // Ghost Cloak Invisibility
        player.isSneaking -> Color(0xFF00B3CC)  // Stealther Blue
        else -> Color(0xFF00FFCC)               // Active Cyber Cyan
    }

    // Dynamic glowing particles (indicates engine speed or cloak matrix)
    val pulse = sin(System.currentTimeMillis() / 200f) * 4f

    // Draw cyber coat armor block
    path.reset()
    path.moveTo(isoPos.x - 12f, isoPos.y - 6f)
    path.lineTo(isoPos.x + 12f, isoPos.y - 6f)
    path.lineTo(isoPos.x + 8f, isoPos.y - 28f)
    path.lineTo(isoPos.x - 8f, isoPos.y - 28f)
    path.close()
    drawPath(path, playerColor)

    // Cyber visor glowing head
    drawCircle(
        color = Color(0xFF00FFCC),
        radius = 7f + pulse * 0.4f,
        center = headCenter
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(headCenter.x - 5f, headCenter.y - 2f),
        size = Size(10f, 3f)
    )

    // Energy Shield sphere outline if equipped with force shield
    if (player.equippedCore.id == "force_shield") {
        drawCircle(
            color = Color(0x2200FF66),
            radius = 28f + pulse * 0.5f,
            center = Offset(isoPos.x, isoPos.y - 18f)
        )
        drawCircle(
            color = Color(0xFF00FF66).copy(alpha = 0.4f),
            radius = 28f + pulse * 0.5f,
            center = Offset(isoPos.x, isoPos.y - 18f),
            style = Stroke(width = 1.5f)
        )
    }
}

// 3D Schematic Level-Stacking Overlay (Renders high-contrast floor levels on the right hand side of HUD)
private fun DrawScope.drawSchematicZStack(path: Path, viewModel: GameViewModel) {
    val screenW = size.width
    val screenH = size.height

    // Dimensions of stack component
    val stackW = 160f
    val stackH = 260f
    val startX = screenW - stackW - 20f
    val startY = 40f

    // Drawer Background box
    drawRect(
        color = Color(0xCC070B0E),
        topLeft = Offset(startX, startY),
        size = Size(stackW, stackH)
    )
    drawRect(
        color = Color(0xFF13232C),
        topLeft = Offset(startX, startY),
        size = Size(stackW, stackH),
        style = Stroke(width = 1.5f)
    )

    // Draw stack title
    // Represent 4 isometric miniature grids stacked on top of each other
    val gridW = 90f
    val gridH = 45f
    val gapZ = 52f

    for (z in 3 downTo 0) {
        val gY = startY + 50f + (3 - z) * gapZ
        val isCurrentZ = (viewModel.currentZLevel == z)

        path.reset()
        path.moveTo(startX + stackW / 2f, gY - gridH / 2f)
        path.lineTo(startX + stackW / 2f + gridW / 2f, gY)
        path.lineTo(startX + stackW / 2f, gY + gridH / 2f)
        path.lineTo(startX + stackW / 2f - gridW / 2f, gY)
        path.close()

        // Color highlighting active level
        val gridFill = if (isCurrentZ) Color(0x2200FFCC) else Color(0x0500FFFF)
        val gridLine = if (isCurrentZ) Color(0xFF00FFCC) else Color(0x4400B3CC)

        drawPath(path, gridFill)
        drawPath(path, gridLine, style = Stroke(width = 1.5f))

        // Mini dot representing Player on respective Z level
        if (viewModel.player.pos.z.toInt() == z) {
            val pxRatio = viewModel.player.pos.x / 20f
            val pyRatio = viewModel.player.pos.y / 20f

            // Project mini coordinates
            val miniIsoX = startX + stackW / 2f + (pxRatio - pyRatio) * (gridW / 2f)
            val miniIsoY = gY + (pxRatio + pyRatio) * (gridH / 2f) - (gridH / 2f)

            drawCircle(
                color = Color(0xFF00F0FF),
                radius = 5f,
                center = Offset(miniIsoX, miniIsoY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.5f,
                center = Offset(miniIsoX, miniIsoY)
            )
        }

        // Mini dots for enemies
        for (enemy in viewModel.enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == z) {
                val exRatio = enemy.pos.x / 20f
                val eyRatio = enemy.pos.y / 20f
                val miniIsoX = startX + stackW / 2f + (exRatio - eyRatio) * (gridW / 2f)
                val miniIsoY = gY + (exRatio + eyRatio) * (gridH / 2f) - (gridH / 2f)

                drawCircle(
                    color = Color(0xFFFF3333),
                    radius = 3.5f,
                    center = Offset(miniIsoX, miniIsoY)
                )
            }
        }
    }
}

private fun isPlayerInVision(enemy: Enemy, player: Player): Boolean {
    if (player.isInvisible || enemy.isDead) return false
    val dist = enemy.pos.distanceTo(player.pos)
    val visionRange = enemy.getVisionRange()
    if (dist > visionRange) return false

    val dx = player.pos.x - enemy.pos.x
    val dy = player.pos.y - enemy.pos.y
    val angleToPlayer = atan2(dy, dx)

    var diff = abs(angleToPlayer - enemy.directionAngle)
    while (diff > PI) diff = (2 * PI - diff).toFloat()

    return diff <= enemy.getVisionConeAngle() / 2f
}

private fun DrawScope.drawPlayerStealthRadius(
    path: Path,
    center: Offset,
    player: Player,
    halfW: Float,
    halfH: Float,
    isDetected: Boolean
) {
    val stealthRadiusGrid = if (player.isSneaking) 1.5f else 3.0f
    val radiusX = stealthRadiusGrid * halfW * 1.5f
    val radiusY = stealthRadiusGrid * halfH * 1.5f

    val pulseSpeed = if (isDetected) 100f else 300f
    val pulse = sin(System.currentTimeMillis() / pulseSpeed) * 0.05f
    
    val ringColor = when {
        isDetected -> Color(0xFFFF3355) // Cyber Red warning
        player.isInvisible -> Color(0x9900F0FF) // Glowing cyan
        player.isSneaking -> Color(0xFF00FF66) // Soft green secure
        else -> Color(0xFFBD93F9) // Purple normal signature
    }

    // Draw the main ellipse
    path.reset()
    path.addOval(androidx.compose.ui.geometry.Rect(
        left = center.x - radiusX * (1f + pulse),
        top = center.y - radiusY * (1f + pulse),
        right = center.x + radiusX * (1f + pulse),
        bottom = center.y + radiusY * (1f + pulse)
    ))
    
    drawPath(
        path = path, 
        color = ringColor.copy(alpha = 0.06f)
    )
    drawPath(
        path = path, 
        color = ringColor.copy(alpha = 0.6f), 
        style = Stroke(
            width = 1.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), System.currentTimeMillis() / 20f)
        )
    )

    // Inner secondary ring
    path.reset()
    path.addOval(androidx.compose.ui.geometry.Rect(
        left = center.x - radiusX * 0.4f,
        top = center.y - radiusY * 0.4f,
        right = center.x + radiusX * 0.4f,
        bottom = center.y + radiusY * 0.4f
    ))
    drawPath(
        path = path,
        color = ringColor.copy(alpha = 0.25f),
        style = Stroke(
            width = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), -System.currentTimeMillis() / 40f)
        )
    )

    // Corner brackets around the player feet
    val bSize = 8f
    val gap = 14f
    // Top-left
    drawLine(ringColor, Offset(center.x - gap, center.y - gap/2f), Offset(center.x - gap + bSize, center.y - gap/2f), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x - gap, center.y - gap/2f), Offset(center.x - gap, center.y - gap/2f + bSize/2f), strokeWidth = 2f)
    // Top-right
    drawLine(ringColor, Offset(center.x + gap, center.y - gap/2f), Offset(center.x + gap - bSize, center.y - gap/2f), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x + gap, center.y - gap/2f), Offset(center.x + gap, center.y - gap/2f + bSize/2f), strokeWidth = 2f)
    // Bottom-left
    drawLine(ringColor, Offset(center.x - gap, center.y + gap/2f), Offset(center.x - gap + bSize, center.y + gap/2f), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x - gap, center.y + gap/2f), Offset(center.x - gap, center.y + gap/2f - bSize/2f), strokeWidth = 2f)
    // Bottom-right
    drawLine(ringColor, Offset(center.x + gap, center.y + gap/2f), Offset(center.x + gap - bSize, center.y + gap/2f), strokeWidth = 2f)
    drawLine(ringColor, Offset(center.x + gap, center.y + gap/2f), Offset(center.x + gap, center.y + gap/2f - bSize/2f), strokeWidth = 2f)
}
