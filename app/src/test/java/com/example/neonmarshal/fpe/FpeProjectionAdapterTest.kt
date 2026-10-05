package com.example.neonmarshal.fpe

import com.example.neonmarshal.bridge.ProjectionHandle
import com.example.neonmarshal.bridge.SemanticWorldRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FpeProjectionAdapterTest {

    @Test
    fun acceptsOnlyMatchingWorldIdentity() {
        val facade = object : FpeProjectionFacade {
            override fun project(world: SemanticWorldRef): ProjectionHandle =
                ProjectionHandle(world.worldId, world.revision, world.fingerprint)
        }
        val adapter = FpeProjectionAdapter(facade)
        val world = SemanticWorldRef("world-a", 4L, "fp-a")

        assertEquals(
            ProjectionHandle("world-a", 4L, "fp-a"),
            adapter.project(world)
        )
    }

    @Test
    fun rejectsProjectionFromDifferentRevision() {
        val adapter = FpeProjectionAdapter(object : FpeProjectionFacade {
            override fun project(world: SemanticWorldRef): ProjectionHandle =
                ProjectionHandle(world.worldId, world.revision + 1L, world.fingerprint)
        })

        assertThrows(IllegalStateException::class.java) {
            adapter.project(SemanticWorldRef("world-a", 4L, "fp-a"))
        }
    }

    @Test
    fun rejectsInvalidWorldReference() {
        val adapter = FpeProjectionAdapter(object : FpeProjectionFacade {
            override fun project(world: SemanticWorldRef): ProjectionHandle =
                ProjectionHandle(world.worldId, world.revision, world.fingerprint)
        })

        assertThrows(IllegalArgumentException::class.java) {
            adapter.project(SemanticWorldRef("", 0L, "fp"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            adapter.project(SemanticWorldRef("world", -1L, "fp"))
        }
    }
}