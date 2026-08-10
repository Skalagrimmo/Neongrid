package com.example.engine

import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.Point3D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interface representing a concrete state in the Enemy AI State Machine.
 */
sealed interface EnemyBehaviorState {
    val name: String
    val alertState: AlertState

    /**
     * Executes logic for the current state on each frame tick.
     * Returns the next state to transition to, or `this` to remain in current state.
     */
    fun update(
        context: EnemyAiContext,
        dt: Float
    ): EnemyBehaviorState
}

/**
 * Environmental and sensory context passed to the Enemy AI State Machine.
 */
data class EnemyAiContext(
    val enemy: Enemy,
    val playerPos: Point3D,
    val playerStealthLevel: Float, // 0.0f (Exposed) to 1.0f (Completely hidden)
    val hasLineOfSight: Boolean,
    val isPlayerInCover: Boolean,
    val noiseLocation: Point3D? = null,
    val patrolWaypoints: List<Point3D> = emptyList(),
    val attackRange: Float = 1.8f,
    val moveSpeed: Float = 2.5f,
    val detectionSensitivity: Float = 1.0f,
    val onStateChanged: ((previous: EnemyBehaviorState, new: EnemyBehaviorState) -> Unit)? = null
)

/**
 * 1. IDLE STATE: Enemy stands guard, scanning angle periodically.
 */
class IdleState(
    var scanTimer: Float = 0f,
    val maxIdleTime: Float = 4f
) : EnemyBehaviorState {
    override val name: String = "IDLE"
    override val alertState: AlertState = AlertState.PATROLLING

    override fun update(context: EnemyAiContext, dt: Float): EnemyBehaviorState {
        val enemy = context.enemy
        val distToPlayer = enemy.pos.distanceTo(context.playerPos)

        // 1. Detection Evaluation
        if (context.hasLineOfSight) {
            val fillRate = calculateAlertFillRate(context, distToPlayer)
            enemy.alertMeter = (enemy.alertMeter + fillRate * dt).coerceAtMost(100f)

            if (enemy.alertMeter >= 100f) {
                enemy.alertState = AlertState.ALERTED
                return AlertedState()
            } else if (enemy.alertMeter > 20f) {
                enemy.alertState = AlertState.SUSPICIOUS
                return SuspiciousState(investigationTarget = context.playerPos.copy())
            }
        } else {
            enemy.alertMeter = (enemy.alertMeter - 10f * dt).coerceAtLeast(0f)
        }

        // 2. Sound disturbance check
        if (context.noiseLocation != null) {
            enemy.alertState = AlertState.SUSPICIOUS
            return SuspiciousState(investigationTarget = context.noiseLocation.copy())
        }

        // 3. Idle rotation / patrol transition
        scanTimer += dt
        if (scanTimer >= maxIdleTime && context.patrolWaypoints.isNotEmpty()) {
            return PatrollingState()
        }

        return this
    }
}

/**
 * 2. PATROLLING STATE: Enemy follows path waypoints.
 */
class PatrollingState(
    var currentWaypointIndex: Int = 0
) : EnemyBehaviorState {
    override val name: String = "PATROLLING"
    override val alertState: AlertState = AlertState.PATROLLING

    override fun update(context: EnemyAiContext, dt: Float): EnemyBehaviorState {
        val enemy = context.enemy
        val distToPlayer = enemy.pos.distanceTo(context.playerPos)

        // 1. Detection Evaluation
        if (context.hasLineOfSight) {
            val fillRate = calculateAlertFillRate(context, distToPlayer)
            enemy.alertMeter = (enemy.alertMeter + fillRate * dt).coerceAtMost(100f)

            if (enemy.alertMeter >= 100f) {
                enemy.alertState = AlertState.ALERTED
                return AlertedState()
            } else if (enemy.alertMeter > 25f) {
                enemy.alertState = AlertState.SUSPICIOUS
                return SuspiciousState(investigationTarget = context.playerPos.copy())
            }
        } else {
            enemy.alertMeter = (enemy.alertMeter - 12f * dt).coerceAtLeast(0f)
        }

        // 2. Noise Detection
        if (context.noiseLocation != null) {
            enemy.alertState = AlertState.SUSPICIOUS
            return SuspiciousState(investigationTarget = context.noiseLocation.copy())
        }

        // 3. Waypoint Movement
        val waypoints = context.patrolWaypoints
        if (waypoints.isNotEmpty()) {
            val targetWaypoint = waypoints[currentWaypointIndex % waypoints.size]
            val distToWaypoint = enemy.pos.distanceTo(targetWaypoint)

            if (distToWaypoint < 0.3f) {
                currentWaypointIndex = (currentWaypointIndex + 1) % waypoints.size
                return IdleState(maxIdleTime = 2f)
            } else {
                moveTowards(enemy, targetWaypoint, speed = context.moveSpeed * 0.6f, dt = dt)
            }
        }

        return this
    }
}

/**
 * 3. SUSPICIOUS STATE: Enemy moves cautiously to investigate anomaly or last noise.
 */
class SuspiciousState(
    val investigationTarget: Point3D,
    var searchTimer: Float = 0f,
    val maxSearchDuration: Float = 5f
) : EnemyBehaviorState {
    override val name: String = "SUSPICIOUS"
    override val alertState: AlertState = AlertState.SUSPICIOUS

    override fun update(context: EnemyAiContext, dt: Float): EnemyBehaviorState {
        val enemy = context.enemy
        val distToPlayer = enemy.pos.distanceTo(context.playerPos)

        // 1. Line of sight directly accelerates alert meter
        if (context.hasLineOfSight) {
            val fillRate = calculateAlertFillRate(context, distToPlayer) * 1.5f
            enemy.alertMeter = (enemy.alertMeter + fillRate * dt).coerceAtMost(100f)

            if (enemy.alertMeter >= 100f) {
                enemy.alertState = AlertState.ALERTED
                return AlertedState()
            }
        }

        // 2. Move towards investigation target
        val distToTarget = enemy.pos.distanceTo(investigationTarget)
        if (distToTarget > 0.4f) {
            moveTowards(enemy, investigationTarget, speed = context.moveSpeed * 0.85f, dt = dt)
        } else {
            // Reached target, perform search
            searchTimer += dt
            if (searchTimer >= maxSearchDuration) {
                enemy.alertMeter = 0f
                enemy.alertState = AlertState.PATROLLING
                return SearchingState()
            }
        }

        return this
    }
}

/**
 * 4. ALERTED STATE: Hostile combat pursuit and engagement state.
 */
class AlertedState(
    var lastKnownPlayerPos: Point3D? = null,
    var timeLostTarget: Float = 0f
) : EnemyBehaviorState {
    override val name: String = "ALERTED"
    override val alertState: AlertState = AlertState.ALERTED

    override fun update(context: EnemyAiContext, dt: Float): EnemyBehaviorState {
        val enemy = context.enemy
        val distToPlayer = enemy.pos.distanceTo(context.playerPos)

        if (context.hasLineOfSight) {
            // Direct combat engagement
            timeLostTarget = 0f
            lastKnownPlayerPos = context.playerPos.copy()
            enemy.alertMeter = 100f

            if (distToPlayer <= context.attackRange) {
                // Execute attack action
                if (enemy.attackCooldown <= 0) {
                    enemy.attackCooldown = 60 // 1 sec at 60fps
                }
            } else {
                // Charge / pursue player
                moveTowards(enemy, context.playerPos, speed = context.moveSpeed * 1.2f, dt = dt)
            }
        } else {
            // Lost line of sight, head to last known location
            timeLostTarget += dt
            val target = lastKnownPlayerPos ?: context.playerPos

            if (enemy.pos.distanceTo(target) > 0.4f) {
                moveTowards(enemy, target, speed = context.moveSpeed, dt = dt)
            }

            if (timeLostTarget >= 4f || enemy.pos.distanceTo(target) <= 0.4f) {
                // Target lost for extended duration or reached last known location -> fallback to searching
                enemy.alertMeter = 50f
                enemy.alertState = AlertState.SUSPICIOUS
                return SearchingState(searchCenter = target)
            }
        }

        return this
    }
}

/**
 * 5. SEARCHING STATE: Sweeps nearby area after losing target before returning to patrol.
 */
class SearchingState(
    val searchCenter: Point3D = Point3D(0f, 0f, 0f),
    var sweepTimer: Float = 0f,
    val maxSweepDuration: Float = 4f
) : EnemyBehaviorState {
    override val name: String = "SEARCHING"
    override val alertState: AlertState = AlertState.SUSPICIOUS

    override fun update(context: EnemyAiContext, dt: Float): EnemyBehaviorState {
        val enemy = context.enemy
        val distToPlayer = enemy.pos.distanceTo(context.playerPos)

        if (context.hasLineOfSight) {
            val fillRate = calculateAlertFillRate(context, distToPlayer)
            enemy.alertMeter = (enemy.alertMeter + fillRate * dt).coerceAtMost(100f)

            if (enemy.alertMeter >= 100f) {
                enemy.alertState = AlertState.ALERTED
                return AlertedState()
            }
        }

        sweepTimer += dt
        if (sweepTimer >= maxSweepDuration) {
            enemy.alertMeter = 0f
            enemy.alertState = AlertState.PATROLLING
            return PatrollingState()
        }

        return this
    }
}

/**
 * Helper calculation for stealth alert fill rate.
 */
private fun calculateAlertFillRate(context: EnemyAiContext, distance: Float): Float {
    val baseRate = 50f
    val distanceFactor = (1f - (distance / 10f).coerceIn(0f, 1f))
    val stealthFactor = (1f - context.playerStealthLevel).coerceIn(0.1f, 1f)
    val coverFactor = if (context.isPlayerInCover) 0.4f else 1.0f

    return baseRate * distanceFactor * stealthFactor * coverFactor * context.detectionSensitivity
}

/**
 * Moves enemy toward target position.
 */
private fun moveTowards(enemy: Enemy, target: Point3D, speed: Float, dt: Float) {
    val dx = target.x - enemy.pos.x
    val dy = target.y - enemy.pos.y
    val dist = sqrt(dx * dx + dy * dy)

    if (dist > 0.05f) {
        val moveDist = (speed * dt).coerceAtMost(dist)
        enemy.pos.x += (dx / dist) * moveDist
        enemy.pos.y += (dy / dist) * moveDist
        enemy.directionAngle = atan2(dy, dx)
    }
}

/**
 * Manager class that holds and updates the current state of an Enemy AI unit.
 */
class EnemyAiStateMachine(
    initialState: EnemyBehaviorState = IdleState()
) {
    var currentState: EnemyBehaviorState = initialState
        private set

    fun tick(context: EnemyAiContext, dt: Float) {
        val nextState = currentState.update(context, dt)
        if (nextState != currentState) {
            context.onStateChanged?.invoke(currentState, nextState)
            currentState = nextState
        }
    }
}
