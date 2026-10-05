package com.example.neonmarshal.bridge

/**
 * Minimal integration contracts between the Android runtime and the external
 * semantic/tactical subsystems.
 *
 * These DTOs intentionally contain no PixelGen, EBE, FPE, or NanoMarshal
 * implementation types. Adapters can therefore change without forcing those
 * runtimes into the Android UI layer.
 */

data class SemanticWorldRef(
    val worldId: String,
    val revision: Long,
    val fingerprint: String
)

data class LocalObservation(
    val observationId: String,
    val sectorId: String,
    val eventId: String,
    val kind: String,
    val payloadJson: String
)

data class SemanticWorldEvent(
    val eventId: String,
    val worldId: String,
    val revision: Long,
    val kind: String,
    val payloadJson: String
)

data class SemanticActionRequest(
    val requestId: String,
    val actorId: String,
    val action: String,
    val targetId: String?,
    val payloadJson: String
)

data class TacticalSessionResult(
    val sessionId: String,
    val worldRevision: Long,
    val outcome: String,
    val emittedEvents: List<SemanticWorldEvent> = emptyList(),
    val rewardCredits: Int = 0,
    val rewardXp: Int = 0
)

interface WorldAuthorityAdapter {
    fun loadWorld(worldId: String, revision: Long? = null): SemanticWorldRef

    fun pollEvents(
        worldId: String,
        sinceRevision: Long? = null
    ): List<SemanticWorldEvent>

    fun applyAction(request: SemanticActionRequest): List<SemanticWorldEvent>
}

interface EpistemicAdapter {
    fun stageObservations(observations: List<LocalObservation>)

    fun assignLocalObservations()

    fun buildActionRequests(): List<SemanticActionRequest>
}

interface TacticalRuntimeAdapter {
    fun startSession(
        sessionId: String,
        world: SemanticWorldRef
    )

    fun step(deltaMs: Long)

    fun finishSession(): TacticalSessionResult
}

interface ProjectionAdapter {
    /**
     * FPE remains read-only. This method returns a detached projection handle;
     * it must never grant objective-world mutation authority.
     */
    fun project(world: SemanticWorldRef): ProjectionHandle
}

data class ProjectionHandle(
    val worldId: String,
    val revision: Long,
    val fingerprint: String
)

interface CampaignStateSink {
    fun commitResult(result: TacticalSessionResult)
}
