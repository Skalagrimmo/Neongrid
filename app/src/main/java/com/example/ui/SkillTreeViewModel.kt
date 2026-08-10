package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataRepository
import com.example.data.PlayerStatsEntity
import com.example.data.UnlockedSkillEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Data model for a skill node within the tiered graph structure.
 */
data class SkillNodeData(
    val id: String,
    val name: String,
    val description: String,
    val tier: Int,
    val cost: Int = 1,
    val prerequisites: List<String> = emptyList(),
    val characterClass: String = "ALL_CLASSES",
    val iconName: String = "Star"
)

/**
 * UI State for the Skill Tree graph view.
 */
data class SkillTreeUiState(
    val selectedSkillId: String? = null,
    val message: String? = null,
    val isError: Boolean = false
)

/**
 * ViewModel managing skill tree progression, tiered prerequisite graphs,
 * and database sync with [DataRepository].
 */
class SkillTreeViewModel(
    private val repository: DataRepository
) : ViewModel() {

    // Default predefined tiered skill tree graph
    val allSkills: List<SkillNodeData> = listOf(
        // Tier 1: Foundation
        SkillNodeData(
            id = "tactical_lens",
            name = "Tactical Lens",
            description = "Highlights enemy line of sight and weak points in real-time.",
            tier = 1,
            cost = 1,
            prerequisites = emptyList(),
            characterClass = "CYBER_NINJA",
            iconName = "Visibility"
        ),
        SkillNodeData(
            id = "nano_shield",
            name = "Nano Shield",
            description = "Deploys a localized energy barrier that absorbs 25% incoming damage.",
            tier = 1,
            cost = 1,
            prerequisites = emptyList(),
            characterClass = "SOLDIER",
            iconName = "Shield"
        ),
        SkillNodeData(
            id = "overclock",
            name = "Overclock",
            description = "Increases movement speed and attack rate by 30% for 6 seconds.",
            tier = 1,
            cost = 1,
            prerequisites = emptyList(),
            characterClass = "NETRUNNER",
            iconName = "Bolt"
        ),

        // Tier 2: Advanced (Requires Tier 1 skills)
        SkillNodeData(
            id = "emp_burst",
            name = "EMP Pulse",
            description = "Disables electronic turrets and stuns nearby cyborgs for 4 seconds.",
            tier = 2,
            cost = 2,
            prerequisites = listOf("tactical_lens"),
            characterClass = "CYBER_NINJA",
            iconName = "FlashOn"
        ),
        SkillNodeData(
            id = "kinetic_surge",
            name = "Kinetic Surge",
            description = "Reflects 40% of physical projectile damage back at attackers.",
            tier = 2,
            cost = 2,
            prerequisites = listOf("nano_shield"),
            characterClass = "SOLDIER",
            iconName = "Security"
        ),
        SkillNodeData(
            id = "neural_hack",
            name = "Neural Hack",
            description = "Overrides hostile drone targeting systems to attack enemy squads.",
            tier = 2,
            cost = 2,
            prerequisites = listOf("overclock"),
            characterClass = "NETRUNNER",
            iconName = "Psychology"
        ),

        // Tier 3: Master (Requires Tier 2 skills)
        SkillNodeData(
            id = "ghost_protocol",
            name = "Ghost Protocol",
            description = "Renders the agent completely invisible to sensors and optical vision for 10s.",
            tier = 3,
            cost = 3,
            prerequisites = listOf("emp_burst"),
            characterClass = "CYBER_NINJA",
            iconName = "VisibilityOff"
        ),
        SkillNodeData(
            id = "singularity_core",
            name = "Singularity Core",
            description = "Unleashes a gravitational shockwave pulling all enemies into a plasma storm.",
            tier = 3,
            cost = 3,
            prerequisites = listOf("kinetic_surge", "neural_hack"),
            characterClass = "NETRUNNER",
            iconName = "AutoAwesome"
        )
    )

    val characterStats: StateFlow<PlayerStatsEntity?> = repository.characterStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unlockedSkillIds: StateFlow<Set<String>> = repository.unlockedSkillIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _uiState = MutableStateFlow(SkillTreeUiState(selectedSkillId = allSkills.firstOrNull()?.id))
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    fun selectSkill(skillId: String) {
        _uiState.update { it.copy(selectedSkillId = skillId, message = null, isError = false) }
    }

    /**
     * Unlocks a skill if the player has enough skill points and prerequisites are met.
     */
    fun unlockSkill(skillId: String) {
        viewModelScope.launch {
            val skill = allSkills.find { it.id == skillId } ?: return@launch
            val currentUnlocked = repository.unlockedSkillIds.first().toSet()
            val currentStats = repository.characterStats.first() ?: PlayerStatsEntity(id = 1, skillPoints = 5)

            if (currentUnlocked.contains(skillId)) {
                _uiState.update { it.copy(message = "Skill '${skill.name}' is already unlocked!", isError = false) }
                return@launch
            }

            // Check prerequisite validation
            val missingPrereqs = skill.prerequisites.filterNot { currentUnlocked.contains(it) }
            if (missingPrereqs.isNotEmpty()) {
                val prereqNames = missingPrereqs.mapNotNull { prereqId -> allSkills.find { it.id == prereqId }?.name }
                _uiState.update {
                    it.copy(
                        message = "Prerequisites required: ${prereqNames.joinToString(", ")}",
                        isError = true
                    )
                }
                return@launch
            }

            // Check skill points cost
            if (currentStats.skillPoints < skill.cost) {
                _uiState.update {
                    it.copy(
                        message = "Insufficient Skill Points! Need ${skill.cost}, but have ${currentStats.skillPoints}.",
                        isError = true
                    )
                }
                return@launch
            }

            // Deduct skill points & save updated stats
            val updatedStats = currentStats.copy(
                skillPoints = (currentStats.skillPoints - skill.cost).coerceAtLeast(0)
            )
            repository.saveCharacterStats(updatedStats)

            // Save unlocked skill into database
            val entity = UnlockedSkillEntity(
                skillId = skill.id,
                skillName = skill.name,
                characterClass = skill.characterClass
            )
            repository.unlockSkill(entity)

            SoundManager.playSkillUnlock()

            _uiState.update {
                it.copy(
                    message = "Successfully unlocked '${skill.name}'!",
                    isError = false
                )
            }
        }
    }

    /**
     * Resets unlocked skills for testing or respec.
     */
    fun resetSkills() {
        viewModelScope.launch {
            repository.clearSkills()
            _uiState.update { it.copy(message = "Skill tree reset.", isError = false) }
        }
    }
}
