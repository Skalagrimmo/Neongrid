package com.example.neonmarshal

import com.example.neonmarshal.bridge.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonMarshalVerticalSliceTest {

    @Test
    fun completesPixelGenEbeTacticalCampaignLoop() {
        val calls = mutableListOf<String>()
        var committed: TacticalSessionResult? = null

        val world = object : WorldAuthorityAdapter {
            override fun loadWorld(worldId: String, revision: Long?): SemanticWorldRef {
                calls += "pixelgen.load"
                return SemanticWorldRef(worldId, 10L, "fp-10")
            }

            override fun pollEvents(worldId: String, sinceRevision: Long?): List<SemanticWorldEvent> {
                calls += "pixelgen.poll"
                return listOf(SemanticWorldEvent("event-11", worldId, 11L, "sector_changed", "{}"))
            }
        }

        val ebe = object : EpistemicAdapter {
            override fun stageObservations(observations: List<LocalObservation>) { calls += "ebe.stage" }
            override fun assignLocalObservations() { calls += "ebe.assign" }
            override fun buildActionRequests(): List<SemanticActionRequest> {
                calls += "ebe.actions"
                return emptyList()
            }
        }

        val tactical = object : TacticalRuntimeAdapter {
            override fun startSession(sessionId: String, world: SemanticWorldRef) { calls += "nano.start" }
            override fun step(deltaMs: Long) { calls += "nano.step" }
            override fun finishSession(): TacticalSessionResult {
                calls += "nano.finish"
                return TacticalSessionResult("run-1", 11L, "success", rewardCredits = 50, rewardXp = 25)
            }
        }

        val projection = object : ProjectionAdapter {
            override fun project(world: SemanticWorldRef): ProjectionHandle {
                calls += "fpe.project"
                return ProjectionHandle(world.worldId, world.revision, world.fingerprint)
            }
        }

        val campaign = object : CampaignStateSink {
            override fun commitResult(result: TacticalSessionResult) { committed = result; calls += "campaign.commit" }
        }

        val runtime = NeonMarshalRuntime(world, ebe, tactical, projection, campaign)
        val handle = runtime.coordinator.start("run-1", "sector-7301")
        runtime.coordinator.step(16L, listOf(LocalObservation("obs-1", "0,0", "event-11", "front", "{}")))
        val result = runtime.coordinator.finish()

        assertEquals("fp-10", handle.fingerprint)
        assertEquals(result, committed)
        assertEquals(50, result.rewardCredits)
        assertEquals(25, result.rewardXp)
        assertTrue(calls.indexOf("pixelgen.load") < calls.indexOf("nano.start"))
        assertTrue(calls.indexOf("ebe.actions") < calls.indexOf("nano.step"))
        assertTrue(calls.indexOf("nano.finish") < calls.indexOf("campaign.commit"))
    }
}