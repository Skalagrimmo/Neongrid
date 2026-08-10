package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val equipmentType: String, // WEAPON, CORE, SYSTEM, CONSUMABLE
    val description: String,
    val quantity: Int = 1,
    val isEquipped: Boolean = false,
    val statBoostHealth: Float = 0f,
    val statBoostEnergy: Float = 0f,
    val statBoostDamage: Float = 0f,
    val statBoostSpeed: Float = 0f,
    val statBoostStealth: Float = 0f,
    val costCredits: Int = 100
)
