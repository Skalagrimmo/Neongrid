package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database class that integrates entities and DAOs for player character attributes,
 * inventory items, unlocked skills, status effects, and custom loadout presets.
 */
@Database(
    entities = [
        PlayerCharacter::class,
        PlayerStatsEntity::class,
        CharacterAttributesEntity::class,
        InventoryItemEntity::class,
        InventoryItem::class,
        UnlockedSkillEntity::class,
        SkillTreeNodeEntity::class,
        Skill::class,
        CharacterStatusEffectEntity::class,
        CustomLoadoutEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PlayerCharacterDatabase : RoomDatabase() {

    abstract fun playerCharacterDao(): PlayerCharacterDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun characterAttributesDao(): CharacterAttributesDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun unlockedSkillDao(): UnlockedSkillDao
    abstract fun skillTreeNodeDao(): SkillTreeNodeDao
    abstract fun characterDao(): CharacterDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun skillDao(): SkillDao
    abstract fun statusEffectDao(): StatusEffectDao
    abstract fun customLoadoutDao(): CustomLoadoutDao

    companion object {
        @Volatile
        private var INSTANCE: PlayerCharacterDatabase? = null

        fun getDatabase(context: Context): PlayerCharacterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayerCharacterDatabase::class.java,
                    "player_character_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
