package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun GameHud(
    viewModel: GameViewModel,
    onBackToMenu: () -> Unit,
    onOpenSkillTree: () -> Unit,
    onOpenLoadout: () -> Unit
) {
    val player = viewModel.player
    val logs = viewModel.consoleLogs
    val currentZ = player.pos.z.toInt()

    // UI state toggles
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var isQuestExpanded by remember { mutableStateOf(true) }

    // Dynamic operative class title
    val className = if (player.unlockedSkills.contains("ronin_ultimate") || player.unlockedSkills.contains("ronin_crit")) {
        "SHADOW BERSERKER"
    } else if (player.unlockedSkills.contains("tech_ultimate") || player.unlockedSkills.contains("tech_base")) {
        "SYS REBOOTER"
    } else if (player.unlockedSkills.contains("ghost_ultimate") || player.unlockedSkills.contains("ghost_base")) {
        "PHANTOM GHOST"
    } else {
        "OPERATIVE CLASSIFIED"
    }

    val levelMap = viewModel.gameLevels[currentZ]
    val currentTile = levelMap?.getTile(player.pos.x.toInt(), player.pos.y.toInt())
    val isNearInteractable = currentTile == TileType.LADDER_UP || currentTile == TileType.LADDER_DOWN || run {
        var near = false
        val px = player.pos.x.toInt()
        val py = player.pos.y.toInt()
        if (levelMap != null) {
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val tile = levelMap.getTile(px + dx, py + dy)
                    if (tile == TileType.TERMINAL || tile == TileType.BARREL_EXPLOSIVE) {
                        near = true
                    }
                }
            }
        }
        near
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulsate")
    val pulseScale by if (isNearInteractable) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // =========================================================================
        // 1. TOP UNIFIED HUD HEADER (STATUS & QUICK ACTIONS)
        // =========================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, Color(0x2AFFFFFF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .align(Alignment.TopCenter)
                .testTag("hud_status_panel")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: OPERATIVE INFO & VITAL BARS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(ImmersiveDeepViolet, CircleShape)
                            .border(1.dp, ImmersiveLavender, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P", color = ImmersiveLavender, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Operative Name & Level
                    Column {
                        Text(
                            text = className,
                            color = ImmersiveSlateLight,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LVL ${player.level}",
                                color = ImmersiveLavender,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(if (player.isSneaking) ImmersiveGreen else ImmersiveAmber, CircleShape)
                            )
                            Text(
                                text = if (player.isSneaking) "STEALTH" else "ALERT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (player.isSneaking) ImmersiveGreen else ImmersiveAmber,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // INTEGRATED LIFE & CORE STATUS BARS
                    Column(
                        modifier = Modifier
                            .width(105.dp)
                            .background(ImmersiveBgDark.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // LIFE / HP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("LIFE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ImmersiveRed, fontFamily = FontFamily.Monospace)
                            Text("${player.health.toInt()}/${player.maxHealth.toInt()}", fontSize = 7.5.sp, color = ImmersiveSlateLight, fontFamily = FontFamily.Monospace)
                        }
                        LinearProgressIndicator(
                            progress = (player.health / player.maxHealth).coerceIn(0f, 1f),
                            color = ImmersiveRed,
                            trackColor = ImmersiveBgDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )

                        // CORE / EP
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CORE", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ImmersiveBlue, fontFamily = FontFamily.Monospace)
                            Text("${player.energy.toInt()}/${player.maxEnergy.toInt()}", fontSize = 7.5.sp, color = ImmersiveSlateLight, fontFamily = FontFamily.Monospace)
                        }
                        LinearProgressIndicator(
                            progress = (player.energy / player.maxEnergy).coerceIn(0f, 1f),
                            color = ImmersiveBlue,
                            trackColor = ImmersiveBgDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        )
                    }
                }

                // RIGHT: CREDITS & SYSTEM ACTION BUTTONS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Credits Badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveBgDark),
                        border = BorderStroke(1.dp, ImmersiveAmber),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${player.credits}C",
                            color = ImmersiveAmber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Tactical overlay button
                    val isTacticalOn = viewModel.isTacticalOverlayActive
                    Button(
                        onClick = { viewModel.toggleTacticalOverlay() },
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, if (isTacticalOn) ImmersiveGreen else ImmersiveSlateMuted),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_tactical_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Tactical Overlay",
                            tint = if (isTacticalOn) ImmersiveGreen else ImmersiveSlateLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "TAC",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isTacticalOn) ImmersiveGreen else ImmersiveSlateLight
                        )
                    }

                    // Low-Spec 2012-2013 Performance Mode button
                    val isLowSpec = viewModel.isLowSpecPerformanceMode
                    Button(
                        onClick = { viewModel.togglePerformanceMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isLowSpec) ImmersiveBgDark else ImmersiveBgHeader),
                        border = BorderStroke(1.dp, if (isLowSpec) ImmersiveAmber else ImmersiveSlateMuted),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_perf_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Low-Spec Performance Mode",
                            tint = if (isLowSpec) ImmersiveAmber else ImmersiveSlateLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isLowSpec) "LOW-SPEC" else "60FPS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isLowSpec) ImmersiveAmber else ImmersiveSlateLight
                        )
                    }

                    // Loadout Gear button
                    Button(
                        onClick = onOpenLoadout,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveAmber),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_loadout_matrix_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Gear", tint = ImmersiveAmber, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("GEAR", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = ImmersiveAmber)
                    }

                    // Skills Upgrades button
                    Button(
                        onClick = onOpenSkillTree,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveLavender),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_skills_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Skills", tint = ImmersiveLavender, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("SKILLS", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = ImmersiveLavender)
                    }

                    // Menu Hub backbutton
                    Button(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_menu_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Menu", tint = ImmersiveRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("HUB", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = ImmersiveRed)
                    }
                }
            }
        }

        // =========================================================================
        // 2. ACTIVE DIRECTIVE CARD (TOP-RIGHT SUB-HEADER)
        // =========================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, if (player.quest.isCompleted) ImmersiveGreen else ImmersiveLavender),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(if (isQuestExpanded) 190.dp else 140.dp)
                .padding(top = 62.dp, end = 8.dp)
                .align(Alignment.TopEnd)
                .testTag("hud_active_quest_card")
        ) {
            Column(modifier = Modifier.padding(7.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ImmersiveLavender, modifier = Modifier.size(11.dp))
                        Text(
                            text = "DIRECTIVE",
                            color = ImmersiveLavender,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (player.quest.isCompleted) "DONE" else "${player.quest.currentProgress}/${player.quest.targetCount}",
                            color = if (player.quest.isCompleted) ImmersiveGreen else ImmersiveSlateLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isQuestExpanded) "[-]" else "[+]",
                            color = ImmersiveLavender,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { isQuestExpanded = !isQuestExpanded }
                        )
                    }
                }
                if (isQuestExpanded) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = player.quest.title,
                        color = ImmersiveSlateLight,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = player.quest.progressRatio,
                        color = if (player.quest.isCompleted) ImmersiveGreen else ImmersiveLavender,
                        trackColor = ImmersiveBgDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }
        }

        // =========================================================================
        // 3. DEV CONSOLE (TOP-LEFT SUB-HEADER)
        // =========================================================================
        Column(
            modifier = Modifier
                .width(if (isConsoleExpanded) 210.dp else 120.dp)
                .wrapContentHeight()
                .padding(top = 62.dp, start = 8.dp)
                .background(ImmersiveBgDark.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(6.dp))
                .padding(6.dp)
                .align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEV-CONSOLE",
                    color = ImmersiveLavender,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isConsoleExpanded) "[-]" else "[+]",
                    color = ImmersiveLavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { isConsoleExpanded = !isConsoleExpanded }
                )
            }

            if (isConsoleExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                val pX = player.pos.x.toInt()
                val pY = player.pos.y.toInt()
                val stateName = if (player.isSneaking) "STEALTH_RT" else "COMBAT_RT"

                Text(
                    text = "POS: ($pX, $pY, Z=$currentZ)",
                    color = ImmersiveSlateLight,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "STATE: $stateName",
                    color = if (player.isSneaking) ImmersiveBlue else ImmersiveRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ACTIVE AI TARGETS: Z=$currentZ",
                    color = ImmersiveSlateLight,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )

                viewModel.enemies.filter { !it.isDead && it.pos.z.toInt() == currentZ }.forEach { e ->
                    val distStr = String.format("%.1f", e.pos.distanceTo(player.pos))
                    Text(
                        text = "- ${e.name} (Dist: $distStr)",
                        color = when (e.alertState) {
                            AlertState.PATROLLING -> ImmersiveGreen
                            AlertState.SUSPICIOUS -> ImmersiveAmber
                            AlertState.ALERTED -> ImmersiveRed
                        },
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color(0x0DFFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                logs.takeLast(3).forEach { log ->
                    Text(
                        text = "> $log",
                        color = ImmersiveSlateMuted,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // =========================================================================
        // 4. INTERACTION / HACK PROGRESS OVERLAY (CENTERED)
        // =========================================================================
        if (viewModel.isHackingActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.dp, ImmersiveLavender),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .width(230.dp)
                    .align(Alignment.Center)
                    .testTag("hud_interaction_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TERMINAL SYNCHRONIZATION",
                        color = ImmersiveLavender,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "DOWNLOADING GATE KEYCODE...",
                        color = ImmersiveSlateMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = viewModel.hackProgress,
                        color = ImmersiveLavender,
                        trackColor = ImmersiveBgDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val pct = (viewModel.hackProgress * 100f).toInt()
                    Text(
                        text = "$pct % COMPLETE",
                        color = ImmersiveLavender,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // =========================================================================
        // 5. BOTTOM LEFT: VIRTUAL JOYSTICK (CLEAN & UNCLUTTERED)
        // =========================================================================
        var joystickOffset by remember { mutableStateOf(Offset.Zero) }
        var isDragging by remember { mutableStateOf(false) }
        val density = LocalDensity.current

        LaunchedEffect(isDragging, joystickOffset) {
            if (isDragging) {
                while (isActive) {
                    val dist = sqrt(joystickOffset.x * joystickOffset.x + joystickOffset.y * joystickOffset.y)
                    if (dist > 6f) {
                        val maxRadiusPx = with(density) { 45.dp.toPx() }
                        val normalizedX = joystickOffset.x / maxRadiusPx
                        val normalizedY = joystickOffset.y / maxRadiusPx

                        val speedMultiplier = 1.2f
                        val dxGrid = (normalizedX / 1f + normalizedY / 0.55f) / 2f * speedMultiplier
                        val dyGrid = (-normalizedX / 1f + normalizedY / 0.55f) / 2f * speedMultiplier

                        viewModel.movePlayer(dxGrid, dyGrid)
                    }
                    delay(16)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 14.dp)
                .size(110.dp)
                .background(ImmersiveBgHeader.copy(alpha = 0.75f), CircleShape)
                .border(1.5.dp, ImmersiveLavender.copy(alpha = 0.45f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            joystickOffset = Offset.Zero
                        },
                        onDragCancel = {
                            isDragging = false
                            joystickOffset = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = joystickOffset + dragAmount
                            val maxRadiusPx = with(density) { 45.dp.toPx() }
                            val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                            joystickOffset = if (dist <= maxRadiusPx) {
                                newOffset
                            } else {
                                Offset((newOffset.x / dist) * maxRadiusPx, (newOffset.y / dist) * maxRadiusPx)
                            }
                        }
                    )
                }
                .testTag("virtual_joystick"),
            contentAlignment = Alignment.Center
        ) {
            // Crosshairs
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(ImmersiveLavender.copy(alpha = 0.15f)))
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(ImmersiveLavender.copy(alpha = 0.15f)))

            // Concentric visual guide circles
            Box(modifier = Modifier.size(70.dp).border(1.dp, ImmersiveLavender.copy(alpha = 0.1f), CircleShape))
            Box(modifier = Modifier.size(35.dp).border(1.dp, ImmersiveLavender.copy(alpha = 0.05f), CircleShape))

            // Stick thumb handle
            Box(
                modifier = Modifier
                    .offset { IntOffset(joystickOffset.x.roundToInt(), joystickOffset.y.roundToInt()) }
                    .size(42.dp)
                    .background(ImmersiveLavender.copy(alpha = 0.9f), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        // =========================================================================
        // 6. BOTTOM CENTER: CONSUMABLES & ACTIVE SKILL HOTKEYS
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Inventory Consumables Quick Use Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.useHealthPack() },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                    border = BorderStroke(1.dp, ImmersiveRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp).testTag("hud_use_health_pack_button")
                ) {
                    Text(
                        text = "MED (${player.inventory.healthPacks})",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveRed,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { viewModel.useEnergyCell() },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                    border = BorderStroke(1.dp, ImmersiveBlue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp).testTag("hud_use_energy_cell_button")
                ) {
                    Text(
                        text = "CELL (${player.inventory.energyCells})",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveBlue,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Skill Hotkey Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeSkillsList = listOf(
                    Pair("ronin_crit", "DASH"),
                    Pair("tech_ultimate", "EMP"),
                    Pair("ghost_smoke", "SMOKE"),
                    Pair("ghost_ultimate", "CLOAK")
                )

                activeSkillsList.forEach { (skillId, label) ->
                    val isUnlocked = player.unlockedSkills.contains(skillId)

                    Button(
                        onClick = { if (isUnlocked) viewModel.triggerActiveSkill(skillId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUnlocked) ImmersiveBgHeader else ImmersiveBgDark,
                            disabledContainerColor = ImmersiveBgDark
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isUnlocked) ImmersiveLavender else Color(0x1AFFFFFF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("hotkey_$skillId")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUnlocked) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val ic = when (skillId) {
                                        "ronin_crit" -> Icons.Default.PlayArrow
                                        "tech_ultimate" -> Icons.Default.Warning
                                        "ghost_smoke" -> Icons.Default.Share
                                        "ghost_ultimate" -> Icons.Default.Refresh
                                        else -> Icons.Default.Star
                                    }
                                    Icon(
                                        imageVector = ic,
                                        contentDescription = label,
                                        tint = ImmersiveLavender,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = label,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveSlateLight,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = ImmersiveSlateMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 7. BOTTOM RIGHT: ERGONOMIC ACTION CONTROLS CLUSTER
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Upper cluster row: ACTION & SNEAK
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // ACTION / INTERACT BUTTON
                Button(
                    onClick = { viewModel.executeInteract() },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveAmber),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isNearInteractable) BorderStroke(2.dp, Color.White) else null,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .testTag("action_interact_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Interact",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ACTION",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // SNEAK / CROUCH BUTTON
                Button(
                    onClick = { viewModel.toggleSneak() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isSneaking) ImmersiveGreen else ImmersiveBgHeader
                    ),
                    border = BorderStroke(1.dp, if (player.isSneaking) ImmersiveGreen else ImmersiveBlue),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("action_sneak_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (player.isSneaking) Icons.Default.KeyboardArrowDown else Icons.Default.NotificationsOff,
                            contentDescription = "Sneak",
                            tint = if (player.isSneaking) Color.Black else ImmersiveBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (player.isSneaking) "CROUCH" else "SNEAK",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player.isSneaking) Color.Black else Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // PRIMARY ATTACK STRIKE BUTTON
            Button(
                onClick = { viewModel.executeAttack() },
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveRed),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(62.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .testTag("action_attack_button")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Attack",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "STRIKE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
