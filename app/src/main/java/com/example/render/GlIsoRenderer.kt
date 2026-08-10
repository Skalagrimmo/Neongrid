package com.example.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.model.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GlIsoRenderer : GLSurfaceView.Renderer {

    private val batch = GlBatchRenderer()

    var screenWidth = 1
    var screenHeight = 1

    // Game State References
    var currentZLevel = 0
    var levelMap: GameLevelMap? = null
    var gameLevels = mapOf<Int, GameLevelMap>()
    var player = Player()
    var renderPlayerX = 0f
    var renderPlayerY = 0f
    var enemies = emptyList<Enemy>()
    var enemyRenderPosMap = mapOf<String, Offset>()
    var noiseRipples = emptyList<NoiseRipple>()
    var projectiles = emptyList<Pair<Point3D, Point3D>>()
    var exploredArray: Array<BooleanArray>? = null
    var isTacticalOverlayActive = false
    var lastMoveX = 0f
    var lastMoveY = 0f
    var tileWidth = 72f
    var zHeightOffset = 50f
    var renderCamX = 0f
    var renderCamY = 0f
    var isLowSpecMode = false
    var gbcSettings = GbcGraphicsSettings()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(
            gbcSettings.palette.bgDark.red,
            gbcSettings.palette.bgDark.green,
            gbcSettings.palette.bgDark.blue,
            1.0f
        )
        batch.initGL()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        batch.setScreenSize(screenWidth, screenHeight)
    }

    override fun onDrawFrame(gl: GL10?) {
        val palette = gbcSettings.palette
        GLES30.glClearColor(palette.bgDark.red, palette.bgDark.green, palette.bgDark.blue, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        batch.beginBatch(
            enableScanlines = gbcSettings.isScanlinesEnabled,
            enableCelShading = gbcSettings.isCelShadingEnabled && gbcSettings.celShadingSettings.isEnabled,
            celBands = gbcSettings.celShadingSettings.bands.toFloat()
        )

        val halfW = tileWidth / 2f
        val halfH = (tileWidth / 1.8f) / 2f

        val centerOffsetX = screenWidth / 2f - renderCamX
        val centerOffsetY = screenHeight / 2f - renderCamY

        fun toIso(x: Float, y: Float, z: Float): Offset {
            val sx = (x - y) * halfW
            val sy = (x + y) * halfH - z * zHeightOffset
            return Offset(centerOffsetX + sx, centerOffsetY + sy)
        }

        fun isVisibleOnScreen(isoPt: Offset, margin: Float = 140f): Boolean {
            return isoPt.x >= -margin && isoPt.x <= screenWidth + margin &&
                   isoPt.y >= -margin && isoPt.y <= screenHeight + margin
        }

        val map = levelMap ?: return
        val exp = exploredArray

        val strokeColor = if (gbcSettings.isPixelOutlineEnabled) palette.gridOutline else palette.wallAccent.copy(alpha = 0.35f)

        // 1. Render Ground Tiles
        for (y in 0 until map.height) {
            for (x in 0 until map.width) {
                val isExplored = exp?.getOrNull(x)?.getOrNull(y) ?: true
                if (!isExplored) continue

                val tile = map.getTile(x, y)
                if (tile == TileType.EMPTY || tile == TileType.WALL) continue

                val iso = toIso(x.toFloat(), y.toFloat(), currentZLevel.toFloat())
                if (!isVisibleOnScreen(iso)) continue

                val tileColor = when (tile) {
                    TileType.FLOOR -> palette.floorPrimary
                    TileType.GRID_ROAD -> palette.floorSecondary
                    TileType.LASER_GRID -> Color(0x66FF0055)
                    TileType.TERMINAL -> palette.terminalColor.copy(alpha = 0.3f)
                    TileType.EXIT_PORTAL -> palette.wallAccent.copy(alpha = 0.4f)
                    TileType.BARREL_EXPLOSIVE -> Color(0x88FF5500)
                    TileType.LADDER_UP, TileType.LADDER_DOWN -> palette.terminalColor.copy(alpha = 0.5f)
                    else -> palette.floorPrimary
                }

                batch.drawIsoDiamond(iso.x, iso.y, halfW, halfH, tileColor, strokeColor, 1.2f)

                if (gbcSettings.isPixelDitherEnabled && (x + y) % 2 == 0) {
                    batch.drawCircle(iso.x, iso.y, 2f, palette.wallAccent.copy(alpha = 0.4f), 8, true)
                }

                if (tile == TileType.LASER_GRID) {
                    batch.drawLine(iso.x - halfW * 0.5f, iso.y, iso.x + halfW * 0.5f, iso.y, Color(0xFFFF0055), 3f)
                } else if (tile == TileType.BARREL_EXPLOSIVE) {
                    batch.drawCircle(iso.x, iso.y - 6f, 8f, Color(0xFFFF3300), 12, true)
                    batch.drawCircle(iso.x, iso.y - 6f, 8f, palette.gridOutline, 12, false, 1.5f)
                } else if (tile == TileType.TERMINAL) {
                    batch.drawIsoDiamond(iso.x, iso.y - 4f, halfW * 0.4f, halfH * 0.4f, palette.terminalColor)
                }
            }
        }

        // 2. Enemy Vision Cones & Sound Ripples
        for (enemy in enemies) {
            if (enemy.isDead || enemy.pos.z.toInt() != currentZLevel) continue
            val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
            val eIso = toIso(ePos.x, ePos.y, enemy.pos.z)

            if (isVisibleOnScreen(eIso)) {
                val coneAngle = 0.8f
                val coneDist = 120f
                val c1 = eIso + Offset(cos(enemy.directionAngle - coneAngle) * coneDist, sin(enemy.directionAngle - coneAngle) * coneDist)
                val c2 = eIso + Offset(cos(enemy.directionAngle + coneAngle) * coneDist, sin(enemy.directionAngle + coneAngle) * coneDist)

                val coneCol = when (enemy.alertState) {
                    AlertState.PATROLLING -> palette.enemyPatrol.copy(alpha = 0.15f)
                    AlertState.SUSPICIOUS -> palette.enemySuspicious.copy(alpha = 0.25f)
                    AlertState.ALERTED -> palette.enemyAlert.copy(alpha = 0.35f)
                }
                batch.drawTriangle(eIso.x, eIso.y - 12f, c1.x, c1.y, c2.x, c2.y, coneCol)
            }
        }

        for (ripple in noiseRipples) {
            if (ripple.pos.z.toInt() == currentZLevel) {
                val rIso = toIso(ripple.pos.x, ripple.pos.y, ripple.pos.z)
                if (isVisibleOnScreen(rIso)) {
                    val alpha = (1.0f - ripple.radius / ripple.maxRadius).coerceIn(0f, 1f)
                    batch.drawCircle(rIso.x, rIso.y, ripple.radius * 16f, palette.wallAccent.copy(alpha = alpha * 0.5f), 24, false, 2f)
                }
            }
        }

        // 3. Interleaved Depth-Sorted Pass (Walls, Player, Enemies)
        val playerGridX = kotlin.math.round(renderPlayerX).toInt()
        val playerGridY = kotlin.math.round(renderPlayerY).toInt()

        val activeEnemiesByTile = mutableMapOf<Pair<Int, Int>, MutableList<Enemy>>()
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                val ex = kotlin.math.round(ePos.x).toInt()
                val ey = kotlin.math.round(ePos.y).toInt()
                activeEnemiesByTile.getOrPut(Pair(ex, ey)) { mutableListOf() }.add(enemy)
            }
        }

        val wallHeight = 40f

        for (sum in 0 until (map.width + map.height)) {
            for (x in 0..sum) {
                val y = sum - x
                if (x in 0 until map.width && y in 0 until map.height) {
                    val tileIso = toIso(x.toFloat(), y.toFloat(), currentZLevel.toFloat())
                    val inView = isVisibleOnScreen(tileIso, 180f)

                    // Draw 3D Wall Block
                    if (inView && map.getTile(x, y) == TileType.WALL) {
                        batch.drawIsoCube(
                            tileIso.x, tileIso.y,
                            halfW, halfH,
                            wallHeight,
                            topColor = palette.wallTop,
                            leftColor = palette.wallPrimary,
                            rightColor = palette.wallPrimary.copy(alpha = 0.85f),
                            strokeColor = strokeColor,
                            strokeWidth = 1.5f
                        )

                        if (gbcSettings.isPixelDitherEnabled) {
                            batch.drawCircle(tileIso.x, tileIso.y - wallHeight, 2f, palette.wallAccent.copy(alpha = 0.4f), 8, true)
                        }
                    }

                    // Draw Enemies at (x, y)
                    activeEnemiesByTile[Pair(x, y)]?.forEach { enemy ->
                        val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                        val enemyIso = toIso(ePos.x, ePos.y, enemy.pos.z)

                        if (inView) {
                            // Enemy Shadow
                            batch.drawCircle(enemyIso.x, enemyIso.y + 4f, 14f, palette.gridOutline.copy(alpha = 0.5f), 16, true)

                            // Enemy Body
                            val enemyColor = when (enemy.alertState) {
                                AlertState.PATROLLING -> palette.enemyPatrol
                                AlertState.SUSPICIOUS -> palette.enemySuspicious
                                AlertState.ALERTED -> palette.enemyAlert
                            }
                            val radius = if (enemy.type == "Boss") 20f else 12f

                            batch.drawCircle(enemyIso.x, enemyIso.y - 14f, radius, enemyColor, 16, true)
                            batch.drawCircle(enemyIso.x, enemyIso.y - 14f, radius, palette.gridOutline, 16, false, 2f)

                            // Direction line
                            val dirIsoX = (cos(enemy.directionAngle) - sin(enemy.directionAngle)) * (radius / 2f + 4f)
                            val dirIsoY = (cos(enemy.directionAngle) + sin(enemy.directionAngle)) * (radius / 2f + 4f)
                            batch.drawLine(enemyIso.x, enemyIso.y - 14f, enemyIso.x + dirIsoX, enemyIso.y - 14f + dirIsoY, palette.terminalColor, 2.5f)

                            // HP Bar if damaged
                            if (enemy.health < enemy.maxHealth) {
                                val hpPct = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
                                val barW = 28f
                                batch.drawQuad(
                                    enemyIso.x - barW / 2f, enemyIso.y - radius - 22f,
                                    enemyIso.x + barW / 2f, enemyIso.y - radius - 22f,
                                    enemyIso.x + barW / 2f, enemyIso.y - radius - 18f,
                                    enemyIso.x - barW / 2f, enemyIso.y - radius - 18f,
                                    palette.gridOutline
                                )
                                batch.drawQuad(
                                    enemyIso.x - barW / 2f, enemyIso.y - radius - 22f,
                                    enemyIso.x - barW / 2f + barW * hpPct, enemyIso.y - radius - 22f,
                                    enemyIso.x - barW / 2f + barW * hpPct, enemyIso.y - radius - 18f,
                                    enemyIso.x - barW / 2f, enemyIso.y - radius - 18f,
                                    palette.enemyAlert
                                )
                            }
                        }
                    }

                    // Draw Player Character at (x, y)
                    if (playerGridX == x && playerGridY == y && inView) {
                        val playerIso = toIso(renderPlayerX, renderPlayerY, currentZLevel.toFloat())
                        val baseAlpha = if (player.isInvisible) 0.35f else 1.0f

                        // Shadow
                        batch.drawCircle(playerIso.x, playerIso.y + 4f, 16f, palette.gridOutline.copy(alpha = 0.5f * baseAlpha), 16, true)

                        // Force shield ring
                        if (player.equippedCore.id == "force_shield") {
                            batch.drawCircle(playerIso.x, playerIso.y - 16f, 24f, palette.wallAccent.copy(alpha = 0.5f * baseAlpha), 20, false, 2.5f)
                        }

                        // Body
                        val suitColor = if (player.isSneaking) palette.wallAccent else palette.playerBody
                        batch.drawCircle(playerIso.x, playerIso.y - 16f, 14f, suitColor.copy(alpha = baseAlpha), 18, true)
                        batch.drawCircle(playerIso.x, playerIso.y - 16f, 14f, strokeColor.copy(alpha = baseAlpha), 18, false, 2f)

                        // Visor
                        val visorColor = if (player.isInvisible) palette.wallAccent else palette.playerVisor
                        batch.drawCircle(playerIso.x, playerIso.y - 20f, 6f, visorColor.copy(alpha = baseAlpha), 12, true)

                        // Direction Pointer
                        val moveLength = kotlin.math.sqrt(lastMoveX * lastMoveX + lastMoveY * lastMoveY)
                        if (moveLength > 0.01f) {
                            val normX = lastMoveX / moveLength
                            val normY = lastMoveY / moveLength
                            val arrowIsoX = (normX - normY) * 20f
                            val arrowIsoY = (normX + normY) * 10f
                            batch.drawLine(playerIso.x, playerIso.y - 16f, playerIso.x + arrowIsoX, playerIso.y - 16f + arrowIsoY, palette.terminalColor.copy(alpha = baseAlpha), 3f)
                        }
                    }
                }
            }
        }

        // 4. Projectiles
        for (proj in projectiles) {
            val pIso = toIso(proj.first.x, proj.first.y, proj.first.z)
            if (isVisibleOnScreen(pIso)) {
                batch.drawCircle(pIso.x, pIso.y - 10f, 6f, palette.wallAccent, 12, true)
            }
        }

        // 5. Tactical Overlay
        if (isTacticalOverlayActive) {
            val pIso = toIso(renderPlayerX, renderPlayerY, currentZLevel.toFloat())
            batch.drawCircle(pIso.x, pIso.y, halfW * 3.5f, palette.wallAccent.copy(alpha = 0.4f), 28, false, 2f)
        }

        batch.flush()
    }
}
