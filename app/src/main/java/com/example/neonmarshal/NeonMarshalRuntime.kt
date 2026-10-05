package com.example.neonmarshal

import com.example.neonmarshal.bridge.CampaignStateSink
import com.example.neonmarshal.bridge.EpistemicAdapter
import com.example.neonmarshal.bridge.NeonMarshalCoordinator
import com.example.neonmarshal.bridge.ProjectionAdapter
import com.example.neonmarshal.bridge.TacticalRuntimeAdapter
import com.example.neonmarshal.bridge.WorldAuthorityAdapter
import com.example.neonmarshal.bridge.WorldMutationAdapter

/**
 * Explicit dependency assembly for the NeonMarshal vertical slice.
 *
 * This is intentionally not an Android ViewModel and contains no UI types.
 * Production composition can replace individual adapters without changing
 * the coordinator or the public subsystem boundaries.
 */
class NeonMarshalRuntime(
    worldAuthority: WorldAuthorityAdapter,
    epistemic: EpistemicAdapter,
    tactical: TacticalRuntimeAdapter,
    projection: ProjectionAdapter,
    campaignState: CampaignStateSink,
    worldMutation: WorldMutationAdapter? = null
) {
    val coordinator = NeonMarshalCoordinator(
        worldAuthority = worldAuthority,
        epistemic = epistemic,
        tactical = tactical,
        projection = projection,
        campaignState = campaignState,
        worldMutation = worldMutation
    )
}