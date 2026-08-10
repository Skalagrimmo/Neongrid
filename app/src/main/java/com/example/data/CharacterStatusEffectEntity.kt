package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing temporary character status effects applied during combat
 * (e.g., 'Hacked', 'Overheated', 'Stealth Buff').
 */
@Entity(tableName = "character_status_effects")
data class CharacterStatusEffectEntity(
    @PrimaryKey val effectId: String,
    val name: String,
    val effectType: String, // e.g., "HACKED", "OVERHEATED", "STEALTH_BUFF"
    val durationSeconds: Float,
    val remainingSeconds: Float,
    val isBuff: Boolean = false,
    val statModifierMultiplier: Float = 1.0f,
    val appliedAtTimestamp: Long = System.currentTimeMillis()
)
