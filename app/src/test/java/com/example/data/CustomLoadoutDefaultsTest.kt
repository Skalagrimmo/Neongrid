package com.example.data

import com.example.model.EquipmentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLoadoutDefaultsTest {

    @Test
    fun presets_referenceExistingEquipmentAndNames() {
        val equipmentById = EquipmentItem.ALL_ITEMS.associateBy { it.id }

        CustomLoadoutDefaults.presets.forEach { preset ->
            val weapon = equipmentById[preset.weaponId]
            val core = equipmentById[preset.coreId]
            val system = equipmentById[preset.systemId]

            assertEquals(weapon?.name, preset.weaponName)
            assertEquals(core?.name, preset.coreName)
            assertEquals(system?.name, preset.systemName)
        }
    }

    @Test
    fun presets_coverEachPlayableClass() {
        val classes = CustomLoadoutDefaults.presets.map { it.characterClass }.toSet()

        assertEquals(setOf("CYBER_RONIN", "TECH_NECROMANCER", "GHOST_INFILTRATOR"), classes)
        assertTrue(CustomLoadoutDefaults.presets.all { it.name.isNotBlank() })
    }
}
