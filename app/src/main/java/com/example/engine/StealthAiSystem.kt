package com.example.engine

import com.example.model.*
import com.example.ui.AudioManager
import com.example.ui.SoundManager
import kotlin.math.*

class StealthAiSystem(
    val enemyAIManager: EnemyAIManager = EnemyAIManager()
) {

    interface AiLogListener {
        fun onLog(message: String)
    }

    var logListener: AiLogListener? = null

    private fun log(msg: String) {
        logListener?.onLog(msg)
    }

    /**
     * Raycasts line of sight on the GameLevelMap grid between start and end.
     * Returns true if no wall or explosive barrel blocks vision.
     */
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

    /**
     * Calculates a list of grid patrol waypoints around a given start position on the level map grid.
     */
    fun calculateGridPatrolWaypoints(
        startPos: Point3D,
        levelMap: GameLevelMap?,
        radius: Int = 4
    ): List<Point3D> {
        if (levelMap == null) {
            return listOf(
                startPos,
                Point3D(startPos.x + radius, startPos.y, startPos.z),
                Point3D(startPos.x + radius, startPos.y + radius, startPos.z),
                Point3D(startPos.x, startPos.y + radius, startPos.z)
            )
        }

        val z = startPos.z
        val startX = startPos.x.toInt().coerceIn(0, levelMap.width - 1)
        val startY = startPos.y.toInt().coerceIn(0, levelMap.height - 1)

        val candidates = listOf(
            Pair(startX, startY),
            Pair((startX + radius).coerceAtMost(levelMap.width - 2), startY),
            Pair((startX + radius).coerceAtMost(levelMap.width - 2), (startY + radius).coerceAtMost(levelMap.height - 2)),
            Pair(startX, (startY + radius).coerceAtMost(levelMap.height - 2))
        )

        val waypoints = mutableListOf<Point3D>()
        for ((cx, cy) in candidates) {
            if (levelMap.getTile(cx, cy).isWalkable) {
                waypoints.add(Point3D(cx + 0.5f, cy + 0.5f, z))
            } else {
                // Find nearest walkable neighbor
                val alt = findNearestWalkableTile(cx, cy, levelMap, z)
                if (alt != null && !waypoints.contains(alt)) {
                    waypoints.add(alt)
                }
            }
        }

        return if (waypoints.size >= 2) waypoints else listOf(startPos)
    }

    private fun findNearestWalkableTile(x: Int, y: Int, levelMap: GameLevelMap, z: Float): Point3D? {
        for (dx in -2..2) {
            for (dy in -2..2) {
                val nx = (x + dx).coerceIn(0, levelMap.width - 1)
                val ny = (y + dy).coerceIn(0, levelMap.height - 1)
                if (levelMap.getTile(nx, ny).isWalkable) {
                    return Point3D(nx + 0.5f, ny + 0.5f, z)
                }
            }
        }
        return null
    }

    /**
     * Simple BFS grid pathfinder to calculate step-by-step waypoints avoiding walls on the map grid.
     */
    fun findGridPath(start: Point3D, target: Point3D, levelMap: GameLevelMap?): List<Point3D> {
        if (levelMap == null) return listOf(target)

        val startX = start.x.toInt().coerceIn(0, levelMap.width - 1)
        val startY = start.y.toInt().coerceIn(0, levelMap.height - 1)
        val targetX = target.x.toInt().coerceIn(0, levelMap.width - 1)
        val targetY = target.y.toInt().coerceIn(0, levelMap.height - 1)

        if (startX == targetX && startY == targetY) return listOf(target)

        val queue = java.util.ArrayDeque<Pair<Int, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val parentMap = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        val startPair = Pair(startX, startY)
        val targetPair = Pair(targetX, targetY)

        queue.add(startPair)
        visited.add(startPair)

        val dirs = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
        var found = false

        while (queue.isNotEmpty()) {
            val curr = queue.poll() ?: break
            if (curr == targetPair) {
                found = true
                break
            }

            for ((dx, dy) in dirs) {
                val nx = curr.first + dx
                val ny = curr.second + dy
                val nextPair = Pair(nx, ny)

                if (nx in 0 until levelMap.width && ny in 0 until levelMap.height) {
                    if (!visited.contains(nextPair) && levelMap.getTile(nx, ny).isWalkable) {
                        visited.add(nextPair)
                        parentMap[nextPair] = curr
                        queue.add(nextPair)
                    }
                }
            }
        }

        if (!found) return listOf(target)

        val path = mutableListOf<Point3D>()
        var curr: Pair<Int, Int>? = targetPair
        while (curr != null && curr != startPair) {
            path.add(0, Point3D(curr.first + 0.5f, curr.second + 0.5f, start.z))
            curr = parentMap[curr]
        }
        return if (path.isEmpty()) listOf(target) else path
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
                // Ensure patrol waypoints exist
                if (enemy.patrolRoute.isEmpty() && enemy.calculatedWaypoints.isEmpty()) {
                    enemy.calculatedWaypoints = calculateGridPatrolWaypoints(enemy.pos, levelMap)
                }

                val dist = enemy.pos.distanceTo(player.pos)
                val isPlayerVisible = checkPlayerVisibility(enemy, player, dist, levelMap)

                if (isPlayerVisible) {
                    if (enemy.hasLostTargetInAggro) {
                        enemy.hasLostTargetInAggro = false
                        enemy.aggroLostSearchTimer = 0f
                        log("RE-ENGAGED: ${enemy.name} RE-SPOTTED TARGET IN LINE-OF-SIGHT")
                    }
                    if (enemy.isSearching && enemy.alertState != AlertState.ALERTED) {
                        enemy.isSearching = false
                        enemy.searchTimer = 0f
                        enemy.pauseTimer = 0f
                    }

                    val rate = when (enemy.alertState) {
                        AlertState.PATROLLING -> 55f
                        AlertState.SUSPICIOUS -> 85f
                        AlertState.ALERTED -> 130f
                    }
                    val factor = player.getStealthFactor()
                    enemy.alertMeter = (enemy.alertMeter + rate * factor * dt).coerceAtMost(100f)

                    if (enemy.alertState == AlertState.PATROLLING && enemy.alertMeter > 25f) {
                        enemy.alertState = AlertState.SUSPICIOUS
                        SoundManager.playStealthWarning()
                        log("SUSPICIOUS: ${enemy.name} DETECTED PLAYER IN VISION CONE")
                    }

                    if (enemy.alertMeter >= 100f && enemy.alertState != AlertState.ALERTED) {
                        enemy.alertState = AlertState.ALERTED
                        enemy.hasLostTargetInAggro = false
                        enemy.aggroLostSearchTimer = 0f
                        SoundManager.playStealthDetectionAlert(1.2f)
                        log("ALERT! ${enemy.name} ENGAGED TARGET (LINE-OF-SIGHT CONFIRMED)")
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

        val activeWaypoints = if (enemy.patrolRoute.isNotEmpty()) enemy.patrolRoute else enemy.calculatedWaypoints

        when (enemy.alertState) {
            AlertState.PATROLLING -> {
                if (enemy.isSearching) {
                    enemy.pauseTimer -= dt
                    enemy.searchTimer += dt
                    enemy.directionAngle = enemy.basePatrolAngle + sin(enemy.searchTimer * 2.5f) * 0.6f
                    if (enemy.pauseTimer <= 0f) {
                        enemy.isSearching = false
                        enemy.pauseTimer = 0f
                        if (activeWaypoints.isNotEmpty()) {
                            enemy.patrolIndex = (enemy.patrolIndex + 1) % activeWaypoints.size
                        }
                    }
                } else {
                    val targetWaypoint = activeWaypoints.getOrNull(enemy.patrolIndex) ?: enemy.pos
                    val dx = targetWaypoint.x - enemy.pos.x
                    val dy = targetWaypoint.y - enemy.pos.y
                    val d = sqrt(dx * dx + dy * dy)

                    if (d > 0.2f) {
                        enemy.pos.x += (dx / d) * speed * dt
                        enemy.pos.y += (dy / d) * speed * dt
                        enemy.directionAngle = atan2(dy, dx)
                    } else if (activeWaypoints.isNotEmpty()) {
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
                        log("${enemy.name} SEARCH COMPLETED. RESUMING GRID PATROL WAYPOINTS.")
                    }
                } else {
                    val target = enemy.lastKnownPlayerPos ?: (activeWaypoints.getOrNull(enemy.patrolIndex) ?: enemy.pos)
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
                        log("${enemy.name} ARRIVED AT ANOMALY. SCANNING WAYPOINT GRID AREA...")
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
