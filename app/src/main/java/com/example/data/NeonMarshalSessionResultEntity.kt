package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Canonical Room model for NeonMarshal tactical session results.
 *
 * Historically this table was created only by `AppDatabase.MIGRATION_8_9`
 * and never registered as an entity, so fresh installs (where Room builds
 * the schema from the entity list) had no `neonmarshal_session_results`
 * table at all and the first commit crashed with "no such table".
 * Registering the entity fixes fresh installs; `MIGRATION_9_10` fixes
 * existing v9 databases created before the entity was registered.
 */
@Entity(
    tableName = "neonmarshal_session_results",
    indices = [Index(value = ["savedAt"])]
)
data class NeonMarshalSessionResultEntity(
    @PrimaryKey val sessionId: String,
    val worldRevision: Long,
    val outcome: String,
    val emittedEventsJson: String,
    val rewardCredits: Int,
    val rewardXp: Int,
    val savedAt: Long = 0L
)

@Dao
interface NeonMarshalSessionResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertResult(entity: NeonMarshalSessionResultEntity)

    @Query("SELECT * FROM neonmarshal_session_results ORDER BY savedAt DESC LIMIT 1")
    fun getLatestResult(): NeonMarshalSessionResultEntity?

    @Query("DELETE FROM neonmarshal_session_results")
    fun clearResults()
}
