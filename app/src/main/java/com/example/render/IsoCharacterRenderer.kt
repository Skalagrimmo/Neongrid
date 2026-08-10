package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.GbcGraphicsSettings
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
        lastMoveY: Float,
        gbcSettings: GbcGraphicsSettings = GbcGraphicsSettings()
    ) {
        val palette = gbcSettings.palette
        val baseAlpha = if (player.isInvisible) 0.35f else 1.0f

        // 8-bit Ground Shadow
        drawScope.drawCircle(
            color = palette.gridOutline.copy(alpha = 0.5f * baseAlpha),
            radius = 16f,
            center = isoPos + Offset(0f, 4f)
        )

        // Force shield ring if active
        if (player.equippedCore.id == "force_shield") {
            drawScope.drawCircle(
                color = palette.wallAccent.copy(alpha = 0.5f * baseAlpha),
                radius = 24f,
                center = isoPos - Offset(0f, 16f),
                style = Stroke(width = 2.5f)
            )
        }

        val celSettings = gbcSettings.celShadingSettings
        val isCel = gbcSettings.isCelShadingEnabled && celSettings.isEnabled

        // 8-Bit Cyber Body Suit
        val rawSuitColor = if (player.isSneaking) palette.wallAccent else palette.playerBody
        val suitColor = if (isCel) CelShadingEngine.applyCelShading(rawSuitColor, lightFactor = 1.10f, settings = celSettings) else rawSuitColor

        drawScope.drawCircle(
            color = suitColor.copy(alpha = baseAlpha),
            radius = 14f,
            center = isoPos - Offset(0f, 16f)
        )
        val outlineCol = if (isCel) celSettings.inkOutlineColor else if (gbcSettings.isPixelOutlineEnabled) palette.gridOutline else Color.White
        val outlineWidth = if (isCel) celSettings.inkOutlineThickness + 0.5f else 2f
        drawScope.drawCircle(
            color = outlineCol.copy(alpha = baseAlpha),
            radius = 14f,
            center = isoPos - Offset(0f, 16f),
            style = Stroke(width = outlineWidth)
        )

        // GBC Cyber Visor Helmet
        val rawVisorColor = if (player.isInvisible) palette.wallAccent else palette.playerVisor
        val visorColor = if (isCel) CelShadingEngine.applyCelShading(rawVisorColor, lightFactor = 1.25f, settings = celSettings) else rawVisorColor
        drawScope.drawCircle(
            color = visorColor.copy(alpha = baseAlpha),
            radius = 6f,
            center = isoPos - Offset(0f, 20f)
        )

        // Cel-Shaded Specular Highlight Spot on Helmet
        if (isCel) {
            drawScope.drawCircle(
                color = Color.White.copy(alpha = 0.85f * baseAlpha),
                radius = 2f,
                center = isoPos - Offset(2f, 22f)
            )
        }

        // 8-Bit Direction Arrow
        val moveLength = kotlin.math.sqrt(lastMoveX * lastMoveX + lastMoveY * lastMoveY)
        if (moveLength > 0.01f) {
            val normX = lastMoveX / moveLength
            val normY = lastMoveY / moveLength
            val arrowIsoX = (normX - normY) * 20f
            val arrowIsoY = (normX + normY) * 10f
            drawScope.drawLine(
                color = palette.terminalColor.copy(alpha = baseAlpha),
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
        isXRaySilhouette: Boolean = false,
        gbcSettings: GbcGraphicsSettings = GbcGraphicsSettings()
    ) {
        val palette = gbcSettings.palette

        if (isXRaySilhouette) {
            drawScope.drawCircle(
                color = palette.enemyAlert.copy(alpha = 0.8f),
                radius = 12f,
                center = isoPos - Offset(0f, 14f),
                style = Stroke(width = 2.5f)
            )
            return
        }

        // Ground Shadow
        drawScope.drawCircle(
            color = palette.gridOutline.copy(alpha = 0.5f),
            radius = 14f,
            center = isoPos + Offset(0f, 4f)
        )

        val celSettings = gbcSettings.celShadingSettings
        val isCel = gbcSettings.isCelShadingEnabled && celSettings.isEnabled

        // Enemy Color by Alert State (GBC Palette Mapping)
        val rawColor = when (enemy.alertState) {
            AlertState.PATROLLING -> palette.enemyPatrol
            AlertState.SUSPICIOUS -> palette.enemySuspicious
            AlertState.ALERTED -> palette.enemyAlert
        }

        val color = if (isCel) CelShadingEngine.applyCelShading(rawColor, lightFactor = 1.15f, settings = celSettings) else rawColor

        val bodyRadius = if (enemy.type == "Boss") 20f else 12f
        drawScope.drawCircle(
            color = color,
            radius = bodyRadius,
            center = isoPos - Offset(0f, 14f)
        )
        val outlineCol = if (isCel) celSettings.inkOutlineColor else if (gbcSettings.isPixelOutlineEnabled) palette.gridOutline else Color.White
        val outlineWidth = if (isCel) celSettings.inkOutlineThickness + 0.5f else 2f
        drawScope.drawCircle(
            color = outlineCol,
            radius = bodyRadius,
            center = isoPos - Offset(0f, 14f),
            style = Stroke(width = outlineWidth)
        )

        // Direction Indicator Line
        val dirIsoX = (cos(enemy.directionAngle) - sin(enemy.directionAngle)) * (bodyRadius + 8f)
        val dirIsoY = (cos(enemy.directionAngle) + sin(enemy.directionAngle)) * (bodyRadius / 2f + 4f)
        drawScope.drawLine(
            color = palette.terminalColor,
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
                color = palette.gridOutline,
                topLeft = barTopLeft,
                size = androidx.compose.ui.geometry.Size(barW, barH)
            )
            val hpPct = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
            drawScope.drawRect(
                color = palette.enemyAlert,
                topLeft = barTopLeft,
                size = androidx.compose.ui.geometry.Size(barW * hpPct, barH)
            )
        }
    }
}

