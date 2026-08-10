package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity class representing an item in the player's inventory.
 * Stores item name, type (weapon/armor/consumable), base stats, and a unique identifier for equipment management.
 */
@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // WEAPON, ARMOR, CONSUMABLE
    val description: String = "",
    val baseAttack: Int = 0,
    val baseDefense: Int = 0,
    val baseHealthBoost: Int = 0,
    val baseStaminaBoost: Int = 0,
    val isEquipped: Boolean = false,
    val quantity: Int = 1,
    val value: Int = 0
)
