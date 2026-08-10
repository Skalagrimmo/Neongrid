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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Enumeration of stealth states representing the player's visibility level.
 */
enum class StealthState(
    val label: String,
    val baseColor: Color,
    val defaultIcon: @Composable () -> Unit
) {
    HIDDEN(
        label = "STEALTHED",
        baseColor = ImmersiveGreen,
        defaultIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = "Stealthed", tint = ImmersiveGreen) }
    ),
    IN_COVER(
        label = "IN COVER",
        baseColor = ImmersiveCyan,
        defaultIcon = { Icon(Icons.Default.Shield, contentDescription = "In Cover", tint = ImmersiveCyan) }
    ),
    SUSPICIOUS(
        label = "SUSPICIOUS",
        baseColor = ImmersiveAmber,
        defaultIcon = { Icon(Icons.Default.Warning, contentDescription = "Suspicious", tint = ImmersiveAmber) }
    ),
    DETECTED(
        label = "DETECTED",
        baseColor = ImmersiveRed,
        defaultIcon = { Icon(Icons.Default.Visibility, contentDescription = "Detected", tint = ImmersiveRed) }
    )
}

/**
 * A Jetpack Compose overlay component that dynamically visualizes the player's
 * 'stealth level', proximity to cover, and proximity to hostile enemies.
 *
 * Uses smooth Compose animation states for vignette canvas glows, pulsating radar rings,
 * and a status badge with live visibility feedback.
 */
@Composable
fun StealthVisibilityOverlay(
    stealthLevel: Float, // 0.0f (Fully Exposed / Detected) to 1.0f (Completely Hidden)
    proximityToEnemy: Float, // 0.0f (Safe / Far) to 1.0f (Immediate Danger)
    isNearCover: Boolean,
    isSneaking: Boolean,
    modifier: Modifier = Modifier,
    enemyDistance: Float? = null,
    showControlsDemo: Boolean = false,
    playerPos: Point3D = Point3D(5f, 5f, 1f),
    playerFacingAngle: Float = 0f,
    enemies: List<Enemy> = emptyList()
) {
    // Determine overall stealth state based on parameters
    val currentState = remember(stealthLevel, proximityToEnemy, isNearCover) {
        when {
            stealthLevel < 0.25f || proximityToEnemy > 0.85f -> StealthState.DETECTED
            proximityToEnemy > 0.45f || stealthLevel < 0.6f -> StealthState.SUSPICIOUS
            isNearCover -> StealthState.IN_COVER
            else -> StealthState.HIDDEN
        }
    }

    // --- Compose Animation States ---
    val targetStealth = stealthLevel.coerceIn(0f, 1f)
    val animatedStealthLevel by animateFloatAsState(
        targetValue = targetStealth,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedStealthLevel"
    )

    val targetProximity = proximityToEnemy.coerceIn(0f, 1f)
    val animatedProximity by animateFloatAsState(
        targetValue = targetProximity,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "animatedProximity"
    )

    // Dynamic state accent color animation
    val animatedAccentColor by animateColorAsState(
        targetValue = currentState.baseColor,
        animationSpec = tween(durationMillis = 400),
        label = "animatedAccentColor"
    )

    // Pulsating animation for threat / detected state
    val infiniteTransition = rememberInfiniteTransition(label = "stealthPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (currentState == StealthState.DETECTED || targetProximity > 0.7f) 1.25f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (currentState == StealthState.DETECTED) 400 else 800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("stealth_visibility_overlay")
    ) {
        // =========================================================================
        // 1. SCREEN CORNER VIGNETTE & HAZARD ATMOSPHERE CANVAS
        // =========================================================================
        val vignetteAlpha = (1f - animatedStealthLevel) * 0.45f + (animatedProximity * 0.35f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("stealth_vignette_canvas")
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension * 0.8f

            // Radial gradient vignette glowing along screen edges
            val edgeBrush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    animatedAccentColor.copy(alpha = 0.05f),
                    animatedAccentColor.copy(alpha = vignetteAlpha.coerceIn(0.1f, 0.7f))
                ),
                center = center,
                radius = maxRadius
            )
            drawRect(brush = edgeBrush)

            // Inner stealth boundary indicator ring
            if (isSneaking || isNearCover) {
                drawCircle(
                    color = animatedAccentColor.copy(alpha = 0.25f),
                    radius = maxRadius * 0.85f * pulseScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // =========================================================================
        // 2. STEALTH & VISIBILITY BADGE (TOP CENTER OVERLAY)
        // =========================================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ImmersiveBgHeader.copy(alpha = 0.92f),
                border = BorderStroke(1.5.dp, animatedAccentColor.copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .scale(if (currentState == StealthState.DETECTED) pulseScale else 1f)
                    .testTag("stealth_badge_indicator")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animated Eye / Status Icon with Ring Meter
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Circular stealth meter track
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            // Animated active arc representing stealth level %
                            drawArc(
                                color = animatedAccentColor,
                                startAngle = -90f,
                                sweepAngle = animatedStealthLevel * 360f,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }

                        // Status Icon
                        currentState.defaultIcon()
                    }

                    // Stealth Level & State Text
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentState.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = animatedAccentColor,
                                letterSpacing = 0.8.sp
                            )
                            if (isNearCover) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• COVER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveCyan,
                                    modifier = Modifier.testTag("cover_status_chip")
                                )
                            }
                        }

                        // Stealth % & Distance Meter
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Stealth: ${(animatedStealthLevel * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("stealth_level_meter")
                            )

                            if (enemyDistance != null && enemyDistance < 15f) {
                                Text(
                                    text = " | Enemy: ${"%.1f".format(enemyDistance)}m",
                                    fontSize = 10.5.sp,
                                    color = if (enemyDistance < 5f) ImmersiveRed else ImmersiveAmber,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Proximity Hazard Alert Strip
            AnimatedVisibility(
                visible = animatedProximity > 0.4f || currentState == StealthState.DETECTED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (currentState == StealthState.DETECTED) ImmersiveRed else ImmersiveAmber).copy(alpha = 0.2f),
                    border = BorderStroke(
                        1.dp,
                        if (currentState == StealthState.DETECTED) ImmersiveRed else ImmersiveAmber
                    ),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .testTag("proximity_hazard_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "Threat Proximity",
                            tint = if (currentState == StealthState.DETECTED) ImmersiveRed else ImmersiveAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (currentState == StealthState.DETECTED) "ALERT: ENEMY PROXIMITY CRITICAL" else "WARNING: HOSTILE MOVEMENT NEARBY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 3. VISUAL RADAR & DIRECTIONAL INDICATOR OVERLAY (BOTTOM RIGHT HUD)
        // =========================================================================
        VisualRadarOverlay(
            playerPos = playerPos,
            playerFacingAngle = playerFacingAngle,
            enemies = enemies,
            stealthLevel = animatedStealthLevel,
            proximityToEnemy = animatedProximity,
            isNearCover = isNearCover,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 95.dp)
        )
    }
}

/**
 * Visual Radar & Directional Indicator Overlay Composable.
 * Renders a circular tactical radar HUD displaying nearby enemies, directional indicators,
 * rotating sweep, and pulsing threat rings that shift color based on current detected stealth level.
 */
@Composable
fun VisualRadarOverlay(
    playerPos: Point3D,
    playerFacingAngle: Float = 0f,
    enemies: List<Enemy> = emptyList(),
    stealthLevel: Float = 1.0f,
    proximityToEnemy: Float = 0.0f,
    isNearCover: Boolean = false,
    modifier: Modifier = Modifier,
    maxRadarDistance: Float = 16f
) {
    val currentState = remember(stealthLevel, proximityToEnemy, isNearCover) {
        when {
            stealthLevel < 0.25f || proximityToEnemy > 0.85f -> StealthState.DETECTED
            proximityToEnemy > 0.45f || stealthLevel < 0.6f -> StealthState.SUSPICIOUS
            isNearCover -> StealthState.IN_COVER
            else -> StealthState.HIDDEN
        }
    }

    val animatedAccentColor by animateColorAsState(
        targetValue = currentState.baseColor,
        animationSpec = tween(durationMillis = 400),
        label = "radarAccentColor"
    )

    val activeEnemies = remember(enemies, playerPos.z) {
        enemies.filter { abs(it.pos.z - playerPos.z) < 0.5f }
    }

    val closestEnemyDist = remember(activeEnemies, playerPos) {
        activeEnemies.minOfOrNull { it.pos.distanceTo(playerPos) } ?: 999f
    }

    val isEnemyNearby = closestEnemyDist < maxRadarDistance || proximityToEnemy > 0.05f

    // Dynamic pulse frequency & scale based on proximity and threat
    val pulseDurationMillis = when {
        currentState == StealthState.DETECTED -> 400
        closestEnemyDist < 5f -> 500
        closestEnemyDist < 10f -> 750
        else -> 1200
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarPulseTransition")

    val pulseRadiusRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulseRadius"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = pulseDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulseAlpha"
    )

    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarSweepAngle"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.92f)),
        border = BorderStroke(1.5.dp, animatedAccentColor.copy(alpha = 0.75f)),
        modifier = modifier
            .size(136.dp)
            .testTag("visual_radar_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Radar Sub-header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(animatedAccentColor, CircleShape)
                    )
                    Text(
                        text = "RADAR",
                        color = animatedAccentColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("radar_title_tag")
                    )
                }
                Text(
                    text = "${activeEnemies.size} HOSTILES",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 7.5.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.testTag("radar_hostile_count")
                )
            }

            // Radar Canvas View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                        .testTag("visual_radar_canvas")
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radarRadius = (size.minDimension / 2f) - 4.dp.toPx()

                    // Background Disc
                    drawCircle(
                        color = Color(0xFF090C14).copy(alpha = 0.9f),
                        radius = radarRadius,
                        center = center
                    )

                    // Concentric Range Rings (5m, 10m, 15m)
                    for (rFactor in listOf(0.33f, 0.66f, 1.0f)) {
                        drawCircle(
                            color = animatedAccentColor.copy(alpha = 0.25f),
                            radius = radarRadius * rFactor,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Crosshair Grid Lines
                    drawLine(
                        color = animatedAccentColor.copy(alpha = 0.2f),
                        start = Offset(center.x - radarRadius, center.y),
                        end = Offset(center.x + radarRadius, center.y),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = animatedAccentColor.copy(alpha = 0.2f),
                        start = Offset(center.x, center.y - radarRadius),
                        end = Offset(center.x, center.y + radarRadius),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Pulsing Threat Ring when nearby enemies detected
                    if (isEnemyNearby) {
                        drawCircle(
                            color = animatedAccentColor.copy(alpha = pulseAlpha * 0.7f),
                            radius = radarRadius * pulseRadiusRatio,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Rotating Sweep Line
                    val sweepRad = Math.toRadians(sweepAngle.toDouble())
                    val sweepEndX = center.x + radarRadius * cos(sweepRad).toFloat()
                    val sweepEndY = center.y + radarRadius * sin(sweepRad).toFloat()

                    drawLine(
                        color = animatedAccentColor.copy(alpha = 0.6f),
                        start = center,
                        end = Offset(sweepEndX, sweepEndY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Center Player Marker
                    drawCircle(
                        color = ImmersiveCyan,
                        radius = 3.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 1.5.dp.toPx(),
                        center = center
                    )

                    // Enemy Blips & Directional Indicators
                    for (enemy in activeEnemies) {
                        val dx = enemy.pos.x - playerPos.x
                        val dy = enemy.pos.y - playerPos.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val angle = atan2(dy, dx)

                        val blipColor = when (enemy.alertState) {
                            AlertState.ALERTED -> ImmersiveRed
                            AlertState.SUSPICIOUS -> ImmersiveAmber
                            AlertState.PATROLLING -> animatedAccentColor
                        }

                        if (dist <= maxRadarDistance) {
                            // On-radar blip
                            val normRadius = (dist / maxRadarDistance) * radarRadius
                            val blipX = center.x + normRadius * cos(angle)
                            val blipY = center.y + normRadius * sin(angle)

                            // Draw enemy blip dot
                            drawCircle(
                                color = blipColor,
                                radius = 3.5.dp.toPx(),
                                center = Offset(blipX, blipY)
                            )

                            // Alerted enemy glow pulse
                            if (enemy.alertState == AlertState.ALERTED) {
                                drawCircle(
                                    color = ImmersiveRed.copy(alpha = pulseAlpha),
                                    radius = 7.dp.toPx(),
                                    center = Offset(blipX, blipY),
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            }
                        } else if (dist <= maxRadarDistance * 1.8f) {
                            // Off-radar directional indicator on radar perimeter edge
                            val edgeRadius = radarRadius - 3.dp.toPx()
                            val tipX = center.x + edgeRadius * cos(angle)
                            val tipY = center.y + edgeRadius * sin(angle)

                            val arrowSize = 5.dp.toPx()
                            val perpAngle1 = angle + Math.PI.toFloat() * 0.8f
                            val perpAngle2 = angle - Math.PI.toFloat() * 0.8f

                            val arrowPath = Path().apply {
                                moveTo(tipX, tipY)
                                lineTo(tipX + arrowSize * cos(perpAngle1), tipY + arrowSize * sin(perpAngle1))
                                lineTo(tipX + arrowSize * cos(perpAngle2), tipY + arrowSize * sin(perpAngle2))
                                close()
                            }
                            drawPath(path = arrowPath, color = blipColor)
                        }
                    }

                    // Outer Bezel Ring
                    drawCircle(
                        color = animatedAccentColor,
                        radius = radarRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}
