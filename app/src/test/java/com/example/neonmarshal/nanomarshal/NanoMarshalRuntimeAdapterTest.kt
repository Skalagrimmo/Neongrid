package com.example.neonmarshal.nanomarshal

import com.example.neonmarshal.bridge.SemanticWorldEvent
import com.example.neonmarshal.bridge.SemanticWorldRef
import com.example.neonmarshal.bridge.TacticalSessionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class NanoMarshalRuntimeAdapterTest {

    @Test
    fun delegatesSessionLifecycleAndKeepsSessionIdentity() {
        val facade = RecordingFacade()
        val adapter = NanoMarshalRuntimeAdapter(facade)
        val world = SemanticWorldRef("world-a", 7L, "fp-a")

        adapter.startSession("session-a", world)
        adapter.step(16L)
        val result = adapter.finishSession()

        assertEquals(listOf("start:session-a:world-a:7", "step:16", "finish"), facade.calls)
        assertEquals("session-a", result.sessionId)
        assertNull(adapter.activeSessionId())
    }

    @Test
    fun delegatesSemanticWorldEvents() {
        val facade = RecordingFacade()
        val adapter = NanoMarshalRuntimeAdapter(facade)

        adapter.startSession(
            "session-a",
            SemanticWorldRef("world-a", 7L, "fp")
        )
        adapter.applyWorldEvents(
            listOf(
                SemanticWorldEvent(
                    eventId = "event-8",
                    worldId = "world-a",
                    revision = 8L,
                    kind = "territory_alert",
                    payloadJson = "{}"
                )
            )
        )

        assertEquals(listOf("start:session-a:world-a:7", "event:event-8:8"), facade.calls)
    }

    @Test
    fun rejectsNestedSession() {
        val adapter = NanoMarshalRuntimeAdapter(RecordingFacade())
        val world = SemanticWorldRef("world-a", 1L, "fp")

        adapter.startSession("one", world)

        assertThrows(IllegalStateException::class.java) {
            adapter.startSession("two", world)
        }
    }

    @Test
    fun rejectsInvalidTickBeforeDelegating() {
        val facade = RecordingFacade()
        val adapter = NanoMarshalRuntimeAdapter(facade)

        adapter.startSession(
            "session-a",
            SemanticWorldRef("world-a", 1L, "fp")
        )

        assertThrows(IllegalArgumentException::class.java) {
            adapter.step(-1L)
        }
        assertEquals(1, facade.calls.count { it.startsWith("start:") })
    }

    private class RecordingFacade : NanoMarshalFacade {
        val calls = mutableListOf<String>()

        override fun startSession(
            sessionId: String,
            world: SemanticWorldRef
        ) {
            calls += "start:" + sessionId + ":" + world.worldId + ":" + world.revision
        }

        override fun applyWorldEvents(events: List<SemanticWorldEvent>) {
            events.forEach { calls += "event:" + it.eventId + ":" + it.revision }
        }

        override fun step(deltaMs: Long) {
            calls += "step:" + deltaMs
        }

        override fun finishSession(): TacticalSessionResult {
            calls += "finish"
            return TacticalSessionResult(
                sessionId = "session-a",
                worldRevision = 7L,
                outcome = "completed"
            )
        }
    }
}