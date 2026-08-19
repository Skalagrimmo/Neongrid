package com.example.data

import com.example.model.EquipmentItem

object CustomLoadoutDefaults {
    val presets: List<CustomLoadoutEntity> = listOf(
        preset(
            name = "RONIN MELEE STRIKER",
            weaponId = "nano_blade",
            coreId = "force_shield",
            systemId = "targeting_chip",
            characterClass = "CYBER_RONIN"
        ),
        preset(
            name = "TECH NECRO OVERLOAD",
            weaponId = "plasma_carbine",
            coreId = "overclock_core",
            systemId = "dash_boosters",
            characterClass = "TECH_NECROMANCER"
        ),
        preset(
            name = "GHOST INFILTRATOR",
            weaponId = "nano_blade",
            coreId = "ghost_cloak",
            systemId = "quiet_soles",
            characterClass = "GHOST_INFILTRATOR"
        )
    )

    private fun preset(
        name: String,
        weaponId: String,
        coreId: String,
        systemId: String,
        characterClass: String
    ): CustomLoadoutEntity {
        val weapon = equipmentById(weaponId)
        val core = equipmentById(coreId)
        val system = equipmentById(systemId)

        return CustomLoadoutEntity(
            name = name,
            weaponId = weapon.id,
            weaponName = weapon.name,
            coreId = core.id,
            coreName = core.name,
            systemId = system.id,
            systemName = system.name,
            characterClass = characterClass
        )
    }

    private fun equipmentById(id: String): EquipmentItem {
        return requireNotNull(EquipmentItem.ALL_ITEMS.find { it.id == id }) {
            "Default loadout references unknown equipment id: $id"
        }
    }
}
