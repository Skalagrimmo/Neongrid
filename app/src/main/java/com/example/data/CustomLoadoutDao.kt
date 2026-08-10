package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomLoadoutDao {
    @Query("SELECT * FROM custom_loadouts ORDER BY createdAtTimestamp DESC")
    fun getAllLoadouts(): Flow<List<CustomLoadoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoadout(loadout: CustomLoadoutEntity)

    @Query("DELETE FROM custom_loadouts WHERE id = :id")
    suspend fun deleteLoadout(id: Int)

    @Query("DELETE FROM custom_loadouts")
    suspend fun clearLoadouts()
}
