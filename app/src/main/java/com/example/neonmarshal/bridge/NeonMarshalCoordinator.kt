package com.example.neonmarshal.bridge

/**
 * Coordinates one NeonMarshal session without owning any subsystem's internal state.
 *
 * The coordinator is deliberately small: it only enforces ordering and authority
 * boundaries. Concrete adapters decide how PixelGen/EBE/NanoMarshal/FPE are called.
 */
class NeonMarshalCoordinator(
    private val worldAuthority: WorldAuthorityAdapter,
    private val epistemic: EpistemicAdapter,
    private val tactical: TacticalRuntimeAdapter,
    private val projection: ProjectionAdapter,
    private val campaignState: CampaignStateSink,
    private val worldMutation: WorldMutationAdapter? = null
) {
    private var world: SemanticWorldRef? = null
    private var lastKnownRevision: Long? = null
    private var started = false

    fun start(
        sessionId: String,
        worldId: String,
        revision: Long? = null
    ): ProjectionHandle {
        check(!started) { "NeonMarshal session is already started" }

        val loadedWorld = worldAuthority.loadWorld(worldId, revision)
        world = loadedWorld
        lastKnownRevision = loadedWorld.revision
        tactical.startSession(sessionId, loadedWorld)
        started = true

        return projection.project(loadedWorld)
    }

    /**
     * Advance the integration boundary by one simulation frame.
     *
     * Observations are supplied after they have been normalized by the PixelGen
     * adapter. EBE is responsible for deciding who actually receives them.
     *
     * Action requests are only committed when an explicit mutation adapter has
     * been installed. Without one, requests are returned to the caller and are
     * never silently treated as applied world state.
     */
    fun step(
        deltaMs: Long,
        observations: List<LocalObservation> = emptyList()
    ): List<SemanticActionRequest> {
        check(started) { "NeonMarshal session has not been started" }
        require(deltaMs >= 0) { "deltaMs must be non-negative" }

        val currentWorld = checkNotNull(world)

        val events = worldAuthority.pollEvents(
            worldId = currentWorld.worldId,
            sinceRevision = lastKnownRevision
        )
        events.maxOfOrNull { it.revision }?.let { lastKnownRevision = it }

        if (observations.isNotEmpty()) {
            epistemic.stageObservations(observations)
            epistemic.assignLocalObservations()
        }

        val actionRequests = epistemic.buildActionRequests()
        worldMutation?.let { mutation ->
            actionRequests
                .filter { it.domain == "pixelgen_world_event" }
                .forEach { mutation.applyAction(it) }
        }

        tactical.step(deltaMs)
        return actionRequests
    }

    fun finish(): TacticalSessionResult {
        check(started) { "NeonMarshal session has not been started" }

        val result = tactical.finishSession()
        campaignState.commitResult(result)

        started = false
        world = null
        lastKnownRevision = null

        return result
    }
}
