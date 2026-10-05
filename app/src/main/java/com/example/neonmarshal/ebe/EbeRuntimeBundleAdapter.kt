package com.example.neonmarshal.ebe

import com.example.neonmarshal.bridge.EpistemicAdapter
import com.example.neonmarshal.bridge.LocalObservation
import com.example.neonmarshal.bridge.SemanticActionRequest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Imports an EBE 1.0 runtime snapshot without reimplementing cognition in Android.
 *
 * The adapter is deliberately conservative:
 * - available observations remain observations;
 * - knowledge/belief is not synthesized from raw evidence;
 * - precomputed action requests are imported only when they exist in the
 *   serialized runtime;
 * - local assignment remains the responsibility of the real EBE runtime.
 *
 * This makes the adapter suitable for frozen bundles and replay/debug tooling.
 */
class EbeRuntimeBundleAdapter(
    private val source: EbeBundleSource
) : EpistemicAdapter {

    private val root by lazy {
        JSONObject(source.read("ebe-runtime.json"))
    }

    private val staged = LinkedHashMap<String, LocalObservation>()

    override fun stageObservations(observations: List<LocalObservation>) {
        for (observation in observations) {
            staged[observation.observationId] = observation
        }
    }

    /**
     * A frozen runtime already contains EBE's assignment result.
     *
     * We therefore validate the staging boundary but intentionally do not perform
     * a second locality expansion or infer knowledge locally.
     */
    override fun assignLocalObservations() {
        staged.values.forEach { observation ->
            require(observation.observationId.isNotBlank()) {
                "EBE observation id must not be blank"
            }
            require(observation.sectorId.isNotBlank()) {
                "EBE observation sector must not be blank"
            }
        }
    }

    override fun buildActionRequests(): List<SemanticActionRequest> {
        val actionObjects = root.optJSONArray("action_requests")
            ?: root.optJSONObject("action_gateway")
                ?.optJSONArray("requests")
            ?: JSONArray()

        return buildList(actionObjects.length()) {
            for (index in 0 until actionObjects.length()) {
                val value = actionObjects.getJSONObject(index)
                add(
                    SemanticActionRequest(
                        requestId = value.getString("id"),
                        actorId = value.getString("actor_id"),
                        action = value.getString("action_type"),
                        targetId = value.optString("target_id").ifBlank { null },
                        payloadJson = value.toString()
                    )
                )
            }
        }
    }

    fun availableObservations(): List<LocalObservation> {
        val observations = root.optJSONArray("available_observations") ?: JSONArray()

        return buildList(observations.length()) {
            for (index in 0 until observations.length()) {
                val raw = observations.getJSONObject(index)
                add(normalizeObservation(raw))
            }
        }
    }

    fun assignedObservations(): List<LocalObservation> {
        val observations = root.optJSONArray("observations") ?: JSONArray()

        return buildList(observations.length()) {
            for (index in 0 until observations.length()) {
                val raw = observations.getJSONObject(index)
                if (raw.optString("knowledge_state") == "assigned_as_evidence") {
                    add(normalizeObservation(raw))
                }
            }
        }
    }

    fun runtimeVersion(): String =
        root.optString("version", "unknown")

    fun snapshotTick(): Long =
        root.optLong("tick", 0L)

    private fun normalizeObservation(raw: JSONObject): LocalObservation {
        val sector = raw.optJSONArray("sector") ?: JSONArray()
        require(sector.length() == 2) {
            "EBE observation sector must be [sx, sy]"
        }

        val sectorId = "${sector.getInt(0)},${sector.getInt(1)}"

        return LocalObservation(
            observationId = raw.getString("id"),
            sectorId = sectorId,
            eventId = raw.optString("source_event_id").ifBlank {
                raw.optString("event_id")
            },
            kind = raw.optString("subject_type", raw.optString("evidence", "observation")),
            payloadJson = raw.toString()
        )
    }
}
