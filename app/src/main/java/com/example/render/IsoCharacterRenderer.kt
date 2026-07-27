package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.Player
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

object IsoCharacterRenderer {

    fun drawPlayerCharacter(
        drawScope: DrawScope,
        drawPath: Path,
        isoPos: Offset,
        player: Player,
        lastMoveX: Float,
        lastMoveY: Float
    ) {
        val baseAlpha = if (player.isInvisible) 0.35f else 1.0f

        // Shadow under player
        drawScope.drawCircle(
            color = Color.Black.copy(alpha = 0.4f * baseAlpha),
            radius = 16f,
            center = isoPos + Offset(0f, 4f)
        )

        // Force shield ring if active
        if (player.equippedCore.id == "force_shield") {
            drawScope.drawCircle(
                color = ImmersiveCyan.copy(alpha = 0.4f * baseAlpha),
                radius = 24f,
                center = isoPos - Offset(0f, 16f),
                style = Stroke(width = 2f)
            )
        }

        // Cyber Body Suit
        val suitColor = if (player.isSneaking) ImmersiveBlue else ImmersiveLavender
        drawScope.drawCircle(
            color = suitColor.copy(alpha = baseAlpha),
            radius = 14f,
            center = isoPos - Offset(0f, 16f)
        )
        drawScope.drawCircle(
            color = Color.White.copy(alpha = baseAlpha),
            radius = 14f,
            center = isoPos - Offset(0f, 16f),
            style = Stroke(width = 2f)
        )

        // Cyber Visor Helmet
        val visorColor = if (player.isInvisible) ImmersiveCyan else ImmersiveGreen
        drawScope.drawCircle(
            color = visorColor.copy(alpha = baseAlpha),
            radius = 6f,
            center = isoPos - Offset(0f, 20f)
        )

        // Direction Arrow
        val moveLength = kotlin.math.sqrt(lastMoveX * lastMoveX + lastMoveY * lastMoveY)
        if (moveLength > 0.01f) {
            val normX = lastMoveX / moveLength
            val normY = lastMoveY / moveLength
            val arrowIsoX = (normX - normY) * 20f
            val arrowIsoY = (normX + normY) * 10f
            drawScope.drawLine(
                color = ImmersiveAmber.copy(alpha = baseAlpha),
                start = isoPos - Offset(0f, 16f),
                end = isoPos - Offset(0f, 16f) + Offset(arrowIsoX, arrowIsoY),
                strokeWidth = 3f
            )
        }
    }

    fun drawEnemyCharacter(
        drawScope: DrawScope,
        isoPos: Offset,
        enemy: Enemy,
        isXRaySilhouette: Boolean = false
    ) {
        if (isXRaySilhouette) {
            drawScope.drawCircle(
                color = ImmersiveRed.copy(alpha = 0.6f),
                radius = 12f,
                center = isoPos - Offset(0f, 14f),
                style = Stroke(width = 2f)
            )
            return
        }

        // Shadow
        drawScope.drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = 14f,
            center = isoPos + Offset(0f, 4f)
        )

        // Enemy Color by Alert State
        val color = when (enemy.alertState) {
            AlertState.PATROLLING -> ImmersiveGreen
            AlertState.SUSPICIOUS -> ImmersiveAmber
            AlertState.ALERTED -> ImmersiveRed
        }

        // Enemy Sprite Shape (Drones are diamond, Sentries are circle, Boss is big circle)
        val bodyRadius = if (enemy.type == "Boss") 20f else 12f
        drawScope.drawCircle(
            color = color,
            radius = bodyRadius,
            center = isoPos - Offset(0f, 14f)
        )
        drawScope.drawCircle(
            color = Color.White,
            radius = bodyRadius,
            center = isoPos - Offset(0f, 14f),
            style = Stroke(width = 1.5f)
        )

        // Direction Indicator Line
        val dirIsoX = (cos(enemy.directionAngle) - sin(enemy.directionAngle)) * (bodyRadius + 8f)
        val dirIsoY = (cos(enemy.directionAngle) + sin(enemy.directionAngle)) * (bodyRadius / 2f + 4f)
        drawScope.drawLine(
            color = Color.Yellow,
            start = isoPos - Offset(0f, 14f),
            end = isoPos - Offset(0f, 14f) + Offset(dirIsoX, dirIsoY),
            strokeWidth = 2.5f
        )

        // Health Bar above enemy head
        if (enemy.health < enemy.maxHealth) {
            val barW = 30f
            val barH = 4f
            val barTopLeft = isoPos - Offset(barW / 2f, bodyRadius + 24f)
            drawScope.drawRect(
                color = Color.Black,
                topLeft = barTopLeft,
                size = androidx.compose.ui.geometry.Size(barW, barH)
            )
            val hpPct = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
            drawScope.drawRect(
                color = ImmersiveRed,
                topLeft = barTopLeft,
                size = androidx.compose.ui.geometry.Size(barW * hpPct, barH)
            )
        }
    }
}
