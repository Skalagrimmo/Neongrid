package com.example.render

import com.example.model.CivilianActivity
import com.example.model.CivilianNpc
import com.example.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Test

class PedestrianSpriteAtlasTest {

    @Test
    fun activityMapsToExpectedAtlasFrames() {
        fun npc(activity: CivilianActivity) = CivilianNpc(
            id = "civilian_01",
            pos = Point3D(4f, 17f, 1f),
            activity = activity,
            visualVariantId = "civilian_01",
            animationTime = 0f
        )

        assertEquals(1, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.IDLE)))
        assertEquals(9, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.WALKING)))
        assertEquals(18, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.SOCIALIZE)))
        assertEquals(20, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.GESTURE)))
        assertEquals(22, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.HURT)))
        assertEquals(23, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.DEAD)))
        assertEquals(24, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.DOWNED)))
        assertEquals(25, PedestrianSpriteAtlas.frameFor(npc(CivilianActivity.VENDOR)))
    }

    @Test
    fun walkingAnimationLoopsInsideSelectedClip() {
        val npc = CivilianNpc(
            id = "civilian_07",
            pos = Point3D(4f, 17f, 1f),
            activity = CivilianActivity.WALKING,
            visualVariantId = "civilian_07",
            animationTime = 10f
        )

        assertEquals(9, PedestrianSpriteAtlas.frameFor(npc))
    }
}