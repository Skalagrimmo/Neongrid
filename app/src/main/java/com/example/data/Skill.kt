package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity and Data Class representing a skill in a hierarchical skill tree.
 * Tracks skill name, power level/tier, character class affiliation, prerequisite requirements,
 * child skills, stat boosts, and unlock status.
 */
@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey val id: String,
    val name: String,
    val characterClass: String = "MultiClass",
    val powerLevel: Int = 1, // Tier in tree hierarchy (1 = Root, 2 = Branch, 3 = Apex)
    val prerequisiteRequirements: String = "",
    val prerequisiteSkillId: String? = null,
    val childSkillIds: String = "",
    val description: String = "",
    val isUnlocked: Boolean = false,
    val skillPointCost: Int = 1,
    val iconName: String = "star",
    val statBoostHealth: Float = 0f,
    val statBoostEnergy: Float = 0f,
    val statBoostDamage: Float = 0f,
    val statBoostStealth: Float = 0f,
    val statBoostSpeed: Float = 0f
) {
    /**
     * Helper function returning all prerequisite skill IDs required to unlock this skill.
     */
    fun getPrerequisiteList(): List<String> {
        val list = mutableListOf<String>()
        if (!prerequisiteSkillId.isNullOrBlank()) list.add(prerequisiteSkillId)
        if (prerequisiteRequirements.isNotBlank()) {
            prerequisiteRequirements.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !list.contains(it) }
                .forEach { list.add(it) }
        }
        return list
    }

    /**
     * Helper function returning child skill IDs unlocked downstream by this skill.
     */
    fun getChildList(): List<String> {
        return if (childSkillIds.isNotBlank()) {
            childSkillIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
    }
}

