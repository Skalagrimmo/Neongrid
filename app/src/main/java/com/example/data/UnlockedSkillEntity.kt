package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unlocked_skill_progress")
data class UnlockedSkillEntity(
    @PrimaryKey val skillId: String,
    val skillName: String,
    val characterClass: String,
    val unlockedAt: Long = System.currentTimeMillis()
)
