package com.example.model

/**
 * Gameplay-space topology for vertical movement between Z layers.
 *
 * Kept separate from rendering so environment art can replace ladders with
 * stairs, ramps, bridges or shafts without changing navigation semantics.
 */
enum class VerticalConnectorType {
    LADDER,
    STAIRS,
    RAMP,
    BRIDGE,
    SHAFT
}

data class VerticalConnector(
    val id: String,
    val source: Point3D,
    val destination: Point3D,
    val type: VerticalConnectorType,
    val bidirectional: Boolean = true
) {
    val sourceGrid: GridPos
        get() = GridPos(source.x.toInt(), source.y.toInt(), source.z.toInt())

    val destinationGrid: GridPos
        get() = GridPos(destination.x.toInt(), destination.y.toInt(), destination.z.toInt())

    fun connects(from: GridPos, to: GridPos): Boolean {
        val forward = sourceGrid == from && destinationGrid == to
        val reverse = bidirectional && destinationGrid == from && sourceGrid == to
        return forward || reverse
    }
}

/**
 * Immutable topology snapshot. LevelManager can own one instance and rebuild
 * it whenever a procedural/layout level changes.
 */
class VerticalWorldTopology(
    connectors: List<VerticalConnector> = emptyList()
) {
    val connectors: List<VerticalConnector> = connectors.toList()

    fun between(from: GridPos, to: GridPos): VerticalConnector? {
        return connectors.firstOrNull { it.connects(from, to) }
    }

    fun from(grid: GridPos): List<VerticalConnector> {
        return connectors.filter {
            it.sourceGrid == grid || (it.bidirectional && it.destinationGrid == grid)
        }
    }

    fun levels(): Set<Int> {
        return connectors.flatMap { listOf(it.sourceZ, it.destinationZ) }.toSet()
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val ids = connectors.map { it.id }
        ids.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .forEach { (id, count) -> errors += "Duplicate connector id '$id' ($count)" }

        connectors.forEach { connector ->
            if (connector.sourceGrid == connector.destinationGrid) {
                errors += "Connector '${connector.id}' has identical endpoints"
            }
            if (connector.sourceZ == connector.destinationZ) {
                errors += "Connector '${connector.id}' does not change Z level"
            }
        }
        return errors
    }

    companion object {
        fun of(vararg connectors: VerticalConnector): VerticalWorldTopology =
            VerticalWorldTopology(connectors.toList())
    }
}
