package com.example.neonmarshal.nanomarshal

import com.example.nanomarshal.core.engine.GameEngine
import com.example.nanomarshal.core.engine.GameState
import com.example.nanomarshal.core.model.Mission
import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.SemanticWorldRef
import com.example.neonmarshal.bridge.TacticalSessionResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Small abstraction around the extracted NanoMarshal engine.
 *
 * Neongrid depends on this interface rather than on GameEngine internals.
 */
interface NanoMarshalEngine {
    val mission: Mission
    val gameState: StateFlow<GameState>

    fun applyWorldEvents(events: List<SemanticWorldEvent>)

    fun update(deltaMs: Long)
}

/**
 * Production binding for the extracted NanoMarshal GameEngine.
 */
class GameEngineNanoMarshalEngine(
    override val mission: Mission
) : NanoMarshalEngine {
    private val delegate = GameEngine(mission)

    override val gameState: StateFlow<GameState>
        get() = delegate.gameState

    override fun applyWorldEvents(events: List<SemanticWorldEvent>) {
        delegate.applyWorldEvents(events.map(::toTacticalEvent))
    }

    override fun update(deltaMs: Long) {
        delegate.update(deltaMs)
    }
}

private fun toTacticalEvent(event: SemanticWorldEvent) =
    com.example.nanomarshal.core.engine.SemanticTacticalEvent(
        eventId = event.eventId,
        worldId = event.worldId,
        revision = event.revision,
        kind = event.kind,
        payloadJson = event.payloadJson
    )

fun interface NanoMarshalMissionResolver {
    fun resolve(world: SemanticWorldRef): Mission
}

fun interface NanoMarshalEngineFactory {
    fun create(mission: Mission): NanoMarshalEngine
}

/**
 * Real NanoMarshal facade used by the NeonMarshal tactical adapter.
 *
 * The world reference selects the tactical mission, while NanoMarshal owns
 * moment-to-moment simulation. The facade only translates the session lifecycle
 * back into Neongrid's neutral result type.
 */
class NanoMarshalGameEngineFacade(
    private val missionResolver: NanoMarshalMissionResolver,
    private val engineFactory: NanoMarshalEngineFactory = NanoMarshalEngineFactory(::GameEngineNanoMarshalEngine)
) : NanoMarshalFacade {

    private var activeSessionId: String? = null
    private var activeWorld: SemanticWorldRef? = null
    private var activeEngine: NanoMarshalEngine? = null

    override fun startSession(
        sessionId: String,
        world: SemanticWorldRef
    ) {
        check(activeSessionId == null) {
            "NanoMarshal facade session is already active"
        }
        require(sessionId.isNotBlank()) {
            "sessionId must not be blank"
        }
        require(world.worldId.isNotBlank()) {
            "worldId must not be blank"
        }
        require(world.revision >= 0L) {
            "world revision must be non-negative"
        }
        require(world.fingerprint.isNotBlank()) {
            "world fingerprint must not be blank"
        }

        val mission = missionResolver.resolve(world)
        val engine = engineFactory.create(mission)

        activeSessionId = sessionId
        activeWorld = world
        activeEngine = engine
    }

    override fun applyWorldEvents(events: List<SemanticWorldEvent>) {
        check(activeSessionId != null) {
            "NanoMarshal facade session has not been started"
        }
        val world = checkNotNull(activeWorld)

        events.forEach { event ->
            require(event.worldId == world.worldId) {
                "Semantic event world id does not match active tactical world"
            }
            require(event.revision >= world.revision) {
                "Semantic event revision precedes active tactical world revision"
            }
            require(event.eventId.isNotBlank()) {
                "Semantic event id must not be blank"
            }
            require(event.kind.isNotBlank()) {
                "Semantic event kind must not be blank"
            }
        }

        activeEngine?.applyWorldEvents(events)
    }

    override fun step(deltaMs: Long) {
        check(activeSessionId != null) {
            "NanoMarshal facade session has not been started"
        }
        require(deltaMs >= 0L) {
            "deltaMs must be non-negative"
        }

        activeEngine?.update(deltaMs)
    }

    override fun finishSession(): TacticalSessionResult {
        val sessionId = checkNotNull(activeSessionId) {
            "NanoMarshal facade session has not been started"
        }
        val world = checkNotNull(activeWorld)
        val engine = checkNotNull(activeEngine)

        val state = engine.gameState.value
        val outcome = when {
            state.isVictory -> "victory"
            state.isGameOver -> "defeat"
            else -> "terminated"
        }

        val result = TacticalSessionResult(
            sessionId = sessionId,
            worldRevision = world.revision,
            outcome = outcome,
            rewardCredits = if (state.isVictory) engine.mission.rewardCredits else 0
        )

        activeSessionId = null
        activeWorld = null
        activeEngine = null
        return result
    }

    fun activeSessionId(): String? = activeSessionId
}
