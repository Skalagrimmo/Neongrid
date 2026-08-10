package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Single row representing the player's saved state
@Entity(tableName = "player_save_state")
data class PlayerSaveState(
    @PrimaryKey val id: Int = 1, // Single profile save slot
    val level: Int = 1,
    val xp: Int = 0,
    val skillPoints: Int = 2,
    val credits: Int = 100,
    val equippedWeaponId: String = "nano_blade",
    val equippedCoreId: String = "force_shield",
    val equippedSystemId: String = "targeting_chip",
    val unlockedSkillIdsString: String = "ronin_base", // Comma-separated ids
    val highScore: Int = 0,
    val highestZLevelCleared: Int = 1,
    val currentZLevel: Int = 1,
    val playerPosX: Float = 2f,
    val playerPosY: Float = 2f,
    val playerHealth: Float = 100f,
    val playerEnergy: Float = 80f,
    val exploredTilesString: String = "",
    // Active Quest Progress Persistence
    val questId: String = "sector_gibson_main",
    val questTitle: String = "Infiltrate Sector Gibson",
    val questDescription: String = "Hack security terminals and reach the Sky Portal Receiver on Level Z=3.",
    val questProgress: Int = 0,
    val questTargetCount: Int = 3,
    val questCompleted: Boolean = false,
    // Inventory State Persistence
    val inventoryEquipmentIdsString: String = "nano_blade,force_shield,targeting_chip",
    val inventoryHealthPacks: Int = 2,
    val inventoryEnergyCells: Int = 2,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PlayerSaveStateDao {
    @Query("SELECT * FROM player_save_state WHERE id = 1")
    fun getSaveState(): Flow<PlayerSaveState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaveState(saveState: PlayerSaveState)

    @Query("DELETE FROM player_save_state WHERE id = 1")
    suspend fun deleteSaveState()
}

@Database(
    entities = [
        PlayerSaveState::class,
        PlayerStatsEntity::class,
        InventoryItemEntity::class,
        UnlockedSkillEntity::class,
        CustomLoadoutEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun saveStateDao(): PlayerSaveStateDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun unlockedSkillDao(): UnlockedSkillDao
    abstract fun customLoadoutDao(): CustomLoadoutDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "neongrid_rpg_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(
    private val saveStateDao: PlayerSaveStateDao,
    private val customLoadoutDao: CustomLoadoutDao? = null,
    private val inventoryItemDao: InventoryItemDao? = null
) {
    val saveState: Flow<PlayerSaveState?> = saveStateDao.getSaveState()
    val allLoadouts: Flow<List<CustomLoadoutEntity>> = customLoadoutDao?.getAllLoadouts() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    val allInventoryItems: Flow<List<InventoryItemEntity>> = inventoryItemDao?.getAllInventoryItems() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun saveGame(state: PlayerSaveState) {
        saveStateDao.insertSaveState(state)
    }

    suspend fun clearSave() {
        saveStateDao.deleteSaveState()
    }

    suspend fun saveCustomLoadout(loadout: CustomLoadoutEntity) {
        customLoadoutDao?.insertLoadout(loadout)
    }

    suspend fun deleteCustomLoadout(id: Int) {
        customLoadoutDao?.deleteLoadout(id)
    }

    suspend fun saveInventoryItem(item: InventoryItemEntity) {
        inventoryItemDao?.insertOrUpdateItem(item)
    }
}
