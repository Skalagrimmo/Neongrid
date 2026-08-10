package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {
    @Query("SELECT * FROM player_character_stats WHERE id = 1")
    fun getCharacterStats(): Flow<PlayerStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCharacterStats(stats: PlayerStatsEntity)

    @Query("DELETE FROM player_character_stats WHERE id = 1")
    suspend fun deleteCharacterStats()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM player_inventory_items")
    fun getAllInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM player_inventory_items WHERE isEquipped = 1")
    fun getEquippedItems(): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItems(items: List<InventoryItemEntity>)

    @Query("DELETE FROM player_inventory_items WHERE itemId = :itemId")
    suspend fun deleteItem(itemId: String)

    @Query("DELETE FROM player_inventory_items")
    suspend fun clearInventory()
}

@Dao
interface StatusEffectDao {
    @Query("SELECT * FROM character_status_effects ORDER BY appliedAtTimestamp DESC")
    fun getAllStatusEffects(): Flow<List<CharacterStatusEffectEntity>>

    @Query("SELECT * FROM character_status_effects WHERE isBuff = 1 ORDER BY appliedAtTimestamp DESC")
    fun getActiveBuffs(): Flow<List<CharacterStatusEffectEntity>>

    @Query("SELECT * FROM character_status_effects WHERE isBuff = 0 ORDER BY appliedAtTimestamp DESC")
    fun getActiveDebuffs(): Flow<List<CharacterStatusEffectEntity>>

    @Query("SELECT * FROM character_status_effects WHERE effectId = :effectId")
    suspend fun getStatusEffectById(effectId: String): CharacterStatusEffectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyStatusEffect(effect: CharacterStatusEffectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun applyStatusEffects(effects: List<CharacterStatusEffectEntity>)

    @Query("DELETE FROM character_status_effects WHERE effectId = :effectId")
    suspend fun removeStatusEffect(effectId: String)

    @Query("DELETE FROM character_status_effects")
    suspend fun clearStatusEffects()
}

@Database(
    entities = [
        PlayerCharacter::class,
        PlayerStatsEntity::class,
        InventoryItemEntity::class,
        InventoryItem::class,
        UnlockedSkillEntity::class,
        Skill::class,
        PlayerSaveState::class,
        CharacterStatusEffectEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerCharacterDao(): PlayerCharacterDao
    abstract fun characterDao(): CharacterDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun skillDao(): SkillDao
    abstract fun statusEffectDao(): StatusEffectDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun unlockedSkillDao(): UnlockedSkillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = getDatabase(context)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
