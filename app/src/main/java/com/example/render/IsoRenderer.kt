package com.example.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.*
import com.example.ui.theme.*

object IsoRenderer {

    fun renderScene(
        drawScope: DrawScope,
        drawPath: Path,
        currentZLevel: Int,
        levelMap: GameLevelMap?,
        gameLevels: Map<Int, GameLevelMap>,
        player: Player,
        renderPlayerX: Float,
        renderPlayerY: Float,
        enemies: List<Enemy>,
        enemyRenderPosMap: Map<String, Offset>,
        noiseRipples: List<NoiseRipple>,
        projectiles: List<Pair<Point3D, Point3D>>,
        exploredArray: Array<BooleanArray>,
        isTacticalOverlayActive: Boolean,
        lastMoveX: Float,
        lastMoveY: Float,
        tileWidth: Float = 72f,
        zHeightOffset: Float = 50f,
        canvasWidth: Float = 0f,
        canvasHeight: Float = 0f,
        centerOffsetX: Float = 0f,
        centerOffsetY: Float = 0f,
        isLowSpecMode: Boolean = true,
        toIsoFunc: (Float, Float, Float) -> Offset
    ) {
        val halfW = tileWidth / 2f
        val halfH = (tileWidth / 1.8f) / 2f

        if (levelMap == null) return

        // Fast Viewport Culling Check
        fun isVisibleOnScreen(isoPt: Offset, margin: Float = 160f): Boolean {
            if (canvasWidth <= 0f || canvasHeight <= 0f) return true
            val sx = isoPt.x + centerOffsetX
            val sy = isoPt.y + centerOffsetY
            return sx >= -margin && sx <= canvasWidth + margin && sy >= -margin && sy <= canvasHeight + margin
        }

        // 1. Z-1 Lower Underlay Pass (Skipped in Low-Spec Mode for 2x Tile Draw Savings)
        if (currentZLevel > 0 && !isLowSpecMode) {
            val levelBelow = gameLevels[currentZLevel - 1]
            if (levelBelow != null) {
                for (x in 0 until levelBelow.width) {
                    for (y in 0 until levelBelow.height) {
                        if (exploredArray[x][y]) {
                            val tileAbove = levelMap.getTile(x, y)
                            if (tileAbove == TileType.EMPTY) {
                                val tileBelow = levelBelow.getTile(x, y)
                                if (tileBelow.isWalkable) {
                                    val isoBelow = toIsoFunc(x.toFloat(), y.toFloat(), (currentZLevel - 1).toFloat())
                                    if (isVisibleOnScreen(isoBelow)) {
                                        IsoTileRenderer.drawTile(drawScope, drawPath, isoBelow, tileBelow, halfW, halfH, true, currentZLevel - 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Vision Cones Pass
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                val enemyIso = toIsoFunc(ePos.x, ePos.y, enemy.pos.z)
                if (isVisibleOnScreen(enemyIso, 250f)) {
                    IsoSchematicRenderer.drawVisionCone(drawScope, drawPath, enemyIso, enemy, toIsoFunc, isLowSpecMode)
                }
            }
        }

        // 3. Noise Ripples Pass
        for (ripple in noiseRipples) {
            if (ripple.pos.z.toInt() == currentZLevel) {
                val ripIso = toIsoFunc(ripple.pos.x, ripple.pos.y, ripple.pos.z)
                if (isVisibleOnScreen(ripIso, 200f)) {
                    IsoSchematicRenderer.drawNoiseRipple(drawScope, ripIso, ripple, halfW)
                }
            }
        }

        // 4. Tiles Floor Pass
        for (x in 0 until levelMap.width) {
            for (y in 0 until levelMap.height) {
                val isExp = exploredArray[x][y]
                if (isExp) {
                    val tile = levelMap.getTile(x, y)
                    if (tile != TileType.WALL && tile != TileType.EMPTY) {
                        val tileIso = toIsoFunc(x.toFloat(), y.toFloat(), currentZLevel.toFloat())
                        if (isVisibleOnScreen(tileIso)) {
                            IsoTileRenderer.drawTile(drawScope, drawPath, tileIso, tile, halfW, halfH, true, currentZLevel)
                        }
                    }
                }
            }
        }

        // 5. Pre-index Enemies & Walls for O(1) Interleaved Depth Pass Lookups
        val activeEnemiesByTile = mutableMapOf<Pair<Int, Int>, MutableList<Enemy>>()
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                val tileKey = Pair(ePos.x.toInt(), ePos.y.toInt())
                activeEnemiesByTile.getOrPut(tileKey) { mutableListOf() }.add(enemy)
            }
        }

        val isWallGrid = Array(levelMap.width) { x ->
            BooleanArray(levelMap.height) { y ->
                levelMap.getTile(x, y) == TileType.WALL
            }
        }

        val playerGridX = renderPlayerX.toInt()
        val playerGridY = renderPlayerY.toInt()

        val occludedEnemyIds = mutableSetOf<String>()

        // 6. Interleaved Depth-Sorted Pass (Optimized with O(1) Spatial Lookups & Viewport Culling)
        for (sum in 0 until (levelMap.width + levelMap.height)) {
            for (x in 0..sum) {
                val y = sum - x
                if (x < levelMap.width && y < levelMap.height) {
                    val isExp = exploredArray[x][y]
                    if (isExp) {
                        val isWall = isWallGrid[x][y]
                        val tileIso = toIsoFunc(x.toFloat(), y.toFloat(), currentZLevel.toFloat())
                        val inView = isVisibleOnScreen(tileIso, 180f)

                        if (inView && isWall) {
                            IsoStructureRenderer.drawWallBlock(drawScope, drawPath, tileIso, halfW, halfH, 40f, true)
                        }

                        // O(1) Enemy Lookup
                        val tileEnemies = activeEnemiesByTile[Pair(x, y)]
                        if (tileEnemies != null) {
                            for (enemy in tileEnemies) {
                                val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                                val enemyIso = toIsoFunc(ePos.x, ePos.y, enemy.pos.z)

                                // Check wall occlusion
                                var occluded = false
                                val maxCheckX = (x + 2).coerceAtMost(levelMap.width - 1)
                                val maxCheckY = (y + 2).coerceAtMost(levelMap.height - 1)
                                for (wx in x + 1..maxCheckX) {
                                    for (wy in y + 1..maxCheckY) {
                                        if (isWallGrid[wx][wy]) {
                                            occluded = true
                                            break
                                        }
                                    }
                                    if (occluded) break
                                }

                                if (occluded) {
                                    occludedEnemyIds.add(enemy.id)
                                } else if (inView) {
                                    IsoCharacterRenderer.drawEnemyCharacter(drawScope, enemyIso, enemy, false)
                                }
                            }
                        }

                        // Player Render
                        if (playerGridX == x && playerGridY == y && inView) {
                            val playerIso = toIsoFunc(renderPlayerX, renderPlayerY, currentZLevel.toFloat())
                            IsoCharacterRenderer.drawPlayerCharacter(drawScope, drawPath, playerIso, player, lastMoveX, lastMoveY)
                        }
                    }
                }
            }
        }

        // 7. X-Ray Silhouette Pass for Occluded Enemies
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel && occludedEnemyIds.contains(enemy.id)) {
                val ePos = enemyRenderPosMap[enemy.id] ?: Offset(enemy.pos.x, enemy.pos.y)
                val enemyIso = toIsoFunc(ePos.x, ePos.y, enemy.pos.z)
                if (isVisibleOnScreen(enemyIso, 120f)) {
                    IsoCharacterRenderer.drawEnemyCharacter(drawScope, enemyIso, enemy, true)
                }
            }
        }

        // 8. Projectiles Pass
        for (proj in projectiles) {
            if (proj.first.z.toInt() == currentZLevel) {
                val projIso = toIsoFunc(proj.first.x, proj.first.y, proj.first.z)
                if (isVisibleOnScreen(projIso, 80f)) {
                    IsoSchematicRenderer.drawProjectile(drawScope, projIso)
                }
            }
        }

        // 9. Tactical Overlay Pass
        if (isTacticalOverlayActive) {
            val pIso = toIsoFunc(renderPlayerX, renderPlayerY, currentZLevel.toFloat())
            drawScope.drawCircle(color = ImmersiveCyan.copy(alpha = 0.3f), radius = halfW * 3.5f, center = pIso, style = Stroke(width = 1.5f))
        }
    }
}
