package com.example.data

import com.example.model.EquipmentItem

object GameStateSerializationService {

    /**
     * Serializes map exploration status: Map<Int, Set<String>> -> String.
     * Format: ZLevel:x,y;x,y|ZLevel:x,y...
     */
    fun serializeExploration(explored: Map<Int, Set<String>>): String {
        return explored.map { (z, set) ->
            val coords = set.joinToString(";")
            "$z:$coords"
        }.joinToString("|")
    }

    /**
     * Deserializes map exploration status: String -> Map<Int, Set<String>>.
     */
    fun deserializeExploration(serialized: String?): Map<Int, Set<String>> {
        val result = mutableMapOf<Int, Set<String>>()
        if (serialized.isNullOrEmpty()) return result
        try {
            val segments = serialized.split("|")
            for (segment in segments) {
                if (segment.isEmpty()) continue
                val parts = segment.split(":", limit = 2)
                if (parts.size == 2) {
                    val z = parts[0].toIntOrNull() ?: continue
                    val coords = parts[1].split(";").filter { it.isNotEmpty() }.toSet()
                    result[z] = coords
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * Serializes list of equipment items into a comma-separated ID string
     */
    fun serializeEquipment(equipment: List<EquipmentItem>): String {
        return equipment.joinToString(",") { it.id }
    }

    /**
     * Deserializes list of equipment items from a comma-separated ID string
     */
    fun deserializeEquipment(serialized: String?): List<EquipmentItem> {
        if (serialized.isNullOrEmpty()) return emptyList()
        return serialized.split(",")
            .mapNotNull { id -> EquipmentItem.ALL_ITEMS.find { it.id == id } }
    }
}
