package com.example.neonmarshal.fpe

import com.example.neonmarshal.bridge.ProjectionAdapter
import com.example.neonmarshal.bridge.ProjectionHandle
import com.example.neonmarshal.bridge.SemanticWorldRef

/**
 * Narrow facade for the external FPE 1.0 read-only runtime.
 *
 * FPE accepts a PixelGen-derived projection and may emit view/intent state,
 * but it never becomes an objective-world authority. The concrete FPE binding
 * is injected through [FpeProjectionFacade].
 */
interface FpeProjectionFacade {
    fun project(world: SemanticWorldRef): ProjectionHandle
}

class FpeProjectionAdapter(
    private val facade: FpeProjectionFacade
) : ProjectionAdapter {

    override fun project(world: SemanticWorldRef): ProjectionHandle {
        require(world.worldId.isNotBlank()) { "worldId must not be blank" }
        require(world.revision >= 0L) { "world revision must be non-negative" }
        require(world.fingerprint.isNotBlank()) { "world fingerprint must not be blank" }

        val handle = facade.project(world)

        check(handle.worldId == world.worldId) {
            "FPE projection returned a different world id"
        }
        check(handle.revision == world.revision) {
            "FPE projection returned a different world revision"
        }
        check(handle.fingerprint == world.fingerprint) {
            "FPE projection returned a different world fingerprint"
        }

        return handle
    }
}