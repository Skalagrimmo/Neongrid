package com.example.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InventoryItemEntity
import com.example.data.PlayerCharacterRepository
import com.example.data.PlayerStatsEntity
import com.example.data.UnlockedSkillEntity
import com.example.di.DatabaseModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * UI State for player character attributes, equipment, inventory, and skills.
 */
data class PlayerCharacterUiState(
    val characterStats: PlayerStatsEntity = PlayerStatsEntity(),
    val inventoryItems: List<InventoryItemEntity> = emptyList(),
    val equippedItems: List<InventoryItemEntity> = emptyList(),
    val unlockedSkills: List<UnlockedSkillEntity> = emptyList(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

/**
 * ViewModel for observing character stats, inventory items, and unlocked skills
 * from Room Database via [PlayerCharacterRepository], providing a reactive single source of truth.
 */
class PlayerCharacterViewModel(
    private val repository: PlayerCharacterRepository
) : ViewModel() {

    // Secondary constructor taking Application for default ViewModel instantiation in Compose
    constructor(application: Application) : this(
        DatabaseModule.getContainer(application).playerCharacterRepository
    )

    // Direct StateFlows from Room Database
    val characterStats: StateFlow<PlayerStatsEntity?> = repository.playerStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val inventoryItems: StateFlow<List<InventoryItemEntity>> = repository.inventoryItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val equippedItems: StateFlow<List<InventoryItemEntity>> = repository.equippedItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unlockedSkills: StateFlow<List<UnlockedSkillEntity>> = repository.unlockedSkills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unlockedSkillIds: StateFlow<List<String>> = repository.unlockedSkillIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unified combined UI State Flow as a single source of truth for the UI
    val uiState: StateFlow<PlayerCharacterUiState> = combine(
        repository.playerStats,
        repository.inventoryItems,
        repository.equippedItems,
        repository.unlockedSkills
    ) { stats, inventory, equipped, skills ->
        PlayerCharacterUiState(
            characterStats = stats ?: PlayerStatsEntity(),
            inventoryItems = inventory,
            equippedItems = equipped,
            unlockedSkills = skills,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerCharacterUiState(isLoading = true)
    )

    private val _userMessage = MutableStateFlow<Pair<String?, Boolean>>(null to false)
    val userMessage: StateFlow<Pair<String?, Boolean>> = _userMessage.asStateFlow()

    init {
        // Automatically seed default state if database table is empty
        viewModelScope.launch {
            repository.playerStats.firstOrNull().let { currentStats ->
                if (currentStats == null) {
                    seedDefaultCharacterData()
                }
            }
        }
    }

    /**
     * Seeds initial default character stats and starter inventory/skills if DB is clean.
     */
    fun seedDefaultCharacterData() {
        viewModelScope.launch {
            val defaultStats = PlayerStatsEntity(
                id = 1,
                level = 1,
                health = 100f,
                maxHealth = 100f,
                energy = 80f,
                maxEnergy = 80f,
                xp = 0,
                skillPoints = 3,
                credits = 250
            )
            repository.savePlayerStats(defaultStats)

            val starterItems = listOf(
                InventoryItemEntity(
                    itemId = "nano_blade",
                    name = "Nano-Edge Katana",
                    description = "High-frequency monomolecular melee blade.",
                    equipmentType = "WEAPON",
                    isEquipped = true,
                    quantity = 1
                ),
                InventoryItemEntity(
                    itemId = "kinetic_shield",
                    name = "Kinetic Barrier",
                    description = "Absorbs incoming physical impact.",
                    equipmentType = "ARMOR",
                    isEquipped = true,
                    quantity = 1
                ),
                InventoryItemEntity(
                    itemId = "med_pack",
                    name = "Med-Pack",
                    description = "Restores +40 HP instantly.",
                    equipmentType = "CONSUMABLE",
                    isEquipped = false,
                    quantity = 3
                )
            )
            repository.saveInventoryItems(starterItems)

            val starterSkills = listOf(
                UnlockedSkillEntity(
                    skillId = "tactical_lens",
                    skillName = "Tactical Lens",
                    characterClass = "CYBER_NINJA"
                )
            )
            repository.unlockSkills(starterSkills)
            setMessage("Initial character profile loaded from Room DB.", isError = false)
        }
    }

    /**
     * Updates complete player character stats in Room DB.
     */
    fun updateStats(stats: PlayerStatsEntity) {
        viewModelScope.launch {
            repository.savePlayerStats(stats)
        }
    }

    /**
     * Updates player health safely within range [0, maxHealth].
     */
    fun updateHealth(newHealth: Float) {
        viewModelScope.launch {
            val current = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            val clampedHealth = newHealth.coerceIn(0f, current.maxHealth)
            repository.savePlayerStats(current.copy(health = clampedHealth))
        }
    }

    /**
     * Updates player energy safely within range [0, maxEnergy].
     */
    fun updateEnergy(newEnergy: Float) {
        viewModelScope.launch {
            val current = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            val clampedEnergy = newEnergy.coerceIn(0f, current.maxEnergy)
            repository.savePlayerStats(current.copy(energy = clampedEnergy))
        }
    }

    /**
     * Adds XP and automatically handles level-ups.
     */
    fun addXp(amount: Int) {
        viewModelScope.launch {
            val current = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            val newXp = current.xp + amount
            val nextLevelXp = current.level * 100
            if (newXp >= nextLevelXp) {
                val updated = current.copy(
                    level = current.level + 1,
                    xp = newXp - nextLevelXp,
                    skillPoints = current.skillPoints + 1,
                    maxHealth = current.maxHealth + 10f,
                    health = current.maxHealth + 10f
                )
                repository.savePlayerStats(updated)
                setMessage("LEVEL UP! Reached Level ${updated.level}! +1 Skill Point", isError = false)
            } else {
                repository.savePlayerStats(current.copy(xp = newXp))
            }
        }
    }

    /**
     * Modifies player credits.
     */
    fun addCredits(amount: Int) {
        viewModelScope.launch {
            val current = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            val newCredits = (current.credits + amount).coerceAtLeast(0)
            repository.savePlayerStats(current.copy(credits = newCredits))
        }
    }

    /**
     * Modifies player skill points.
     */
    fun addSkillPoints(amount: Int) {
        viewModelScope.launch {
            val current = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            val newSp = (current.skillPoints + amount).coerceAtLeast(0)
            repository.savePlayerStats(current.copy(skillPoints = newSp))
        }
    }

    /**
     * Equips or unequips an inventory item in Room DB.
     */
    fun toggleEquipItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            val targetEquippedState = !item.isEquipped

            // If equipping a weapon or armor, optionally un-equip other items of the same type if single slot rule applies
            if (targetEquippedState && (item.equipmentType == "WEAPON" || item.equipmentType == "ARMOR")) {
                val currentEquipped = repository.equippedItems.first()
                val sameTypeEquipped = currentEquipped.filter { it.equipmentType == item.equipmentType && it.itemId != item.itemId }
                sameTypeEquipped.forEach { unequipTarget ->
                    repository.setEquippedStatus(unequipTarget.itemId, isEquipped = false)
                }
            }

            repository.setEquippedStatus(item.itemId, targetEquippedState)
            val actionName = if (targetEquippedState) "Equipped" else "Unequipped"
            setMessage("$actionName ${item.name}", isError = false)
        }
    }

    /**
     * Inserts or updates an inventory item in Room DB.
     */
    fun saveInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.saveInventoryItem(item)
        }
    }

    /**
     * Deletes an inventory item from Room DB.
     */
    fun deleteInventoryItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteInventoryItem(itemId)
            setMessage("Item removed from inventory.", isError = false)
        }
    }

    /**
     * Unlocks a new skill in Room DB if skill points are sufficient.
     */
    fun unlockSkill(skillId: String, skillName: String, cost: Int = 1, characterClass: String = "ALL_CLASSES") {
        viewModelScope.launch {
            val currentStats = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            if (currentStats.skillPoints < cost) {
                setMessage("Insufficient skill points! Requires $cost SP.", isError = true)
                return@launch
            }

            val existingSkill = repository.getSkillById(skillId)
            if (existingSkill != null) {
                setMessage("Skill '$skillName' is already unlocked.", isError = false)
                return@launch
            }

            // Deduct skill points & unlock
            val updatedStats = currentStats.copy(skillPoints = (currentStats.skillPoints - cost).coerceAtLeast(0))
            repository.savePlayerStats(updatedStats)
            repository.unlockSkill(
                UnlockedSkillEntity(
                    skillId = skillId,
                    skillName = skillName,
                    characterClass = characterClass
                )
            )
            setMessage("Skill '$skillName' unlocked!", isError = false)
        }
    }

    /**
     * Locks a skill and refunds skill points.
     */
    fun lockSkill(skillId: String, refundCost: Int = 1) {
        viewModelScope.launch {
            repository.lockSkill(skillId)
            val currentStats = repository.getPlayerStatsSync() ?: PlayerStatsEntity()
            repository.savePlayerStats(currentStats.copy(skillPoints = currentStats.skillPoints + refundCost))
            setMessage("Skill points refunded.", isError = false)
        }
    }

    /**
     * Clears and resets all player stats, equipment, and skills.
     */
    fun resetAllProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
            seedDefaultCharacterData()
            setMessage("Character attributes and inventory reset to defaults.", isError = false)
        }
    }

    private fun setMessage(msg: String, isError: Boolean) {
        _userMessage.value = msg to isError
    }

    fun clearUserMessage() {
        _userMessage.value = null to false
    }

    /**
     * Factory pattern for instantiating [PlayerCharacterViewModel] with [PlayerCharacterRepository].
     */
    class Factory(private val repository: PlayerCharacterRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayerCharacterViewModel::class.java)) {
                return PlayerCharacterViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
