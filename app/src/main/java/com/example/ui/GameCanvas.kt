package com.example.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.*
import com.example.render.GlIsoRenderer
import kotlinx.coroutines.isActive
import kotlin.math.*

@Composable
fun GameCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onMovePlayer: (Float, Float) -> Unit = { _, _ -> }
) {
    val player = viewModel.player
    val enemies = viewModel.enemies
    val civilians = viewModel.civilians
    val levelMap = viewModel.gameLevels[viewModel.currentZLevel]
    val noiseRipples = viewModel.noiseRipples
    val projectiles = viewModel.activeProjectiles

    val exploredArray = remember(viewModel.exploredTiles, viewModel.currentZLevel) {
        val arr = Array(35) { BooleanArray(35) }
        val activeExploredSet = viewModel.exploredTiles[viewModel.currentZLevel] ?: emptySet()
        for (s in activeExploredSet) {
            val parts = s.split(",")
            if (parts.size == 2) {
                val x = parts[0].toIntOrNull()
                val y = parts[1].toIntOrNull()
                if (x != null && y != null && x in 0..34 && y in 0..34) {
                    arr[x][y] = true
                }
            }
        }
        arr
    }

    val tileWidth = 72f
    val tileHeight = tileWidth / 1.8f
    val tileHalfWidth = tileWidth / 2f
    val tileHalfHeight = tileHeight / 2f
    val zHeightOffset = 50f

    fun toIso(x: Float, y: Float, z: Float): Offset {
        val screenX = (x - y) * tileHalfWidth
        val screenY = (x + y) * tileHalfHeight - z * zHeightOffset
        return Offset(screenX, screenY)
    }

    var renderPlayerX by remember { mutableFloatStateOf(player.pos.x) }
    var renderPlayerY by remember { mutableFloatStateOf(player.pos.y) }
    var renderCamX by remember { mutableFloatStateOf(0f) }
    var renderCamY by remember { mutableFloatStateOf(0f) }
    var isCamInitialized by remember { mutableStateOf(false) }

    val enemyRenderPosMap = remember { mutableStateMapOf<String, Offset>() }

    val context = LocalContext.current
    val glRenderer = remember(context) { GlIsoRenderer(context.assets) }

    LaunchedEffect(player.pos.x, player.pos.y, viewModel.currentZLevel, viewModel.gbcGraphicsSettings) {
        var lastNanos = System.nanoTime()
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos

                val targetPX = player.pos.x
                val targetPY = player.pos.y
                if (abs(targetPX - renderPlayerX) > 6f || abs(targetPY - renderPlayerY) > 6f) {
                    renderPlayerX = targetPX
                    renderPlayerY = targetPY
                } else {
                    renderPlayerX += (targetPX - renderPlayerX) * (18f * dt)
                    renderPlayerY += (targetPY - renderPlayerY) * (18f * dt)
                }

                for (enemy in enemies) {
                    val currentPos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                    val targetX = enemy.pos.x
                    val targetY = enemy.pos.y
                    val dx = targetX - currentPos.x
                    val dy = targetY - currentPos.y
                    if (abs(dx) > 0.01f || abs(dy) > 0.01f) {
                        if (abs(dx) > 6f || abs(dy) > 6f) {
                            enemyRenderPosMap[enemy.id] = Offset(targetX, targetY)
                        } else {
                            val newX = currentPos.x + dx * (12f * dt)
                            val newY = currentPos.y + dy * (12f * dt)
                            enemyRenderPosMap[enemy.id] = Offset(newX, newY)
                        }
                    }
                }

                val pIso = toIso(renderPlayerX, renderPlayerY, viewModel.currentZLevel.toFloat())
                if (!isCamInitialized) {
                    renderCamX = pIso.x
                    renderCamY = pIso.y
                    isCamInitialized = true
                } else {
                    renderCamX += (pIso.x - renderCamX) * (10f * dt)
                    renderCamY += (pIso.y - renderCamY) * (10f * dt)
                }

                // Push frame parameters to OpenGL ES 3.0 renderer
                glRenderer.currentZLevel = viewModel.currentZLevel
                glRenderer.levelMap = levelMap
                glRenderer.gameLevels = viewModel.gameLevels
                glRenderer.player = player
                glRenderer.renderPlayerX = renderPlayerX
                glRenderer.renderPlayerY = renderPlayerY
                glRenderer.enemies = enemies
                glRenderer.civilians = civilians
                glRenderer.enemyRenderPosMap = enemyRenderPosMap
                glRenderer.noiseRipples = noiseRipples
                glRenderer.projectiles = projectiles
                glRenderer.exploredArray = exploredArray
                glRenderer.isTacticalOverlayActive = viewModel.isTacticalOverlayActive
                glRenderer.lastMoveX = viewModel.lastMoveX
                glRenderer.lastMoveY = viewModel.lastMoveY
                glRenderer.tileWidth = tileWidth
                glRenderer.zHeightOffset = zHeightOffset
                glRenderer.renderCamX = renderCamX
                glRenderer.renderCamY = renderCamY
                glRenderer.isLowSpecMode = viewModel.isLowSpecPerformanceMode
                glRenderer.gbcSettings = viewModel.gbcGraphicsSettings
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
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

        // Custom Isometric Grid Coordinate Rendering and Touch Navigation Composable
        if (viewModel.isIsoCoordinateGridActive) {
            IsometricGridNavigationCanvas(
                viewModel = viewModel,
                tileWidth = tileWidth,
                zHeightOffset = zHeightOffset,
                renderCamX = renderCamX,
                renderCamY = renderCamY,
                onDirectMovementVector = onMovePlayer
            )
        }

        // Grid-based action & movement range overlay with enemy threat zones
        if (viewModel.isTacticalOverlayActive || viewModel.activeGridAction != GridActionType.NONE) {
            IsoActionGridOverlay(
                viewModel = viewModel,
                tileWidth = tileWidth,
                zHeightOffset = zHeightOffset,
                renderCamX = renderCamX,
                renderCamY = renderCamY
            )
        }
    }
}

