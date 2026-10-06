package com.example.neonmarshal.nanomarshal

import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.SemanticWorldRef
import com.example.neonmarshal.bridge.TacticalRuntimeAdapter
import com.example.neonmarshal.bridge.TacticalSessionResult
import com.example.neonmarshal.bridge.TacticalWorldEventSink

/**
 * Narrow boundary around the NanoMarshal runtime.
 *
 * The Android app depends only on this facade. The concrete facade implementation
 * can live beside the imported NanoMarshal runtime without exposing its model types
 * to Neongrid UI, persistence, or the semantic-world adapters.
 */
interface NanoMarshalFacade {
    fun startSession(
        sessionId: String,
        world: SemanticWorldRef
    )

    fun applyWorldEvents(events: List<SemanticWorldEvent>)

    fun step(deltaMs: Long)

    fun finishSession(): TacticalSessionResult
}

/**
 * Lifecycle adapter used by NeonMarshalCoordinator.
 *
 * This class deliberately contains no engine-specific model types. It enforces
 * session ownership and delegates the actual tactical simulation to an injected
 * facade implementation.
 */
class NanoMarshalRuntimeAdapter(
    private val facade: NanoMarshalFacade
) : TacticalRuntimeAdapter, TacticalWorldEventSink {

    private var activeSessionId: String? = null

    override fun startSession(
        sessionId: String,
        world: SemanticWorldRef
    ) {
        check(activeSessionId == null) {
            "NanoMarshal tactical session is already active"
        }
        require(sessionId.isNotBlank()) {
            "sessionId must not be blank"
        }

        facade.startSession(sessionId, world)
        activeSessionId = sessionId
    }

    override fun applyWorldEvents(events: List<SemanticWorldEvent>) {
        check(activeSessionId != null) {
            "NanoMarshal tactical session has not been started"
        }
        if (events.isNotEmpty()) {
            facade.applyWorldEvents(events)
        }
    }

    override fun step(deltaMs: Long) {
        check(activeSessionId != null) {
            "NanoMarshal tactical session has not been started"
        }
        require(deltaMs >= 0) {
            "deltaMs must be non-negative"
        }

        facade.step(deltaMs)
    }

    override fun finishSession(): TacticalSessionResult {
        check(activeSessionId != null) {
            "NanoMarshal tactical session has not been started"
        }

        val result = facade.finishSession()
        check(result.sessionId == activeSessionId) {
            "NanoMarshal facade returned a different session id"
        }

        activeSessionId = null
        return result
    }

    fun activeSessionId(): String? = activeSessionId
}
