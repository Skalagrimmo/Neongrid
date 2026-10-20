package com.example.neonmarshal

import com.example.data.AppDatabase
import com.example.data.NeonMarshalSessionResultMapper
import com.example.neonmarshal.bridge.TacticalSessionResult

/**
 * Persistent campaign boundary for NeonMarshal tactical results.
 * Room owns the schema (`NeonMarshalSessionResultEntity` in AppDatabase);
 * this gateway only maps domain results through the canonical DAO.
 */
class RoomNeonMarshalCampaignRepository(
    private val database: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis
) : NeonMarshalCampaignRepository {

    override fun commitResult(result: TacticalSessionResult) {
        database.neonMarshalSessionResultDao().insertResult(
            NeonMarshalSessionResultMapper.toRow(result, savedAt = clock())
        )
    }

    override fun restoreLastResult(): TacticalSessionResult? {
        val latest = database.neonMarshalSessionResultDao().getLatestResult() ?: return null
        return NeonMarshalSessionResultMapper.fromRow(latest)
    }
}
