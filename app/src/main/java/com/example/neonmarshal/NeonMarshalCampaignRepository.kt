package com.example.neonmarshal

import com.example.neonmarshal.bridge.CampaignStateSink
import com.example.neonmarshal.bridge.TacticalSessionResult

/**
 * Persistence-facing boundary for campaign progression.
 *
 * The NeonMarshal coordinator commits a completed tactical result here,
 * while the concrete implementation decides whether storage is Room,
 * another local store, or a remote campaign backend.
 */
interface NeonMarshalCampaignRepository : CampaignStateSink {
    fun restoreLastResult(): TacticalSessionResult?
}

class InMemoryNeonMarshalCampaignRepository : NeonMarshalCampaignRepository {
    private var lastResult: TacticalSessionResult? = null

    override fun commitResult(result: TacticalSessionResult) {
        lastResult = result.copy(
            emittedEvents = result.emittedEvents.toList()
        )
    }

    override fun restoreLastResult(): TacticalSessionResult? =
        lastResult?.copy(emittedEvents = lastResult!!.emittedEvents.toList())
}