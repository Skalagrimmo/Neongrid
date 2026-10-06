package com.example.neonmarshal.nanomarshal

import com.example.nanomarshal.core.engine.GameState
import com.example.nanomarshal.core.model.DefaultMissions
import com.example.nanomarshal.core.model.Mission
import com.example.neonmarshal.bridge.SemanticWorldRef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NanoMarshalGameEngineFacadeTest {

    @Test
    fun delegatesSessionLifecycleAndMapsVictoryReward() {
        val mission = DefaultMissions.MISSION_1
        val fakeEngine = FakeEngine(mission)
        val facade = NanoMarshalGameEngineFacade(
            missionResolver = NanoMarshalMissionResolver { mission },
            engineFactory = NanoMarshalEngineFactory { fakeEngine }
        )
        val world = worldRef()

        facade.startSession("session-1", world)
        facade.step(16L)
        fakeEngine.setVictory()

        val result = facade.finishSession()

        assertEquals("session-1", result.sessionId)
        assertEquals(7L, result.worldRevision)
        assertEquals("victory", result.outcome)
        assertEquals(mission.rewardCredits, result.rewardCredits)
        assertEquals(listOf(16L), fakeEngine.updates)
        assertNull(facade.activeSessionId())
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsNestedSession() {
        val mission = DefaultMissions.MISSION_1
        val facade = NanoMarshalGameEngineFacade(
            missionResolver = NanoMarshalMissionResolver { mission },
            engineFactory = NanoMarshalEngineFactory { FakeEngine(mission) }
        )

        facade.startSession("one", worldRef())
        facade.startSession("two", worldRef())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeStep() {
        val mission = DefaultMissions.MISSION_1
        val facade = NanoMarshalGameEngineFacade(
            missionResolver = NanoMarshalMissionResolver { mission },
            engineFactory = NanoMarshalEngineFactory { FakeEngine(mission) }
        )

        facade.startSession("one", worldRef())
        facade.step(-1L)
    }

    @Test
    fun nonVictoryFinishProducesNoReward() {
        val mission = DefaultMissions.MISSION_1
        val facade = NanoMarshalGameEngineFacade(
            missionResolver = NanoMarshalMissionResolver { mission },
            engineFactory = NanoMarshalEngineFactory { FakeEngine(mission) }
        )

        facade.startSession("one", worldRef())

        val result = facade.finishSession()

        assertEquals("terminated", result.outcome)
        assertEquals(0, result.rewardCredits)
    }

    private fun worldRef() = SemanticWorldRef(
        worldId = "m_outpost9",
        revision = 7L,
        fingerprint = "world-fp"
    )

    private class FakeEngine(
        override val mission: Mission
    ) : NanoMarshalEngine {
        private val _state = MutableStateFlow(GameState(currentMission = mission))
        override val gameState = _state.asStateFlow()
        val updates = mutableListOf<Long>()

        override fun update(deltaMs: Long) {
            updates += deltaMs
        }

        fun setVictory() {
            _state.value = _state.value.copy(
                isVictory = true,
                isGameOver = true
            )
        }
    }
}
