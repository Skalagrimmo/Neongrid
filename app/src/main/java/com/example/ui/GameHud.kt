package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    // Minimize console by default so it does not hide gameplay
    var isConsoleExpanded by remember { mutableStateOf(false) }

    // Determine character class name dynamically based on unlocked tree milestones
    val className = if (player.unlockedSkills.contains("ronin_ultimate") || player.unlockedSkills.contains("ronin_crit")) {
        "SHADOW BERSERKER"
    } else if (player.unlockedSkills.contains("tech_ultimate") || player.unlockedSkills.contains("tech_base")) {
        "SYS REBOOTER"
    } else if (player.unlockedSkills.contains("ghost_ultimate") || player.unlockedSkills.contains("ghost_base")) {
        "PHANTOM GHOST"
    } else {
        "OPERATIVE CLASSIFIED"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        // --- TOP-LEVEL STATUS PANEL (Unified, optimized for 5-inch screens) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .align(Alignment.TopCenter)
                .testTag("hud_status_panel")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rounded Avatar with Level indicator in lavender and class in white
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ImmersiveDeepViolet, CircleShape)
                            .border(1.dp, ImmersiveLavender, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("P", color = ImmersiveLavender, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = className,
                            color = ImmersiveSlateLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "LEVEL ${player.level}",
                            color = ImmersiveLavender,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Credits tracker
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                    border = BorderStroke(1.dp, ImmersiveAmber),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${player.credits}C",
                        color = ImmersiveAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // NAVIGATION ACCESS OVERLAYS
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Menu backbutton
                    Button(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveRed),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(34.dp).testTag("hud_menu_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Menu", tint = ImmersiveRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HUB", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ImmersiveRed)
                    }

                    // Skills overlay tree
                    Button(
                        onClick = onOpenSkillTree,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveLavender),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(34.dp).testTag("hud_skills_button")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Skills", tint = ImmersiveLavender, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UPGRADES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ImmersiveLavender)
                    }

                    // Loadout customization overlay
                    Button(
                        onClick = onOpenLoadout,
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveAmber),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(34.dp).testTag("hud_loadout_matrix_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Gear", tint = ImmersiveAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EQUIP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = ImmersiveAmber)
                    }
                }
            }
        }

        // --- DEV CONSOLE OVERLAY (Left Edge - Exact match of user image style) ---
        Column(
            modifier = Modifier
                .width(if (isConsoleExpanded) 220.dp else 125.dp)
                .wrapContentHeight()
                .padding(14.dp)
                .offset(y = 65.dp)
                .background(ImmersiveBgDark.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(4.dp))
                .padding(8.dp)
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Button(
                    onClick = { isConsoleExpanded = !isConsoleExpanded },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(18.dp)
                ) {
                    Text(
                        text = if (isConsoleExpanded) "[-]" else "[+]",
                        color = ImmersiveLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isConsoleExpanded) {
                Spacer(modifier = Modifier.height(4.dp))

                val pX = player.pos.x.toInt()
                val pY = player.pos.y.toInt()
                val stateName = if (player.isSneaking) "STEALTH_RT" else "COMBAT_RT"

                Text(
                    text = "PLAYER: ($pX, $pY, ${currentZ}) - Z$currentZ",
                    color = ImmersiveSlateLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "STATE: $stateName",
                    color = if (player.isSneaking) ImmersiveBlue else ImmersiveRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ACTIVE AI TARGETS: Z=$currentZ+",
                    color = ImmersiveSlateLight,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Dynamic Targets list
                viewModel.enemies.filter { !it.isDead && it.pos.z.toInt() == currentZ }.forEach { e ->
                    val distStr = String.format("%.1f", e.pos.distanceTo(player.pos))
                    Text(
                        text = "- ${e.name} (Dist: $distStr)",
                        color = when (e.alertState) {
                            AlertState.PATROLLING -> ImmersiveGreen
                            AlertState.SUSPICIOUS -> ImmersiveAmber
                            AlertState.ALERTED -> ImmersiveRed
                        },
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0x0DFFFFFF), thickness = 1.dp)
                Spacer(modifier = Modifier.height(4.dp))

                // Console text logs stream
                logs.takeLast(4).forEach { log ->
                    Text(
                        text = "> $log",
                        color = ImmersiveSlateMuted,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        // --- INTERACTION / HACK PROGRESS OVERLAYS ---
        if (viewModel.isHackingActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.dp, ImmersiveLavender),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .width(240.dp)
                    .align(Alignment.Center)
                    .testTag("hud_interaction_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TERMINAL SYNCHRONIZATION",
                        color = ImmersiveLavender,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DOWNLOADING COGNITIVE GATE KEYCODE...",
                        color = ImmersiveSlateMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = viewModel.hackProgress,
                        color = ImmersiveLavender,
                        trackColor = ImmersiveBgDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val pct = (viewModel.hackProgress * 100f).toInt()
                    Text(
                        text = "$pct % COMPLETE",
                        color = ImmersiveLavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- BOTTOM LEFT CONTROLS CONTAINER (MGS Tactical Style) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. LIFE & CORE METERS (High contrast MGS style)
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .width(160.dp)
                    .testTag("hud_status_panel_compact")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Vitality Progress (LIFE bar)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIFE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveSlateLight,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${player.health.toInt()}/${player.maxHealth.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = (player.health / player.maxHealth).coerceIn(0f, 1f),
                            color = ImmersiveRed,
                            trackColor = ImmersiveBgDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }

                    // Power/Energy (CORE bar)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CORE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveSlateLight,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${player.energy.toInt()}/${player.maxEnergy.toInt()}",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = (player.energy / player.maxEnergy).coerceIn(0f, 1f),
                            color = ImmersiveBlue,
                            trackColor = ImmersiveBgDark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }

                    // Stealth radar text status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(if (player.isSneaking) ImmersiveGreen else ImmersiveAmber, CircleShape)
                        )
                        Text(
                            text = if (player.isSneaking) "SNEAKING" else "RUNNING (ALERT)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player.isSneaking) ImmersiveGreen else ImmersiveAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. SMOOTH RETRO-TACTICAL ANALOG STICK
            var joystickOffset by remember { mutableStateOf(Offset.Zero) }
            var isDragging by remember { mutableStateOf(false) }
            val density = LocalDensity.current

            // Coroutine loop for continuous movement while dragging
            LaunchedEffect(isDragging, joystickOffset) {
                if (isDragging) {
                    while (isActive) {
                        val dist = sqrt(joystickOffset.x * joystickOffset.x + joystickOffset.y * joystickOffset.y)
                        if (dist > 6f) {
                            val maxRadiusPx = with(density) { 45.dp.toPx() }
                            val normalizedX = joystickOffset.x / maxRadiusPx
                            val normalizedY = joystickOffset.y / maxRadiusPx
                            
                            // Proportional speed multiplier
                            val speedMultiplier = 1.2f
                            
                            // Map stick directions directly into Isometric movement
                            val dxGrid = (normalizedX / 1f + normalizedY / 0.55f) / 2f * speedMultiplier
                            val dyGrid = (-normalizedX / 1f + normalizedY / 0.55f) / 2f * speedMultiplier
                            
                            viewModel.movePlayer(dxGrid, dyGrid)
                        }
                        delay(16) // Continuous 60fps tick
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(ImmersiveBgHeader.copy(alpha = 0.7f), CircleShape)
                    .border(1.5.dp, ImmersiveLavender.copy(alpha = 0.4f), CircleShape)
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
                
                // Concentric visual lines
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
                    // Tactile center grip
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }

        // --- SKILL ACTIVE HOTKEY SYSTEM (Bottom Center) ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        .size(50.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = label,
                                    fontSize = 8.sp,
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
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- RIGHT ACTION BUTTONS (Tactile Action, Sneak, Melee CQC) ---
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
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
        } else {
            remember { mutableStateOf(1.0f) }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Interact/Action button (Immersive Amber / Pulsing when near terminal/ladder)
            Button(
                onClick = { viewModel.executeInteract() },
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveAmber),
                shape = RoundedCornerShape(10.dp),
                border = if (isNearInteractable) BorderStroke(2.dp, Color.White) else null,
                modifier = Modifier
                    .size((60f * pulseScale).dp)
                    .testTag("action_interact_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Interact",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ACTION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Crouch/Sneak Toggle (Immersive Blue / ImmersiveBgHeader - glows Green when actively crawling/sneaking)
            Button(
                onClick = { viewModel.toggleSneak() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (player.isSneaking) ImmersiveGreen else ImmersiveBgHeader
                ),
                border = BorderStroke(1.dp, if (player.isSneaking) ImmersiveGreen else ImmersiveBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .size(60.dp)
                    .testTag("action_sneak_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (player.isSneaking) Icons.Default.KeyboardArrowDown else Icons.Default.NotificationsOff,
                            contentDescription = "Sneak",
                            tint = if (player.isSneaking) Color.Black else ImmersiveBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (player.isSneaking) "CROUCH" else "SNEAK",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player.isSneaking) Color.Black else Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Slash/Attack Melee CQC (Immersive Red)
            Button(
                onClick = { viewModel.executeAttack() },
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(72.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .testTag("action_attack_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Attack",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STRIKE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
