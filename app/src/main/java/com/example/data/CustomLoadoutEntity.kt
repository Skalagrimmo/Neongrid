package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_loadouts")
data class CustomLoadoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val weaponId: String,
    val weaponName: String,
    val coreId: String,
    val coreName: String,
    val systemId: String,
    val systemName: String,
    val characterClass: String = "CYBER_RONIN",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
