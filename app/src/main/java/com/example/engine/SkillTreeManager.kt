package com.example.engine

import com.example.data.Skill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Validation result when checking if a skill can be unlocked in the hierarchical tree.
 */
sealed interface SkillValidationResult {
    object Allowed : SkillValidationResult
    data class Denied(val reason: String) : SkillValidationResult
}

/**
 * Result of an unlock operation in the skill tree.
 */
sealed interface SkillUnlockResult {
    data class Success(
        val unlockedSkill: Skill,
        val updatedState: SkillTreeState,
        val newlyUnlockedChildren: List<Skill>
    ) : SkillUnlockResult

    data class Failure(val reason: String) : SkillUnlockResult
}

/**
 * Aggregated character stat bonuses provided by unlocked skills.
 */
data class SkillStatBonuses(
    val bonusHealth: Float = 0f,
    val bonusEnergy: Float = 0f,
    val bonusDamage: Float = 0f,
    val bonusStealth: Float = 0f,
    val bonusSpeed: Float = 0f
)

/**
 * State container for the character skill tree hierarchy and unlock progression.
 */
data class SkillTreeState(
    val allSkills: List<Skill> = SkillTreeManager.DEFAULT_SKILLS,
    val unlockedSkillIds: Set<String> = setOf("ronin_base"),
    val availableSkillPoints: Int = 3,
    val selectedSkillId: String? = "ronin_base"
) {
    val totalUnlockedCount: Int
        get() = unlockedSkillIds.size

    val selectedSkill: Skill?
        get() = allSkills.find { it.id == selectedSkillId }
}

/**
 * Manager handling hierarchical skill tree unlock paths, prerequisite verification,
 * skill point allocation, and stat bonus aggregation for player character abilities.
 */
class SkillTreeManager(
    initialSkills: List<Skill> = DEFAULT_SKILLS,
    initialUnlockedIds: Set<String> = setOf("ronin_base"),
    initialSkillPoints: Int = 3
) {
    private val _skillTreeState = MutableStateFlow(
        SkillTreeState(
            allSkills = markUnlockedSkills(initialSkills, initialUnlockedIds),
            unlockedSkillIds = initialUnlockedIds,
            availableSkillPoints = initialSkillPoints
        )
    )
    val skillTreeState: StateFlow<SkillTreeState> = _skillTreeState.asStateFlow()

    /**
     * Checks if [skillId] meets all hierarchical prerequisite requirements and point costs.
     */
    fun canUnlockSkill(
        skillId: String,
        state: SkillTreeState = _skillTreeState.value
    ): SkillValidationResult {
        val skill = state.allSkills.find { it.id == skillId }
            ?: return SkillValidationResult.Denied("Skill with ID '$skillId' does not exist in skill tree.")

        if (state.unlockedSkillIds.contains(skillId)) {
            return SkillValidationResult.Denied("Ability '${skill.name}' is already unlocked.")
        }

        if (state.availableSkillPoints < skill.skillPointCost) {
            return SkillValidationResult.Denied(
                "Insufficient skill points. Required: ${skill.skillPointCost}, Available: ${state.availableSkillPoints}"
            )
        }

        // Hierarchical prerequisite verification: All prerequisite parent skills must be unlocked first
        val prerequisites = skill.getPrerequisiteList()
        val missingPrereqs = prerequisites.filter { !state.unlockedSkillIds.contains(it) }

        if (missingPrereqs.isNotEmpty()) {
            val missingNames = missingPrereqs.map { missingId ->
                state.allSkills.find { it.id == missingId }?.name ?: missingId
            }
            return SkillValidationResult.Denied(
                "Hierarchical prerequisite locked. You must unlock parent ability: [${missingNames.joinToString(", ")}]"
            )
        }

        return SkillValidationResult.Allowed
    }

    /**
     * Attempts to unlock [skillId] in the character's hierarchical skill tree.
     */
    fun unlockSkill(skillId: String): SkillUnlockResult {
        val currentState = _skillTreeState.value
        val validation = canUnlockSkill(skillId, currentState)

        if (validation is SkillValidationResult.Denied) {
            return SkillUnlockResult.Failure(validation.reason)
        }

        val targetSkill = currentState.allSkills.first { it.id == skillId }
        val newUnlockedIds = currentState.unlockedSkillIds + skillId
        val remainingPoints = currentState.availableSkillPoints - targetSkill.skillPointCost
        val updatedSkills = markUnlockedSkills(currentState.allSkills, newUnlockedIds)

        val newState = currentState.copy(
            allSkills = updatedSkills,
            unlockedSkillIds = newUnlockedIds,
            availableSkillPoints = remainingPoints,
            selectedSkillId = skillId
        )

        _skillTreeState.value = newState

        // Identify children that just became unlockable
        val children = getChildSkills(skillId, updatedSkills)

        return SkillUnlockResult.Success(
            unlockedSkill = targetSkill,
            updatedState = newState,
            newlyUnlockedChildren = children
        )
    }

    /**
     * Locks [skillId] and refunds skill points, provided no dependent child skills are currently unlocked.
     */
    fun lockSkill(skillId: String): SkillUnlockResult {
        val currentState = _skillTreeState.value
        if (!currentState.unlockedSkillIds.contains(skillId)) {
            return SkillUnlockResult.Failure("Ability is not currently unlocked.")
        }

        // Check if any unlocked skills rely on this skill as a prerequisite
        val dependentSkills = currentState.allSkills.filter { skill ->
            currentState.unlockedSkillIds.contains(skill.id) && skill.getPrerequisiteList().contains(skillId)
        }

        if (dependentSkills.isNotEmpty()) {
            val depNames = dependentSkills.joinToString(", ") { it.name }
            return SkillUnlockResult.Failure(
                "Cannot lock '${skillId}'. The following unlocked downstream abilities depend on it: [$depNames]"
            )
        }

        val targetSkill = currentState.allSkills.first { it.id == skillId }
        val newUnlockedIds = currentState.unlockedSkillIds - skillId
        val refundedPoints = currentState.availableSkillPoints + targetSkill.skillPointCost
        val updatedSkills = markUnlockedSkills(currentState.allSkills, newUnlockedIds)

        val newState = currentState.copy(
            allSkills = updatedSkills,
            unlockedSkillIds = newUnlockedIds,
            availableSkillPoints = refundedPoints,
            selectedSkillId = skillId
        )

        _skillTreeState.value = newState

        return SkillUnlockResult.Success(
            unlockedSkill = targetSkill.copy(isUnlocked = false),
            updatedState = newState,
            newlyUnlockedChildren = emptyList()
        )
    }

    /**
     * Resets all unlocked skills back to default root skill and refunds spent skill points.
     */
    fun resetSkillTree() {
        _skillTreeState.update { current ->
            val spentPoints = current.allSkills
                .filter { current.unlockedSkillIds.contains(it.id) && it.id != "ronin_base" }
                .sumOf { it.skillPointCost }

            val rootSet = setOf("ronin_base")
            val updatedSkills = markUnlockedSkills(current.allSkills, rootSet)

            current.copy(
                allSkills = updatedSkills,
                unlockedSkillIds = rootSet,
                availableSkillPoints = current.availableSkillPoints + spentPoints,
                selectedSkillId = "ronin_base"
            )
        }
    }

    /**
     * Grants additional skill points to the character.
     */
    fun addSkillPoints(points: Int) {
        if (points <= 0) return
        _skillTreeState.update { current ->
            current.copy(availableSkillPoints = current.availableSkillPoints + points)
        }
    }

    /**
     * Sets the currently selected skill for detailed UI inspection.
     */
    fun selectSkill(skillId: String?) {
        _skillTreeState.update { current ->
            current.copy(selectedSkillId = skillId)
        }
    }

    /**
     * Returns a list of skills that can currently be unlocked based on prerequisites and skill points.
     */
    fun getUnlockableSkills(state: SkillTreeState = _skillTreeState.value): List<Skill> {
        return state.allSkills.filter { skill ->
            canUnlockSkill(skill.id, state) is SkillValidationResult.Allowed
        }
    }

    /**
     * Returns direct downstream child skills dependent on [parentSkillId].
     */
    fun getChildSkills(
        parentSkillId: String,
        skills: List<Skill> = _skillTreeState.value.allSkills
    ): List<Skill> {
        return skills.filter { it.getPrerequisiteList().contains(parentSkillId) }
    }

    /**
     * Returns prerequisite parent skills required by [skillId].
     */
    fun getParentSkills(
        skillId: String,
        skills: List<Skill> = _skillTreeState.value.allSkills
    ): List<Skill> {
        val target = skills.find { it.id == skillId } ?: return emptyList()
        val prereqIds = target.getPrerequisiteList()
        return skills.filter { prereqIds.contains(it.id) }
    }

    /**
     * Calculates total aggregated stat bonuses from all currently unlocked skills.
     */
    fun calculateStatBonuses(
        unlockedIds: Set<String> = _skillTreeState.value.unlockedSkillIds,
        skills: List<Skill> = _skillTreeState.value.allSkills
    ): SkillStatBonuses {
        var totalHealth = 0f
        var totalEnergy = 0f
        var totalDamage = 0f
        var totalStealth = 0f
        var totalSpeed = 0f

        for (skill in skills) {
            if (unlockedIds.contains(skill.id)) {
                totalHealth += skill.statBoostHealth
                totalEnergy += skill.statBoostEnergy
                totalDamage += skill.statBoostDamage
                totalStealth += skill.statBoostStealth
                totalSpeed += skill.statBoostSpeed
            }
        }

        return SkillStatBonuses(
            bonusHealth = totalHealth,
            bonusEnergy = totalEnergy,
            bonusDamage = totalDamage,
            bonusStealth = totalStealth,
            bonusSpeed = totalSpeed
        )
    }

    private fun markUnlockedSkills(skills: List<Skill>, unlockedIds: Set<String>): List<Skill> {
        return skills.map { skill ->
            skill.copy(isUnlocked = unlockedIds.contains(skill.id))
        }
    }

    companion object {
        /**
         * Hierarchical skill tree definition across Tier 1 (Root), Tier 2 (Branch), and Tier 3 (Apex) paths.
         */
        val DEFAULT_SKILLS: List<Skill> = listOf(
            // --- TIER 1: ROOT FOUNDATION ABILITIES ---
            Skill(
                id = "ronin_base",
                name = "Ronin Combat Matrix",
                characterClass = "COMBAT",
                powerLevel = 1,
                prerequisiteRequirements = "",
                childSkillIds = "blade_mastery,kinetic_surge",
                description = "Core melee combat telemetry that increases base melee damage and combat stance responsiveness.",
                isUnlocked = true,
                skillPointCost = 1,
                iconName = "swords",
                statBoostDamage = 10f,
                statBoostHealth = 15f
            ),
            Skill(
                id = "ghost_base",
                name = "Ghost Stealth Protocol",
                characterClass = "STEALTH",
                powerLevel = 1,
                prerequisiteRequirements = "",
                childSkillIds = "shadow_step,optical_cloak",
                description = "Advanced sound dampening and visual distortion that reduces enemy vision fill rate.",
                isUnlocked = false,
                skillPointCost = 1,
                iconName = "visibility_off",
                statBoostStealth = 20f,
                statBoostSpeed = 0.1f
            ),
            Skill(
                id = "hacker_base",
                name = "Netrunner Cyber Link",
                characterClass = "TECH",
                powerLevel = 1,
                prerequisiteRequirements = "",
                childSkillIds = "neural_overclock,emp_shockwave",
                description = "Direct neural interface enhancing cybernetic energy capacity and ability recharge rates.",
                isUnlocked = false,
                skillPointCost = 1,
                iconName = "memory",
                statBoostEnergy = 25f
            ),

            // --- TIER 2: ADVANCED BRANCH ABILITIES ---
            Skill(
                id = "blade_mastery",
                name = "Nano-Blade Mastery",
                characterClass = "COMBAT",
                powerLevel = 2,
                prerequisiteSkillId = "ronin_base",
                childSkillIds = "dragon_strike",
                description = "Refines plasma blade strikes to bypass 25% of armored mechanical targets.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "precision_manufacturing",
                statBoostDamage = 20f
            ),
            Skill(
                id = "kinetic_surge",
                name = "Kinetic Surge Blast",
                characterClass = "COMBAT",
                powerLevel = 2,
                prerequisiteSkillId = "ronin_base",
                childSkillIds = "",
                description = "Converts physical impact feedback into a forceful kinetic shockwave.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "flash_on",
                statBoostHealth = 30f,
                statBoostDamage = 10f
            ),
            Skill(
                id = "shadow_step",
                name = "Shadow Step Teleport",
                characterClass = "STEALTH",
                powerLevel = 2,
                prerequisiteSkillId = "ghost_base",
                childSkillIds = "ghost_execution",
                description = "Instantly blinks 5 meters through shadows silently without alerting guards.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "directions_run",
                statBoostStealth = 25f,
                statBoostSpeed = 0.2f
            ),
            Skill(
                id = "optical_cloak",
                name = "Active Optical Cloak",
                characterClass = "STEALTH",
                powerLevel = 2,
                prerequisiteSkillId = "ghost_base",
                childSkillIds = "",
                description = "Renders the suit 95% invisible to optical sensors and thermal optics.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "eye_tracking",
                statBoostStealth = 35f
            ),
            Skill(
                id = "neural_overclock",
                name = "Neural Speed Overclock",
                characterClass = "TECH",
                powerLevel = 2,
                prerequisiteSkillId = "hacker_base",
                childSkillIds = "system_override",
                description = "Overclocks motor reflexes, granting 30% attack speed and fast hacking.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "speed",
                statBoostEnergy = 30f,
                statBoostSpeed = 0.25f
            ),
            Skill(
                id = "emp_shockwave",
                name = "EMP Pulse Field",
                characterClass = "TECH",
                powerLevel = 2,
                prerequisiteSkillId = "hacker_base",
                childSkillIds = "",
                description = "Emits an electromagnetic pulse that temporarily disables security turrets.",
                isUnlocked = false,
                skillPointCost = 2,
                iconName = "electric_bolt",
                statBoostDamage = 15f,
                statBoostEnergy = 20f
            ),

            // --- TIER 3: APEX MASTER ABILITIES ---
            Skill(
                id = "dragon_strike",
                name = "Dragon Blade Apex",
                characterClass = "COMBAT",
                powerLevel = 3,
                prerequisiteSkillId = "blade_mastery",
                childSkillIds = "",
                description = "Unleashes a rapid flurry of plasma slashes inflicting massive physical damage.",
                isUnlocked = false,
                skillPointCost = 3,
                iconName = "local_fire_department",
                statBoostDamage = 45f,
                statBoostHealth = 40f
            ),
            Skill(
                id = "ghost_execution",
                name = "Ghost Execution Strike",
                characterClass = "STEALTH",
                powerLevel = 3,
                prerequisiteSkillId = "shadow_step",
                childSkillIds = "",
                description = "Guaranteed instant lethal stealth takedown against undetected elite targets.",
                isUnlocked = false,
                skillPointCost = 3,
                iconName = "gavel",
                statBoostStealth = 50f,
                statBoostDamage = 30f
            ),
            Skill(
                id = "system_override",
                name = "Total Cyber System Override",
                characterClass = "TECH",
                powerLevel = 3,
                prerequisiteSkillId = "neural_overclock",
                childSkillIds = "",
                description = "Hacks the mainframe to force all hostile drones and turrets into automated allies.",
                isUnlocked = false,
                skillPointCost = 3,
                iconName = "settings_power",
                statBoostEnergy = 50f,
                statBoostSpeed = 0.3f
            )
        )
    }
}
