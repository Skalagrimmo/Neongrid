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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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

enum class GridActionType(val displayName: String, val description: String) {
    NONE("TACTICAL OFF", "Standard real-time stealth combat mode"),
    MOVE("MOVE RANGE", "Highlights walkable movement tiles & path distance"),
    ATTACK("ATTACK TARGET", "Weapon targeting range & line of fire"),
    HACK("SYSTEM HACK", "Remote terminal Uplink & Explosive Barrel hack radius"),
    SKILL("SKILL EXECUTE", "Active operative directive targeting zone")
}

enum class ThreatLevel {
    ALERTED,    // Red hazard - Hostile actively targeting
    SUSPICIOUS, // Amber warning - Hostile investigating
    PATROL      // Yellow-Orange scan cone - Standard patrol vision
}

data class TileThreat(
    val level: ThreatLevel,
    val enemyName: String,
    val distance: Float
)

/**
 * Custom Jetpack Compose Overlay that renders movement ranges, enemy threat zones,
 * vector lines, and action initiation reticles on the isometric grid.
 */
@Composable
fun IsoActionGridOverlay(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    tileWidth: Float = 72f,
    zHeightOffset: Float = 50f,
    renderCamX: Float = 0f,
    renderCamY: Float = 0f
) {
    val player = viewModel.player
    val enemies = viewModel.enemies
    val levelMap = viewModel.gameLevels[viewModel.currentZLevel] ?: return
    val currentZ = viewModel.currentZLevel

    val activeAction = viewModel.activeGridAction
    val isTacticalActive = viewModel.isTacticalOverlayActive || activeAction != GridActionType.NONE
    val showThreat = viewModel.isThreatZoneOverlayVisible && isTacticalActive
    val showVisionCones = viewModel.isVisionConeOverlayVisible && isTacticalActive
    val showMove = viewModel.isMovementRangeOverlayVisible && isTacticalActive

    var selectedTarget by remember { mutableStateOf<GridPos?>(viewModel.hoveredTargetTile) }
    
    // Sync external hovered state
    LaunchedEffect(viewModel.hoveredTargetTile) {
        selectedTarget = viewModel.hoveredTargetTile
    }

    // --- Animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "gridActionPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dashOffset"
    )

    // --- Calculate Reachable Movement Tiles (BFS) ---
    val maxMoveRange = remember(player.energy, player.isSneaking, player.equippedSystem) {
        val baseRange = if (player.isSneaking) 5 else 7
        val bonus = if (player.equippedSystem.id == "dash_boosters") 2 else 0
        (baseRange + bonus).coerceIn(3, 12)
    }

    val reachableTiles = remember(player.pos.x.toInt(), player.pos.y.toInt(), currentZ, levelMap) {
        val px = player.pos.x.toInt()
        val py = player.pos.y.toInt()
        calculateReachableTiles(levelMap, px, py, maxMoveRange)
    }

    // --- Calculate Enemy Threat Zones ---
    val threatZones = remember(enemies, currentZ, levelMap, viewModel.gameTick) {
        calculateThreatZones(enemies, levelMap, currentZ)
    }

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("iso_action_grid_overlay")
    ) {
        // =========================================================================
        // 1. ISOMETRIC CANVAS GRID OVERLAY
        // =========================================================================
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("iso_action_canvas")
                .pointerInput(levelMap, player.pos, activeAction) {
                    detectTapGestures { tapOffset ->
                        val halfW = tileWidth / 2f
                        val halfH = (tileWidth / 1.8f) / 2f
                        val centerOffsetX = size.width / 2f - renderCamX
                        val centerOffsetY = size.height / 2f - renderCamY

                        // Reverse Iso projection to find clicked tile (x, y)
                        val relX = tapOffset.x - centerOffsetX
                        val relY = tapOffset.y - centerOffsetY + currentZ * zHeightOffset

                        val isoX = (relX / halfW + relY / halfH) / 2f
                        val isoY = (relY / halfH - relX / halfW) / 2f

                        val clickedGridX = isoX.toInt()
                        val clickedGridY = isoY.toInt()

                        if (clickedGridX in 0 until levelMap.width && clickedGridY in 0 until levelMap.height) {
                            val pos = GridPos(clickedGridX, clickedGridY, currentZ)
                            selectedTarget = pos
                            viewModel.setHoveredTile(pos)
                            AudioManager.playInteract()
                        }
                    }
                }
        ) {
            val halfW = tileWidth / 2f
            val halfH = (tileWidth / 1.8f) / 2f
            val centerOffsetX = size.width / 2f - renderCamX
            val centerOffsetY = size.height / 2f - renderCamY

            fun toIso(x: Float, y: Float, z: Float): Offset {
                val sx = (x - y) * halfW
                val sy = (x + y) * halfH - z * zHeightOffset
                return Offset(centerOffsetX + sx, centerOffsetY + sy)
            }

            fun createDiamondPath(center: Offset): Path {
                return Path().apply {
                    moveTo(center.x, center.y - halfH)
                    lineTo(center.x + halfW, center.y)
                    lineTo(center.x, center.y + halfH)
                    lineTo(center.x - halfW, center.y)
                    close()
                }
            }

            val strokeDash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), dashOffset)

            // A. Draw Reachable Movement Tiles (Cyan Overlay)
            if (showMove) {
                for ((pos, dist) in reachableTiles) {
                    val isoCenter = toIso(pos.x.toFloat() + 0.5f, pos.y.toFloat() + 0.5f, currentZ.toFloat())
                    val diamond = createDiamondPath(isoCenter)

                    val isBorder = dist == maxMoveRange || dist == 1
                    val fillColor = if (activeAction == GridActionType.MOVE) {
                        CyberNeonCyan.copy(alpha = 0.22f + 0.08f * sin(dashOffset * 0.1f + dist).toFloat())
                    } else {
                        CyberNeonCyan.copy(alpha = 0.14f)
                    }

                    drawPath(path = diamond, color = fillColor)

                    drawPath(
                        path = diamond,
                        color = CyberNeonCyan.copy(alpha = 0.65f),
                        style = Stroke(width = if (isBorder) 2.dp.toPx() else 1.dp.toPx())
                    )

                    // Step marker dot for key steps
                    if (dist > 0 && dist % 2 == 0) {
                        drawCircle(
                            color = CyberNeonCyan.copy(alpha = 0.7f),
                            radius = 2.5.dp.toPx(),
                            center = isoCenter
                        )
                    }
                }
            }

            // B. Draw Enemy Threat Zones (Red / Amber Hazard Overlay)
            if (showThreat) {
                for ((pos, threat) in threatZones) {
                    val isoCenter = toIso(pos.x.toFloat() + 0.5f, pos.y.toFloat() + 0.5f, currentZ.toFloat())
                    val diamond = createDiamondPath(isoCenter)

                    val baseColor = when (threat.level) {
                        ThreatLevel.ALERTED -> CyberNeonRed
                        ThreatLevel.SUSPICIOUS -> CyberNeonAmber
                        ThreatLevel.PATROL -> Color(0xFFFF9900)
                    }

                    val alpha = when (threat.level) {
                        ThreatLevel.ALERTED -> 0.32f * pulseAlpha
                        ThreatLevel.SUSPICIOUS -> 0.22f
                        ThreatLevel.PATROL -> 0.15f
                    }

                    drawPath(path = diamond, color = baseColor.copy(alpha = alpha))

                    drawPath(
                        path = diamond,
                        color = baseColor.copy(alpha = 0.8f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = strokeDash
                        )
                    )

                    // Draw danger cross-hatch accent for alerted threat tiles
                    if (threat.level == ThreatLevel.ALERTED) {
                        drawLine(
                            color = CyberNeonRed.copy(alpha = 0.4f),
                            start = Offset(isoCenter.x - halfW * 0.4f, isoCenter.y),
                            end = Offset(isoCenter.x + halfW * 0.4f, isoCenter.y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // B2. Draw Dynamic Isometric Enemy Vision Cones (Rotation-Based FOV)
            if (showVisionCones) {
                val activeEnemies = enemies.filter { it.pos.z.toInt() == currentZ && !it.isDead }
                for (enemy in activeEnemies) {
                    val ex = enemy.pos.x
                    val ey = enemy.pos.y
                    val enemyIso = toIso(ex, ey, currentZ.toFloat())

                    val fovAngleRad = when (enemy.alertState) {
                        AlertState.ALERTED -> (2.0 * PI).toFloat()
                        AlertState.SUSPICIOUS -> (120.0 * PI / 180.0).toFloat()
                        AlertState.PATROLLING -> (90.0 * PI / 180.0).toFloat()
                    }

                    val rangeTiles = if (enemy.alertState == AlertState.ALERTED) 9.0f else 6.5f
                    val halfFov = fovAngleRad / 2.0f

                    val coneColor = when (enemy.alertState) {
                        AlertState.ALERTED -> CyberNeonRed
                        AlertState.SUSPICIOUS -> CyberNeonAmber
                        AlertState.PATROLLING -> Color(0xFF00FF88)
                    }

                    // Build field-of-view path projected into isometric space
                    val steps = 18
                    val conePath = Path().apply {
                        moveTo(enemyIso.x, enemyIso.y)
                        val startAngle = enemy.directionAngle - halfFov
                        val angleStep = fovAngleRad / steps

                        for (i in 0..steps) {
                            val a = startAngle + i * angleStep
                            val px = ex + rangeTiles * cos(a)
                            val py = ey + rangeTiles * sin(a)
                            val ptIso = toIso(px, py, currentZ.toFloat())
                            lineTo(ptIso.x, ptIso.y)
                        }
                        close()
                    }

                    // 1. Fill vision cone polygon with dynamic translucent highlight
                    val alphaFill = if (enemy.alertState == AlertState.ALERTED) 0.28f * pulseAlpha else 0.16f
                    drawPath(path = conePath, color = coneColor.copy(alpha = alphaFill))

                    // 2. Vision Cone Outer Boundary with path dashing
                    drawPath(
                        path = conePath,
                        color = coneColor.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = strokeDash
                        )
                    )

                    // 3. Dynamic Sweeping Radar Beam pivoting around enemy.directionAngle
                    val sweepOffset = sin(dashOffset * 0.12f).toFloat() * halfFov * 0.85f
                    val sweepAngle = enemy.directionAngle + sweepOffset
                    val sweepPx = ex + rangeTiles * cos(sweepAngle)
                    val sweepPy = ey + rangeTiles * sin(sweepAngle)
                    val sweepIso = toIso(sweepPx, sweepPy, currentZ.toFloat())

                    drawLine(
                        color = coneColor.copy(alpha = 0.95f),
                        start = enemyIso,
                        end = sweepIso,
                        strokeWidth = 2.dp.toPx()
                    )

                    // 4. Directional Orientation Sight Vector
                    val arrowLength = 1.8f
                    val arrowPx = ex + arrowLength * cos(enemy.directionAngle)
                    val arrowPy = ey + arrowLength * sin(enemy.directionAngle)
                    val arrowIso = toIso(arrowPx, arrowPy, currentZ.toFloat())

                    drawLine(
                        color = Color.White,
                        start = enemyIso,
                        end = arrowIso,
                        strokeWidth = 3.dp.toPx()
                    )
                    drawCircle(
                        color = coneColor,
                        radius = 4.dp.toPx(),
                        center = arrowIso
                    )

                    // 5. Enemy Vision Badge on Grid
                    val infoText = "${enemy.name} [FOV ${(fovAngleRad * 180 / PI).toInt()}°]"
                    drawText(
                        textMeasurer = textMeasurer,
                        text = infoText,
                        topLeft = Offset(enemyIso.x - 32.dp.toPx(), enemyIso.y - 28.dp.toPx()),
                        style = TextStyle(
                            color = coneColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            background = ImmersiveBgDark.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            // C. Draw Player Line of Sight / Action Target Vector Beam
            val playerIso = toIso(player.pos.x, player.pos.y, currentZ.toFloat())
            val target = selectedTarget

            if (target != null && isTacticalActive) {
                val targetIso = toIso(target.x.toFloat() + 0.5f, target.y.toFloat() + 0.5f, currentZ.toFloat())
                val isReachable = reachableTiles.containsKey(target)
                val isThreat = threatZones.containsKey(target)

                val lineColor = when {
                    activeAction == GridActionType.ATTACK -> CyberNeonMagenta
                    activeAction == GridActionType.HACK -> CyberNeonCyan
                    isThreat -> CyberNeonRed
                    isReachable -> CyberNeonCyan
                    else -> CyberNeonAmber
                }

                // Action vector beam line
                drawLine(
                    color = lineColor.copy(alpha = 0.85f),
                    start = playerIso,
                    end = targetIso,
                    strokeWidth = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), dashOffset)
                )

                // Target Diamond Highlight
                val targetDiamond = createDiamondPath(targetIso)
                drawPath(path = targetDiamond, color = lineColor.copy(alpha = 0.35f))
                drawPath(
                    path = targetDiamond,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Crosshair Reticle
                val r = halfW * 0.6f
                drawLine(lineColor, Offset(targetIso.x - r, targetIso.y), Offset(targetIso.x + r, targetIso.y), strokeWidth = 2.dp.toPx())
                drawLine(lineColor, Offset(targetIso.x, targetIso.y - r), Offset(targetIso.x, targetIso.y + r), strokeWidth = 2.dp.toPx())
                drawCircle(lineColor, radius = 5.dp.toPx(), center = targetIso, style = Stroke(width = 1.5.dp.toPx()))

                // Target Distance Badge
                val distMeters = sqrt(
                    (target.x - player.pos.x).pow(2) + (target.y - player.pos.y).pow(2)
                )
                val distText = "${"%.1f".format(distMeters)}m"
                drawText(
                    textMeasurer = textMeasurer,
                    text = distText,
                    topLeft = Offset(targetIso.x - 14.dp.toPx(), targetIso.y - halfH - 18.dp.toPx()),
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        background = ImmersiveBgDark.copy(alpha = 0.9f)
                    )
                )
            }
        }

        // =========================================================================
        // 2. TOP ACTION CONTROL BANNER & QUICK OVERLAY TOGGLES
        // =========================================================================
        AnimatedVisibility(
            visible = isTacticalActive,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ImmersiveBgHeader.copy(alpha = 0.94f),
                border = BorderStroke(1.5.dp, CyberNeonCyan.copy(alpha = 0.8f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("action_grid_header_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Action Mode Title Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (activeAction != GridActionType.NONE) CyberNeonMagenta else CyberNeonCyan, CircleShape)
                        )
                        Text(
                            text = activeAction.displayName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(16.dp), color = Color.White.copy(alpha = 0.2f))

                    // Movement Range Layer Toggle
                    FilterChip(
                        selected = showMove,
                        onClick = { viewModel.toggleMovementRangeOverlay() },
                        label = {
                            Text("MOVE RANGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsRun, contentDescription = "Toggle Move Range", modifier = Modifier.size(12.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberDarkCyan,
                            selectedLabelColor = CyberNeonCyan
                        ),
                        modifier = Modifier.testTag("movement_range_toggle")
                    )

                    // Threat Zone Layer Toggle
                    FilterChip(
                        selected = showThreat,
                        onClick = { viewModel.toggleThreatZoneOverlay() },
                        label = {
                            Text("THREAT ZONES", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = "Toggle Threat Zones", modifier = Modifier.size(12.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberDarkMagenta,
                            selectedLabelColor = CyberNeonMagenta
                        ),
                        modifier = Modifier.testTag("threat_zone_toggle")
                    )

                    // Vision Cones Layer Toggle
                    FilterChip(
                        selected = showVisionCones,
                        onClick = { viewModel.toggleVisionConeOverlay() },
                        label = {
                            Text("VISION CONES", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = "Toggle Vision Cones", modifier = Modifier.size(12.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberDarkPurple,
                            selectedLabelColor = CyberNeonPurple
                        ),
                        modifier = Modifier.testTag("vision_cone_toggle")
                    )
                }
            }
        }

        // =========================================================================
        // 3. BOTTOM FLOATING ACTION INITIATION TOOLBAR
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 78.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selected Target Tile Action Execute Card (if target clicked)
            selectedTarget?.let { target ->
                AnimatedVisibility(
                    visible = activeAction != GridActionType.NONE,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.95f)),
                        border = BorderStroke(1.5.dp, CyberNeonCyan),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .wrapContentWidth()
                            .testTag("target_execute_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "TARGET: (${target.x}, ${target.y}) Z=${target.z}",
                                    color = CyberNeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = when (activeAction) {
                                        GridActionType.MOVE -> "Reachable Step Tile"
                                        GridActionType.ATTACK -> "Melee / Ranged Line of Fire"
                                        GridActionType.HACK -> "Uplink Node Intercept"
                                        GridActionType.SKILL -> "Direct Operative Directive"
                                        else -> "Grid Sector Selected"
                                    },
                                    color = CyberTextMedium,
                                    fontSize = 9.5.sp
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.executeSelectedGridAction(target)
                                    selectedTarget = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan, contentColor = CyberBgVoid),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("action_execute_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Execute Action", modifier = Modifier.size(16.dp))
                                    Text("EXECUTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Action Selector Toolbar Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgDark.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, Color(0x3AFFFFFF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .wrapContentWidth()
                    .testTag("grid_action_toolbar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MOVE ACTION BUTTON
                    ActionButtonPill(
                        label = "MOVE",
                        icon = Icons.Default.DirectionsRun,
                        isActive = activeAction == GridActionType.MOVE,
                        activeColor = CyberNeonCyan,
                        onClick = { viewModel.setGridAction(GridActionType.MOVE) },
                        testTag = "action_move_pill"
                    )

                    // ATTACK ACTION BUTTON
                    ActionButtonPill(
                        label = "ATTACK",
                        icon = Icons.Default.GpsFixed,
                        isActive = activeAction == GridActionType.ATTACK,
                        activeColor = CyberNeonMagenta,
                        onClick = { viewModel.setGridAction(GridActionType.ATTACK) },
                        testTag = "action_attack_pill"
                    )

                    // HACK ACTION BUTTON
                    ActionButtonPill(
                        label = "HACK",
                        icon = Icons.Default.Terminal,
                        isActive = activeAction == GridActionType.HACK,
                        activeColor = CyberNeonCyan,
                        onClick = { viewModel.setGridAction(GridActionType.HACK) },
                        testTag = "action_hack_pill"
                    )

                    // SKILL ACTION BUTTON
                    ActionButtonPill(
                        label = "SKILL",
                        icon = Icons.Default.Bolt,
                        isActive = activeAction == GridActionType.SKILL,
                        activeColor = CyberNeonAmber,
                        onClick = { viewModel.setGridAction(GridActionType.SKILL) },
                        testTag = "action_skill_pill"
                    )

                    // CLEAR / CLOSE ACTION
                    if (isTacticalActive) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberNeonRed.copy(alpha = 0.2f))
                                .border(1.dp, CyberNeonRed, CircleShape)
                                .clickable {
                                    viewModel.clearAction()
                                    viewModel.toggleTacticalOverlay()
                                    selectedTarget = null
                                }
                                .testTag("action_cancel_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Action", tint = CyberNeonRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) activeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (isActive) activeColor else Color.White.copy(alpha = 0.2f)
        ),
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (isActive) activeColor else Color.White.copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// --- Helper Functions ---

private fun calculateReachableTiles(
    map: GameLevelMap,
    startX: Int,
    startY: Int,
    maxRange: Int
): Map<GridPos, Int> {
    val queue = ArrayDeque<Triple<Int, Int, Int>>()
    val visited = mutableMapOf<GridPos, Int>()
    val startPos = GridPos(startX, startY, map.zLevel)

    queue.add(Triple(startX, startY, 0))
    visited[startPos] = 0

    val directions = listOf(
        Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1),
        Pair(1, 1), Pair(-1, -1), Pair(1, -1), Pair(-1, 1)
    )

    while (queue.isNotEmpty()) {
        val (cx, cy, dist) = queue.removeFirst()
        if (dist >= maxRange) continue

        for ((dx, dy) in directions) {
            val nx = cx + dx
            val ny = cy + dy
            val nextDist = dist + 1

            if (nx in 0 until map.width && ny in 0 until map.height && nextDist <= maxRange) {
                val tile = map.getTile(nx, ny)
                if (tile.isWalkable) {
                    val pos = GridPos(nx, ny, map.zLevel)
                    if (!visited.containsKey(pos) || visited[pos]!! > nextDist) {
                        visited[pos] = nextDist
                        queue.add(Triple(nx, ny, nextDist))
                    }
                }
            }
        }
    }
    return visited
}

private fun calculateThreatZones(
    enemies: List<Enemy>,
    map: GameLevelMap,
    zLevel: Int
): Map<GridPos, TileThreat> {
    val threatMap = mutableMapOf<GridPos, TileThreat>()
    val activeEnemies = enemies.filter { it.pos.z.toInt() == zLevel && !it.isDead }

    for (enemy in activeEnemies) {
        val ex = enemy.pos.x.toInt()
        val ey = enemy.pos.y.toInt()
        val visionRange = if (enemy.alertState == AlertState.ALERTED) 9 else 6

        for (dx in -visionRange..visionRange) {
            for (dy in -visionRange..visionRange) {
                val tx = ex + dx
                val ty = ey + dy
                if (tx in 0 until map.width && ty in 0 until map.height) {
                    val dist = sqrt((dx * dx + dy * dy).toFloat())
                    if (dist <= visionRange) {
                        val tileAngle = atan2(dy.toFloat(), dx.toFloat())
                        var angleDiff = abs(tileAngle - enemy.directionAngle)
                        while (angleDiff > PI) angleDiff -= (2 * PI).toFloat()
                        angleDiff = abs(angleDiff)

                        val inCone = enemy.alertState == AlertState.ALERTED || angleDiff <= 0.87f
                        if (inCone) {
                            val threatLevel = when (enemy.alertState) {
                                AlertState.ALERTED -> ThreatLevel.ALERTED
                                AlertState.SUSPICIOUS -> ThreatLevel.SUSPICIOUS
                                AlertState.PATROLLING -> ThreatLevel.PATROL
                            }
                            val pos = GridPos(tx, ty, zLevel)
                            val existing = threatMap[pos]
                            if (existing == null || threatLevel.ordinal < existing.level.ordinal) {
                                threatMap[pos] = TileThreat(threatLevel, enemy.name, dist)
                            }
                        }
                    }
                }
            }
        }
    }
    return threatMap
}
