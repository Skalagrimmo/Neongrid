package com.example.render

import com.example.model.Enemy
import com.example.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Test

class EnemyVisualVariantResolverTest {
    private fun enemy(type: String) = Enemy(
        id = "test",
        name = "test",
        pos = Point3D(1f, 1f, 1f),
        type = type
    )

    @Test
    fun gameplayTypesResolveToReusableVisualProfiles() {
        assertEquals("heavy_elite", EnemyVisualVariantResolver.resolve(enemy("Boss")))
        assertEquals("heavy_elite", EnemyVisualVariantResolver.resolve(enemy("Syntrob")))
        assertEquals("hooded_operator", EnemyVisualVariantResolver.resolve(enemy("Sentry")))
        assertEquals("stalker_beast", EnemyVisualVariantResolver.resolve(enemy("SentryDrone")))
        assertEquals("sewer_mutant", EnemyVisualVariantResolver.resolve(enemy("Mutant")))
    }

    @Test
    fun unknownTypesRemainRenderable() {
        assertEquals("heavy_elite", EnemyVisualVariantResolver.resolve(enemy("Unknown")))
    }
}