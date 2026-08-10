package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StealthVisibilityOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stealthVisibilityOverlay_rendersCorrectlyWhenHidden() {
        composeTestRule.setContent {
            StealthVisibilityOverlay(
                stealthLevel = 1.0f,
                proximityToEnemy = 0.0f,
                isNearCover = true,
                isSneaking = true,
                enemyDistance = 20.0f
            )
        }

        composeTestRule.onNodeWithTag("stealth_visibility_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stealth_vignette_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stealth_badge_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cover_status_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stealth_level_meter").assertIsDisplayed()
    }

    @Test
    fun stealthVisibilityOverlay_showsProximityHazardWhenDetected() {
        composeTestRule.setContent {
            StealthVisibilityOverlay(
                stealthLevel = 0.1f,
                proximityToEnemy = 0.9f,
                isNearCover = false,
                isSneaking = false,
                enemyDistance = 2.5f
            )
        }

        composeTestRule.onNodeWithTag("stealth_visibility_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("proximity_hazard_banner").assertIsDisplayed()
    }
}
