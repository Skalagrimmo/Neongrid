package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun NativeIsoCanvasScreen(
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeZLayer by remember { mutableIntStateOf(0) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var showWireframe by remember { mutableStateOf(true) }
    var showNodes by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "canvas_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Entity State
    var playerX by remember { mutableFloatStateOf(5f) }
    var playerY by remember { mutableFloatStateOf(5f) }
    var playerDirX by remember { mutableFloatStateOf(0f) }
    var playerDirY by remember { mutableFloatStateOf(1f) }

    // Drone NPC state
    val droneAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drone_anim"
    )
    val droneX = 8f + kotlin.math.cos(droneAnim) * 2f
    val droneY = 8f + kotlin.math.sin(droneAnim) * 2f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .safeDrawingPadding()
    ) {
        // --- Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBgHeader)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToMenu,
                    modifier = Modifier
                        .size(38.dp)
                        .background(ImmersiveBgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, ImmersiveGreen, RoundedCornerShape(8.dp))
                        .testTag("native_iso_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Hub Menu",
                        tint = ImmersiveGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NATIVE KOTLIN ISO-CANVAS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "PURE COMPOSE GRAPHICS // 60 FPS DIRECT HARDWARE DRAW",
                        color = ImmersiveSlateMuted,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Controls
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { panOffset = Offset.Zero },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgDark),
                    border = BorderStroke(1.dp, ImmersiveLavender),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Camera", tint = ImmersiveLavender, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CENTER", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = ImmersiveLavender)
                }
            }
        }

        Divider(color = Color(0x1A00FFCC), thickness = 1.dp)

        // Tool bar layer controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImmersiveBgDark.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Z-LAYER:", color = ImmersiveSlateMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                listOf(0, 1, 2).forEach { z ->
                    FilterChip(
                        selected = activeZLayer == z,
                        onClick = { activeZLayer = z },
                        label = { Text("Z=$z", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ImmersiveLavender,
                            selectedLabelColor = Color.Black,
                            containerColor = ImmersiveBgHeader,
                            labelColor = ImmersiveSlateLight
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { showWireframe = !showWireframe },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showWireframe) ImmersiveGreen.copy(alpha = 0.2f) else ImmersiveBgHeader),
                    border = BorderStroke(1.dp, if (showWireframe) ImmersiveGreen else ImmersiveSlateMuted),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(if (showWireframe) "GRID: ON" else "GRID: OFF", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = if (showWireframe) ImmersiveGreen else ImmersiveSlateMuted)
                }

                Button(
                    onClick = { showNodes = !showNodes },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showNodes) ImmersiveAmber.copy(alpha = 0.2f) else ImmersiveBgHeader),
                    border = BorderStroke(1.dp, if (showNodes) ImmersiveAmber else ImmersiveSlateMuted),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(if (showNodes) "NODES: ON" else "NODES: OFF", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = if (showNodes) ImmersiveAmber else ImmersiveSlateMuted)
                }
            }
        }

        // --- Native Compose Isometric Canvas ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffset += dragAmount
                    }
                }
                .testTag("native_iso_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridCols = 16
                val gridRows = 16
                val tileWidth = 64f
                val tileHeight = tileWidth / 1.8f
                val tileHalfWidth = tileWidth / 2f
                val tileHalfHeight = tileHeight / 2f
                val zOffsetHeight = 40f

                val centerX = size.width / 2f + panOffset.x
                val centerY = size.height / 3f + panOffset.y

                fun toIso(x: Float, y: Float, z: Float): Offset {
                    val sx = (x - y) * tileHalfWidth
                    val sy = (x + y) * tileHalfHeight - z * zOffsetHeight
                    return Offset(centerX + sx, centerY + sy)
                }

                fun isVisibleOnScreen(isoPt: Offset, margin: Float = 120f): Boolean {
                    return isoPt.x >= -margin && isoPt.x <= size.width + margin &&
                           isoPt.y >= -margin && isoPt.y <= size.height + margin
                }

                val reusablePath = Path()

                // Interleaved Depth-Sorted Render Loop (Sum = x + y painter's algorithm)
                val pillarSet = setOf(Pair(2, 3), Pair(8, 8), Pair(12, 5))
                val playerGridX = kotlin.math.round(playerX).toInt()
                val playerGridY = kotlin.math.round(playerY).toInt()
                val droneGridX = kotlin.math.round(droneX).toInt()
                val droneGridY = kotlin.math.round(droneY).toInt()

                for (sum in 0 until (gridCols + gridRows)) {
                    for (x in 0..sum) {
                        val y = sum - x
                        if (x in 0 until gridCols && y in 0 until gridRows) {
                            val topIso = toIso(x.toFloat(), y.toFloat(), activeZLayer.toFloat())

                            // 1. Draw Tile Floor if visible
                            if (isVisibleOnScreen(topIso)) {
                                val p1 = topIso
                                val p2 = Offset(topIso.x + tileHalfWidth, topIso.y + tileHalfHeight)
                                val p3 = Offset(topIso.x, topIso.y + tileHeight)
                                val p4 = Offset(topIso.x - tileHalfWidth, topIso.y + tileHalfHeight)

                                reusablePath.reset()
                                reusablePath.moveTo(p1.x, p1.y)
                                reusablePath.lineTo(p2.x, p2.y)
                                reusablePath.lineTo(p3.x, p3.y)
                                reusablePath.lineTo(p4.x, p4.y)
                                reusablePath.close()

                                val isAlt = (x + y) % 2 == 0
                                val fillColor = if (isAlt) Color(0xFF131522) else Color(0xFF0D0E1A)
                                drawPath(reusablePath, color = fillColor)

                                if (showWireframe) {
                                    drawPath(
                                        path = reusablePath,
                                        color = Color(0x2200FFCC),
                                        style = Stroke(width = 1f)
                                    )
                                }

                                if (showNodes && (x % 4 == 2) && (y % 4 == 2)) {
                                    val nodeCenter = Offset(topIso.x, topIso.y + tileHalfHeight)
                                    drawCircle(
                                        color = ImmersiveAmber.copy(alpha = pulseAlpha),
                                        radius = 6f,
                                        center = nodeCenter
                                    )
                                    drawCircle(
                                        color = ImmersiveAmber,
                                        radius = 8f,
                                        center = nodeCenter,
                                        style = Stroke(width = 1.5f)
                                    )
                                }
                            }

                            // 2. Draw Pillar Structure at (x, y) sorted by depth
                            if (pillarSet.contains(Pair(x, y))) {
                                val baseIso = toIso(x.toFloat(), y.toFloat(), activeZLayer.toFloat())
                                if (isVisibleOnScreen(baseIso, 180f)) {
                                    val topPillarIso = toIso(x.toFloat(), y.toFloat(), activeZLayer.toFloat() + 1.2f)

                                    val pTop1 = topPillarIso
                                    val pTop2 = Offset(topPillarIso.x + tileHalfWidth, topPillarIso.y + tileHalfHeight)
                                    val pTop3 = Offset(topPillarIso.x, topPillarIso.y + tileHeight)
                                    val pTop4 = Offset(topPillarIso.x - tileHalfWidth, topPillarIso.y + tileHalfHeight)

                                    // Top Face
                                    reusablePath.reset()
                                    reusablePath.moveTo(pTop1.x, pTop1.y)
                                    reusablePath.lineTo(pTop2.x, pTop2.y)
                                    reusablePath.lineTo(pTop3.x, pTop3.y)
                                    reusablePath.lineTo(pTop4.x, pTop4.y)
                                    reusablePath.close()
                                    drawPath(reusablePath, color = ImmersiveLavender.copy(alpha = 0.85f))
                                    drawPath(reusablePath, color = Color.White, style = Stroke(width = 1.5f))

                                    // Left Wall Face
                                    val pBot4 = Offset(baseIso.x - tileHalfWidth, baseIso.y + tileHalfHeight)
                                    val pBot3 = Offset(baseIso.x, baseIso.y + tileHeight)

                                    reusablePath.reset()
                                    reusablePath.moveTo(pTop4.x, pTop4.y)
                                    reusablePath.lineTo(pTop3.x, pTop3.y)
                                    reusablePath.lineTo(pBot3.x, pBot3.y)
                                    reusablePath.lineTo(pBot4.x, pBot4.y)
                                    reusablePath.close()
                                    drawPath(reusablePath, color = Color(0xFF4A3B6B))

                                    // Right Wall Face
                                    val pBot2 = Offset(baseIso.x + tileHalfWidth, baseIso.y + tileHalfHeight)
                                    reusablePath.reset()
                                    reusablePath.moveTo(pTop3.x, pTop3.y)
                                    reusablePath.lineTo(pTop2.x, pTop2.y)
                                    reusablePath.lineTo(pBot2.x, pBot2.y)
                                    reusablePath.lineTo(pBot3.x, pBot3.y)
                                    reusablePath.close()
                                    drawPath(reusablePath, color = Color(0xFF33274D))
                                }
                            }

                            // 3. Draw Drone Entity Sprite if at (x, y) sorted by depth
                            if (droneGridX == x && droneGridY == y) {
                                val droneIso = toIso(droneX, droneY, activeZLayer.toFloat())
                                if (isVisibleOnScreen(droneIso, 100f)) {
                                    // Drone shadow
                                    drawCircle(color = Color.Black.copy(alpha = 0.4f), radius = 12f, center = droneIso + Offset(0f, 4f))
                                    // Drone Body Sprite
                                    drawCircle(color = ImmersiveCyan, radius = 10f, center = droneIso - Offset(0f, 14f))
                                    drawCircle(color = Color.White, radius = 10f, center = droneIso - Offset(0f, 14f), style = Stroke(width = 1.5f))
                                    drawCircle(color = ImmersiveAmber, radius = 4f, center = droneIso - Offset(0f, 14f))
                                }
                            }

                            // 4. Draw Player Entity Sprite if at (x, y) sorted by depth
                            if (playerGridX == x && playerGridY == y) {
                                val pIso = toIso(playerX, playerY, activeZLayer.toFloat())
                                if (isVisibleOnScreen(pIso, 120f)) {
                                    // Shadow
                                    drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 16f, center = pIso + Offset(0f, 4f))
                                    // Cyber Suit Body Sprite
                                    drawCircle(color = ImmersiveLavender, radius = 14f, center = pIso - Offset(0f, 16f))
                                    drawCircle(color = Color.White, radius = 14f, center = pIso - Offset(0f, 16f), style = Stroke(width = 2f))
                                    // Cyber Visor
                                    drawCircle(color = ImmersiveGreen, radius = 6f, center = pIso - Offset(0f, 20f))
                                    // Direction Pointer
                                    val arrowIsoX = (playerDirX - playerDirY) * 20f
                                    val arrowIsoY = (playerDirX + playerDirY) * 10f
                                    drawLine(
                                        color = ImmersiveAmber,
                                        start = pIso - Offset(0f, 16f),
                                        end = pIso - Offset(0f, 16f) + Offset(arrowIsoX, arrowIsoY),
                                        strokeWidth = 3f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Info hint badge & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, ImmersiveGreen),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "ENTITY DEPTH SORTING ACTIVE",
                            color = ImmersiveGreen,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "PLAYER (X:${playerX.toInt()}, Y:${playerY.toInt()}) // DRONE ACTIVE",
                            color = ImmersiveSlateLight,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // D-Pad entity movement buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (playerY > 0) { playerY -= 1f; playerDirX = 0f; playerDirY = -1f }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(ImmersiveBgDark, RoundedCornerShape(6.dp))
                            .border(1.dp, ImmersiveLavender, RoundedCornerShape(6.dp))
                            .testTag("iso_move_up")
                    ) {
                        Text("▲", color = ImmersiveLavender, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                if (playerX > 0) { playerX -= 1f; playerDirX = -1f; playerDirY = 0f }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(ImmersiveBgDark, RoundedCornerShape(6.dp))
                                .border(1.dp, ImmersiveLavender, RoundedCornerShape(6.dp))
                                .testTag("iso_move_left")
                        ) {
                            Text("◄", color = ImmersiveLavender, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = {
                                if (playerX < 15) { playerX += 1f; playerDirX = 1f; playerDirY = 0f }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(ImmersiveBgDark, RoundedCornerShape(6.dp))
                                .border(1.dp, ImmersiveLavender, RoundedCornerShape(6.dp))
                                .testTag("iso_move_right")
                        ) {
                            Text("►", color = ImmersiveLavender, fontSize = 12.sp)
                        }
                    }
                    IconButton(
                        onClick = {
                            if (playerY < 15) { playerY += 1f; playerDirX = 0f; playerDirY = 1f }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(ImmersiveBgDark, RoundedCornerShape(6.dp))
                            .border(1.dp, ImmersiveLavender, RoundedCornerShape(6.dp))
                            .testTag("iso_move_down")
                    ) {
                        Text("▼", color = ImmersiveLavender, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
