package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing the core RPG character attributes.
 */
@Entity(tableName = "character_attributes")
data class CharacterAttributesEntity(
    @PrimaryKey val characterId: Int = 1,
    val strength: Int = 10,
    val agility: Int = 10,
    val intelligence: Int = 10,
    val stealth: Int = 10,
    val tech: Int = 10,
    val vitality: Int = 10,
    val critRate: Float = 0.05f,
    val critDamage: Float = 1.5f,
    val armorRating: Int = 15,
    val movementSpeed: Float = 1.0f,
    val updatedAt: Long = System.currentTimeMillis()
)
