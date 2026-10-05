package com.example.neonmarshal.ebe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EbeRuntimeBundleAdapterTest {

    private val runtime = """
        {
          "version": "1.0.0",
          "tick": 5,
          "available_observations": {
            "observation_1_0": {
              "id": "observation_1_0",
              "source_event_id": "derived_event_1",
              "subject_type": "front",
              "evidence": "direct_local",
              "knowledge_state": "not_yet_assigned_to_any_agent",
              "sector": [0, 2],
              "revision": 1
            }
          },
          "agents": {
            "mara": {
              "memory": {
                "entries": {
                  "observation_1_0": {
                    "observation": {
                      "id": "observation_1_0",
                      "source_event_id": "derived_event_1",
                      "subject_type": "front",
                      "evidence": "direct_local",
                      "knowledge_state": "assigned_as_evidence",
                      "sector": [0, 2],
                      "revision": 1
                    }
                  }
                }
              }
            }
          },
          "action_gateway": {
            "requests": [
              {
                "id": "action_1",
                "actor_id": "mara",
                "action_type": "avoid_front",
                "target_id": "front_0",
                "status": "pending"
              }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun availableObservations_preserveLocalEvidenceBoundary() {
        val adapter = adapter()

        val observations = adapter.availableObservations()

        assertEquals(1, observations.size)
        assertEquals("observation_1_0", observations.single().observationId)
        assertEquals("0,2", observations.single().sectorId)
        assertTrue(observations.single().payloadJson.contains("not_yet_assigned_to_any_agent"))
    }

    @Test
    fun assignedObservations_onlyReturnsExplicitlyAssignedEvidence() {
        val adapter = adapter()

        assertEquals(
            listOf("observation_1_0"),
            adapter.assignedObservations().map { it.observationId }
        )
    }

    @Test
    fun actionRequests_areImportedWithoutChangingAuthority() {
        val adapter = adapter()

        val requests = adapter.buildActionRequests()

        assertEquals(1, requests.size)
        assertEquals("action_1", requests.single().requestId)
        assertEquals("mara", requests.single().actorId)
        assertEquals("avoid_front", requests.single().action)
        assertEquals("front_0", requests.single().targetId)
    }

    @Test
    fun stageAndAssign_doesNotExpandObservationLocality() {
        val adapter = adapter()

        adapter.stageObservations(adapter.availableObservations())
        adapter.assignLocalObservations()

        assertEquals(1, adapter.availableObservations().size)
    }

    private fun adapter(): EbeRuntimeBundleAdapter =
        EbeRuntimeBundleAdapter(
            object : EbeBundleSource {
                override fun read(path: String): String =
                    when (path) {
                        "ebe-runtime.json" -> runtime
                        else -> error("unexpected path: $path")
                    }
            }
        )
}
