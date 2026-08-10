package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing nodes within the RPG skill tree graph.
 */
@Entity(tableName = "skill_tree_nodes")
data class SkillTreeNodeEntity(
    @PrimaryKey val nodeId: String,
    val title: String,
    val description: String,
    val branch: String = "COMBAT", // COMBAT, STEALTH, HACKING, TECH
    val tier: Int = 1,
    val cost: Int = 1,
    val isUnlocked: Boolean = false,
    val prerequisiteNodeId: String? = null,
    val iconResName: String = "",
    val attributeBonus: String = ""
)
