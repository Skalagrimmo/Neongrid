package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_character_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val skillPoints: Int = 2,
    val credits: Int = 120,
    val health: Float = 100f,
    val maxHealth: Float = 100f,
    val energy: Float = 80f,
    val maxEnergy: Float = 80f,
    val posX: Float = 2f,
    val posY: Float = 2f,
    val posZ: Float = 1f,
    val equippedWeaponId: String = "nano_blade",
    val equippedCoreId: String = "force_shield",
    val equippedSystemId: String = "targeting_chip",
    val isSneaking: Boolean = false,
    val isInvisible: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
