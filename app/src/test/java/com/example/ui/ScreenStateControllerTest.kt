package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStateControllerTest {

    private enum class TestScreen {
        MENU,
        PLAY,
        SETTINGS
    }

    @Test
    fun changeTo_updatesCurrentScreenAndReportsChanges() {
        val controller = ScreenStateController(TestScreen.MENU)

        assertEquals(TestScreen.MENU, controller.currentScreen)
        assertTrue(controller.changeTo(TestScreen.PLAY))
        assertEquals(TestScreen.PLAY, controller.currentScreen)
        assertFalse(controller.changeTo(TestScreen.PLAY))
        assertTrue(controller.changeTo(TestScreen.SETTINGS))
        assertEquals(TestScreen.SETTINGS, controller.currentScreen)
    }
}
