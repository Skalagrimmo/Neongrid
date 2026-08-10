package com.example.di

import android.content.Context
import com.example.data.*

/**
 * Dependency Injection container and provider module for Room Databases and DAOs.
 * Provides singleton access to AppDatabase, GameDatabase, DAOs, and PlayerCharacterRepository.
 */
class AppContainer(private val context: Context) {

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val gameDatabase: GameDatabase by lazy {
        GameDatabase.getDatabase(context)
    }

    val playerCharacterDatabase: PlayerCharacterDatabase by lazy {
        PlayerCharacterDatabase.getDatabase(context)
    }

    val playerCharacterDao: PlayerCharacterDao by lazy {
        appDatabase.playerCharacterDao()
    }

    val characterDao: CharacterDao by lazy {
        appDatabase.characterDao()
    }

    val inventoryDao: InventoryDao by lazy {
        appDatabase.inventoryDao()
    }

    val skillDao: SkillDao by lazy {
        appDatabase.skillDao()
    }

    val playerStatsDao: PlayerStatsDao by lazy {
        appDatabase.playerStatsDao()
    }

    val inventoryItemDao: InventoryItemDao by lazy {
        appDatabase.inventoryItemDao()
    }

    val unlockedSkillDao: UnlockedSkillDao by lazy {
        appDatabase.unlockedSkillDao()
    }

    val statusEffectDao: StatusEffectDao by lazy {
        appDatabase.statusEffectDao()
    }

    val playerSaveStateDao: PlayerSaveStateDao by lazy {
        gameDatabase.saveStateDao()
    }

    val playerCharacterRepository: PlayerCharacterRepository by lazy {
        PlayerCharacterRepository(playerStatsDao, inventoryItemDao, unlockedSkillDao)
    }

    val dataRepository: DataRepository by lazy {
        DataRepository(characterDao, inventoryDao, skillDao, playerSaveStateDao, statusEffectDao, unlockedSkillDao)
    }
}

object DatabaseModule {
    @Volatile
    private var container: AppContainer? = null

    fun getContainer(context: Context): AppContainer {
        return container ?: synchronized(this) {
            container ?: AppContainer(context.applicationContext).also { container = it }
        }
    }
}
