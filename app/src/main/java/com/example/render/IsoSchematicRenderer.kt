package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.NoiseRipple
import com.example.model.Point3D
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

object IsoSchematicRenderer {

    fun drawVisionCone(
        drawScope: DrawScope,
        drawPath: Path,
        enemyIso: Offset,
        enemy: Enemy,
        toIsoFunc: (Float, Float, Float) -> Offset,
        isLowSpecMode: Boolean = true
    ) {
        val fov = enemy.getVisionConeAngle()
        val range = enemy.getVisionRange()
        val startAngle = enemy.directionAngle - fov / 2f
        val steps = if (isLowSpecMode) 4 else 10
        val stepAngle = fov / steps

        drawPath.reset()
        val eyeIso = enemyIso - Offset(0f, 12f)
        drawPath.moveTo(eyeIso.x, eyeIso.y)

        for (i in 0..steps) {
            val angle = startAngle + i * stepAngle
            val gridX = enemy.pos.x + cos(angle) * range
            val gridY = enemy.pos.y + sin(angle) * range
            val isoPt = toIsoFunc(gridX, gridY, enemy.pos.z)
            drawPath.lineTo(isoPt.x, isoPt.y)
        }
        drawPath.close()

        val coneColor = when (enemy.alertState) {
            AlertState.PATROLLING -> ImmersiveGreen.copy(alpha = 0.18f)
            AlertState.SUSPICIOUS -> ImmersiveAmber.copy(alpha = 0.28f)
            AlertState.ALERTED -> ImmersiveRed.copy(alpha = 0.38f)
        }
        val strokeColor = when (enemy.alertState) {
            AlertState.PATROLLING -> ImmersiveGreen.copy(alpha = 0.5f)
            AlertState.SUSPICIOUS -> ImmersiveAmber.copy(alpha = 0.7f)
            AlertState.ALERTED -> ImmersiveRed.copy(alpha = 0.9f)
        }

        drawScope.drawPath(drawPath, color = coneColor, style = Fill)
        drawScope.drawPath(drawPath, color = strokeColor, style = Stroke(width = 1.5f))
    }

    fun drawNoiseRipple(
        drawScope: DrawScope,
        rippleIso: Offset,
        ripple: NoiseRipple,
        tileHalfWidth: Float
    ) {
        val radPx = ripple.radius * tileHalfWidth
        val alpha = (1f - ripple.radius / ripple.maxRadius).coerceIn(0f, 1f)
        drawScope.drawCircle(
            color = ImmersiveCyan.copy(alpha = alpha * 0.7f),
            radius = radPx,
            center = rippleIso,
            style = Stroke(width = 2f)
        )
    }

    fun drawProjectile(
        drawScope: DrawScope,
        projIso: Offset
    ) {
        drawScope.drawCircle(
            color = ImmersiveRed,
            radius = 6f,
            center = projIso
        )
        drawScope.drawCircle(
            color = Color.Yellow,
            radius = 3f,
            center = projIso
        )
    }
}
