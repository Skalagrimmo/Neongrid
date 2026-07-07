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
    val exploredTilesString: String = "",
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

@Database(entities = [PlayerSaveState::class], version = 2, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun saveStateDao(): PlayerSaveStateDao

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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val saveStateDao: PlayerSaveStateDao) {
    val saveState: Flow<PlayerSaveState?> = saveStateDao.getSaveState()

    suspend fun saveGame(state: PlayerSaveState) {
        saveStateDao.insertSaveState(state)
    }

    suspend fun clearSave() {
        saveStateDao.deleteSaveState()
    }
}
