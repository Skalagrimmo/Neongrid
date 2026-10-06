package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetProfileTest {

    @Test
    fun catalogIdsAreUnique() {
        val ids = VisualAssetCatalog.profiles.map(VisualAssetProfile::id)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun pedestrianProfilesSupportCoreAmbientAnimations() {
        assertEquals(7, VisualAssetCatalog.civilianProfiles.size)

        VisualAssetCatalog.civilianProfiles.forEach { profile ->
            assertTrue(profile.supports(SpriteAnimation.IDLE))
            assertTrue(profile.supports(SpriteAnimation.WALK))
            assertTrue(profile.supports(SpriteAnimation.GAZE_CHAT))
            assertTrue(profile.supports(SpriteAnimation.GESTURE))
        }
    }

    @Test
    fun vendorProfileExposesInteractionLoop() {
        val vendor = VisualAssetCatalog.resolve("vendor_01")

        assertEquals(VisualProfileRole.VENDOR, vendor.role)
        assertTrue(vendor.supports(SpriteAnimation.VENDOR))
        assertTrue(vendor.supports(SpriteAnimation.INTERACTION_LOOP))
    }

    @Test
    fun unknownProfileResolvesToFallback() {
        val fallback = VisualAssetCatalog.resolve("does_not_exist")

        assertEquals(VisualAssetCatalog.FALLBACK_PROFILE_ID, fallback.id)
        assertEquals(VisualProfileRole.FALLBACK, fallback.role)
        assertTrue(fallback.supports(SpriteAnimation.IDLE))
    }

    @Test
    fun enemyProfilesRemainPresentationOnly() {
        assertEquals(4, VisualAssetCatalog.enemyProfiles.size)

        VisualAssetCatalog.enemyProfiles.forEach { profile ->
            assertEquals(VisualProfileRole.ENEMY, profile.role)
            assertNotNull(profile.sourceSheetId)
            assertTrue(profile.supports(SpriteAnimation.IDLE))
            assertTrue(profile.supports(SpriteAnimation.WALK))
        }
    }
}
