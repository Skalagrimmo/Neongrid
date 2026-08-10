package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity class for player character persistent state.
 * Contains health, stamina, current level, and experience points.
 */
@Entity(tableName = "player_character")
data class PlayerCharacter(
    @PrimaryKey val id: Int = 1,
    val name: String = "Cyber Rogue",
    val health: Int = 100,
    val maxHealth: Int = 100,
    val stamina: Int = 100,
    val maxStamina: Int = 100,
    val currentLevel: Int = 1,
    val experiencePoints: Int = 0
)
