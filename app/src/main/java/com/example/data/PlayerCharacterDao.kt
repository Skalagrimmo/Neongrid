package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for the [PlayerCharacter] entity.
 * Provides methods for querying, inserting, updating, and deleting player character data.
 */
@Dao
interface PlayerCharacterDao {
    @Query("SELECT * FROM player_character WHERE id = :id")
    fun getPlayerCharacterById(id: Int = 1): Flow<PlayerCharacter?>

    @Query("SELECT * FROM player_character WHERE id = :id")
    suspend fun getPlayerCharacterByIdSync(id: Int = 1): PlayerCharacter?

    @Query("SELECT * FROM player_character")
    fun getAllPlayerCharacters(): Flow<List<PlayerCharacter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerCharacter(character: PlayerCharacter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerCharacters(characters: List<PlayerCharacter>)

    @Update
    suspend fun updatePlayerCharacter(character: PlayerCharacter)

    @Delete
    suspend fun deletePlayerCharacter(character: PlayerCharacter)

    @Query("DELETE FROM player_character WHERE id = :id")
    suspend fun deletePlayerCharacterById(id: Int = 1)
}
