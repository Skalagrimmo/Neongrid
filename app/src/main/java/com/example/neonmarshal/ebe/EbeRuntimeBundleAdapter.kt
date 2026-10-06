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
        val actionObjects = root.opt("action_requests")
            ?.let(::jsonObjects)
            ?: root.optJSONObject("action_gateway")
                ?.opt("requests")
                ?.let(::jsonObjects)
            ?: emptyList()

        return actionObjects.map { value ->
            SemanticActionRequest(
                requestId = value.getString("id"),
                domain = value.optString("domain", "agent_intent"),
                actorId = value.getString("actor_id"),
                action = value.getString("action_type"),
                targetId = value.optString("target_id").ifBlank { null },
                payloadJson = value.toString()
            )
        }
    }

    fun availableObservations(): List<LocalObservation> =
        root.opt("available_observations")
            ?.let(::jsonObjects)
            ?.map(::normalizeObservation)
            ?: emptyList()

    /**
     * Snapshot format stores assigned observations inside each agent's memory.
     * This method reads those explicit assignments without inferring any new ones.
     */
    fun assignedObservations(): List<LocalObservation> {
        val agents = root.optJSONObject("agents") ?: return emptyList()
        val result = LinkedHashMap<String, LocalObservation>()

        for (agentId in agents.keys()) {
            val agent = agents.optJSONObject(agentId) ?: continue
            val entries = agent.optJSONObject("memory")
                ?.opt("entries")
                ?.let(::jsonObjects)
                ?: emptyList()

            for (entry in entries) {
                val observation = entry.optJSONObject("observation") ?: continue
                if (observation.optString("knowledge_state") == "assigned_as_evidence") {
                    val normalized = normalizeObservation(observation)
                    result.putIfAbsent(normalized.observationId, normalized)
                }
            }
        }

        return result.values.toList()
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
            kind = raw.optString(
                "subject_type",
                raw.optString("evidence", "observation")
            ),
            payloadJson = raw.toString()
        )
    }

    private fun jsonObjects(value: Any): List<JSONObject> = when (value) {
        is JSONArray -> buildList(value.length()) {
            for (index in 0 until value.length()) {
                add(value.getJSONObject(index))
            }
        }

        is JSONObject -> buildList {
            for (key in value.keys()) {
                value.optJSONObject(key)?.let(::add)
            }
        }

        else -> emptyList()
    }
}
