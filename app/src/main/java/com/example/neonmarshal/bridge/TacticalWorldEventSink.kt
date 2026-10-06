package com.example.neonmarshal.bridge

/**
 * Optional tactical-side sink for semantic world events.
 *
 * The sink receives events observed from the objective world authority.
 * It does not grant the tactical runtime authority to mutate PixelGen.
 */
interface TacticalWorldEventSink {
    fun applyWorldEvents(events: List<SemanticWorldEvent>)
}
