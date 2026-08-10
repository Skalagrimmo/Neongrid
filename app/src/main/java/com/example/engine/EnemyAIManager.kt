package com.example.engine

import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.GameLevelMap
import com.example.model.Player
import com.example.model.Point3D
import com.example.model.TileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Type of patrol route movement behavior for NPC enemies.
 */
enum class PatrolType {
    LOOP,          // Cycles through waypoints in order (0 -> 1 -> 2 -> ... -> 0)
    PING_PONG,     // Moves back and forth along waypoints (0 -> 1 -> 2 -> 1 -> 0)
    STATIONARY,    // Remains at base position and sweeps vision cone
    DYNAMIC_GRID   // Calculates walkable grid paths dynamically
}

/**
 * Waypoint data structure defining a location, pause duration, and facing angle during patrol.
 */
data class PatrolWaypoint(
    val position: Point3D,
    val waitTimeSeconds: Float = 2.0f,
    val preferredFacingAngle: Float? = null // Optional angle to face while waiting
)

/**
 * Path configuration containing waypoints and movement behavior for a patrolling enemy.
 */
data class PatrolPath(
    val pathId: String,
    val waypoints: List<PatrolWaypoint>,
    val type: PatrolType = PatrolType.LOOP
)

/**
 * Calculated snapshot of an enemy's active vision cone.
 */
data class VisionConeState(
    val enemyId: String,
    val origin: Point3D,
    val centerAngle: Float,
    val fovAngleRadians: Float,
    val maxRange: Float,
    val isPlayerInCone: Boolean = false,
    val isPlayerOccluded: Boolean = false,
    val isPlayerDetected: Boolean = false,
    val alertState: AlertState = AlertState.PATROLLING
) {
    val leftEdgeAngle: Float
        get() = centerAngle - fovAngleRadians / 2f

    val rightEdgeAngle: Float
        get() = centerAngle + fovAngleRadians / 2f
}

/**
 * Manager class defining basic patrol paths, vision cone behaviors, line-of-sight detection,
 * and stealth AI updates for non-player characters (NPCs).
 */
class EnemyAIManager {

    private val patrolPathMap = mutableMapOf<String, PatrolPath>()
    private val pingPongDirectionMap = mutableMapOf<String, Int>() // 1 for forward, -1 for backward

    private val _visionConeStates = MutableStateFlow<Map<String, VisionConeState>>(emptyMap())
    val visionConeStates: StateFlow<Map<String, VisionConeState>> = _visionConeStates.asStateFlow()

    /**
     * Registers or updates a custom patrol path for a specific enemy unit.
     */
    fun registerPatrolPath(enemyId: String, path: PatrolPath) {
        patrolPathMap[enemyId] = path
        pingPongDirectionMap[enemyId] = 1
    }

    /**
     * Generates a circular loop patrol path around a central position.
     */
    fun generateLoopPatrolPath(
        pathId: String,
        center: Point3D,
        radius: Float = 3.0f,
        waypointCount: Int = 4,
        waitTimePerNode: Float = 2.0f
    ): PatrolPath {
        val waypoints = mutableListOf<PatrolWaypoint>()
        val step = (2 * PI / waypointCount).toFloat()

        for (i in 0 until waypointCount) {
            val angle = i * step
            val wx = center.x + radius * cos(angle)
            val wy = center.y + radius * sin(angle)
            val wpPos = Point3D(wx, wy, center.z)
            val facingAngle = angle + (PI / 2).toFloat() // Tangent facing angle

            waypoints.add(
                PatrolWaypoint(
                    position = wpPos,
                    waitTimeSeconds = waitTimePerNode,
                    preferredFacingAngle = facingAngle
                )
            )
        }

        return PatrolPath(pathId = pathId, waypoints = waypoints, type = PatrolType.LOOP)
    }

    /**
     * Generates a linear ping-pong patrol path between two endpoints.
     */
    fun generatePingPongPatrolPath(
        pathId: String,
        startPos: Point3D,
        endPos: Point3D,
        waitTimeSeconds: Float = 2.5f
    ): PatrolPath {
        val dx = endPos.x - startPos.x
        val dy = endPos.y - startPos.y
        val startFacing = atan2(dy, dx)
        val endFacing = atan2(-dy, -dx)

        val waypoints = listOf(
            PatrolWaypoint(position = startPos, waitTimeSeconds = waitTimeSeconds, preferredFacingAngle = startFacing),
            PatrolWaypoint(position = endPos, waitTimeSeconds = waitTimeSeconds, preferredFacingAngle = endFacing)
        )

        return PatrolPath(pathId = pathId, waypoints = waypoints, type = PatrolType.PING_PONG)
    }

    /**
     * Core tick function to update all enemy AI patrol movements, vision cones, and alert states.
     */
    fun updateAllEnemies(
        enemies: List<Enemy>,
        player: Player,
        levelMap: GameLevelMap?,
        dt: Float,
        noiseLocations: List<Point3D> = emptyList(),
        onPlayerDetected: ((Enemy) -> Unit)? = null,
        onPlayerDamaged: ((Float) -> Unit)? = null
    ) {
        val newVisionCones = mutableMapOf<String, VisionConeState>()

        for (enemy in enemies) {
            if (enemy.isDead) continue

            // 1. Attack Cooldown Management
            if (enemy.attackCooldown > 0) {
                enemy.attackCooldown--
            }

            // 2. Vision Cone & Detection Evaluation
            val distToPlayer = enemy.pos.distanceTo(player.pos)
            val inCone = isPointInVisionCone(enemy, player.pos)
            val hasLos = inCone && hasLineOfSight(enemy.pos, player.pos, levelMap)
            val isPlayerVisible = hasLos && !player.isInvisible

            if (isPlayerVisible) {
                val fillRate = calculateDetectionRate(enemy, player, distToPlayer)
                enemy.alertMeter = (enemy.alertMeter + fillRate * dt).coerceAtMost(100f)

                if (enemy.alertMeter >= 100f && enemy.alertState != AlertState.ALERTED) {
                    enemy.alertState = AlertState.ALERTED
                    onPlayerDetected?.invoke(enemy)
                } else if (enemy.alertMeter > 25f && enemy.alertState == AlertState.PATROLLING) {
                    enemy.alertState = AlertState.SUSPICIOUS
                }

                enemy.lastKnownPlayerPos = player.pos.copy()
            } else {
                // Cool down alert meter when out of line of sight
                if (enemy.alertState != AlertState.ALERTED) {
                    enemy.alertMeter = (enemy.alertMeter - 12f * dt).coerceAtLeast(0f)
                    if (enemy.alertMeter <= 0f && enemy.alertState == AlertState.SUSPICIOUS) {
                        enemy.alertState = AlertState.PATROLLING
                    }
                }
            }

            // 3. React to Environmental Noise
            for (noisePos in noiseLocations) {
                if (noisePos.z.toInt() == enemy.pos.z.toInt()) {
                    val distToNoise = enemy.pos.distanceTo(noisePos)
                    if (distToNoise <= 6.0f && enemy.alertState != AlertState.ALERTED) {
                        enemy.alertState = AlertState.SUSPICIOUS
                        enemy.alertMeter = enemy.alertMeter.coerceAtLeast(40f)
                        enemy.lastKnownPlayerPos = noisePos.copy()
                        enemy.isSearching = false
                        enemy.pauseTimer = 0f
                    }
                }
            }

            // 4. Update Movement & Patrol Path logic
            updatePatrolMovement(enemy, levelMap, dt)

            // 5. Combat Engagement logic when Alerted
            if (enemy.alertState == AlertState.ALERTED && distToPlayer <= 1.5f && enemy.attackCooldown <= 0) {
                enemy.attackCooldown = 50 // Cooldown in frames
                onPlayerDamaged?.invoke(15f)
            }

            // 6. Record Vision Cone State snapshot
            val coneState = VisionConeState(
                enemyId = enemy.id,
                origin = enemy.pos.copy(),
                centerAngle = enemy.directionAngle,
                fovAngleRadians = enemy.getVisionConeAngle(),
                maxRange = enemy.getVisionRange(),
                isPlayerInCone = inCone,
                isPlayerOccluded = inCone && !hasLos,
                isPlayerDetected = isPlayerVisible && enemy.alertMeter >= 100f,
                alertState = enemy.alertState
            )
            newVisionCones[enemy.id] = coneState
        }

        _visionConeStates.update { newVisionCones }
    }

    /**
     * Evaluates if a target position falls inside an enemy's active field of view angle and range.
     */
    fun isPointInVisionCone(enemy: Enemy, targetPos: Point3D): Boolean {
        val visionRange = enemy.getVisionRange()
        val dist = enemy.pos.distanceTo(targetPos)

        if (dist > visionRange) return false

        val dx = targetPos.x - enemy.pos.x
        val dy = targetPos.y - enemy.pos.y
        val angleToTarget = atan2(dy, dx)

        var diff = abs(angleToTarget - enemy.directionAngle)
        while (diff > PI) {
            diff = (2 * PI - diff).toFloat()
        }

        val fovAngle = enemy.getVisionConeAngle()
        return diff <= (fovAngle / 2f)
    }

    /**
     * Raycasts line-of-sight on the map grid to check if walls or obstacles obstruct vision.
     */
    fun hasLineOfSight(start: Point3D, end: Point3D, levelMap: GameLevelMap?): Boolean {
        if (levelMap == null) return true

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
     * Calculates detection alert meter fill rate based on distance, stealth stance, and alert state.
     */
    fun calculateDetectionRate(enemy: Enemy, player: Player, distance: Float): Float {
        val baseRate = when (enemy.alertState) {
            AlertState.PATROLLING -> 50f
            AlertState.SUSPICIOUS -> 80f
            AlertState.ALERTED -> 130f
        }

        val maxRange = enemy.getVisionRange()
        val distanceFactor = (1f - (distance / maxRange).coerceIn(0f, 1f))
        val stealthFactor = player.getStealthFactor()

        return baseRate * distanceFactor * stealthFactor
    }

    /**
     * Updates an enemy's position along its assigned or calculated patrol path waypoints.
     */
    fun updatePatrolMovement(enemy: Enemy, levelMap: GameLevelMap?, dt: Float) {
        val speed = when (enemy.alertState) {
            AlertState.PATROLLING -> 1.0f
            AlertState.SUSPICIOUS -> 1.8f
            AlertState.ALERTED -> 2.6f
        }

        val registeredPath = patrolPathMap[enemy.id]

        if (registeredPath != null && registeredPath.waypoints.isNotEmpty()) {
            advanceAlongPatrolPath(enemy, registeredPath, speed, dt)
        } else {
            // Fallback to legacy patrol route or dynamic grid waypoints
            val activeWaypoints = if (enemy.patrolRoute.isNotEmpty()) {
                enemy.patrolRoute
            } else if (enemy.calculatedWaypoints.isNotEmpty()) {
                enemy.calculatedWaypoints
            } else {
                emptyList()
            }

            if (activeWaypoints.isNotEmpty()) {
                advanceAlongWaypointList(enemy, activeWaypoints, speed, dt)
            } else {
                // Stationary vision cone scanning
                performIdleVisionSweep(enemy, dt)
            }
        }
    }

    private fun advanceAlongPatrolPath(
        enemy: Enemy,
        path: PatrolPath,
        speed: Float,
        dt: Float
    ) {
        val waypoints = path.waypoints
        if (waypoints.isEmpty()) return

        val currentIndex = enemy.patrolIndex.coerceIn(0, waypoints.size - 1)
        val currentWaypoint = waypoints[currentIndex]

        if (enemy.pauseTimer > 0f) {
            enemy.pauseTimer -= dt
            currentWaypoint.preferredFacingAngle?.let { targetAngle ->
                enemy.directionAngle = lerpAngle(enemy.directionAngle, targetAngle, dt * 3f)
            }
            if (enemy.pauseTimer <= 0f) {
                // Advance waypoint index based on patrol type
                enemy.patrolIndex = getNextWaypointIndex(enemy.id, currentIndex, waypoints.size, path.type)
            }
            return
        }

        val targetPos = currentWaypoint.position
        val dx = targetPos.x - enemy.pos.x
        val dy = targetPos.y - enemy.pos.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 0.15f) {
            enemy.pos.x += (dx / dist) * speed * dt
            enemy.pos.y += (dy / dist) * speed * dt
            enemy.directionAngle = atan2(dy, dx)
        } else {
            // Reached waypoint: pause and sweep
            enemy.pauseTimer = currentWaypoint.waitTimeSeconds
            enemy.basePatrolAngle = enemy.directionAngle
        }
    }

    private fun advanceAlongWaypointList(
        enemy: Enemy,
        waypoints: List<Point3D>,
        speed: Float,
        dt: Float
    ) {
        val currentIndex = enemy.patrolIndex % waypoints.size
        val target = waypoints[currentIndex]

        if (enemy.pauseTimer > 0f) {
            enemy.pauseTimer -= dt
            performIdleVisionSweep(enemy, dt)
            if (enemy.pauseTimer <= 0f) {
                enemy.patrolIndex = (currentIndex + 1) % waypoints.size
            }
            return
        }

        val dx = target.x - enemy.pos.x
        val dy = target.y - enemy.pos.y
        val dist = sqrt(dx * dx + dy * dy)

        if (dist > 0.2f) {
            enemy.pos.x += (dx / dist) * speed * dt
            enemy.pos.y += (dy / dist) * speed * dt
            enemy.directionAngle = atan2(dy, dx)
        } else {
            enemy.pauseTimer = 2.0f
            enemy.basePatrolAngle = enemy.directionAngle
            enemy.searchTimer = 0f
        }
    }

    private fun performIdleVisionSweep(enemy: Enemy, dt: Float) {
        enemy.searchTimer += dt
        val sweepOffset = sin(enemy.searchTimer * 2.5f) * 0.5f // Radians head sweep
        enemy.directionAngle = enemy.basePatrolAngle + sweepOffset
    }

    private fun getNextWaypointIndex(
        enemyId: String,
        currentIndex: Int,
        totalWaypoints: Int,
        patrolType: PatrolType
    ): Int {
        if (totalWaypoints <= 1) return 0

        return when (patrolType) {
            PatrolType.LOOP -> (currentIndex + 1) % totalWaypoints

            PatrolType.PING_PONG -> {
                var dir = pingPongDirectionMap[enemyId] ?: 1
                var next = currentIndex + dir

                if (next >= totalWaypoints) {
                    dir = -1
                    next = totalWaypoints - 2
                } else if (next < 0) {
                    dir = 1
                    next = 1
                }

                pingPongDirectionMap[enemyId] = dir
                next.coerceIn(0, totalWaypoints - 1)
            }

            PatrolType.STATIONARY -> 0

            PatrolType.DYNAMIC_GRID -> (currentIndex + 1) % totalWaypoints
        }
    }

    private fun lerpAngle(start: Float, end: Float, alpha: Float): Float {
        var diff = end - start
        while (diff < -PI) diff += (2 * PI).toFloat()
        while (diff > PI) diff -= (2 * PI).toFloat()
        return start + diff * alpha.coerceIn(0f, 1f)
    }
}
