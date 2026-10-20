package com.example.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        CharacterStatusEffectEntity::class,
        NeonMarshalSessionResultEntity::class
    ],
    version = 10,
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
    abstract fun neonMarshalSessionResultDao(): NeonMarshalSessionResultDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(neonMarshalSessionResultsCreateSql)
                db.execSQL(neonMarshalSessionResultsSavedAtIndexSql)
            }
        }

        // v9 databases created before NeonMarshalSessionResultEntity was
        // registered lack the neonmarshal_session_results table (it was only
        // in MIGRATION_8_9), so v9 -> v10 must create it. IF NOT EXISTS keeps
        // this safe for 8 -> 9 -> 10 upgrades where MIGRATION_8_9 already
        // created the table.
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(neonMarshalSessionResultsCreateSql)
                db.execSQL(neonMarshalSessionResultsSavedAtIndexSql)
            }
        }

        private const val neonMarshalSessionResultsCreateSql = """
            CREATE TABLE IF NOT EXISTS neonmarshal_session_results (
                sessionId TEXT NOT NULL,
                worldRevision INTEGER NOT NULL,
                outcome TEXT NOT NULL,
                emittedEventsJson TEXT NOT NULL,
                rewardCredits INTEGER NOT NULL,
                rewardXp INTEGER NOT NULL,
                savedAt INTEGER NOT NULL,
                PRIMARY KEY(sessionId)
            )
        """

        private const val neonMarshalSessionResultsSavedAtIndexSql =
            "CREATE INDEX IF NOT EXISTS index_neonmarshal_session_results_savedAt " +
                "ON neonmarshal_session_results(savedAt)"

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
                .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
                // No destructive fallback for 8 -> 9 -> 10: player data must
                // survive. Only pre-baseline installs (versions 1..7, which
                // predate the current schema and have no recorded migrations)
                // rebuild instead of crashing; downgrades wipe for dev safety.
                .fallbackToDestructiveMigrationFrom(true, 1, 2, 3, 4, 5, 6, 7)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
