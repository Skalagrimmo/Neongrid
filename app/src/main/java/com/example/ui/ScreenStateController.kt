package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ScreenStateController<T : Any>(initialScreen: T) {
    var currentScreen by mutableStateOf(initialScreen)
        private set

    fun changeTo(screen: T): Boolean {
        if (screen == currentScreen) return false
        currentScreen = screen
        return true
    }
}
