package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Touch and Navigation modes for the isometric environment.
 */
enum class IsoTouchNavigationMode(val label: String, val description: String) {
    TACTICAL_HYBRID("HYBRID MATRIX", "Tap to set destination waypoint; Drag for direct real-time movement"),
    TAP_TO_MOVE("TAP WAYPOINT", "Tap on isometric tile coordinates to navigate with path prediction"),
    DIRECT_DRAG("DIRECT JOYSTICK", "Drag anywhere on isometric plane for continuous analog hack-and-slash movement"),
    ATTACK_TARGETING("ATTACK RETICLE", "Tap on grid coordinates or hostiles to aim and execute strikes")
}

/**
 * Mathematical projection engine for bidirectional conversion between
 * 3D/2.5D World Coordinates (worldX, worldY, zLevel, elevation) and Screen Pixel Coordinates (screenX, screenY).
 */
object IsoProjectionEngine {

    /**
     * Converts world floating-point coordinates into screen pixel offset.
     */
    fun worldToScreen(
        worldX: Float,
        worldY: Float,
        zLevel: Float,
        elevationHeight: Float = 0f,
        centerOffsetX: Float,
        centerOffsetY: Float,
        tileHalfWidth: Float,
        tileHalfHeight: Float,
        zHeightOffset: Float = 50f,
        elevationScale: Float = 40f
    ): Offset {
        val screenX = (worldX - worldY) * tileHalfWidth
        val screenY = (worldX + worldY) * tileHalfHeight - (zLevel * zHeightOffset) - (elevationHeight * elevationScale)
        return Offset(centerOffsetX + screenX, centerOffsetY + screenY)
    }

    /**
     * Converts a 2D screen touch point into isometric world space coordinates.
     * Takes into account current camera pan and active Z-layer elevation.
     */
    fun screenToWorld(
        touchOffset: Offset,
        centerOffsetX: Float,
        centerOffsetY: Float,
        currentZ: Int,
        tileHalfWidth: Float,
        tileHalfHeight: Float,
        zHeightOffset: Float = 50f,
        elevationHeight: Float = 0f,
        elevationScale: Float = 40f
    ): Point3D {
        val relX = touchOffset.x - centerOffsetX
        val relY = touchOffset.y - centerOffsetY + (currentZ * zHeightOffset) + (elevationHeight * elevationScale)

        val worldX = (relX / tileHalfWidth + relY / tileHalfHeight) / 2f
        val worldY = (relY / tileHalfHeight - relX / tileHalfWidth) / 2f

        return Point3D(worldX, worldY, currentZ.toFloat())
    }

    /**
     * Converts a 2D screen drag vector (deltaX, deltaY) into an isometric world movement velocity (dx, dy).
     * In an isometric view:
     * - Dragging Screen UP (-Y) translates to isometric (-X, -Y) [Moving North]
     * - Dragging Screen DOWN (+Y) translates to isometric (+X, +Y) [Moving South]
     * - Dragging Screen RIGHT (+X) translates to isometric (+X, -Y) [Moving East]
     * - Dragging Screen LEFT (-X) translates to isometric (-X, +Y) [Moving West]
     */
    fun screenDragToWorldVelocity(
        dragDelta: Offset,
        tileHalfWidth: Float,
        tileHalfHeight: Float,
        sensitivityMultiplier: Float = 1.0f
    ): Pair<Float, Float> {
        val normScreenX = dragDelta.x / (tileHalfWidth.coerceAtLeast(1f))
        val normScreenY = dragDelta.y / (tileHalfHeight.coerceAtLeast(1f))

        val worldDx = (normScreenX + normScreenY) * 0.5f * sensitivityMultiplier
        val worldDy = (normScreenY - normScreenX) * 0.5f * sensitivityMultiplier

        return Pair(worldDx, worldDy)
    }

    /**
     * Constructs a closed isometric diamond polygon path around a center screen point.
     */
    fun createDiamondPath(center: Offset, halfW: Float, halfH: Float): Path {
        return Path().apply {
            moveTo(center.x, center.y - halfH)
            lineTo(center.x + halfW, center.y)
            lineTo(center.x, center.y + halfH)
            lineTo(center.x - halfW, center.y)
            close()
        }
    }
}

/**
 * Custom Jetpack Compose Composable for Isometric Grid Coordinate Rendering & Touch Navigation.
 * Provides interactive coordinate projection, touch input translation, path prediction splines,
 * multi-level elevation indicators, and combat targeting reticles.
 */
@Composable
fun IsometricGridNavigationCanvas(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    tileWidth: Float = 72f,
    zHeightOffset: Float = 50f,
    renderCamX: Float = 0f,
    renderCamY: Float = 0f,
    onNavigateWaypoint: ((GridPos) -> Unit)? = null,
    onDirectMovementVector: ((Float, Float) -> Unit)? = null,
    onTargetLock: ((Enemy?) -> Unit)? = null
) {
    val player = viewModel.player
    val enemies = viewModel.enemies
    val levelMap = viewModel.gameLevels[viewModel.currentZLevel] ?: return
    val currentZ = viewModel.currentZLevel

    val halfW = tileWidth / 2f
    val halfH = (tileWidth / 1.8f) / 2f

    // Touch & Navigation settings
    var navMode by remember { mutableStateOf(IsoTouchNavigationMode.TACTICAL_HYBRID) }
    var showGridLines by remember { mutableStateOf(true) }
    var showCoordinates by remember { mutableStateOf(false) }
    var showElevationContours by remember { mutableStateOf(true) }
    var showPathPrediction by remember { mutableStateOf(true) }

    // Interactive Hover & Waypoint state
    var hoveredGridPos by remember { mutableStateOf<GridPos?>(null) }
    var activeWaypoint by remember { mutableStateOf<GridPos?>(null) }
    var touchRippleOrigin by remember { mutableStateOf<Offset?>(null) }
    var activeDragOrigin by remember { mutableStateOf<Offset?>(null) }
    var activeDragCurrent by remember { mutableStateOf<Offset?>(null) }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "isoNavGridAnim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iso_pulse_alpha"
    )

    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iso_flow_offset"
    )

    // Animated ripple expansion
    var rippleTrigger by remember { mutableLongStateOf(0L) }
    val rippleProgress = remember { Animatable(0f) }
    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0) {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            )
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("isometric_grid_navigation_container")
    ) {
        // =========================================================================
        // 1. ISOMETRIC CANVAS GRID & TOUCH LAYER
        // =========================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("isometric_grid_canvas")
                .pointerInput(levelMap, player.pos, navMode, currentZ) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val centerOffsetX = size.width / 2f - renderCamX
                            val centerOffsetY = size.height / 2f - renderCamY

                            val worldPoint = IsoProjectionEngine.screenToWorld(
                                touchOffset = tapOffset,
                                centerOffsetX = centerOffsetX,
                                centerOffsetY = centerOffsetY,
                                currentZ = currentZ,
                                tileHalfWidth = halfW,
                                tileHalfHeight = halfH,
                                zHeightOffset = zHeightOffset
                            )

                            val gridX = worldPoint.x.toInt()
                            val gridY = worldPoint.y.toInt()

                            if (gridX in 0 until levelMap.width && gridY in 0 until levelMap.height) {
                                val targetTile = GridPos(gridX, gridY, currentZ)
                                hoveredGridPos = targetTile
                                activeWaypoint = targetTile
                                touchRippleOrigin = tapOffset
                                rippleTrigger = System.currentTimeMillis()

                                SoundManager.playMenuClick()

                                // Check if enemy tapped
                                val tappedEnemy = enemies.firstOrNull {
                                    it.pos.z.toInt() == currentZ && !it.isDead &&
                                    abs(it.pos.x - worldPoint.x) < 1.0f && abs(it.pos.y - worldPoint.y) < 1.0f
                                }

                                if (tappedEnemy != null) {
                                    onTargetLock?.invoke(tappedEnemy)
                                    viewModel.logToConsole("TARGET ACQUIRED: ${tappedEnemy.name} [(${tappedEnemy.pos.x.toInt()}, ${tappedEnemy.pos.y.toInt()})]")
                                    if (navMode == IsoTouchNavigationMode.ATTACK_TARGETING) {
                                        viewModel.executeAttack()
                                    }
                                } else {
                                    onNavigateWaypoint?.invoke(targetTile)
                                    if (navMode == IsoTouchNavigationMode.TAP_TO_MOVE || navMode == IsoTouchNavigationMode.TACTICAL_HYBRID) {
                                        val dx = (gridX + 0.5f) - player.pos.x
                                        val dy = (gridY + 0.5f) - player.pos.y
                                        viewModel.movePlayer(dx, dy)
                                    }
                                }
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            val centerOffsetX = size.width / 2f - renderCamX
                            val centerOffsetY = size.height / 2f - renderCamY

                            val worldPoint = IsoProjectionEngine.screenToWorld(
                                touchOffset = tapOffset,
                                centerOffsetX = centerOffsetX,
                                centerOffsetY = centerOffsetY,
                                currentZ = currentZ,
                                tileHalfWidth = halfW,
                                tileHalfHeight = halfH,
                                zHeightOffset = zHeightOffset
                            )

                            // Double tap triggers tactical blink dash towards coordinate
                            val dx = worldPoint.x - player.pos.x
                            val dy = worldPoint.y - player.pos.y
                            viewModel.dash()
                            viewModel.movePlayer(dx * 1.5f, dy * 1.5f)
                            touchRippleOrigin = tapOffset
                            rippleTrigger = System.currentTimeMillis()
                            viewModel.logToConsole("TACTICAL DASH DIRECTED TO ISOMETRIC COORD [${worldPoint.x.toInt()}, ${worldPoint.y.toInt()}]")
                        },
                        onLongPress = { tapOffset ->
                            val centerOffsetX = size.width / 2f - renderCamX
                            val centerOffsetY = size.height / 2f - renderCamY

                            val worldPoint = IsoProjectionEngine.screenToWorld(
                                touchOffset = tapOffset,
                                centerOffsetX = centerOffsetX,
                                centerOffsetY = centerOffsetY,
                                currentZ = currentZ,
                                tileHalfWidth = halfW,
                                tileHalfHeight = halfH,
                                zHeightOffset = zHeightOffset
                            )
                            val targetPos = GridPos(worldPoint.x.toInt(), worldPoint.y.toInt(), currentZ)
                            hoveredGridPos = targetPos
                            viewModel.setHoveredTile(targetPos)
                            SoundManager.playSkillActivation("LOCK_ON")
                            viewModel.logToConsole("HEAVY STRIKE AIM LOCKED: (${targetPos.x}, ${targetPos.y})")
                        }
                    )
                }
                .pointerInput(navMode, currentZ) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            activeDragOrigin = offset
                            activeDragCurrent = offset
                        },
                        onDragEnd = {
                            activeDragOrigin = null
                            activeDragCurrent = null
                        },
                        onDragCancel = {
                            activeDragOrigin = null
                            activeDragCurrent = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            activeDragCurrent = (activeDragCurrent ?: change.position) + dragAmount

                            val origin = activeDragOrigin ?: change.position
                            val current = activeDragCurrent ?: change.position
                            val dragDelta = current - origin

                            if (dragDelta.getDistance() > 12f) {
                                // Convert 2D screen drag directly to Isometric World Velocity
                                val (worldDx, worldDy) = IsoProjectionEngine.screenDragToWorldVelocity(
                                    dragDelta = dragDelta,
                                    tileHalfWidth = halfW,
                                    tileHalfHeight = halfH,
                                    sensitivityMultiplier = 1.25f
                                )

                                onDirectMovementVector?.invoke(worldDx, worldDy)
                                viewModel.movePlayer(worldDx.coerceIn(-1.5f, 1.5f), worldDy.coerceIn(-1.5f, 1.5f))
                            }
                        }
                    )
                }
        ) {
            val centerOffsetX = size.width / 2f - renderCamX
            val centerOffsetY = size.height / 2f - renderCamY

            fun toIso(x: Float, y: Float, z: Float = currentZ.toFloat(), elev: Float = 0f): Offset {
                return IsoProjectionEngine.worldToScreen(
                    worldX = x,
                    worldY = y,
                    zLevel = z,
                    elevationHeight = elev,
                    centerOffsetX = centerOffsetX,
                    centerOffsetY = centerOffsetY,
                    tileHalfWidth = halfW,
                    tileHalfHeight = halfH,
                    zHeightOffset = zHeightOffset
                )
            }

            val strokeDash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), flowOffset)

            // -------------------------------------------------------------------------
            // A. ISOMETRIC TILE GRID WIREFRAME & COORDINATE LABELS
            // -------------------------------------------------------------------------
            if (showGridLines) {
                val pX = player.pos.x.toInt()
                val pY = player.pos.y.toInt()
                val gridRadius = 10

                val minX = (pX - gridRadius).coerceAtLeast(0)
                val maxX = (pX + gridRadius).coerceAtMost(levelMap.width - 1)
                val minY = (pY - gridRadius).coerceAtLeast(0)
                val maxY = (pY + gridRadius).coerceAtMost(levelMap.height - 1)

                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        val tile = levelMap.getTile(x, y)
                        val tileCenter = toIso(x.toFloat() + 0.5f, y.toFloat() + 0.5f)

                        // Skip drawing offscreen tiles
                        if (tileCenter.x < -100f || tileCenter.x > size.width + 100f ||
                            tileCenter.y < -100f || tileCenter.y > size.height + 100f) {
                            continue
                        }

                        val diamondPath = IsoProjectionEngine.createDiamondPath(tileCenter, halfW, halfH)

                        // Subtle tile outline based on walkability and terrain type
                        val lineColor = when {
                            !tile.isWalkable -> Color(0x33FF0055)
                            tile == TileType.TERMINAL -> CyberNeonCyan.copy(alpha = 0.35f)
                            tile == TileType.LADDER_UP || tile == TileType.LADDER_DOWN -> ImmersiveAmber.copy(alpha = 0.4f)
                            (x + y) % 4 == 0 -> Color(0x2200FFCC)
                            else -> Color(0x12FFFFFF)
                        }

                        drawPath(
                            path = diamondPath,
                            color = lineColor,
                            style = Stroke(width = if (tile == TileType.TERMINAL) 1.5.dp.toPx() else 0.8.dp.toPx())
                        )

                        // Elevation contour accent
                        if (showElevationContours && (x % 5 == 0 && y % 5 == 0)) {
                            drawCircle(
                                color = ImmersiveLavender.copy(alpha = 0.3f),
                                radius = 2.dp.toPx(),
                                center = tileCenter
                            )
                        }

                        // Optional Coordinate text readout on grid intersections
                        if (showCoordinates && x % 2 == 0 && y % 2 == 0) {
                            drawText(
                                textMeasurer = textMeasurer,
                                text = "$x,$y",
                                topLeft = Offset(tileCenter.x - 10.dp.toPx(), tileCenter.y - 6.dp.toPx()),
                                style = TextStyle(
                                    color = Color(0x5500FFCC),
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }

                // Coordinate Origin Axes (+X Cyan, +Y Magenta)
                val originIso = toIso(0.5f, 0.5f)
                val axisXEnd = toIso(4.5f, 0.5f)
                val axisYEnd = toIso(0.5f, 4.5f)

                // +X Axis line
                drawLine(
                    color = CyberNeonCyan.copy(alpha = 0.7f),
                    start = originIso,
                    end = axisXEnd,
                    strokeWidth = 2.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "+X (EAST)",
                    topLeft = Offset(axisXEnd.x + 4.dp.toPx(), axisXEnd.y - 8.dp.toPx()),
                    style = TextStyle(color = CyberNeonCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )

                // +Y Axis line
                drawLine(
                    color = CyberNeonMagenta.copy(alpha = 0.7f),
                    start = originIso,
                    end = axisYEnd,
                    strokeWidth = 2.dp.toPx()
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "+Y (SOUTH)",
                    topLeft = Offset(axisYEnd.x - 40.dp.toPx(), axisYEnd.y + 4.dp.toPx()),
                    style = TextStyle(color = CyberNeonMagenta, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )
            }

            // -------------------------------------------------------------------------
            // B. ACTIVE PLAYER ISOMETRIC HEADING & POSITION VECTOR
            // -------------------------------------------------------------------------
            val playerIso = toIso(player.pos.x, player.pos.y)
            
            // Player orientation pointer on isometric plane
            val headingLength = 1.4f
            val headingEnd = toIso(
                player.pos.x + viewModel.lastMoveX * headingLength,
                player.pos.y + viewModel.lastMoveY * headingLength
            )

            // Dynamic direction arrow
            drawLine(
                color = ImmersiveGreen.copy(alpha = 0.85f),
                start = playerIso,
                end = headingEnd,
                strokeWidth = 2.5.dp.toPx()
            )
            drawCircle(
                color = ImmersiveGreen,
                radius = 3.5.dp.toPx(),
                center = headingEnd
            )

            // -------------------------------------------------------------------------
            // C. HOVERED / SELECTED WAYPOINT TILE RETICLE & TRAVERSAL VECTOR
            // -------------------------------------------------------------------------
            val waypoint = activeWaypoint ?: hoveredGridPos
            if (waypoint != null && showPathPrediction) {
                val wpCenterIso = toIso(waypoint.x.toFloat() + 0.5f, waypoint.y.toFloat() + 0.5f)
                val diamondWp = IsoProjectionEngine.createDiamondPath(wpCenterIso, halfW, halfH)

                // Diamond highlight
                drawPath(
                    path = diamondWp,
                    color = CyberNeonCyan.copy(alpha = 0.25f * pulseAlpha)
                )
                drawPath(
                    path = diamondWp,
                    color = CyberNeonCyan,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = strokeDash)
                )

                // Traversal Navigation Spline from Player to Waypoint
                drawLine(
                    color = CyberNeonCyan.copy(alpha = 0.85f),
                    start = playerIso,
                    end = wpCenterIso,
                    strokeWidth = 2.5.dp.toPx(),
                    pathEffect = strokeDash
                )

                // Intermediate Navigation Nodes
                val distMeters = sqrt(
                    (waypoint.x + 0.5f - player.pos.x).pow(2) + (waypoint.y + 0.5f - player.pos.y).pow(2)
                )
                val nodeCount = (distMeters).toInt().coerceIn(1, 6)
                for (i in 1 until nodeCount) {
                    val frac = i.toFloat() / nodeCount.toFloat()
                    val interX = player.pos.x + (waypoint.x + 0.5f - player.pos.x) * frac
                    val interY = player.pos.y + (waypoint.y + 0.5f - player.pos.y) * frac
                    val nodeIso = toIso(interX, interY)

                    drawCircle(
                        color = CyberNeonCyan.copy(alpha = 0.7f),
                        radius = 2.5.dp.toPx(),
                        center = nodeIso
                    )
                }

                // Coordinate & Distance HUD Badge over Waypoint
                val estSeconds = distMeters / player.getSpeed().coerceAtLeast(1.0f)
                val badgeText = "GRID [${waypoint.x}, ${waypoint.y}] | ${"%.1f".format(distMeters)}m (${"%.1f".format(estSeconds)}s)"

                drawText(
                    textMeasurer = textMeasurer,
                    text = badgeText,
                    topLeft = Offset(wpCenterIso.x - 36.dp.toPx(), wpCenterIso.y - halfH - 20.dp.toPx()),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        background = ImmersiveBgDark.copy(alpha = 0.9f)
                    )
                )
            }

            // -------------------------------------------------------------------------
            // D. TOUCH RIPPLE EFFECT ON ISOMETRIC PLANE
            // -------------------------------------------------------------------------
            touchRippleOrigin?.let { origin ->
                val progress = rippleProgress.value
                if (progress in 0.01f..0.99f) {
                    val maxRadius = 45.dp.toPx()
                    val currentRadius = maxRadius * progress
                    val alpha = (1f - progress) * 0.8f

                    drawCircle(
                        color = CyberNeonCyan.copy(alpha = alpha),
                        radius = currentRadius,
                        center = origin,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = CyberNeonMagenta.copy(alpha = alpha * 0.5f),
                        radius = currentRadius * 0.6f,
                        center = origin,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            // -------------------------------------------------------------------------
            // E. DIRECT DRAG VIRTUAL JOYSTICK RETICLE (WHEN DRAGGING)
            // -------------------------------------------------------------------------
            if (activeDragOrigin != null && activeDragCurrent != null) {
                val origin = activeDragOrigin!!
                val current = activeDragCurrent!!
                val dragVector = current - origin
                val distance = dragVector.getDistance()

                // Base joystick circle
                drawCircle(
                    color = ImmersiveBgDark.copy(alpha = 0.7f),
                    radius = 45.dp.toPx(),
                    center = origin
                )
                drawCircle(
                    color = CyberNeonCyan.copy(alpha = 0.6f),
                    radius = 45.dp.toPx(),
                    center = origin,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Dragged thumb knob position clamped to radius
                val maxDragRadius = 45.dp.toPx()
                val clampedOffset = if (distance > maxDragRadius) {
                    origin + (dragVector / distance) * maxDragRadius
                } else {
                    current
                }

                // Vector stem line
                drawLine(
                    color = CyberNeonCyan.copy(alpha = 0.85f),
                    start = origin,
                    end = clampedOffset,
                    strokeWidth = 3.dp.toPx()
                )

                // Thumb knob circle
                drawCircle(
                    color = CyberNeonCyan,
                    radius = 12.dp.toPx(),
                    center = clampedOffset
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = clampedOffset
                )
            }
        }

        // =========================================================================
        // 2. TOP TACTICAL NAVIGATION MATRIX BAR
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ImmersiveBgHeader.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.6f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp)
                .testTag("iso_nav_matrix_header")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Coordinate live readout
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(ImmersiveGreen, CircleShape))
                        Text(
                            text = "POS: (${"%.1f".format(player.pos.x)}, ${"%.1f".format(player.pos.y)}) Z=$currentZ",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "SPEED: ${"%.1f".format(player.getSpeed())}x | HEADING: ${"%.0f".format(atan2(viewModel.lastMoveY, viewModel.lastMoveX) * 180 / PI)}°",
                        color = ImmersiveSlateMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                VerticalDivider(modifier = Modifier.height(20.dp), color = Color.White.copy(alpha = 0.2f))

                // Navigation Mode Switcher Dropdown / Toggle Button
                Button(
                    onClick = {
                        val nextOrdinal = (navMode.ordinal + 1) % IsoTouchNavigationMode.values().size
                        navMode = IsoTouchNavigationMode.values()[nextOrdinal]
                        SoundManager.playMenuClick()
                        viewModel.logToConsole("TOUCH MODE SWITCHED: ${navMode.label}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgDark),
                    border = BorderStroke(1.dp, CyberNeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("iso_mode_switch_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = when (navMode) {
                                IsoTouchNavigationMode.TACTICAL_HYBRID -> Icons.Default.SwapCalls
                                IsoTouchNavigationMode.TAP_TO_MOVE -> Icons.AutoMirrored.Filled.DirectionsRun
                                IsoTouchNavigationMode.DIRECT_DRAG -> Icons.Default.GpsFixed
                                IsoTouchNavigationMode.ATTACK_TARGETING -> Icons.Default.FlashOn
                            },
                            contentDescription = "Switch Nav Mode",
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = navMode.label,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberNeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Grid Lines Toggle
                IconButton(
                    onClick = { showGridLines = !showGridLines },
                    modifier = Modifier.size(28.dp).background(if (showGridLines) CyberDarkCyan else ImmersiveBgDark, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = "Toggle Grid Lines",
                        tint = if (showGridLines) CyberNeonCyan else ImmersiveSlateMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Coordinate Numbers Toggle
                IconButton(
                    onClick = { showCoordinates = !showCoordinates },
                    modifier = Modifier.size(28.dp).background(if (showCoordinates) CyberDarkMagenta else ImmersiveBgDark, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Toggle Coordinate Labels",
                        tint = if (showCoordinates) CyberNeonMagenta else ImmersiveSlateMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 3. BOTTOM TOUCH ACTION INFO CARD
        // =========================================================================
        hoveredGridPos?.let { pos ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ImmersiveBgDark.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, CyberNeonCyan),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 85.dp)
                    .testTag("hovered_coordinate_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text(
                            text = "GRID COORDINATE: (${pos.x}, ${pos.y}) [Z:${pos.z}]",
                            color = CyberNeonCyan,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val tile = levelMap.getTile(pos.x, pos.y)
                        Text(
                            text = "TERRAIN: ${tile.name} | ${if (tile.isWalkable) "WALKABLE" else "OBSTACLE"}",
                            color = if (tile.isWalkable) ImmersiveGreen else CyberNeonRed,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = {
                            val dx = (pos.x + 0.5f) - player.pos.x
                            val dy = (pos.y + 0.5f) - player.pos.y
                            viewModel.movePlayer(dx, dy)
                            hoveredGridPos = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("STEP", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
