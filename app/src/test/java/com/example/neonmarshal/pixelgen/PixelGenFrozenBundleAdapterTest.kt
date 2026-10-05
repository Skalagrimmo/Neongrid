package com.example.neonmarshal.pixelgen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelGenFrozenBundleAdapterTest {

    private val manifest = """
        {
          "artifact_count": 4,
          "manifest_version": "1.0.0",
          "artifacts": [
            {
              "artifact_type": "world",
              "path": "world.json",
              "payload_version": "0.7"
            },
            {
              "artifact_type": "world_state",
              "path": "state.json",
              "payload_version": "0.8.0",
              "revision": 3
            },
            {
              "artifact_type": "runtime_events",
              "path": "events.json",
              "payload_version": "0.8.0"
            },
            {
              "artifact_type": "ebe_runtime",
              "path": "ebe-runtime.json",
              "payload_version": "0.8.0"
            }
          ],
          "contract_fingerprint": "contract",
          "relationships": {
            "state_matches_world": true,
            "events_match_state_log": true,
            "world_semantic_fingerprint": "ff06afa542ab7ddc1f8d142cb979e2f8f2fa78023ce3bd049bc5b36615ba072a"
          }
        }
    """.trimIndent()

    private val state = """
        {
          "clock": {"revision": 3, "tick": 3},
          "derived_events": [
            {
              "id": "derived_event_1",
              "revision": 1,
              "event_type": "front_state_changed",
              "sector": [0, 3],
              "subject_id": "front_0",
              "subject_type": "front"
            },
            {
              "id": "derived_event_2",
              "revision": 2,
              "event_type": "territory_state_changed",
              "sector": [1, 0],
              "subject_id": "territory_0",
              "subject_type": "territory"
            },
            {
              "id": "derived_event_3",
              "revision": 3,
              "event_type": "front_state_changed",
              "sector": [1, 1],
              "subject_id": "front_1",
              "subject_type": "front"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun loadWorld_usesCanonicalSemanticFingerprintAndStateRevision() {
        val adapter = adapter()

        val ref = adapter.loadWorld(
            "ff06afa542ab7ddc1f8d142cb979e2f8f2fa78023ce3bd049bc5b36615ba072a"
        )

        assertEquals(3L, ref.revision)
        assertEquals(
            "ff06afa542ab7ddc1f8d142cb979e2f8f2fa78023ce3bd049bc5b36615ba072a",
            ref.fingerprint
        )
    }

    @Test
    fun pollEvents_preservesOrderedPixelGenRevisions() {
        val adapter = adapter()

        val events = adapter.pollEvents(
            worldId = "ff06afa542ab7ddc1f8d142cb979e2f8f2fa78023ce3bd049bc5b36615ba072a",
            sinceRevision = 1
        )

        assertEquals(listOf(2L, 3L), events.map { it.revision })
        assertEquals(
            listOf("territory_state_changed", "front_state_changed"),
            events.map { it.kind }
        )
        assertTrue(events.first().payloadJson.contains("\"subject_id\":\"territory_0\""))
    }

    @Test
    fun applyAction_rejectsMutationOnFrozenBundle() {
        val adapter = adapter()

        assertThrows(UnsupportedOperationException::class.java) {
            adapter.applyAction(
                SemanticActionRequest(
                    requestId = "req-1",
                    actorId = "player",
                    action = "hack_terminal",
                    targetId = "terminal_1",
                    payloadJson = "{}"
                )
            )
        }
    }

    private fun adapter(): PixelGenFrozenBundleAdapter {
        return PixelGenFrozenBundleAdapter(
            object : PixelGenBundleSource {
                override fun read(path: String): String = when (path) {
                    "pixelgen_contract_manifest.json" -> manifest
                    "state.json" -> state
                    "world.json" -> "{}"
                    "events.json" -> "[]"
                    else -> error("unexpected path: $path")
                }
            }
        )
    }
}
