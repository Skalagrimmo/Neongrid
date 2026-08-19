package com.example.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameLoopController(
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    private var loopJob: Job? = null

    val isRunning: Boolean
        get() = loopJob != null

    fun start(
        scope: CoroutineScope,
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        targetDelayMillis: () -> Long,
        onTick: (deltaTimeSeconds: Float) -> Unit
    ) {
        stop()
        loopJob = scope.launch(dispatcher) {
            var lastTime = timeProvider()
            while (isActive) {
                val currentTime = timeProvider()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                onTick(deltaTime)
                delay(targetDelayMillis())
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }
}
