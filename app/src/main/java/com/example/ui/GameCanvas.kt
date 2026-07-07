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

    // Reusable Paints for Tactical Overlay text rendering
    val coordPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#AA00FFCC") // Semi-trans cyan
            textSize = 10f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    
    val elevationPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#80BB99FF") // Semi-trans lavender
            textSize = 9f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.NORMAL)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val npcPatrolPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FF00FF66") // Neon green
            textSize = 10f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val npcSuspiciousPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFFFCC00") // Amber
            textSize = 10f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val npcAlertedPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#FFFF3366") // Red
            textSize = 10f
            typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

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
            // 1a. Draw levels below with translucency to show sewers/lower floors and verticality
            for (z in 0 until viewModel.currentZLevel) {
                val belowMap = viewModel.gameLevels[z]
                if (belowMap != null) {
                    val depthDiff = viewModel.currentZLevel - z
                    val subAlpha = when (depthDiff) {
                        1 -> 0.35f
                        2 -> 0.15f
                        else -> 0.08f
                    }
                    val depthYOffset = depthDiff * zHeightOffset
                    
                    for (x in 0 until belowMap.width) {
                        for (y in 0 until belowMap.height) {
                            val tile = belowMap.getTile(x, y)
                            if (tile != TileType.EMPTY) {
                                val isoX = (x.toFloat() - y.toFloat()) * tileHalfWidth
                                val isoY = (x.toFloat() + y.toFloat()) * tileHalfHeight - viewModel.currentZLevel.toFloat() * zHeightOffset + depthYOffset
                                
                                drawIsoTile(drawPath, isoX, isoY, tileHalfWidth, tileHalfHeight, tile, alpha = subAlpha)
                                drawIsoStructures(drawPath, isoX, isoY, tileHalfWidth, tileHalfHeight, zHeightOffset, tile, alpha = subAlpha * 0.4f)
                            }
                        }
                    }
                }
            }

            // 1b. Draw current active level fully opaque (except unexplored tiles which are in fog)
            val width = levelMap.width
            val height = levelMap.height
            val activeExploredSet = viewModel.exploredTiles[viewModel.currentZLevel] ?: emptySet()

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val tile = levelMap.getTile(x, y)
                    val isoX = (x.toFloat() - y.toFloat()) * tileHalfWidth
                    val isoY = (x.toFloat() + y.toFloat()) * tileHalfHeight - viewModel.currentZLevel.toFloat() * zHeightOffset

                    val isExplored = activeExploredSet.contains("$x,$y")

                    // Draw floor/grid tile
                    if (tile != TileType.EMPTY) {
                        val alpha = if (isExplored) 1.0f else 0.12f
                        drawIsoTile(drawPath, isoX, isoY, tileHalfWidth, tileHalfHeight, tile, alpha = alpha)
                    }

                    // Draw special vertical structures (Walls, Lasers, Terminals, Barrels) only if explored
                    if (isExplored) {
                        drawIsoStructures(drawPath, isoX, isoY, tileHalfWidth, tileHalfHeight, zHeightOffset, tile)
                    }

                    // Tactical overlay grid coordinates & elevation
                    if (viewModel.isTacticalOverlayActive && tile != TileType.EMPTY && isExplored) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "[$x,$y]",
                            isoX,
                            isoY - 1f,
                            coordPaint
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "H:${viewModel.currentZLevel * 3}m",
                            isoX,
                            isoY + 11f,
                            elevationPaint
                        )
                    }
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

            // 3. Draw Enemy Vision Cones (Rendered flat on floor beneath characters if enemy is visible)
            val playerIso = toIso(player.pos.x, player.pos.y, player.pos.z)
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != viewModel.currentZLevel) continue

                val ex = enemy.pos.x.toInt()
                val ey = enemy.pos.y.toInt()
                val isEnemyExplored = activeExploredSet.contains("$ex,$ey")
                if (!isEnemyExplored) continue // Hide vision cones in fog of war!

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

            // 5. Draw Enemies (Characters) only if in explored tiles
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != viewModel.currentZLevel) continue

                val ex = enemy.pos.x.toInt()
                val ey = enemy.pos.y.toInt()
                val isEnemyExplored = activeExploredSet.contains("$ex,$ey")
                if (!isEnemyExplored) continue // Hide enemies in fog of war!

                val enemyIso = toIso(enemy.pos.x, enemy.pos.y, enemy.pos.z)
                drawEnemyCharacter(enemyIso, enemy, zHeightOffset)

                // Tactical NPC alert status icon/badge overlay
                if (viewModel.isTacticalOverlayActive) {
                    val badgeY = enemyIso.y - 58f
                    val badgeW = 90f
                    val badgeH = 15f
                    val (badgeColor, text, paint) = when (enemy.alertState) {
                        AlertState.PATROLLING -> Triple(Color(0xFF00FF66), "SECURE", npcPatrolPaint)
                        AlertState.SUSPICIOUS -> Triple(Color(0xFFFFCC00), "SUSP: ${enemy.alertMeter.toInt()}%", npcSuspiciousPaint)
                        AlertState.ALERTED -> Triple(Color(0xFFFF0033), "▲ ENGAGED", npcAlertedPaint)
                    }

                    // Draw capsule backdrop
                    drawRoundRect(
                        color = Color(0xCC070B0E),
                        topLeft = Offset(enemyIso.x - badgeW / 2f, badgeY - badgeH / 2f),
                        size = Size(badgeW, badgeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = badgeColor.copy(alpha = 0.8f),
                        topLeft = Offset(enemyIso.x - badgeW / 2f, badgeY - badgeH / 2f),
                        size = Size(badgeW, badgeH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        style = Stroke(width = 1f)
                    )

                    // Draw centered status text
                    drawContext.canvas.nativeCanvas.drawText(
                        text,
                        enemyIso.x,
                        badgeY + 4f,
                        paint
                    )
                }
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

    // 9. Cyberpunk Map Scale & Verticality HUD Overlay (Bottom-Left)
    Box(
        modifier = with(boxScope) {
            Modifier.align(Alignment.BottomStart)
        }
            .padding(start = 16.dp, bottom = 16.dp)
            .background(Color(0xE6070B0E), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF13232C), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .width(180.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TACTICAL RADAR SCALE",
                    color = Color(0xFF00FFCC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color(0xFF00FFCC), RoundedCornerShape(3.dp))
                )
            }
            
            // Custom Separator Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF13232C))
            )
            
            // Scale Bar Visual representation
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // Ruler tick marks line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left prong
                    Box(modifier = Modifier.width(1.5.dp).height(6.dp).background(Color(0xFF00FFCC)))
                    // Center horizontal line
                    Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Color(0xFF00FFCC).copy(alpha = 0.5f)))
                    // Mid prong
                    Box(modifier = Modifier.width(1.5.dp).height(4.dp).background(Color(0xFF00FFCC)))
                    // Right-center horizontal line
                    Box(modifier = Modifier.weight(1f).height(1.5.dp).background(Color(0xFF00FFCC).copy(alpha = 0.5f)))
                    // Right prong
                    Box(modifier = Modifier.width(1.5.dp).height(6.dp).background(Color(0xFF00FFCC)))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0m", color = Color(0xFF5F7582), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("15m", color = Color(0xFF5F7582), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("30m", color = Color(0xFF5F7582), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
            }
            
            // Floor Elevation details
            val floorName = when(viewModel.currentZLevel) {
                0 -> "Z=0 SEWER TUNNELS"
                1 -> "Z=1 MAIN STREET"
                2 -> "Z=2 MEZZANINE DECK"
                3 -> "Z=3 SKY GATEWAY"
                else -> "Z=N COGNITIVE GRID"
            }
            val heightEstimate = viewModel.currentZLevel * 5.5f
            
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "LEVEL: $floorName",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "ELEVATION: ${String.format("%.1f", heightEstimate)}m",
                    color = Color(0xFFFF9900),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "GRID RESOLUTION: 20x20m",
                    color = Color(0xFF8899A6),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
}

// Function to draw isometric base tiles
private fun DrawScope.drawIsoTile(
    path: Path,
    isoPosX: Float,
    isoPosY: Float,
    halfW: Float,
    halfH: Float,
    tile: TileType,
    alpha: Float = 1.0f
) {
    path.reset()
    path.moveTo(isoPosX, isoPosY - halfH)
    path.lineTo(isoPosX + halfW, isoPosY)
    path.lineTo(isoPosX, isoPosY + halfH)
    path.lineTo(isoPosX - halfW, isoPosY)
    path.close()

    val tileColor = when (tile) {
        TileType.GRID_ROAD -> Color(0xFF132B32) // Cyan neon road base
        TileType.LADDER_UP, TileType.LADDER_DOWN -> Color(0xFF261D15) // Amber industrial shaft
        TileType.EXIT_PORTAL -> Color(0xFF142E20) // Glowing green portal floor
        else -> Color(0xFF11171C) // Carbon dark blocks
    }.copy(alpha = alpha)

    val strokeColor = when (tile) {
        TileType.GRID_ROAD -> Color(0xFF00FFCC)
        TileType.EXIT_PORTAL -> Color(0xFF00FF66)
        else -> Color(0xFF1D272F) // Normal grid lines
    }.copy(alpha = alpha)

    drawPath(path = path, color = tileColor)
    drawPath(path = path, color = strokeColor, style = Stroke(width = 1.5f))

    // If Grid Road, draw custom neon wire line patterns
    if (tile == TileType.GRID_ROAD) {
        drawLine(
            color = Color(0xFF00F0FF).copy(alpha = alpha),
            start = Offset(isoPosX - halfW * 0.5f, isoPosY - halfH * 0.5f),
            end = Offset(isoPosX + halfW * 0.5f, isoPosY + halfH * 0.5f),
            strokeWidth = 2f
        )
    }
}

// Function to draw vertical walls and other 3D elements in isometric space
private fun DrawScope.drawIsoStructures(
    path: Path,
    isoPosX: Float,
    isoPosY: Float,
    halfW: Float,
    halfH: Float,
    zHeight: Float,
    tile: TileType,
    alpha: Float = 1.0f
) {
    val wallHeight = 55f

    when (tile) {
        TileType.WALL -> {
            // Render 3D isometric box
            val topCenter = Offset(isoPosX, isoPosY - wallHeight)

            path.reset()
            path.moveTo(isoPosX - halfW, isoPosY)
            path.lineTo(isoPosX, isoPosY + halfH)
            path.lineTo(isoPosX, isoPosY + halfH - wallHeight)
            path.lineTo(isoPosX - halfW, isoPosY - wallHeight)
            path.close()
            // Draw left side (shadowed)
            drawPath(path = path, color = Color(0xFF151B1F).copy(alpha = alpha))

            path.reset()
            path.moveTo(isoPosX, isoPosY + halfH)
            path.lineTo(isoPosX + halfW, isoPosY)
            path.lineTo(isoPosX + halfW, isoPosY - wallHeight)
            path.lineTo(isoPosX, isoPosY + halfH - wallHeight)
            path.close()
            // Draw right side (shaded)
            drawPath(path = path, color = Color(0xFF1B2329).copy(alpha = alpha))

            path.reset()
            path.moveTo(topCenter.x, topCenter.y - halfH)
            path.lineTo(topCenter.x + halfW, topCenter.y)
            path.lineTo(topCenter.x, topCenter.y + halfH)
            path.lineTo(topCenter.x - halfW, topCenter.y)
            path.close()
            // Draw top face
            drawPath(path = path, color = Color(0xFF242E36).copy(alpha = alpha))
            // Draw highlights
            drawPath(path = path, color = Color(0xFF33424E).copy(alpha = alpha), style = Stroke(width = 1.5f))
        }

        TileType.BARREL_EXPLOSIVE -> {
            // Draw simple 3D cylindrical container
            val cy = isoPosY - 12f
            drawRect(
                color = Color(0xFFFF3333).copy(alpha = alpha),
                topLeft = Offset(isoPosX - 14f, cy - 25f),
                size = Size(28f, 32f)
            )
            // Glowing radioactive core ring
            drawRect(
                color = Color(0xFFFFCC00).copy(alpha = alpha),
                topLeft = Offset(isoPosX - 14f, cy - 14f),
                size = Size(28f, 8f)
            )
            // Metal rim rings
            drawLine(Color.DarkGray.copy(alpha = alpha), Offset(isoPosX - 14f, cy - 25f), Offset(isoPosX + 14f, cy - 25f), strokeWidth = 3f)
            drawLine(Color.DarkGray.copy(alpha = alpha), Offset(isoPosX - 14f, cy + 7f), Offset(isoPosX + 14f, cy + 7f), strokeWidth = 3f)
        }

        TileType.TERMINAL -> {
            // Drawing holographic server console
            val baseCenter = Offset(isoPosX, isoPosY - 10f)
            // Cyber pillar console pedestal
            drawRect(
                color = Color(0xFF26333D).copy(alpha = alpha),
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
            drawPath(path, Color(0x3300FFCC).copy(alpha = alpha * 0.2f))
            drawPath(path, Color(0xFF00FFCC).copy(alpha = alpha), style = Stroke(width = 2f))
        }

        TileType.LASER_GRID -> {
            // Draw metal gate posts on either side
            drawLine(Color(0xFF2E3D48).copy(alpha = alpha), Offset(isoPosX - halfW, isoPosY), Offset(isoPosX - halfW, isoPosY - 45f), strokeWidth = 6f)
            drawLine(Color(0xFF2E3D48).copy(alpha = alpha), Offset(isoPosX + halfW, isoPosY), Offset(isoPosX + halfW, isoPosY - 45f), strokeWidth = 6f)

            // Dynamic pulsing laser beams
            val beamAlpha = 0.5f + abs(sin(System.currentTimeMillis() / 100f) * 0.4f)
            drawLine(
                color = Color(0xFFFF0055).copy(alpha = beamAlpha * alpha),
                start = Offset(isoPosX - halfW, isoPosY - 15f),
                end = Offset(isoPosX + halfW, isoPosY - 15f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFFFF0055).copy(alpha = beamAlpha * alpha),
                start = Offset(isoPosX - halfW, isoPosY - 35f),
                end = Offset(isoPosX + halfW, isoPosY - 35f),
                strokeWidth = 3f
            )
        }

        TileType.LADDER_UP -> {
            // Vertical ladder scaffold ascending out of level
            val topY = isoPosY - 65f
            drawLine(Color(0xFFE65C00).copy(alpha = alpha), Offset(isoPosX - 8f, isoPosY + 10f), Offset(isoPosX - 8f, topY), strokeWidth = 3f)
            drawLine(Color(0xFFE65C00).copy(alpha = alpha), Offset(isoPosX + 8f, isoPosY + 10f), Offset(isoPosX + 8f, topY), strokeWidth = 3f)
            // Rungs
            for (stepY in isoPosY.toInt() + 10 downTo topY.toInt() step 12) {
                drawLine(Color(0xFFFF9933).copy(alpha = alpha), Offset(isoPosX - 8f, stepY.toFloat()), Offset(isoPosX + 8f, stepY.toFloat()), strokeWidth = 2.5f)
            }
        }

        TileType.LADDER_DOWN -> {
            // Drawing floor trapdoor grate
            path.reset()
            path.moveTo(isoPosX, isoPosY - halfH * 0.6f)
            path.lineTo(isoPosX + halfW * 0.6f, isoPosY)
            path.lineTo(isoPosX, isoPosY + halfH * 0.6f)
            path.lineTo(isoPosX - halfW * 0.6f, isoPosY)
            path.close()
            drawPath(path, Color(0xFFB34700).copy(alpha = alpha), style = Stroke(width = 3f))
            drawLine(Color(0xFFB34700).copy(alpha = alpha), Offset(isoPosX - 6f, isoPosY), Offset(isoPosX + 6f, isoPosY), strokeWidth = 3f)
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
    
    // Commandos stealth mechanics: dynamic color shift based on alert level
    val detectionFactor = (enemy.alertMeter / 100f).coerceIn(0f, 1f)
    val baseColor = if (enemy.alertState == AlertState.ALERTED) {
        Color(0xFFFF0055) // pure danger red
    } else {
        // Smoothly interpolate from stealth green (0% alert) to suspicious orange-yellow (100% alert)
        // Green: 0xFF00FF66 (R=0, G=255, B=102)
        // Amber/Orange: 0xFFFF9900 (R=255, G=153, B=0)
        val r = (0 + (255 - 0) * detectionFactor).toInt()
        val g = (255 + (153 - 255) * detectionFactor).toInt()
        val b = (102 + (0 - 102) * detectionFactor).toInt()
        Color(r, g, b)
    }
    
    val coneColorBase = if (isTracking) Color(0xFFFF0055) else baseColor

    val rangePixels = enemy.getVisionRange() * halfW * 1.5f
    val fov = enemy.getVisionConeAngle()
    val baseAngle = enemy.directionAngle

    val startAngle = baseAngle - fov / 2f
    val endAngle = baseAngle + fov / 2f
    val samples = 12

    // 1. Draw Outer Zone Fill (50% to 100% range, very translucent)
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
    drawPath(path = path, color = coneColorBase.copy(alpha = 0.05f))

    // 2. Draw Inner Zone Fill (0% to 50% range, more solid)
    path.reset()
    path.moveTo(origin.x, origin.y)
    for (i in 0..samples) {
        val ratio = i.toFloat() / samples
        val angle = startAngle + (endAngle - startAngle) * ratio
        val dx = cos(angle) * rangePixels * 0.5f
        val dy = sin(angle) * rangePixels * 0.5f
        val pIsoX = origin.x + (dx - dy) * 0.5f
        val pIsoY = origin.y + (dx + dy) * 0.25f
        path.lineTo(pIsoX, pIsoY)
    }
    path.close()
    drawPath(path = path, color = coneColorBase.copy(alpha = 0.16f))

    // 3. Draw Radial spokes in the outer zone (striped/patterned effect where crawling is safe)
    val spokesCount = 8
    for (i in 0..spokesCount) {
        val ratio = i.toFloat() / spokesCount
        val angle = startAngle + (endAngle - startAngle) * ratio
        val dx = cos(angle)
        val dy = sin(angle)
        
        // Start of spoke (at 50% range)
        val p1X = origin.x + (dx * rangePixels * 0.5f - dy * rangePixels * 0.5f) * 0.5f
        val p1Y = origin.y + (dx * rangePixels * 0.5f + dy * rangePixels * 0.5f) * 0.25f
        
        // End of spoke (at 100% range)
        val p2X = origin.x + (dx * rangePixels - dy * rangePixels) * 0.5f
        val p2Y = origin.y + (dx * rangePixels + dy * rangePixels) * 0.25f
        
        drawLine(
            color = coneColorBase.copy(alpha = 0.25f),
            start = Offset(p1X, p1Y),
            end = Offset(p2X, p2Y),
            strokeWidth = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        )
    }

    // 4. Draw 50% Crawl-Safe Boundary Arc (Bold warning dashed separator)
    path.reset()
    for (i in 0..samples) {
        val ratio = i.toFloat() / samples
        val angle = startAngle + (endAngle - startAngle) * ratio
        val dx = cos(angle) * rangePixels * 0.5f
        val dy = sin(angle) * rangePixels * 0.5f
        val pIsoX = origin.x + (dx - dy) * 0.5f
        val pIsoY = origin.y + (dx + dy) * 0.25f
        if (i == 0) path.moveTo(pIsoX, pIsoY) else path.lineTo(pIsoX, pIsoY)
    }
    drawPath(
        path = path,
        color = coneColorBase.copy(alpha = 0.65f),
        style = Stroke(
            width = 2.0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    )

    // 5. Draw 100% Outer Vision Arc
    path.reset()
    for (i in 0..samples) {
        val ratio = i.toFloat() / samples
        val angle = startAngle + (endAngle - startAngle) * ratio
        val dx = cos(angle) * rangePixels
        val dy = sin(angle) * rangePixels
        val pIsoX = origin.x + (dx - dy) * 0.5f
        val pIsoY = origin.y + (dx + dy) * 0.25f
        if (i == 0) path.moveTo(pIsoX, pIsoY) else path.lineTo(pIsoX, pIsoY)
    }
    drawPath(
        path = path,
        color = coneColorBase.copy(alpha = 0.4f),
        style = Stroke(width = 1.0f)
    )

    // 6. Draw high-tension red threat laser line when player is target locked
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

    // Commandos-style Stealth HUD Alert Indicators above head
    if (!enemy.isDead) {
        val indicatorX = headCenter.x
        val indicatorY = headCenter.y - 18f
        
        when (enemy.alertState) {
            AlertState.ALERTED -> {
                // Glow circle
                drawCircle(
                    color = Color(0x33FF0055),
                    radius = 12f + sin(System.currentTimeMillis() / 80f) * 3f,
                    center = Offset(indicatorX, indicatorY - 8f)
                )
                // Red Exclamation Mark '!'
                drawRect(
                    color = Color(0xFFFF0055),
                    topLeft = Offset(indicatorX - 2.5f, indicatorY - 14f),
                    size = Size(5f, 9f)
                )
                drawCircle(
                    color = Color(0xFFFF0055),
                    radius = 2.5f,
                    center = Offset(indicatorX, indicatorY - 2f)
                )
            }
            AlertState.SUSPICIOUS -> {
                // Amber Warning Triangle with '?' style center
                val triPath = Path()
                triPath.moveTo(indicatorX, indicatorY - 16f)
                triPath.lineTo(indicatorX + 11f, indicatorY)
                triPath.lineTo(indicatorX - 11f, indicatorY)
                triPath.close()
                drawPath(path = triPath, color = Color(0xFFFFCC00))
                
                // Black central mark inside triangle
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(indicatorX - 1.5f, indicatorY - 12f),
                    size = Size(3f, 5f)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 1.5f,
                    center = Offset(indicatorX, indicatorY - 3f)
                )
            }
            AlertState.PATROLLING -> {
                if (enemy.alertMeter > 0f) {
                    // Modern horizontal detection meter progress bar
                    val meterW = 28f
                    val meterH = 5f
                    val meterX = headCenter.x - meterW / 2f
                    val meterY = headCenter.y - 22f
                    
                    // Bezel background
                    drawRect(
                        color = Color(0x99000000),
                        topLeft = Offset(meterX, meterY),
                        size = Size(meterW, meterH)
                    )
                    // Border outline
                    drawRect(
                        color = Color(0x33FFFFFF),
                        topLeft = Offset(meterX, meterY),
                        size = Size(meterW, meterH),
                        style = Stroke(width = 1f)
                    )
                    // Interpolated fill color based on progress (Teal -> Yellow -> Orange)
                    val progress = (enemy.alertMeter / 100f).coerceIn(0f, 1f)
                    val meterColor = if (progress > 0.6f) Color(0xFFFF9900) else Color(0xFFFFE500)
                    drawRect(
                        color = meterColor,
                        topLeft = Offset(meterX, meterY),
                        size = Size(meterW * progress, meterH)
                    )
                }
            }
        }
    }
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

    if (diff > enemy.getVisionConeAngle() / 2f) return false

    // Commandos stealth gameplay mechanics:
    // Inner half: always visible.
    // Outer half: hidden if sneaking.
    val isInnerZone = dist <= visionRange * 0.5f
    if (!isInnerZone && player.isSneaking) {
        return false
    }

    return true
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
