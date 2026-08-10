package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PlayerCharacter
import com.example.data.PlayerCharacterDao
import com.example.data.Skill
import com.example.engine.SkillStatBonuses
import com.example.engine.SkillTreeManager
import com.example.engine.SkillTreeState
import com.example.engine.SkillUnlockResult
import com.example.engine.SkillValidationResult
import com.example.stealth.DetectionStatus
import com.example.stealth.LightLevel
import com.example.stealth.Stance
import com.example.stealth.StealthManager
import com.example.stealth.StealthState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for [PlayerCharacter] screen with integrated stealth state and skill tree state.
 */
sealed interface CharacterUiState {
    object Loading : CharacterUiState
    data class Success(
        val character: PlayerCharacter,
        val stealthState: StealthState = StealthState(),
        val skillTreeState: SkillTreeState = SkillTreeState()
    ) : CharacterUiState
    object Empty : CharacterUiState
}

/**
 * [ViewModel] for managing [PlayerCharacter] state, stealth mechanics using [StealthManager],
 * and hierarchical skill tree progression using [SkillTreeManager].
 * Employs Kotlin Coroutines and Flows for real-time UI state updates.
 */
class CharacterViewModel(
    private val playerCharacterDao: PlayerCharacterDao,
    val stealthManager: StealthManager = StealthManager(),
    val skillTreeManager: SkillTreeManager = SkillTreeManager()
) : ViewModel() {

    val stealthState: StateFlow<StealthState> = stealthManager.stealthState
    val skillTreeState: StateFlow<SkillTreeState> = skillTreeManager.skillTreeState

    val playerCharacter: StateFlow<PlayerCharacter?> = playerCharacterDao.getPlayerCharacterById(1)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val uiState: StateFlow<CharacterUiState> = combine(
        playerCharacterDao.getPlayerCharacterById(1),
        stealthManager.stealthState,
        skillTreeManager.skillTreeState
    ) { character, stealth, skillTree ->
        if (character != null) {
            CharacterUiState.Success(
                character = character,
                stealthState = stealth,
                skillTreeState = skillTree
            )
        } else {
            CharacterUiState.Empty
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CharacterUiState.Loading
    )

    fun saveCharacter(character: PlayerCharacter) {
        viewModelScope.launch {
            playerCharacterDao.insertPlayerCharacter(character)
        }
    }

    fun updateCharacter(character: PlayerCharacter) {
        viewModelScope.launch {
            playerCharacterDao.updatePlayerCharacter(character)
        }
    }

    fun updateHealth(newHealth: Int) {
        viewModelScope.launch {
            val current = playerCharacter.value ?: PlayerCharacter()
            val clampedHealth = newHealth.coerceIn(0, current.maxHealth)
            playerCharacterDao.updatePlayerCharacter(current.copy(health = clampedHealth))
        }
    }

    fun updateStamina(newStamina: Int) {
        viewModelScope.launch {
            val current = playerCharacter.value ?: PlayerCharacter()
            val clampedStamina = newStamina.coerceIn(0, current.maxStamina)
            playerCharacterDao.updatePlayerCharacter(current.copy(stamina = clampedStamina))
        }
    }

    fun addExperience(expPoints: Int) {
        viewModelScope.launch {
            val current = playerCharacter.value ?: PlayerCharacter()
            val oldLevel = current.currentLevel
            var currentExp = current.experiencePoints + expPoints
            var level = oldLevel

            while (currentExp >= level * 100) {
                currentExp -= level * 100
                level++
            }

            if (level > oldLevel) {
                val awardedSkillPoints = (level - oldLevel) * 2
                skillTreeManager.addSkillPoints(awardedSkillPoints)
            }

            val updated = current.copy(
                currentLevel = level,
                experiencePoints = currentExp,
                maxHealth = 100 + (level - 1) * 10,
                maxStamina = 100 + (level - 1) * 5
            )
            playerCharacterDao.insertPlayerCharacter(updated)
        }
    }

    fun deleteCharacter(character: PlayerCharacter) {
        viewModelScope.launch {
            playerCharacterDao.deletePlayerCharacter(character)
        }
    }

    fun resetToDefaultCharacter() {
        viewModelScope.launch {
            playerCharacterDao.deletePlayerCharacterById(1)
            playerCharacterDao.insertPlayerCharacter(PlayerCharacter(id = 1))
            skillTreeManager.resetSkillTree()
        }
    }

    // --- Skill Tree Mechanics Delegate Methods ---

    fun canUnlockSkill(skillId: String): SkillValidationResult {
        return skillTreeManager.canUnlockSkill(skillId)
    }

    fun unlockSkill(skillId: String): SkillUnlockResult {
        return skillTreeManager.unlockSkill(skillId)
    }

    fun lockSkill(skillId: String): SkillUnlockResult {
        return skillTreeManager.lockSkill(skillId)
    }

    fun resetSkillTree() {
        skillTreeManager.resetSkillTree()
    }

    fun addSkillPoints(points: Int) {
        skillTreeManager.addSkillPoints(points)
    }

    fun selectSkillInTree(skillId: String?) {
        skillTreeManager.selectSkill(skillId)
    }

    fun getUnlockableSkills(): List<Skill> {
        return skillTreeManager.getUnlockableSkills()
    }

    fun getChildSkills(parentSkillId: String): List<Skill> {
        return skillTreeManager.getChildSkills(parentSkillId)
    }

    fun getParentSkills(skillId: String): List<Skill> {
        return skillTreeManager.getParentSkills(skillId)
    }

    fun calculateSkillStatBonuses(): SkillStatBonuses {
        return skillTreeManager.calculateStatBonuses()
    }

    // --- Stealth Mechanics Delegate Methods ---

    fun setStance(stance: Stance) {
        stealthManager.setStance(stance)
    }

    fun setLightLevel(lightLevel: LightLevel) {
        stealthManager.setLightLevel(lightLevel)
    }

    fun setInCover(inCover: Boolean) {
        stealthManager.setInCover(inCover)
    }

    fun setInvisibility(invisible: Boolean) {
        stealthManager.setInvisibility(invisible)
    }

    fun setCamouflaged(camouflaged: Boolean) {
        stealthManager.setCamouflaged(camouflaged)
    }

    fun updateNoiseLevel(noiseLevel: Float) {
        stealthManager.updateNoiseLevel(noiseLevel)
    }

    fun updateDetectionProgress(delta: Float) {
        stealthManager.updateDetectionProgress(delta)
    }

    fun setDetectionStatus(status: DetectionStatus) {
        stealthManager.setDetectionStatus(status)
    }

    fun resetStealth() {
        stealthManager.resetStealth()
    }
}

