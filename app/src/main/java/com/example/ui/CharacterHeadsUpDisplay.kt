package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CharacterStatusEffectEntity
import com.example.data.PlayerCharacter
import com.example.stealth.DetectionStatus
import com.example.stealth.LightLevel
import com.example.stealth.Stance
import com.example.stealth.StealthState
import com.example.ui.theme.*

/**
 * Data class representing an active status buff/debuff for HUD visualization.
 */
data class ActiveBuffUiModel(
    val id: String,
    val name: String,
    val icon: @Composable () -> Unit,
    val isBuff: Boolean = true,
    val durationSeconds: Float = 30f,
    val remainingSeconds: Float = 22f,
    val modifierText: String = "+25%"
)

/**
 * High-fidelity, real-time Compose-based Heads-Up Display (HUD).
 * Visually indicates:
 * 1. Current Health & Stamina with smooth progress transitions and critical low-HP warnings.
 * 2. Active Buffs & Status Effects with animated duration sweep meters and stat modifiers.
 * 3. Real-time Detected Stealth Status (Hidden, Suspicious, Alerted, Detected) with noise waveforms,
 *    detection gauges, and environmental stance indicators.
 */
@Composable
fun CharacterHeadsUpDisplay(
    character: PlayerCharacter = PlayerCharacter(),
    stealthState: StealthState = StealthState(),
    activeBuffs: List<ActiveBuffUiModel> = defaultDemoBuffs(),
    modifier: Modifier = Modifier,
    onHealthChange: ((Int) -> Unit)? = null,
    onStanceChange: ((Stance) -> Unit)? = null,
    onDetectionChange: ((DetectionStatus) -> Unit)? = null,
    onToggleInvisibility: ((Boolean) -> Unit)? = null,
    onToggleCover: ((Boolean) -> Unit)? = null
) {
    var showTestControls by remember { mutableStateOf(false) }

    // --- Health Metrics & Animations ---
    val maxHp = character.maxHealth.coerceAtLeast(1)
    val currentHp = character.health.coerceIn(0, maxHp)
    val hpRatio = currentHp.toFloat() / maxHp.toFloat()

    val animatedHpRatio by animateFloatAsState(
        targetValue = hpRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedHpRatio"
    )

    val hpColor by animateColorAsState(
        targetValue = when {
            hpRatio < 0.25f -> ImmersiveRed
            hpRatio < 0.55f -> ImmersiveAmber
            else -> ImmersiveGreen
        },
        animationSpec = tween(400),
        label = "hpColor"
    )

    // Critical Health Pulse Animation
    val isCriticalHp = hpRatio < 0.30f
    val infiniteTransition = rememberInfiniteTransition(label = "hudPulse")
    val pulseScale by if (isCriticalHp) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val stealthColor by animateColorAsState(
        targetValue = when (stealthState.detectionStatus) {
            DetectionStatus.HIDDEN -> ImmersiveGreen
            DetectionStatus.SUSPECTED -> ImmersiveAmber
            DetectionStatus.DETECTED -> ImmersiveRed
            DetectionStatus.ALERTED -> ImmersiveRed
            DetectionStatus.COMPROMISED -> Color(0xFFFF0055)
        },
        animationSpec = tween(300),
        label = "stealthColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("character_heads_up_display")
    ) {
        // =========================================================================
        // CRITICAL HP VIGNETTE WARNING (If Health is < 30%)
        // =========================================================================
        if (isCriticalHp) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("critical_hp_vignette")
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension * 0.85f
                val edgeBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        ImmersiveRed.copy(alpha = 0.05f),
                        ImmersiveRed.copy(alpha = 0.45f * pulseScale)
                    ),
                    center = center,
                    radius = maxRadius
                )
                drawRect(brush = edgeBrush)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // =========================================================================
            // 1. TOP MAIN HUD CARD: VITAL HEALTH BAR & STEALTH DETECTION METRIC
            // =========================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.94f)),
                border = BorderStroke(1.5.dp, if (isCriticalHp) ImmersiveRed else Color(0x33FFFFFF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hud_main_vitals_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // UPPER ROW: OPERATIVE AVATAR, HEALTH & STAMINA BARS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT: OPERATIVE VITAL METRICS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Badge
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(ImmersiveBgDark, CircleShape)
                                    .border(1.5.dp, hpColor, CircleShape)
                                    .scale(pulseScale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCriticalHp) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                                    contentDescription = "Health Pulse",
                                    tint = hpColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Health & Stamina Indicators
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // HP Row Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "HEALTH",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hpColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = hpColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = when {
                                                    isCriticalHp -> "CRITICAL"
                                                    hpRatio < 0.55f -> "INJURED"
                                                    else -> "OPTIMAL"
                                                },
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hpColor,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "$currentHp / $maxHp HP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                // Health Progress Bar
                                LinearProgressIndicator(
                                    progress = { animatedHpRatio },
                                    color = hpColor,
                                    trackColor = ImmersiveBgDark,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .testTag("hud_health_progress_bar")
                                )

                                // Stamina Bar
                                val staminaRatio = (character.stamina.toFloat() / character.maxStamina.coerceAtLeast(1)).coerceIn(0f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "STAMINA",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveBlue,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${character.stamina} SP",
                                        fontSize = 8.sp,
                                        color = ImmersiveSlateLight,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { staminaRatio },
                                    color = ImmersiveBlue,
                                    trackColor = ImmersiveBgDark,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.5.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // RIGHT: DETECTED STEALTH STATUS BADGE
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ImmersiveBgDark,
                            border = BorderStroke(1.2.dp, stealthColor),
                            modifier = Modifier.testTag("stealth_status_badge")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = when (stealthState.detectionStatus) {
                                            DetectionStatus.HIDDEN -> Icons.Default.VisibilityOff
                                            DetectionStatus.SUSPECTED -> Icons.Default.Warning
                                            else -> Icons.Default.Visibility
                                        },
                                        contentDescription = "Stealth State",
                                        tint = stealthColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stealthState.detectionStatus.label.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stealthColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "Visibility: ${stealthState.visibilityLevel.toInt()}%",
                                    fontSize = 8.sp,
                                    color = ImmersiveSlateLight,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // LOWER ROW: REAL-TIME STEALTH DETECTION GAUGE & NOISE WAVEFORM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Detection Progress Bar (0% - 100%)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ENEMY DETECTION ALERT",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stealthColor,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${stealthState.detectionProgress.toInt()}%",
                                    fontSize = 7.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = stealthColor,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (stealthState.detectionProgress / 100f).coerceIn(0f, 1f) },
                                color = stealthColor,
                                trackColor = ImmersiveBgDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .testTag("detection_alert_progress")
                            )
                        }

                        // Stance Chip Indicator
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ImmersiveBgDark,
                            border = BorderStroke(1.dp, ImmersiveLavender)
                        ) {
                            Text(
                                text = "STANCE: ${stealthState.stance.name}",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveLavender,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Noise Level dB Chip
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ImmersiveBgDark,
                            border = BorderStroke(1.dp, ImmersiveAmber)
                        ) {
                            Text(
                                text = "NOISE: ${stealthState.noiseLevel.toInt()}dB",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveAmber,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. ACTIVE BUFFS & STATUS EFFECTS TRAY
            // =========================================================================
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_buffs_container")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Active Buffs",
                                tint = ImmersiveLavender,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE STATUS BUFFS (${activeBuffs.size})",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveLavender,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = if (showTestControls) "HIDE CONTROLS" else "HUD MATRIX TEST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveAmber,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clickable { showTestControls = !showTestControls }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .testTag("hud_test_controls_toggle")
                        )
                    }

                    if (activeBuffs.isEmpty()) {
                        Text(
                            text = "NO ACTIVE BUFFS / DEBUFFS APPLIED",
                            fontSize = 8.sp,
                            color = ImmersiveSlateMuted,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeBuffs, key = { it.id }) { buff ->
                                ActiveBuffChip(buff = buff)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. INTERACTIVE HUD TEST CONTROLS (EXPANDABLE)
            // =========================================================================
            AnimatedVisibility(visible = showTestControls) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveBgDark),
                    border = BorderStroke(1.dp, ImmersiveAmber),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hud_test_controls_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "REAL-TIME HUD SIMULATION CONTROLS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveAmber,
                            fontFamily = FontFamily.Monospace
                        )

                        // Health Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HEALTH ADJ:",
                                fontSize = 8.5.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { onHealthChange?.invoke((currentHp - 25).coerceAtLeast(10)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveRed),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("-25 HP", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { onHealthChange?.invoke((currentHp + 25).coerceAtMost(maxHp)) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGreen),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("+25 HP", fontSize = 8.sp, color = Color.Black, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { onHealthChange?.invoke(20) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("CRITICAL 20%", fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Stealth Stance Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "STANCE:",
                                fontSize = 8.5.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Stance.values().forEach { st ->
                                    val isSelected = stealthState.stance == st
                                    Button(
                                        onClick = { onStanceChange?.invoke(st) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) ImmersiveLavender else ImmersiveBgHeader
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = st.name.take(4),
                                            fontSize = 7.5.sp,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Stealth Detection Level Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DETECTION:",
                                fontSize = 8.5.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                DetectionStatus.values().forEach { status ->
                                    val isSelected = stealthState.detectionStatus == status
                                    Button(
                                        onClick = { onDetectionChange?.invoke(status) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) ImmersiveAmber else ImmersiveBgHeader
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text(
                                            text = status.label.take(5),
                                            fontSize = 7.5.sp,
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Active Status Buff Chip Composable with remaining duration sweep meter.
 */
@Composable
fun ActiveBuffChip(buff: ActiveBuffUiModel) {
    val accentColor = if (buff.isBuff) ImmersiveGreen else ImmersiveRed
    val progressRatio = (buff.remainingSeconds / buff.durationSeconds.coerceAtLeast(1f)).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ImmersiveBgDark,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f)),
        modifier = Modifier.testTag("buff_chip_${buff.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Circular duration sweep
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = progressRatio * 360f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                buff.icon()
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buff.name,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = buff.modifierText,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${buff.remainingSeconds.toInt()}s remaining",
                    fontSize = 7.sp,
                    color = ImmersiveSlateLight,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Default sample buffs for HUD demonstration.
 */
fun defaultDemoBuffs(): List<ActiveBuffUiModel> {
    return listOf(
        ActiveBuffUiModel(
            id = "cloaking_field",
            name = "CLOAK MATRIX",
            icon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = ImmersiveGreen, modifier = Modifier.size(11.dp)) },
            isBuff = true,
            durationSeconds = 20f,
            remainingSeconds = 14f,
            modifierText = "+75% STEALTH"
        ),
        ActiveBuffUiModel(
            id = "nano_regen",
            name = "NANO REGEN",
            icon = { Icon(Icons.Default.AddReaction, contentDescription = null, tint = ImmersiveCyan, modifier = Modifier.size(11.dp)) },
            isBuff = true,
            durationSeconds = 15f,
            remainingSeconds = 9f,
            modifierText = "+5 HP/s"
        ),
        ActiveBuffUiModel(
            id = "overclock",
            name = "OVERCLOCK",
            icon = { Icon(Icons.Default.Bolt, contentDescription = null, tint = ImmersiveAmber, modifier = Modifier.size(11.dp)) },
            isBuff = true,
            durationSeconds = 30f,
            remainingSeconds = 22f,
            modifierText = "+20% SPD"
        )
    )
}
