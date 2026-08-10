package com.example.ui

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.render.GlBatchRenderer
import com.example.ui.theme.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GlNativeIsoRenderer : GLSurfaceView.Renderer {
    private val batch = GlBatchRenderer()
    var activeZLayer = 0
    var panOffsetX = 0f
    var panOffsetY = 0f
    var showWireframe = true
    var showNodes = true
    var playerX = 5f
    var playerY = 5f
    var playerDirX = 0f
    var playerDirY = 1f
    var droneX = 8f
    var droneY = 8f
    var pulseAlpha = 0.5f

    private var screenWidth = 1
    private var screenHeight = 1

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        batch.initGL()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        batch.setScreenSize(screenWidth, screenHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        batch.beginBatch(enableScanlines = false, enableCelShading = true, celBands = 3.0f)

        val gridCols = 16
        val gridRows = 16
        val tileWidth = 64f
        val tileHeight = tileWidth / 1.8f
        val tileHalfWidth = tileWidth / 2f
        val tileHalfHeight = tileHeight / 2f
        val zOffsetHeight = 40f

        val centerX = screenWidth / 2f + panOffsetX
        val centerY = screenHeight / 3f + panOffsetY

        fun toIso(x: Float, y: Float, z: Float): Offset {
            val sx = (x - y) * tileHalfWidth
            val sy = (x + y) * tileHalfHeight - z * zOffsetHeight
            return Offset(centerX + sx, centerY + sy)
        }

        fun isVisibleOnScreen(isoPt: Offset, margin: Float = 120f): Boolean {
            return isoPt.x >= -margin && isoPt.x <= screenWidth + margin &&
                   isoPt.y >= -margin && isoPt.y <= screenHeight + margin
        }

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

                    if (isVisibleOnScreen(topIso)) {
                        val isAlt = (x + y) % 2 == 0
                        val fillColor = if (isAlt) Color(0xFF131522) else Color(0xFF0D0E1A)
                        val strokeColor = if (showWireframe) Color(0x2200FFCC) else null

                        batch.drawIsoDiamond(topIso.x, topIso.y + tileHalfHeight, tileHalfWidth, tileHalfHeight, fillColor, strokeColor)

                        if (showNodes && (x % 4 == 2) && (y % 4 == 2)) {
                            val nodeCenter = Offset(topIso.x, topIso.y + tileHalfHeight)
                            batch.drawCircle(nodeCenter.x, nodeCenter.y, 6f, ImmersiveAmber.copy(alpha = pulseAlpha), 12, true)
                            batch.drawCircle(nodeCenter.x, nodeCenter.y, 8f, ImmersiveAmber, 12, false, 1.5f)
                        }
                    }

                    if (pillarSet.contains(Pair(x, y))) {
                        val baseIso = toIso(x.toFloat(), y.toFloat(), activeZLayer.toFloat())
                        if (isVisibleOnScreen(baseIso, 180f)) {
                            batch.drawIsoCube(
                                baseIso.x, baseIso.y + tileHalfHeight,
                                tileHalfWidth, tileHalfHeight,
                                wallHeight = 48f,
                                topColor = ImmersiveLavender.copy(alpha = 0.85f),
                                leftColor = Color(0xFF4A3B6B),
                                rightColor = Color(0xFF33274D),
                                strokeColor = Color.White,
                                strokeWidth = 1.5f
                            )
                        }
                    }

                    if (droneGridX == x && droneGridY == y) {
                        val droneIso = toIso(droneX, droneY, activeZLayer.toFloat())
                        if (isVisibleOnScreen(droneIso, 100f)) {
                            batch.drawCircle(droneIso.x, droneIso.y + 4f, 12f, Color.Black.copy(alpha = 0.4f), 14, true)
                            batch.drawCircle(droneIso.x, droneIso.y - 14f, 10f, ImmersiveCyan, 14, true)
                            batch.drawCircle(droneIso.x, droneIso.y - 14f, 10f, Color.White, 14, false, 1.5f)
                            batch.drawCircle(droneIso.x, droneIso.y - 14f, 4f, ImmersiveAmber, 8, true)
                        }
                    }

                    if (playerGridX == x && playerGridY == y) {
                        val pIso = toIso(playerX, playerY, activeZLayer.toFloat())
                        if (isVisibleOnScreen(pIso, 120f)) {
                            batch.drawCircle(pIso.x, pIso.y + 4f, 16f, Color.Black.copy(alpha = 0.45f), 16, true)
                            batch.drawCircle(pIso.x, pIso.y - 16f, 14f, ImmersiveLavender, 16, true)
                            batch.drawCircle(pIso.x, pIso.y - 16f, 14f, Color.White, 16, false, 2f)
                            batch.drawCircle(pIso.x, pIso.y - 20f, 6f, ImmersiveGreen, 12, true)

                            val arrowIsoX = (playerDirX - playerDirY) * 20f
                            val arrowIsoY = (playerDirX + playerDirY) * 10f
                            batch.drawLine(pIso.x, pIso.y - 16f, pIso.x + arrowIsoX, pIso.y - 16f + arrowIsoY, ImmersiveAmber, 3f)
                        }
                    }
                }
            }
        }

        batch.flush()
    }
}

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

    val glRenderer = remember { GlNativeIsoRenderer() }
    glRenderer.activeZLayer = activeZLayer
    glRenderer.panOffsetX = panOffset.x
    glRenderer.panOffsetY = panOffset.y
    glRenderer.showWireframe = showWireframe
    glRenderer.showNodes = showNodes
    glRenderer.playerX = playerX
    glRenderer.playerY = playerY
    glRenderer.playerDirX = playerDirX
    glRenderer.playerDirY = playerDirY
    glRenderer.droneX = droneX
    glRenderer.droneY = droneY
    glRenderer.pulseAlpha = pulseAlpha

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
                        text = "OPENGL ES 3.0 ISO-CANVAS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "OPENGL ES 3.0 HARDWARE ACCELERATED // HIGH PERFORMANCE",
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

        HorizontalDivider(color = Color(0x1A00FFCC), thickness = 1.dp)

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

        // --- OpenGL ES 3.0 Canvas ---
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
            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        setRenderer(glRenderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

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
                            text = "OPENGL ES 3.0 DEPTH SORTING ACTIVE",
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

