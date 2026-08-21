package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
    var isTelemetrySheetOpen by remember { mutableStateOf(false) }
    var selectedLogCategoryFilter by remember { mutableStateOf<LogCategory?>(null) }
    var isQuestExpanded by remember { mutableStateOf(true) }
    var isFullHudExpanded by remember { mutableStateOf(false) }

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
                            progress = { (player.health / player.maxHealth).coerceIn(0f, 1f) },
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
                            progress = { (player.energy / player.maxEnergy).coerceIn(0f, 1f) },
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

                    // Game Boy Color Graphics & Palette Dialog Button
                    var showGbcDialog by remember { mutableStateOf(false) }
                    val gbcSettings = viewModel.gbcGraphicsSettings

                    Button(
                        onClick = { showGbcDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                        border = BorderStroke(1.dp, ImmersiveGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_gbc_graphics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "GBC Graphics",
                            tint = ImmersiveGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "GBC",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ImmersiveGreen
                        )
                    }

                    if (showGbcDialog) {
                        AlertDialog(
                            onDismissRequest = { showGbcDialog = false },
                            containerColor = ImmersiveBgDark,
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = ImmersiveGreen)
                                    Spacer(Modifier.width(8.dp))
                                    Text("GBC GRAPHICS & PALETTE", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("PALETTE MODE:", color = ImmersiveSlateLight, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

                                    val palettes = listOf(GbcPalette.CLASSIC_GBC, GbcPalette.CYBER_8BIT, GbcPalette.POCKET_DMG, GbcPalette.RETRO_ARCADE)
                                    palettes.forEach { pal ->
                                        val isSel = gbcSettings.palette == pal
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSel) ImmersiveBgHeader else Color.Transparent, RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isSel) ImmersiveGreen else Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                                .clickable { viewModel.setGbcPalette(pal) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(pal.displayName, color = if (isSel) ImmersiveGreen else Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Box(Modifier.size(12.dp).background(pal.bgDark, CircleShape).border(1.dp, Color.White, CircleShape))
                                                Box(Modifier.size(12.dp).background(pal.floorPrimary, CircleShape).border(1.dp, Color.White, CircleShape))
                                                Box(Modifier.size(12.dp).background(pal.wallAccent, CircleShape).border(1.dp, Color.White, CircleShape))
                                                Box(Modifier.size(12.dp).background(pal.terminalColor, CircleShape).border(1.dp, Color.White, CircleShape))
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = Color(0x33FFFFFF))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("8-BIT OUTLINES", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Switch(
                                            checked = gbcSettings.isPixelOutlineEnabled,
                                            onCheckedChange = { viewModel.toggleGbcPixelOutlines() }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PIXEL DITHER MATRIX", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Switch(
                                            checked = gbcSettings.isPixelDitherEnabled,
                                            onCheckedChange = { viewModel.toggleGbcDither() }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("CRT SCANLINES", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Switch(
                                            checked = gbcSettings.isScanlinesEnabled,
                                            onCheckedChange = { viewModel.toggleGbcScanlines() }
                                        )
                                    }

                                    HorizontalDivider(color = Color(0x33FFFFFF))

                                    // Cel-Shading Technology Section
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CEL-SHADING ENGINE", color = ImmersiveGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Text("Toon/Comic Banded Shading", color = ImmersiveSlateMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Switch(
                                            checked = gbcSettings.isCelShadingEnabled,
                                            onCheckedChange = { viewModel.toggleCelShading() }
                                        )
                                    }

                                    if (gbcSettings.isCelShadingEnabled) {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("CEL BANDING LEVEL:", color = ImmersiveSlateLight, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val bandsList = listOf(2 to "2-BAND RETRO", 3 to "3-BAND ANIME", 4 to "4-BAND COMIC")
                                                bandsList.forEach { (b, label) ->
                                                    val isSel = gbcSettings.celShadingSettings.bands == b
                                                    Button(
                                                        onClick = { viewModel.setCelShadingBands(b) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) ImmersiveGreen else ImmersiveBgHeader),
                                                        modifier = Modifier.weight(1f).height(28.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text(label, fontSize = 8.sp, color = if (isSel) Color.Black else Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Text("INK OUTLINE THICKNESS:", color = ImmersiveSlateLight, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                val thicknessList = listOf(1.5f to "THIN (1.5px)", 2.5f to "BOLD (2.5px)", 3.5f to "HEAVY (3.5px)")
                                                thicknessList.forEach { (th, label) ->
                                                    val isSel = kotlin.math.abs(gbcSettings.celShadingSettings.inkOutlineThickness - th) < 0.2f
                                                    Button(
                                                        onClick = { viewModel.setCelInkOutlineThickness(th) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = if (isSel) ImmersiveLavender else ImmersiveBgHeader),
                                                        modifier = Modifier.weight(1f).height(28.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text(label, fontSize = 8.sp, color = if (isSel) Color.Black else Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showGbcDialog = false }) {
                                    Text("DONE", color = ImmersiveGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
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

                    // Real-time Heads-Up Display (HUD) Matrix button
                    Button(
                        onClick = { isFullHudExpanded = !isFullHudExpanded },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isFullHudExpanded) ImmersiveBgDark else ImmersiveBgHeader),
                        border = BorderStroke(1.dp, if (isFullHudExpanded) ImmersiveGreen else ImmersiveLavender),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("hud_matrix_expand_button")
                    ) {
                        Icon(Icons.Default.MonitorHeart, contentDescription = "HUD Matrix", tint = if (isFullHudExpanded) ImmersiveGreen else ImmersiveLavender, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("HUD", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = if (isFullHudExpanded) ImmersiveGreen else ImmersiveLavender)
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
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Menu", tint = ImmersiveRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("HUB", fontSize = 9.5.sp, fontFamily = FontFamily.Monospace, color = ImmersiveRed)
                    }
                }
            }
        }

        // Expanded Real-Time Heads-Up Display Overlay Modal
        if (isFullHudExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ImmersiveBgDark.copy(alpha = 0.85f))
                    .clickable { isFullHudExpanded = false }
                    .padding(top = 54.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                    border = BorderStroke(1.5.dp, ImmersiveGreen),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = ImmersiveGreen)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "REAL-TIME HEADS-UP DISPLAY (HUD)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { isFullHudExpanded = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close HUD", tint = Color.White)
                            }
                        }

                        // Compute active stealth stance and state for character HUD
                        val currentStealthState = com.example.stealth.StealthState(
                            stance = if (player.isSneaking) com.example.stealth.Stance.CROUCHING else com.example.stealth.Stance.STANDING,
                            detectionStatus = when {
                                player.health <= 0f -> com.example.stealth.DetectionStatus.COMPROMISED
                                player.isSneaking -> com.example.stealth.DetectionStatus.HIDDEN
                                else -> com.example.stealth.DetectionStatus.SUSPECTED
                            },
                            visibilityLevel = if (player.isSneaking) 20f else 65f,
                            detectionProgress = if (player.isSneaking) 15f else 45f,
                            noiseLevel = if (player.isSneaking) 10f else 40f
                        )

                        // Compute dynamic active buffs from player's inventory, equipped gear and active skills
                        val liveBuffs = mutableListOf<ActiveBuffUiModel>()
                        if (player.isInvisible) {
                            liveBuffs.add(
                                ActiveBuffUiModel(
                                    id = "cloak",
                                    name = "OPTICAL CLOAK",
                                    icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = ImmersiveGreen, modifier = Modifier.size(11.dp)) },
                                    isBuff = true,
                                    durationSeconds = 10f,
                                    remainingSeconds = player.invisibleTimer.coerceAtLeast(1f),
                                    modifierText = "+100% STEALTH"
                                )
                            )
                        }
                        if (player.unlockedSkills.contains("ronin_base")) {
                            liveBuffs.add(
                                ActiveBuffUiModel(
                                    id = "ronin_blade",
                                    name = "BLADE MASTERY",
                                    icon = { Icon(Icons.Default.FlashOn, contentDescription = null, tint = ImmersiveAmber, modifier = Modifier.size(11.dp)) },
                                    isBuff = true,
                                    durationSeconds = 60f,
                                    remainingSeconds = 48f,
                                    modifierText = "+15% CRIT"
                                )
                            )
                        }
                        if (player.inventory.healthPacks > 0) {
                            liveBuffs.add(
                                ActiveBuffUiModel(
                                    id = "nano_med",
                                    name = "NANO INJECTOR",
                                    icon = { Icon(Icons.Default.AddReaction, contentDescription = null, tint = ImmersiveCyan, modifier = Modifier.size(11.dp)) },
                                    isBuff = true,
                                    durationSeconds = 30f,
                                    remainingSeconds = 25f,
                                    modifierText = "+5 HP/s"
                                )
                            )
                        }

                        CharacterHeadsUpDisplay(
                            character = com.example.data.PlayerCharacter(
                                name = className,
                                health = player.health.toInt(),
                                maxHealth = player.maxHealth.toInt(),
                                stamina = player.energy.toInt(),
                                maxStamina = player.maxEnergy.toInt()
                            ),
                            stealthState = currentStealthState,
                            activeBuffs = liveBuffs,
                            onHealthChange = { newHp ->
                                player.health = newHp.toFloat()
                            },
                            onStanceChange = { newStance ->
                                player.isSneaking = (newStance == com.example.stealth.Stance.CROUCHING || newStance == com.example.stealth.Stance.SLIDING)
                            }
                        )
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
                        progress = { player.quest.progressRatio },
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
        // 3. SPACE-SAVING ANIMATED RADAR & TRANSIENT EVENT TICKER (TOP-LEFT)
        // =========================================================================
        val activeEnemiesOnZ = remember(viewModel.enemies, currentZ) {
            viewModel.enemies.filter { !it.isDead && it.pos.z.toInt() == currentZ }
        }
        val alertedEnemyCount = remember(activeEnemiesOnZ) {
            activeEnemiesOnZ.count { it.alertState == AlertState.ALERTED }
        }
        val suspiciousEnemyCount = remember(activeEnemiesOnZ) {
            activeEnemiesOnZ.count { it.alertState == AlertState.SUSPICIOUS }
        }

        val threatDotColor = when {
            alertedEnemyCount > 0 -> ImmersiveRed
            suspiciousEnemyCount > 0 -> ImmersiveAmber
            else -> ImmersiveGreen
        }

        val latestToast = viewModel.latestToastEvent
        LaunchedEffect(latestToast?.id) {
            if (latestToast != null) {
                delay(2500L)
                viewModel.dismissToastEvent()
            }
        }

        // Animated Radar Sweep
        val radarAnimation = rememberInfiniteTransition(label = "radar_sweep")
        val radarPulseScale by radarAnimation.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radar_scale"
        )
        val radarPulseAlpha by radarAnimation.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radar_alpha"
        )

        Column(
            modifier = Modifier
                .padding(top = 62.dp, start = 8.dp)
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // A. Compact Space-Saving Radar Badge Button
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, threatDotColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .clickable { isTelemetrySheetOpen = true }
                    .testTag("hud_radar_telemetry_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = radarPulseScale
                                    scaleY = radarPulseScale
                                    alpha = radarPulseAlpha
                                }
                                .background(threatDotColor.copy(alpha = 0.25f), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Radar Telemetry",
                            tint = threatDotColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = "RADAR",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Box(
                        modifier = Modifier
                            .background(threatDotColor.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, threatDotColor, CircleShape)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${activeEnemiesOnZ.size}",
                            color = threatDotColor,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // B. Transient Floating Event Toast Ticker
            AnimatedVisibility(
                visible = latestToast != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
            ) {
                val toast = latestToast
                if (toast != null) {
                    val (toastBg, toastIcon, toastTint) = when (toast.category) {
                        LogCategory.COMBAT -> Triple(ImmersiveRed.copy(alpha = 0.15f), Icons.Default.FlashOn, ImmersiveRed)
                        LogCategory.STEALTH -> Triple(ImmersiveGreen.copy(alpha = 0.15f), Icons.Default.NotificationsOff, ImmersiveGreen)
                        LogCategory.QUEST -> Triple(ImmersiveLavender.copy(alpha = 0.15f), Icons.Default.Star, ImmersiveLavender)
                        LogCategory.REWARD -> Triple(ImmersiveAmber.copy(alpha = 0.15f), Icons.Default.ShoppingCart, ImmersiveAmber)
                        LogCategory.SYSTEM -> Triple(ImmersiveBlue.copy(alpha = 0.15f), Icons.Default.Build, ImmersiveBlue)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ImmersiveBgDark.copy(alpha = 0.94f)),
                        border = BorderStroke(1.dp, toastTint.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clickable { isTelemetrySheetOpen = true }
                            .testTag("hud_event_toast_ticker")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(toastBg, CircleShape)
                                    .border(1.dp, toastTint, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = toastIcon,
                                    contentDescription = null,
                                    tint = toastTint,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = toast.message,
                                color = ImmersiveSlateLight,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // C. Interactive Telemetry & Event Inspector Modal
        if (isTelemetrySheetOpen) {
            AlertDialog(
                onDismissRequest = { isTelemetrySheetOpen = false },
                containerColor = ImmersiveBgDark,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ImmersiveLavender)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TACTICAL TELEMETRY & EVENTS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { isTelemetrySheetOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Position & State Telemetry Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ImmersiveBgHeader, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("POSITION", color = ImmersiveSlateMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text("X:${player.pos.x.toInt()} Y:${player.pos.y.toInt()} Z:${player.pos.z.toInt()}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("STANCE", color = ImmersiveSlateMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    if (player.isSneaking) "STEALTH (QUIET)" else "COMBAT (LOUD)",
                                    color = if (player.isSneaking) ImmersiveGreen else ImmersiveAmber,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("ENEMIES", color = ImmersiveSlateMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                Text("${activeEnemiesOnZ.size} ACTIVE", color = threatDotColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Interactive Tactical Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerNoiseEvent(player.pos) },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                                border = BorderStroke(1.dp, ImmersiveBlue),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = ImmersiveBlue, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("NOISE PING", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = { viewModel.toggleThreatZoneOverlay() },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                                border = BorderStroke(1.dp, if (viewModel.isThreatZoneOverlayVisible) ImmersiveGreen else ImmersiveSlateMuted),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = if (viewModel.isThreatZoneOverlayVisible) ImmersiveGreen else ImmersiveSlateMuted, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("THREATS", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = { viewModel.clearLogEvents() },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader),
                                border = BorderStroke(1.dp, ImmersiveRed),
                                modifier = Modifier.weight(1f).height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = ImmersiveRed, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("CLEAR", fontSize = 8.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }

                        HorizontalDivider(color = Color(0x33FFFFFF))

                        // Category Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val categories = listOf(null to "ALL") + LogCategory.values().map { it to it.displayName }
                            categories.forEach { (cat, label) ->
                                val isSel = selectedLogCategoryFilter == cat
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isSel) ImmersiveLavender else ImmersiveBgHeader),
                                    border = BorderStroke(1.dp, if (isSel) ImmersiveLavender else Color(0x33FFFFFF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedLogCategoryFilter = cat }
                                        .padding(vertical = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            fontSize = 7.sp,
                                            color = if (isSel) Color.Black else Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Event Log List
                        val filteredEvents = remember(viewModel.recentLogEvents.toList(), selectedLogCategoryFilter) {
                            if (selectedLogCategoryFilter == null) {
                                viewModel.recentLogEvents.toList().reversed()
                            } else {
                                viewModel.recentLogEvents.filter { it.category == selectedLogCategoryFilter }.reversed()
                            }
                        }

                        if (filteredEvents.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("NO TELEMETRY EVENTS RECORDED", color = ImmersiveSlateMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                items(items = filteredEvents, key = { it.id }) { event ->
                                    val (catColor, catIcon) = when (event.category) {
                                        LogCategory.COMBAT -> ImmersiveRed to Icons.Default.FlashOn
                                        LogCategory.STEALTH -> ImmersiveGreen to Icons.Default.NotificationsOff
                                        LogCategory.QUEST -> ImmersiveLavender to Icons.Default.Star
                                        LogCategory.REWARD -> ImmersiveAmber to Icons.Default.ShoppingCart
                                        LogCategory.SYSTEM -> ImmersiveBlue to Icons.Default.Build
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ImmersiveBgHeader.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(catColor.copy(alpha = 0.2f), CircleShape)
                                                .border(1.dp, catColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(catIcon, contentDescription = null, tint = catColor, modifier = Modifier.size(11.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(event.category.displayName, color = catColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text(event.formattedTime, color = ImmersiveSlateMuted, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            Text(event.message, color = Color.White, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isTelemetrySheetOpen = false }) {
                        Text("CLOSE", color = ImmersiveLavender, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            )
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
                        progress = { viewModel.hackProgress },
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
