package com.example.engine

import com.example.model.EquipmentItem
import com.example.model.EquipmentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Equipment slots available on the player character.
 * Enforces strict item type restrictions per slot.
 */
enum class EquipmentSlot(
    val displayName: String,
    val allowedTypes: List<EquipmentType>
) {
    WEAPON("Weapon Slot", listOf(EquipmentType.WEAPON)),
    ARMOR("Armor Body Slot", listOf(EquipmentType.ARMOR)),
    CORE("Core System Slot", listOf(EquipmentType.CORE)),
    SYSTEM("Subsystem Slot", listOf(EquipmentType.SYSTEM)),
    HEAD("Headgear Slot", listOf(EquipmentType.HEAD, EquipmentType.ARMOR)),
    ACCESSORY("Accessory Slot", listOf(EquipmentType.ACCESSORY, EquipmentType.SYSTEM));

    /**
     * Checks if this slot accepts the provided item type.
     */
    fun accepts(type: EquipmentType): Boolean = allowedTypes.contains(type)
}

/**
 * Validation result when attempting to equip an item into a slot.
 */
sealed interface EquipValidationResult {
    object Allowed : EquipValidationResult
    data class Denied(val reason: String) : EquipValidationResult
}

/**
 * Result of an equip or unequip operation.
 */
sealed interface EquipOperationResult {
    data class Success(
        val previousItem: EquipmentItem?,
        val updatedLoadout: EquippedLoadout,
        val newCombinedStats: CombinedStats
    ) : EquipOperationResult

    data class Failure(val reason: String) : EquipOperationResult
}

/**
 * Base unequipped stats of a player character.
 */
data class BaseStats(
    val health: Float = 100f,
    val energy: Float = 80f,
    val damage: Float = 25f,
    val armor: Float = 5f,
    val speed: Float = 1.0f,
    val stealth: Float = 20f
)

/**
 * Combined character stats resulting from base stats + all equipped items.
 */
data class CombinedStats(
    val baseStats: BaseStats = BaseStats(),
    val totalHealth: Float = 100f,
    val totalEnergy: Float = 80f,
    val totalDamage: Float = 25f,
    val totalArmor: Float = 5f,
    val totalSpeed: Float = 1.0f,
    val totalStealth: Float = 20f,
    val totalBonusHealth: Float = 0f,
    val totalBonusEnergy: Float = 0f,
    val totalBonusDamage: Float = 0f,
    val totalBonusArmor: Float = 0f,
    val totalBonusSpeed: Float = 0f,
    val totalBonusStealth: Float = 0f
)

/**
 * Stat differences when previewing equipping an item versus current loadout.
 */
data class StatDelta(
    val healthDelta: Float = 0f,
    val energyDelta: Float = 0f,
    val damageDelta: Float = 0f,
    val armorDelta: Float = 0f,
    val speedDelta: Float = 0f,
    val stealthDelta: Float = 0f
) {
    val hasPositiveChange: Boolean
        get() = healthDelta > 0 || energyDelta > 0 || damageDelta > 0 || armorDelta > 0 || speedDelta > 0 || stealthDelta > 0
}

/**
 * Container mapping equipment slots to currently equipped items.
 */
data class EquippedLoadout(
    val slots: Map<EquipmentSlot, EquipmentItem> = mapOf(
        EquipmentSlot.WEAPON to EquipmentItem.DEFAULT_WEAPON,
        EquipmentSlot.CORE to EquipmentItem.DEFAULT_CORE,
        EquipmentSlot.SYSTEM to EquipmentItem.DEFAULT_SYSTEM,
        EquipmentSlot.ARMOR to EquipmentItem.DEFAULT_ARMOR,
        EquipmentSlot.HEAD to EquipmentItem.DEFAULT_HEAD
    )
) {
    fun getEquipped(slot: EquipmentSlot): EquipmentItem? = slots[slot]
}

/**
 * Engine system that enforces item slot restrictions and calculates combined base stats.
 */
class EquipmentSystem(
    val baseStats: BaseStats = BaseStats()
) {
    private val _loadoutState = MutableStateFlow(EquippedLoadout())
    val loadoutState: StateFlow<EquippedLoadout> = _loadoutState.asStateFlow()

    private val _combinedStatsState = MutableStateFlow(calculateCombinedStats(baseStats, _loadoutState.value))
    val combinedStatsState: StateFlow<CombinedStats> = _combinedStatsState.asStateFlow()

    /**
     * Validates if [item] can be equipped in [targetSlot].
     */
    fun canEquip(item: EquipmentItem, targetSlot: EquipmentSlot): EquipValidationResult {
        return if (targetSlot.accepts(item.type)) {
            EquipValidationResult.Allowed
        } else {
            val allowedList = targetSlot.allowedTypes.joinToString(", ") { it.name }
            EquipValidationResult.Denied(
                "Cannot equip '${item.name}' (${item.type.name}) into ${targetSlot.displayName}. " +
                        "This slot only accepts [$allowedList]."
            )
        }
    }

    /**
     * Attempts to equip [item] into [targetSlot].
     * Returns [EquipOperationResult.Success] if restriction passes, or [EquipOperationResult.Failure].
     */
    fun equipItem(
        currentLoadout: EquippedLoadout,
        item: EquipmentItem,
        targetSlot: EquipmentSlot
    ): EquipOperationResult {
        val validation = canEquip(item, targetSlot)
        if (validation is EquipValidationResult.Denied) {
            return EquipOperationResult.Failure(validation.reason)
        }

        val previousItem = currentLoadout.slots[targetSlot]
        val updatedMap = currentLoadout.slots.toMutableMap().apply {
            put(targetSlot, item)
        }
        val updatedLoadout = EquippedLoadout(updatedMap)
        val newStats = calculateCombinedStats(baseStats, updatedLoadout)

        return EquipOperationResult.Success(
            previousItem = previousItem,
            updatedLoadout = updatedLoadout,
            newCombinedStats = newStats
        )
    }

    /**
     * Unequips whatever item is currently in [slot].
     */
    fun unequipItem(
        currentLoadout: EquippedLoadout,
        slot: EquipmentSlot
    ): EquipOperationResult {
        val previousItem = currentLoadout.slots[slot]
            ?: return EquipOperationResult.Failure("No item equipped in ${slot.displayName}")

        val updatedMap = currentLoadout.slots.toMutableMap().apply {
            remove(slot)
        }
        val updatedLoadout = EquippedLoadout(updatedMap)
        val newStats = calculateCombinedStats(baseStats, updatedLoadout)

        return EquipOperationResult.Success(
            previousItem = previousItem,
            updatedLoadout = updatedLoadout,
            newCombinedStats = newStats
        )
    }

    /**
     * Calculates the combined base stats from player base stats + stats of all items in [loadout].
     */
    fun calculateCombinedStats(
        base: BaseStats = baseStats,
        loadout: EquippedLoadout
    ): CombinedStats {
        var bonusHealth = 0f
        var bonusEnergy = 0f
        var bonusDamage = 0f
        var bonusArmor = 0f
        var bonusSpeed = 0f
        var bonusStealth = 0f

        for ((_, item) in loadout.slots) {
            bonusHealth += item.statBoostHealth
            bonusEnergy += item.statBoostEnergy
            bonusDamage += item.statBoostDamage
            bonusArmor += item.statBoostArmor
            bonusSpeed += item.statBoostSpeed
            bonusStealth += item.statBoostStealth
        }

        return CombinedStats(
            baseStats = base,
            totalHealth = (base.health + bonusHealth).coerceAtLeast(1f),
            totalEnergy = (base.energy + bonusEnergy).coerceAtLeast(0f),
            totalDamage = (base.damage + bonusDamage).coerceAtLeast(0f),
            totalArmor = (base.armor + bonusArmor).coerceAtLeast(0f),
            totalSpeed = (base.speed + bonusSpeed).coerceAtLeast(0.1f),
            totalStealth = (base.stealth + bonusStealth).coerceAtLeast(0f),
            totalBonusHealth = bonusHealth,
            totalBonusEnergy = bonusEnergy,
            totalBonusDamage = bonusDamage,
            totalBonusArmor = bonusArmor,
            totalBonusSpeed = bonusSpeed,
            totalBonusStealth = bonusStealth
        )
    }

    /**
     * Calculates the delta in stats if [newItem] is equipped in [targetSlot] compared to [currentLoadout].
     */
    fun previewStatChange(
        base: BaseStats = baseStats,
        currentLoadout: EquippedLoadout,
        newItem: EquipmentItem,
        targetSlot: EquipmentSlot
    ): StatDelta {
        val currentStats = calculateCombinedStats(base, currentLoadout)

        // Calculate potential loadout
        val tempMap = currentLoadout.slots.toMutableMap()
        tempMap[targetSlot] = newItem
        val tempStats = calculateCombinedStats(base, EquippedLoadout(tempMap))

        return StatDelta(
            healthDelta = tempStats.totalHealth - currentStats.totalHealth,
            energyDelta = tempStats.totalEnergy - currentStats.totalEnergy,
            damageDelta = tempStats.totalDamage - currentStats.totalDamage,
            armorDelta = tempStats.totalArmor - currentStats.totalArmor,
            speedDelta = tempStats.totalSpeed - currentStats.totalSpeed,
            stealthDelta = tempStats.totalStealth - currentStats.totalStealth
        )
    }

    // --- Stateful Operations ---

    fun setEquippedItem(item: EquipmentItem, slot: EquipmentSlot): EquipOperationResult {
        val result = equipItem(_loadoutState.value, item, slot)
        if (result is EquipOperationResult.Success) {
            _loadoutState.value = result.updatedLoadout
            _combinedStatsState.value = result.newCombinedStats
        }
        return result
    }

    fun removeEquippedItem(slot: EquipmentSlot): EquipOperationResult {
        val result = unequipItem(_loadoutState.value, slot)
        if (result is EquipOperationResult.Success) {
            _loadoutState.value = result.updatedLoadout
            _combinedStatsState.value = result.newCombinedStats
        }
        return result
    }
}
