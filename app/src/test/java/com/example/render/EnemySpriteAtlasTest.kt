package com.example.render

import com.example.model.Enemy
import com.example.model.Point3D
import com.example.model.SpriteAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnemySpriteAtlasTest {
    private fun enemy(type: String, dead: Boolean = false) = Enemy(
        id = "test",
        name = "test",
        pos = Point3D(1f, 1f, 1f),
        type = type,
        isDead = dead
    )

    @Test
    fun profileMappingUsesExistingEnemyPresentationResolver() {
        assertEquals("heavy_elite", EnemySpriteAtlas.profileIdFor(enemy("Boss")))
        assertEquals("sewer_mutant", EnemySpriteAtlas.profileIdFor(enemy("Mutant")))
        assertEquals("stalker_beast", EnemySpriteAtlas.profileIdFor(enemy("SentryDrone")))
        assertEquals("hooded_operator", EnemySpriteAtlas.profileIdFor(enemy("Sentry")))
    }

    @Test
    fun deadEnemyUsesDeathPresentation() {
        assertEquals(SpriteAnimation.DEATH, EnemySpriteAtlas.animationFor(enemy("Syntrob", dead = true)))
    }

    @Test
    fun productionAtlasLayoutIsConsistent() {
        assertEquals(6, EnemySpriteAtlas.columns)
        assertEquals(6, EnemySpriteAtlas.rows)
        assertEquals(224, EnemySpriteAtlas.cellWidth)
        assertEquals(176, EnemySpriteAtlas.cellHeight)
        assertTrue(EnemySpriteAtlas.assetPathFor("heavy_elite")!!.endsWith("heavy_elite_atlas.webp"))
    }
}
