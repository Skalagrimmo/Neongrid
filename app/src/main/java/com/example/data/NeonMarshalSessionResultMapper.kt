package com.example.data

import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.TacticalSessionResult
import org.json.JSONArray
import org.json.JSONObject

data class NeonMarshalSessionResultRow(
    val sessionId: String,
    val worldRevision: Long,
    val outcome: String,
    val emittedEventsJson: String,
    val rewardCredits: Int,
    val rewardXp: Int
)

object NeonMarshalSessionResultMapper {
    fun toRow(result: TacticalSessionResult): NeonMarshalSessionResultRow {
        require(result.sessionId.isNotBlank())
        return NeonMarshalSessionResultRow(
            sessionId = result.sessionId,
            worldRevision = result.worldRevision,
            outcome = result.outcome,
            emittedEventsJson = encodeEvents(result.emittedEvents),
            rewardCredits = result.rewardCredits,
            rewardXp = result.rewardXp
        )
    }

    fun fromRow(row: NeonMarshalSessionResultRow): TacticalSessionResult =
        TacticalSessionResult(
            sessionId = row.sessionId,
            worldRevision = row.worldRevision,
            outcome = row.outcome,
            emittedEvents = decodeEvents(row.emittedEventsJson),
            rewardCredits = row.rewardCredits,
            rewardXp = row.rewardXp
        )

    private fun encodeEvents(events: List<SemanticWorldEvent>): String {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put("eventId", event.eventId)
                    .put("worldId", event.worldId)
                    .put("revision", event.revision)
                    .put("kind", event.kind)
                    .put("payloadJson", event.payloadJson)
            )
        }
        return array.toString()
    }

    private fun decodeEvents(serialized: String): List<SemanticWorldEvent> {
        val array = JSONArray(serialized)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SemanticWorldEvent(
                        eventId = item.getString("eventId"),
                        worldId = item.getString("worldId"),
                        revision = item.getLong("revision"),
                        kind = item.getString("kind"),
                        payloadJson = item.getString("payloadJson")
                    )
                )
            }
        }
    }
}
