package com.example.data

import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.TacticalSessionResult
import org.junit.Assert.assertEquals
import org.junit.Test

class NeonMarshalSessionResultMapperTest {
    @Test
    fun `round trip preserves result and emitted events`() {
        val result = TacticalSessionResult(
            sessionId = "session-17",
            worldRevision = 42L,
            outcome = "extracted",
            emittedEvents = listOf(
                SemanticWorldEvent(
                    eventId = "event-1",
                    worldId = "world-7",
                    revision = 43L,
                    kind = "territory_alert",
                    payloadJson = "{\"amount\":0.25}"
                )
            ),
            rewardCredits = 120,
            rewardXp = 80
        )

        val restored = NeonMarshalSessionResultMapper.fromRow(
            NeonMarshalSessionResultMapper.toRow(result)
        )

        assertEquals(result, restored)
    }

    @Test
    fun `empty event list survives round trip`() {
        val result = TacticalSessionResult(
            sessionId = "session-empty",
            worldRevision = 3L,
            outcome = "failed"
        )

        val restored = NeonMarshalSessionResultMapper.fromRow(
            NeonMarshalSessionResultMapper.toRow(result)
        )

        assertEquals(result, restored)
    }
}
