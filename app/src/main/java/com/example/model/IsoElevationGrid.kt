package com.example.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// Enum representing the functional elevation terrain shape
enum class ElevationType(val description: String, val isWalkable: Boolean = true) {
    FLAT("Flat Surface", true),
    RAMP_NORTH("Ramp Ascending North", true),
    RAMP_EAST("Ramp Ascending East", true),
    RAMP_SOUTH("Ramp Ascending South", true),
    RAMP_WEST("Ramp Ascending West", true),
    STAIRS_UP("Tactical Stairwell", true),
    ELEVATED_PLATFORM("Raised Combat Platform", true),
    LEDGE("High-Ground Ledge", true),
    PIT_FALL("Sewer Drop Pit", false)
}

// Cover classification for combat and stealth
enum class CoverType(val damageReduction: Float, val stealthBonus: Float) {
    NONE(0.0f, 0.0f),
    HALF_COVER(0.35f, 0.25f),
    FULL_COVER(0.70f, 0.60f)
}

// Data node representing a single tile within an elevated isometric grid
data class ElevationTileNode(
    val gridX: Int,
    val gridY: Int,
    val zLevel: Int,
    var elevationHeight: Float = 0.0f, // Relative height within the layer (0.0f = floor, 0.5f = half crate, 1.0f = full platform)
    var elevationType: ElevationType = ElevationType.FLAT,
    var tileType: TileType = TileType.FLOOR,
    var cover: CoverType = CoverType.NONE,
    var isExplored: Boolean = true,
    var isLit: Boolean = true,
    var tacticalHighGroundBonus: Float = 0.15f // Hit chance / damage boost when firing from elevated position
) {
    fun getAbsoluteZ(): Float = zLevel.toFloat() + elevationHeight
}

// Data structure managing multi-level isometric grid system with elevation logic
class IsoMultiLevelGrid(
    val width: Int,
    val height: Int,
    val maxZLevels: Int
) {
    // Map of Z level index -> 2D Array of ElevationTileNode
    private val layers = mutableMapOf<Int, Array<Array<ElevationTileNode>>>()

    init {
        for (z in 0 until maxZLevels) {
            val layer = Array(width) { x ->
                Array(height) { y ->
                    ElevationTileNode(
                        gridX = x,
                        gridY = y,
                        zLevel = z,
                        elevationHeight = 0.0f,
                        elevationType = ElevationType.FLAT,
                        tileType = if (z == 0) TileType.FLOOR else TileType.EMPTY
                    )
                }
            }
            layers[z] = layer
        }
    }

    fun getTileNode(x: Int, y: Int, z: Int): ElevationTileNode? {
        if (z !in 0 until maxZLevels) return null
        val layer = layers[z] ?: return null
        if (x !in 0 until width || y !in 0 until height) return null
        return layer[x][y]
    }

    fun setTileNode(x: Int, y: Int, z: Int, node: ElevationTileNode) {
        if (z in 0 until maxZLevels && x in 0 until width && y in 0 until height) {
            layers[z]?.get(x)?.set(y, node)
        }
    }

    // Calculates interpolated continuous elevation height at a floating point coordinate (x, y, z)
    fun getEffectiveHeight(x: Float, y: Float, z: Int): Float {
        val ix = x.toInt().coerceIn(0, width - 1)
        val iy = y.toInt().coerceIn(0, height - 1)
        val node = getTileNode(ix, iy, z) ?: return z.toFloat()

        val baseElevation = z.toFloat() + node.elevationHeight
        val fracX = x - ix
        val fracY = y - iy

        // Interpolate ramps smoothly
        val rampSlope = when (node.elevationType) {
            ElevationType.RAMP_NORTH -> (1f - fracY) * 1.0f
            ElevationType.RAMP_SOUTH -> fracY * 1.0f
            ElevationType.RAMP_EAST -> fracX * 1.0f
            ElevationType.RAMP_WEST -> (1f - fracX) * 1.0f
            ElevationType.STAIRS_UP -> ((fracX + fracY) * 0.5f) * 1.0f
            else -> 0.0f
        }

        return baseElevation + rampSlope
    }

    // Validates whether an entity can navigate between two positions considering elevation differences and ramps
    fun isStepMovementValid(from: Point3D, to: Point3D, maxClimbHeight: Float = 0.6f): Boolean {
        val fromNode = getTileNode(from.x.toInt(), from.y.toInt(), from.z.toInt()) ?: return false
        val toNode = getTileNode(to.x.toInt(), to.y.toInt(), to.z.toInt()) ?: return false

        if (!toNode.elevationType.isWalkable || !toNode.tileType.isWalkable) {
            return false
        }

        val fromH = getEffectiveHeight(from.x, from.y, from.z.toInt())
        val toH = getEffectiveHeight(to.x, to.y, to.z.toInt())
        val heightDiff = abs(toH - fromH)

        // Ramps and stairs permit smooth ascension across height differences up to 1.5 units
        val isRampTransition = fromNode.elevationType == ElevationType.RAMP_NORTH ||
                fromNode.elevationType == ElevationType.RAMP_SOUTH ||
                fromNode.elevationType == ElevationType.RAMP_EAST ||
                fromNode.elevationType == ElevationType.RAMP_WEST ||
                fromNode.elevationType == ElevationType.STAIRS_UP ||
                toNode.elevationType == ElevationType.STAIRS_UP

        if (isRampTransition) {
            return heightDiff <= 1.5f
        }

        // Standard ledge/step height restriction
        return heightDiff <= maxClimbHeight
    }

    // Line of sight raycasting taking into account elevated obstacles and full cover
    fun calculateLineOfSight3D(start: Point3D, end: Point3D): Boolean {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val dz = end.z - start.z
        val distance = sqrt(dx * dx + dy * dy)

        if (distance <= 0.1f) return true

        val steps = (distance * 4).toInt().coerceAtLeast(4)
        val stepX = dx / steps
        val stepY = dy / steps
        val stepZ = dz / steps

        var currX = start.x
        var currY = start.y
        var currZ = start.z

        for (i in 1 until steps) {
            currX += stepX
            currY += stepY
            currZ += stepZ

            val tx = currX.toInt()
            val ty = currY.toInt()
            val tz = currZ.toInt().coerceIn(0, maxZLevels - 1)

            val node = getTileNode(tx, ty, tz)
            if (node != null) {
                val rayH = currZ + (currZ - tz)
                val nodeH = getEffectiveHeight(currX, currY, tz)

                // Blocking condition: ray passes below tile elevation or hits wall / full cover
                if (rayH < nodeH - 0.1f && node.elevationHeight > 0.3f) {
                    return false
                }
                if (node.tileType == TileType.WALL || node.cover == CoverType.FULL_COVER) {
                    if (abs(currZ - tz) < 0.8f) {
                        return false
                    }
                }
            }
        }
        return true
    }

    // High ground tactical advantage bonus calculation
    fun getTacticalAdvantage(attackerPos: Point3D, defenderPos: Point3D): Float {
        val attH = getEffectiveHeight(attackerPos.x, attackerPos.y, attackerPos.z.toInt())
        val defH = getEffectiveHeight(defenderPos.x, defenderPos.y, defenderPos.z.toInt())

        val heightDelta = attH - defH
        return when {
            heightDelta >= 1.0f -> 0.30f // High ground bonus: +30% damage/crit
            heightDelta >= 0.5f -> 0.15f // Moderate elevation bonus: +15%
            heightDelta <= -0.5f -> -0.15f // Low ground penalty: -15%
            else -> 0.0f
        }
    }

    companion object {
        // Factory helper to construct a sample tactical level with multi-level platforms, ramps and stairs
        fun createSampleTacticalGrid(width: Int = 16, height: Int = 16, maxZ: Int = 4): IsoMultiLevelGrid {
            val grid = IsoMultiLevelGrid(width, height, maxZ)

            // Populate ground floor (Z = 0)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val tile = if (x == 0 || y == 0 || x == width - 1 || y == height - 1) TileType.WALL else TileType.FLOOR
                    grid.setTileNode(
                        x, y, 0,
                        ElevationTileNode(
                            gridX = x, gridY = y, zLevel = 0,
                            elevationHeight = 0.0f,
                            elevationType = ElevationType.FLAT,
                            tileType = tile,
                            cover = if ((x + y) % 5 == 0) CoverType.HALF_COVER else CoverType.NONE
                        )
                    )
                }
            }

            // Create raised platform on Z = 0 (Center elevated ridge)
            for (x in 5..10) {
                for (y in 5..10) {
                    grid.setTileNode(
                        x, y, 0,
                        ElevationTileNode(
                            gridX = x, gridY = y, zLevel = 0,
                            elevationHeight = 1.0f, // 1m elevated platform
                            elevationType = ElevationType.ELEVATED_PLATFORM,
                            tileType = TileType.GRID_ROAD,
                            cover = CoverType.HALF_COVER,
                            tacticalHighGroundBonus = 0.25f
                        )
                    )
                }
            }

            // Ramps leading up to platform
            grid.setTileNode(4, 7, 0, ElevationTileNode(4, 7, 0, 0.5f, ElevationType.RAMP_EAST, TileType.FLOOR))
            grid.setTileNode(11, 7, 0, ElevationTileNode(11, 7, 0, 0.5f, ElevationType.RAMP_WEST, TileType.FLOOR))

            // Upper floor (Z = 1) walkways and sniper ledge
            for (x in 2..6) {
                for (y in 2..6) {
                    grid.setTileNode(
                        x, y, 1,
                        ElevationTileNode(
                            gridX = x, gridY = y, zLevel = 1,
                            elevationHeight = 0.0f,
                            elevationType = ElevationType.LEDGE,
                            tileType = TileType.FLOOR,
                            cover = CoverType.FULL_COVER,
                            tacticalHighGroundBonus = 0.35f
                        )
                    )
                }
            }

            // Stairwell connecting Z=0 to Z=1
            grid.setTileNode(2, 7, 0, ElevationTileNode(2, 7, 0, 0.8f, ElevationType.STAIRS_UP, TileType.LADDER_UP))

            return grid
        }
    }
}
