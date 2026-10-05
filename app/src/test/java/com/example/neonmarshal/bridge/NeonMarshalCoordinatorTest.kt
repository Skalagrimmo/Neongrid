package com.example.neonmarshal.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeonMarshalCoordinatorTest {

    @Test
    fun step_preservesAuthorityOrderAndReturnsUncommittedIntents() {
        val calls = mutableListOf<String>()

        val world = object : WorldAuthorityAdapter {
            override fun loadWorld(worldId: String, revision: Long?): SemanticWorldRef {
                calls += "world.load"
                return SemanticWorldRef(worldId, 3L, "fp-7301")
            }

            override fun pollEvents(
                worldId: String,
                sinceRevision: Long?
            ): List<SemanticWorldEvent> {
                calls += "world.poll"
                return listOf(
                    SemanticWorldEvent(
                        eventId = "event-4",
                        worldId = worldId,
                        revision = 4L,
                        kind = "front_state_changed",
                        payloadJson = "{}"
                    )
                )
            }
        }

        val epistemic = object : EpistemicAdapter {
            override fun stageObservations(observations: List<LocalObservation>) {
                calls += "ebe.stage"
                assertEquals(1, observations.size)
            }

            override fun assignLocalObservations() {
                calls += "ebe.assign"
            }

            override fun buildActionRequests(): List<SemanticActionRequest> {
                calls += "ebe.actions"
                return listOf(
                    SemanticActionRequest(
                        requestId = "request-1",
                        domain = "agent_intent",
                        actorId = "mara",
                        action = "avoid_front",
                        targetId = "front_0",
                        payloadJson = "{}"
                    )
                )
            }
        }

        val tactical = object : TacticalRuntimeAdapter {
            override fun startSession(sessionId: String, world: SemanticWorldRef) {
                calls += "tactical.start"
                assertEquals("fp-7301", world.fingerprint)
            }

            override fun step(deltaMs: Long) {
                calls += "tactical.step"
                assertEquals(16L, deltaMs)
            }

            override fun finishSession(): TacticalSessionResult {
                calls += "tactical.finish"
                return TacticalSessionResult(
                    sessionId = "session-1",
                    worldRevision = 4L,
                    outcome = "success"
                )
            }
        }

        val projection = object : ProjectionAdapter {
            override fun project(world: SemanticWorldRef): ProjectionHandle {
                calls += "fpe.project"
                return ProjectionHandle(world.worldId, world.revision, world.fingerprint)
            }
        }

        val campaign = object : CampaignStateSink {
            override fun commitResult(result: TacticalSessionResult) {
                calls += "campaign.commit"
                assertEquals("success", result.outcome)
            }
        }

        val coordinator = NeonMarshalCoordinator(
            worldAuthority = world,
            epistemic = epistemic,
            tactical = tactical,
            projection = projection,
            campaignState = campaign
        )

        val projectionHandle = coordinator.start(
            sessionId = "session-1",
            worldId = "fp-7301"
        )
        assertEquals("fp-7301", projectionHandle.fingerprint)

        val requests = coordinator.step(
            deltaMs = 16L,
            observations = listOf(
                LocalObservation(
                    observationId = "observation-1",
                    sectorId = "0,2",
                    eventId = "derived_event_1",
                    kind = "front",
                    payloadJson = "{}"
                )
            )
        )

        assertEquals(listOf("request-1"), requests.map { it.requestId })
        assertTrue(
            calls.indexOf("world.poll") < calls.indexOf("ebe.stage") &&
                calls.indexOf("ebe.assign") < calls.indexOf("ebe.actions") &&
                calls.indexOf("ebe.actions") < calls.indexOf("tactical.step")
        )

        val result = coordinator.finish()
        assertEquals("success", result.outcome)
        assertEquals("campaign.commit", calls.last())
    }
}
