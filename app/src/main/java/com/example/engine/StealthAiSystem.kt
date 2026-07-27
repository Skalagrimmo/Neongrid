package com.example.engine

import com.example.model.*
import com.example.ui.AudioManager
import kotlin.math.*

class StealthAiSystem {

    interface AiLogListener {
        fun onLog(message: String)
    }

    var logListener: AiLogListener? = null

    private fun log(msg: String) {
        logListener?.onLog(msg)
    }

    fun hasLineOfSight(start: Point3D, end: Point3D, levelMap: GameLevelMap): Boolean {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= 0.1f) return true

        val steps = (dist * 3).toInt().coerceAtLeast(3)
        val stepX = dx / steps
        val stepY = dy / steps

        var currX = start.x
        var currY = start.y

        for (i in 0 until steps) {
            currX += stepX
            currY += stepY
            val tx = currX.toInt()
            val ty = currY.toInt()

            val tile = levelMap.getTile(tx, ty)
            if (tile == TileType.WALL || tile == TileType.BARREL_EXPLOSIVE) {
                return false
            }
        }
        return true
    }

    fun updateEnemies(
        enemies: List<Enemy>,
        player: Player,
        currentZLevel: Int,
        noiseRipples: List<NoiseRipple>,
        dt: Float,
        levelMap: GameLevelMap? = null,
        onPlayerDamaged: (Float) -> Unit
    ) {
        for (enemy in enemies) {
            if (enemy.isDead) continue

            if (enemy.attackCooldown > 0) enemy.attackCooldown--

            if (enemy.pos.z.toInt() == currentZLevel) {
                val dist = enemy.pos.distanceTo(player.pos)
                val isPlayerVisible = checkPlayerVisibility(enemy, player, dist, levelMap)

                if (isPlayerVisible) {
                    if (enemy.hasLostTargetInAggro) {
                        enemy.hasLostTargetInAggro = false
                        enemy.aggroLostSearchTimer = 0f
                        log("RE-ENGAGED: ${enemy.name} RE-SPOTTED TARGET")
                    }
                    if (enemy.isSearching && enemy.alertState != AlertState.ALERTED) {
                        enemy.isSearching = false
                        enemy.searchTimer = 0f
                        enemy.pauseTimer = 0f
                    }

                    val rate = when (enemy.alertState) {
                        AlertState.PATROLLING -> 45f
                        AlertState.SUSPICIOUS -> 75f
                        AlertState.ALERTED -> 120f
                    }
                    val factor = player.getStealthFactor()
                    enemy.alertMeter = (enemy.alertMeter + rate * factor * dt).coerceAtMost(100f)

                    if (enemy.alertState == AlertState.PATROLLING && enemy.alertMeter > 25f) {
                        enemy.alertState = AlertState.SUSPICIOUS
                        log("SUSPICIOUS: ${enemy.name} DETECTED MINOR ANOMALY")
                    }

                    if (enemy.alertMeter >= 100f && enemy.alertState != AlertState.ALERTED) {
                        enemy.alertState = AlertState.ALERTED
                        enemy.hasLostTargetInAggro = false
                        enemy.aggroLostSearchTimer = 0f
                        AudioManager.playAlert()
                        log("ALERT! ${enemy.name} ENGAGED")
                    }
                    enemy.lastKnownPlayerPos = player.pos.copy()
                } else {
                    if (enemy.alertState != AlertState.ALERTED) {
                        if (!enemy.isSearching) {
                            enemy.alertMeter = (enemy.alertMeter - 15f * dt).coerceAtLeast(0f)
                            if (enemy.alertMeter <= 0f && enemy.alertState == AlertState.SUSPICIOUS) {
                                enemy.alertState = AlertState.PATROLLING
                            }
                        }
                    } else {
                        if (!enemy.hasLostTargetInAggro) {
                            enemy.suspicionTimer += dt
                            if (enemy.suspicionTimer > 8.0f) {
                                enemy.alertState = AlertState.SUSPICIOUS
                                enemy.alertMeter = 50f
                                enemy.suspicionTimer = 0f
                                log("${enemy.name} COMBAT CONTACT LOST. SCANNING REGION...")
                            }
                        }
                    }
                }

                // Noise detection
                for (ripple in noiseRipples) {
                    if (ripple.pos.z.toInt() == enemy.pos.z.toInt()) {
                        val dToRipple = enemy.pos.distanceTo(ripple.pos)
                        if (dToRipple <= ripple.radius && enemy.alertState != AlertState.ALERTED) {
                            enemy.alertState = AlertState.SUSPICIOUS
                            enemy.alertMeter = max(enemy.alertMeter, 40f)
                            enemy.lastKnownPlayerPos = ripple.pos.copy()
                            enemy.isSearching = false
                            enemy.pauseTimer = 0f
                            enemy.searchTimer = 0f
                            log("${enemy.name} HEARD SOUND RIPPLE")
                        }
                    }
                }

                // Melee or ranged attack execution
                if (enemy.alertState == AlertState.ALERTED && dist < 1.5f && enemy.attackCooldown <= 0) {
                    val damage = 15f
                    enemy.attackCooldown = 50
                    AudioManager.playAttack()
                    log("DAMAGE! MELEE HIT FROM ${enemy.name}: -15HP")
                    onPlayerDamaged(damage)
                } else if (enemy.alertState == AlertState.ALERTED && dist in 1.5f..6.5f && enemy.attackCooldown <= 0 && enemy.type == "SentryDrone") {
                    val damage = 10f
                    enemy.attackCooldown = 75
                    AudioManager.playLaser()
                    log("DAMAGE! PULSE SHOT BY ${enemy.name}: -10HP")
                    onPlayerDamaged(damage)
                }
            }

            updateEnemyMovement(enemy, player, dt, levelMap)
        }
    }

    fun checkPlayerVisibility(enemy: Enemy, player: Player, dist: Float, levelMap: GameLevelMap? = null): Boolean {
        if (player.isInvisible) return false
        val visionRange = enemy.getVisionRange()
        if (dist > visionRange) return false

        val dx = player.pos.x - enemy.pos.x
        val dy = player.pos.y - enemy.pos.y
        val angleToPlayer = atan2(dy, dx)

        var diff = abs(angleToPlayer - enemy.directionAngle)
        while (diff > PI) diff = (2 * PI - diff).toFloat()

        if (diff > enemy.getVisionConeAngle() / 2f) return false

        val isInnerZone = dist <= visionRange * 0.5f
        if (!isInnerZone && player.isSneaking) {
            return false
        }

        if (levelMap != null && !hasLineOfSight(enemy.pos, player.pos, levelMap)) {
            return false
        }

        return true
    }

    private fun updateEnemyMovement(enemy: Enemy, player: Player, dt: Float, levelMap: GameLevelMap? = null) {
        val speed = when (enemy.alertState) {
            AlertState.PATROLLING -> 1.0f
            AlertState.SUSPICIOUS -> 1.8f
            AlertState.ALERTED -> 2.6f
        }

        when (enemy.alertState) {
            AlertState.PATROLLING -> {
                if (enemy.isSearching) {
                    enemy.pauseTimer -= dt
                    enemy.searchTimer += dt
                    enemy.directionAngle = enemy.basePatrolAngle + sin(enemy.searchTimer * 2.5f) * 0.6f
                    if (enemy.pauseTimer <= 0f) {
                        enemy.isSearching = false
                        enemy.pauseTimer = 0f
                        if (enemy.patrolRoute.isNotEmpty()) {
                            enemy.patrolIndex = (enemy.patrolIndex + 1) % enemy.patrolRoute.size
                        }
                    }
                } else {
                    val target = enemy.patrolRoute.getOrNull(enemy.patrolIndex) ?: enemy.pos
                    val dx = target.x - enemy.pos.x
                    val dy = target.y - enemy.pos.y
                    val d = sqrt(dx * dx + dy * dy)

                    if (d > 0.15f) {
                        enemy.pos.x += (dx / d) * speed * dt
                        enemy.pos.y += (dy / d) * speed * dt
                        enemy.directionAngle = atan2(dy, dx)
                    } else if (enemy.patrolRoute.isNotEmpty()) {
                        enemy.isSearching = true
                        enemy.pauseTimer = 2.0f
                        enemy.searchTimer = 0f
                        enemy.basePatrolAngle = enemy.directionAngle
                    }
                }
            }
            AlertState.SUSPICIOUS -> {
                if (enemy.isSearching) {
                    enemy.suspicionTimer -= dt
                    enemy.searchTimer += dt
                    enemy.directionAngle = enemy.basePatrolAngle + sin(enemy.searchTimer * 3.0f) * 1.2f
                    if (enemy.suspicionTimer <= 0f) {
                        enemy.isSearching = false
                        enemy.suspicionTimer = 0f
                        enemy.alertState = AlertState.PATROLLING
                        enemy.alertMeter = 0f
                        enemy.lastKnownPlayerPos = null
                        log("${enemy.name} SEARCH COMPLETED. RESUMING PATROL.")
                    }
                } else {
                    val target = enemy.lastKnownPlayerPos ?: (enemy.patrolRoute.getOrNull(enemy.patrolIndex) ?: enemy.pos)
                    val dx = target.x - enemy.pos.x
                    val dy = target.y - enemy.pos.y
                    val d = sqrt(dx * dx + dy * dy)

                    if (d > 0.2f) {
                        enemy.pos.x += (dx / d) * speed * dt
                        enemy.pos.y += (dy / d) * speed * dt
                        enemy.directionAngle = atan2(dy, dx)
                    } else {
                        enemy.isSearching = true
                        enemy.suspicionTimer = 4.0f
                        enemy.searchTimer = 0f
                        enemy.basePatrolAngle = enemy.directionAngle
                        log("${enemy.name} ARRIVED AT ANOMALY. SCANNING...")
                    }
                }
            }
            AlertState.ALERTED -> {
                val isPlayerVisible = checkPlayerVisibility(enemy, player, enemy.pos.distanceTo(player.pos), levelMap) && 
                                      player.pos.z.toInt() == enemy.pos.z.toInt() && 
                                      !player.isInvisible
                if (isPlayerVisible) {
                    enemy.hasLostTargetInAggro = false
                    enemy.aggroLostSearchTimer = 0f
                    enemy.suspicionTimer = 0f
                    
                    val target = player.pos
                    val dx = target.x - enemy.pos.x
                    val dy = target.y - enemy.pos.y
                    val d = sqrt(dx * dx + dy * dy)

                    if (d > 0.15f) {
                        enemy.pos.x += (dx / d) * speed * dt
                        enemy.pos.y += (dy / d) * speed * dt
                        enemy.directionAngle = atan2(dy, dx)
                    }
                } else {
                    val target = enemy.lastKnownPlayerPos ?: enemy.pos
                    val dx = target.x - enemy.pos.x
                    val dy = target.y - enemy.pos.y
                    val d = sqrt(dx * dx + dy * dy)

                    if (d > 0.2f && !enemy.hasLostTargetInAggro) {
                        enemy.pos.x += (dx / d) * speed * dt
                        enemy.pos.y += (dy / d) * speed * dt
                        enemy.directionAngle = atan2(dy, dx)
                    } else {
                        if (!enemy.hasLostTargetInAggro) {
                            enemy.hasLostTargetInAggro = true
                            enemy.aggroLostSearchTimer = 0f
                            log("${enemy.name} AT LAST KNOWN POS. INIT COMBAT SWEEP")
                        }
                        
                        enemy.aggroLostSearchTimer += dt
                        enemy.directionAngle += dt * 4.5f
                        
                        if (enemy.aggroLostSearchTimer > 4.0f) {
                            enemy.alertState = AlertState.SUSPICIOUS
                            enemy.alertMeter = 50f
                            enemy.suspicionTimer = 4.0f
                            enemy.isSearching = true
                            enemy.searchTimer = 0f
                            enemy.basePatrolAngle = enemy.directionAngle
                            enemy.hasLostTargetInAggro = false
                            enemy.aggroLostSearchTimer = 0f
                            log("${enemy.name} COMBAT SWEEP FAILED. SEARCHING...")
                        }
                    }
                }
            }
        }
    }
}
