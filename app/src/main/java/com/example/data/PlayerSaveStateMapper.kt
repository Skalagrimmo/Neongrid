package com.example.data

import com.example.model.EquipmentItem
import com.example.model.Inventory
import com.example.model.Player
import com.example.model.Point3D
import com.example.model.Quest
import com.example.model.SkillNode
import kotlin.math.max

data class RestoredRunState(
    val player: Player,
    val currentZLevel: Int,
    val exploredTiles: MutableMap<Int, Set<String>>,
    val skillNodes: List<SkillNode>
)

object PlayerSaveStateMapper {

    fun toSaveState(
        player: Player,
        currentZLevel: Int,
        currentScore: Int,
        exploredTiles: Map<Int, Set<String>>,
        timestamp: Long = System.currentTimeMillis()
    ): PlayerSaveState {
        return PlayerSaveState(
            level = player.level,
            xp = player.xp,
            skillPoints = player.skillPoints,
            credits = player.credits,
            equippedWeaponId = player.equippedWeapon.id,
            equippedCoreId = player.equippedCore.id,
            equippedSystemId = player.equippedSystem.id,
            unlockedSkillIdsString = player.unlockedSkills.joinToString(","),
            highScore = max(currentScore, player.credits),
            highestZLevelCleared = max(currentZLevel, 1),
            currentZLevel = currentZLevel,
            playerPosX = player.pos.x,
            playerPosY = player.pos.y,
            playerHealth = player.health,
            playerEnergy = player.energy,
            exploredTilesString = GameStateSerializationService.serializeExploration(exploredTiles),
            questId = player.quest.id,
            questTitle = player.quest.title,
            questDescription = player.quest.description,
            questProgress = player.quest.currentProgress,
            questTargetCount = player.quest.targetCount,
            questCompleted = player.quest.isCompleted,
            inventoryEquipmentIdsString = player.inventory.ownedEquipmentIds.joinToString(","),
            inventoryHealthPacks = player.inventory.healthPacks,
            inventoryEnergyCells = player.inventory.energyCells,
            timestamp = timestamp
        )
    }

    fun restore(saveState: PlayerSaveState, basePlayer: Player = Player()): RestoredRunState {
        val equippedWeapon = equipmentById(saveState.equippedWeaponId, EquipmentItem.DEFAULT_WEAPON)
        val equippedCore = equipmentById(saveState.equippedCoreId, EquipmentItem.DEFAULT_CORE)
        val equippedSystem = equipmentById(saveState.equippedSystemId, EquipmentItem.DEFAULT_SYSTEM)
        val loadedMaxHealth = baseMaxHealth + equippedWeapon.statBoostHealth + equippedCore.statBoostHealth + equippedSystem.statBoostHealth
        val loadedMaxEnergy = baseMaxEnergy + equippedWeapon.statBoostEnergy + equippedCore.statBoostEnergy + equippedSystem.statBoostEnergy
        val unlockedSkills = saveState.unlockedSkillIdsString
            .split(",")
            .filter { it.isNotEmpty() }
            .toSet()
        val ownedEquipment = saveState.inventoryEquipmentIdsString
            .split(",")
            .filter { it.isNotEmpty() }
            .toSet()
            .ifEmpty { defaultEquipmentIds }

        val restoredPlayer = basePlayer.copy(
            pos = Point3D(saveState.playerPosX, saveState.playerPosY, saveState.currentZLevel.toFloat()),
            level = saveState.level,
            xp = saveState.xp,
            skillPoints = saveState.skillPoints,
            credits = saveState.credits,
            equippedWeapon = equippedWeapon,
            equippedCore = equippedCore,
            equippedSystem = equippedSystem,
            unlockedSkills = unlockedSkills,
            maxHealth = loadedMaxHealth,
            maxEnergy = loadedMaxEnergy,
            health = saveState.playerHealth.coerceAtMost(loadedMaxHealth),
            energy = saveState.playerEnergy.coerceAtMost(loadedMaxEnergy),
            quest = Quest(
                id = saveState.questId,
                title = saveState.questTitle,
                description = saveState.questDescription,
                currentProgress = saveState.questProgress,
                targetCount = saveState.questTargetCount,
                isCompleted = saveState.questCompleted
            ),
            inventory = Inventory(
                ownedEquipmentIds = ownedEquipment,
                healthPacks = saveState.inventoryHealthPacks,
                energyCells = saveState.inventoryEnergyCells
            )
        )

        val restoredSkills = SkillNode.getSkillTree().map { node ->
            node.copy(isUnlocked = unlockedSkills.contains(node.id))
        }

        return RestoredRunState(
            player = restoredPlayer,
            currentZLevel = saveState.currentZLevel,
            exploredTiles = GameStateSerializationService.deserializeExploration(saveState.exploredTilesString).toMutableMap(),
            skillNodes = restoredSkills
        )
    }

    private fun equipmentById(id: String, fallback: EquipmentItem): EquipmentItem {
        return EquipmentItem.ALL_ITEMS.find { it.id == id } ?: fallback
    }

    private val defaultEquipmentIds = setOf("nano_blade", "force_shield", "targeting_chip")
    private const val baseMaxHealth = 100f
    private const val baseMaxEnergy = 80f
}
