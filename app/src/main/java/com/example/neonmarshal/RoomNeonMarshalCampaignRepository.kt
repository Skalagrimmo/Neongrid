package com.example.neonmarshal

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.data.AppDatabase
import com.example.data.NeonMarshalSessionResultMapper
import com.example.neonmarshal.bridge.TacticalSessionResult

/**
 * Persistent campaign boundary for NeonMarshal tactical results.
 * Room remains the canonical database owner; this small gateway uses its
 * SQLite boundary without adding another Room KSP model to AppDatabase.
 */
class RoomNeonMarshalCampaignRepository(
    private val database: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis
) : NeonMarshalCampaignRepository {

    override fun commitResult(result: TacticalSessionResult) {
        val savedAt = clock()
        val row = NeonMarshalSessionResultMapper.toRow(result)
        val values = ContentValues().apply {
            put("sessionId", row.sessionId)
            put("worldRevision", row.worldRevision)
            put("outcome", row.outcome)
            put("emittedEventsJson", row.emittedEventsJson)
            put("rewardCredits", row.rewardCredits)
            put("rewardXp", row.rewardXp)
            put("savedAt", savedAt)
        }

        database.openHelper.writableDatabase.insert(
            "neonmarshal_session_results",
            SQLiteDatabase.CONFLICT_REPLACE,
            values
        )
    }

    override fun restoreLastResult(): TacticalSessionResult? {
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT sessionId, worldRevision, outcome, emittedEventsJson, rewardCredits, rewardXp " +
                "FROM neonmarshal_session_results ORDER BY savedAt DESC LIMIT 1"
        )

        cursor.use {
            if (!it.moveToFirst()) return null

            val entity = com.example.data.NeonMarshalSessionResultRow(
                sessionId = it.getString(0),
                worldRevision = it.getLong(1),
                outcome = it.getString(2),
                emittedEventsJson = it.getString(3),
                rewardCredits = it.getInt(4),
                rewardXp = it.getInt(5)
            )
            return NeonMarshalSessionResultMapper.fromRow(entity)
        }
    }
}
