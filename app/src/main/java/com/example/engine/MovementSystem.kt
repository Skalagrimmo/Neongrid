package com.example.engine

import com.example.model.*
import com.example.ui.AudioManager
import kotlin.math.sqrt

class MovementSystem(private val levelManager: LevelManager) {

    private val playerRadius = 0.35f
    private var lastFootstepTime: Long = 0L

    fun checkCollisionAt(x: Float, y: Float, z: Int): Boolean {
        val map = levelManager.getLevelMap(z) ?: return true

        if (x - playerRadius < 0f || x + playerRadius >= map.width ||
            y - playerRadius < 0f || y + playerRadius >= map.height) {
            return true
        }

        val minX = (x - playerRadius).toInt().coerceIn(0, map.width - 1)
        val maxX = (x + playerRadius).toInt().coerceIn(0, map.width - 1)
        val minY = (y - playerRadius).toInt().coerceIn(0, map.height - 1)
        val maxY = (y + playerRadius).toInt().coerceIn(0, map.height - 1)

        for (tx in minX..maxX) {
            for (ty in minY..maxY) {
                val tile = map.getTile(tx, ty)
                if (!tile.isWalkable) {
                    var isWalkableBelow = false
                    if (tile == TileType.EMPTY && z > 0) {
                        val mapBelow = levelManager.getLevelMap(z - 1)
                        if (mapBelow != null && mapBelow.getTile(tx, ty).isWalkable) {
                            isWalkableBelow = true
                        }
                    }

                    if (!isWalkableBelow) {
                        val closestX = x.coerceIn(tx.toFloat(), tx.toFloat() + 1f)
                        val closestY = y.coerceIn(ty.toFloat(), ty.toFloat() + 1f)

                        val distX = x - closestX
                        val distY = y - closestY
                        val distanceSquared = distX * distX + distY * distY

                        if (distanceSquared < playerRadius * playerRadius) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    data class MoveResult(
        var moved: Boolean = false,
        var lastMoveX: Float = 0f,
        var lastMoveY: Float = 0f,
        var fallDamageTriggered: Boolean = false,
        var newZLevel: Int? = null,
        var noiseRippleCreated: Boolean = false
    )

    fun processPlayerMove(
        player: Player,
        currentZLevel: Int,
        dx: Float,
        dy: Float,
        noiseRipples: MutableList<NoiseRipple>
    ): MoveResult {
        val result = MoveResult()
        if (dx == 0f && dy == 0f) return result

        val d = sqrt(dx * dx + dy * dy)
        if (d > 0.001f) {
            result.lastMoveX = dx / d
            result.lastMoveY = dy / d
        }

        val speed = player.getSpeed()
        val totalDistX = dx * speed
        val totalDistY = dy * speed

        // Multi-substep movement for smooth collision and wall sliding
        val steps = 3
        val subDx = totalDistX / steps
        val subDy = totalDistY / steps

        var currX = player.pos.x
        var currY = player.pos.y

        val map = levelManager.getLevelMap(currentZLevel) ?: return result

        for (i in 0 until steps) {
            val nextX = currX + subDx
            val nextY = currY + subDy

            if (!checkCollisionAt(nextX, nextY, currentZLevel)) {
                currX = nextX
                currY = nextY
                result.moved = true
            } else {
                // Try sliding on X axis
                if (!checkCollisionAt(nextX, currY, currentZLevel)) {
                    currX = nextX
                    result.moved = true
                }
                // Try sliding on Y axis
                else if (!checkCollisionAt(currX, nextY, currentZLevel)) {
                    currY = nextY
                    result.moved = true
                }
            }
        }

        player.pos.x = currX
        player.pos.y = currY

        if (result.moved) {
            val px = player.pos.x.toInt()
            val py = player.pos.y.toInt()
            val tile = map.getTile(px, py)

            // Play footstep sounds
            val now = System.currentTimeMillis()
            val stepInterval = if (player.isSneaking) 550L else 320L
            if (now - lastFootstepTime >= stepInterval) {
                AudioManager.playFootstep(sneaking = player.isSneaking)
                lastFootstepTime = now
            }

            // Elevation ledge drops
            if (!tile.isWalkable && currentZLevel > 0) {
                val mapBelow = levelManager.getLevelMap(currentZLevel - 1)
                if (mapBelow != null && mapBelow.getTile(px, py).isWalkable) {
                    val newZ = currentZLevel - 1
                    player.pos.z = newZ.toFloat()
                    result.newZLevel = newZ
                    result.fallDamageTriggered = true
                    
                    noiseRipples.add(NoiseRipple(player.pos.copy(), 0.5f, 8.5f))
                    result.noiseRippleCreated = true
                }
            }

            // Running sound ripples
            if (!player.isSneaking && !player.isInvisible) {
                val soundRadius = if (player.equippedSystem.id == "quiet_soles") 2.5f else 5.5f
                if (noiseRipples.isEmpty() || noiseRipples.last().pos.distanceTo(player.pos) > 2.0f) {
                    noiseRipples.add(NoiseRipple(player.pos.copy(), 0.5f, soundRadius))
                    result.noiseRippleCreated = true
                }
            }
        }

        return result
    }
}
