package com.example.engine

import com.example.model.CivilianActivity
import com.example.model.CivilianNpc
import com.example.model.Point3D
import kotlin.math.abs

/**
 * Lightweight ambient civilian simulation.
 *
 * Civilians deliberately do not enter the combat AI state machine. They are a
 * presentation-oriented population layer driven by deterministic route/activity
 * changes and therefore remain cheap enough for the low-spec render preset.
 */
class CivilianNpcSystem(private val levelManager: LevelManager) {

    fun spawnMainStreet(): List<CivilianNpc> {
        val z = 1f
        return listOf(
            civilian("civilian_00", Point3D(4f, 17f, z), route(4f, 17f, 12f, 17f, 24f, 17f, 30f, 17f), 1, 0.95f),
            civilian("civilian_01", Point3D(30f, 17f, z), route(30f, 17f, 24f, 17f, 12f, 17f, 4f, 17f), 2, 1.05f),
            civilian("civilian_02", Point3D(17f, 4f, z), route(17f, 4f, 17f, 12f, 17f, 24f, 17f, 30f), 3, 0.90f),
            civilian("civilian_03", Point3D(17f, 30f, z), route(17f, 30f, 17f, 24f, 17f, 12f, 17f, 4f), 4, 1.10f),
            civilian("civilian_04", Point3D(7f, 17f, z), route(7f, 17f, 13f, 17f, 19f, 17f, 25f, 17f), 5, 1.00f),
            civilian("civilian_05", Point3D(25f, 17f, z), route(25f, 17f, 19f, 17f, 13f, 17f, 7f, 17f), 6, 0.92f),
            CivilianNpc(
                id = "vendor_00",
                pos = Point3D(11f, 17f, z),
                activity = CivilianActivity.VENDOR,
                visualVariantId = "vendor_01",
                activityTimer = 9999f
            )
        )
    }

    fun update(civilians: List<CivilianNpc>, dt: Float) {
        if (dt <= 0f) return

        civilians.forEach { npc ->
            if (!npc.isActive) return@forEach
            npc.animationTime += dt

            when (npc.activity) {
                CivilianActivity.VENDOR,
                CivilianActivity.HURT,
                CivilianActivity.DOWNED,
                CivilianActivity.DEAD -> Unit

                CivilianActivity.IDLE,
                CivilianActivity.SOCIALIZE,
                CivilianActivity.GESTURE -> {
                    npc.activityTimer -= dt
                    if (npc.activityTimer <= 0f) {
                        npc.activity = CivilianActivity.WALKING
                        npc.activityTimer = 0f
                    }
                }

                CivilianActivity.WALKING -> moveAlongRoute(npc, dt)
            }
        }
    }

    private fun moveAlongRoute(npc: CivilianNpc, dt: Float) {
        if (npc.route.isEmpty()) return

        val target = npc.route[npc.routeIndex.coerceIn(0, npc.route.lastIndex)]
        val dx = target.x - npc.pos.x
        val dy = target.y - npc.pos.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= 0.05f) {
            npc.pos.x = target.x
            npc.pos.y = target.y
            npc.routeIndex = (npc.routeIndex + 1) % npc.route.size
            chooseWaypointActivity(npc)
            return
        }

        val step = (npc.moveSpeed * dt).coerceAtMost(distance)
        val nx = dx / distance
        val ny = dy / distance
        npc.facingX = nx
        npc.facingY = ny

        var nextX = npc.pos.x + nx * step
        var nextY = npc.pos.y + ny * step

        val map = levelManager.getLevelMap(npc.pos.z.toInt())
        if (map != null) {
            val tx = nextX.toInt().coerceIn(0, map.width - 1)
            val ty = nextY.toInt().coerceIn(0, map.height - 1)
            if (!map.getTile(tx, ty).isWalkable) {
                nextX = npc.pos.x
                nextY = npc.pos.y
                npc.activity = CivilianActivity.IDLE
                npc.activityTimer = 0.6f
            }
        }

        npc.pos.x = nextX
        npc.pos.y = nextY
    }

    private fun chooseWaypointActivity(npc: CivilianNpc) {
        val seed = abs(npc.id.hashCode() + npc.routeIndex * 31)
        when (seed % 6) {
            0 -> {
                npc.activity = CivilianActivity.SOCIALIZE
                npc.activityTimer = 1.0f
            }
            1 -> {
                npc.activity = CivilianActivity.GESTURE
                npc.activityTimer = 0.8f
            }
            2 -> {
                npc.activity = CivilianActivity.IDLE
                npc.activityTimer = 0.65f
            }
            else -> npc.activity = CivilianActivity.WALKING
        }
    }

    private fun civilian(
        id: String,
        start: Point3D,
        route: List<Point3D>,
        variant: Int,
        speed: Float
    ) = CivilianNpc(
        id = id,
        pos = start,
        route = route,
        routeIndex = 1,
        activity = CivilianActivity.WALKING,
        visualVariantId = "civilian_%02d".format((variant % 7) + 1),
        moveSpeed = speed
    )

    private fun route(vararg values: Float): List<Point3D> {
        return values.toList().chunked(2).map { Point3D(it[0], it[1], 1f) }
    }
}
