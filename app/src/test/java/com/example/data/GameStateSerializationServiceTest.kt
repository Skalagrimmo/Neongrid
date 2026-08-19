package com.example.data

import com.example.model.EquipmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateSerializationServiceTest {

    @Test
    fun explorationRoundTrip_preservesZLevelsAndTiles() {
        val explored = mapOf(
            1 to setOf("2,2", "2,3", "3,3"),
            3 to setOf("9,10")
        )

        val serialized = GameStateSerializationService.serializeExploration(explored)
        val restored = GameStateSerializationService.deserializeExploration(serialized)

        assertEquals(explored, restored)
    }

    @Test
    fun deserializeExploration_ignoresMalformedSegments() {
        val restored = GameStateSerializationService.deserializeExploration("1:2,2;2,3|bad|x:4,4|3:")

        assertEquals(setOf("2,2", "2,3"), restored[1])
        assertEquals(emptySet<String>(), restored[3])
        assertEquals(2, restored.size)
    }

    @Test
    fun equipmentRoundTrip_preservesKnownEquipmentIds() {
        val equipment = listOf(
            EquipmentItem.DEFAULT_WEAPON,
            EquipmentItem.DEFAULT_CORE,
            EquipmentItem.DEFAULT_SYSTEM
        )

        val serialized = GameStateSerializationService.serializeEquipment(equipment)
        val restored = GameStateSerializationService.deserializeEquipment(serialized)

        assertEquals(equipment.map { it.id }, restored.map { it.id })
    }

    @Test
    fun deserializeEquipment_filtersUnknownIds() {
        val restored = GameStateSerializationService.deserializeEquipment("nano_blade,unknown_item,force_shield")

        assertEquals(listOf("nano_blade", "force_shield"), restored.map { it.id })
        assertTrue(restored.none { it.id == "unknown_item" })
    }
}
