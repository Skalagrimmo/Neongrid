package com.example.stealth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Player stance affecting visibility and movement noise.
 */
enum class Stance(val visibilityMultiplier: Float, val baseNoiseLevel: Float) {
    CROUCHING(visibilityMultiplier = 0.3f, baseNoiseLevel = 10f),
    STANDING(visibilityMultiplier = 1.0f, baseNoiseLevel = 35f),
    SPRINTING(visibilityMultiplier = 1.6f, baseNoiseLevel = 85f),
    SLIDING(visibilityMultiplier = 0.5f, baseNoiseLevel = 45f)
}

/**
 * Ambient environmental lighting levels affecting player visibility.
 */
enum class LightLevel(val visibilityModifier: Float) {
    DARK(visibilityModifier = 0.15f),
    SHADOWED(visibilityModifier = 0.4f),
    DIM(visibilityModifier = 0.7f),
    DIRECT_LIGHT(visibilityModifier = 1.25f)
}

/**
 * Player detection status level.
 */
enum class DetectionStatus(val label: String) {
    HIDDEN("Hidden"),
    SUSPECTED("Suspicious"),
    DETECTED("Detected"),
    ALERTED("Alerted"),
    COMPROMISED("Compromised")
}

/**
 * State container for player stealth mechanics.
 */
data class StealthState(
    val visibilityLevel: Float = 15f,
    val detectionProgress: Float = 0f,
    val detectionStatus: DetectionStatus = DetectionStatus.HIDDEN,
    val stance: Stance = Stance.CROUCHING,
    val lightLevel: LightLevel = LightLevel.SHADOWED,
    val noiseLevel: Float = 10f,
    val isInCover: Boolean = false,
    val isInvisible: Boolean = false,
    val isCamouflaged: Boolean = false,
    val statusMessage: String = "Hidden in shadows"
)

/**
 * Manager tracking player visibility levels, noise emissions, and detection status.
 * Employs [StateFlow] to broadcast state changes to UI layer and game loop.
 */
class StealthManager {

    private val _stealthState = MutableStateFlow(StealthState())
    val stealthState: StateFlow<StealthState> = _stealthState.asStateFlow()

    /**
     * Updates player stance and recalculates stealth metrics.
     */
    fun setStance(newStance: Stance) {
        _stealthState.update { current ->
            val updated = current.copy(
                stance = newStance,
                noiseLevel = newStance.baseNoiseLevel
            )
            recalculateStealthState(updated)
        }
    }

    /**
     * Updates environmental light level.
     */
    fun setLightLevel(newLightLevel: LightLevel) {
        _stealthState.update { current ->
            val updated = current.copy(lightLevel = newLightLevel)
            recalculateStealthState(updated)
        }
    }

    /**
     * Toggles or sets cover state.
     */
    fun setInCover(inCover: Boolean) {
        _stealthState.update { current ->
            val updated = current.copy(isInCover = inCover)
            recalculateStealthState(updated)
        }
    }

    /**
     * Sets active active optical cloaking/invisibility state.
     */
    fun setInvisibility(invisible: Boolean) {
        _stealthState.update { current ->
            val updated = current.copy(isInvisible = invisible)
            recalculateStealthState(updated)
        }
    }

    /**
     * Toggles camouflage mode.
     */
    fun setCamouflaged(camouflaged: Boolean) {
        _stealthState.update { current ->
            val updated = current.copy(isCamouflaged = camouflaged)
            recalculateStealthState(updated)
        }
    }

    /**
     * Updates movement or action noise level (0..100).
     */
    fun updateNoiseLevel(noise: Float) {
        _stealthState.update { current ->
            val clampedNoise = noise.coerceIn(0f, 100f)
            val updated = current.copy(noiseLevel = clampedNoise)
            recalculateStealthState(updated)
        }
    }

    /**
     * Updates detection progress (0..100) incrementally.
     */
    fun updateDetectionProgress(delta: Float) {
        _stealthState.update { current ->
            val newProgress = (current.detectionProgress + delta).coerceIn(0f, 100f)
            val newStatus = calculateDetectionStatus(newProgress)
            val updated = current.copy(
                detectionProgress = newProgress,
                detectionStatus = newStatus
            )
            recalculateStealthState(updated)
        }
    }

    /**
     * Directly sets detection status and updates detection progress accordingly.
     */
    fun setDetectionStatus(status: DetectionStatus) {
        _stealthState.update { current ->
            val progress = when (status) {
                DetectionStatus.HIDDEN -> 0f
                DetectionStatus.SUSPECTED -> 30f
                DetectionStatus.DETECTED -> 65f
                DetectionStatus.ALERTED -> 85f
                DetectionStatus.COMPROMISED -> 100f
            }
            val updated = current.copy(
                detectionStatus = status,
                detectionProgress = progress
            )
            recalculateStealthState(updated)
        }
    }

    /**
     * Resets stealth parameters to default hidden state.
     */
    fun resetStealth() {
        _stealthState.value = StealthState()
    }

    /**
     * Calculates combined player visibility level based on current stance, light, cover, and abilities.
     */
    fun calculateVisibilityLevel(state: StealthState = _stealthState.value): Float {
        if (state.isInvisible) return 0f

        var baseVisibility = 50f
        baseVisibility *= state.stance.visibilityMultiplier
        baseVisibility *= state.lightLevel.visibilityModifier

        if (state.isInCover) {
            baseVisibility *= 0.4f
        }
        if (state.isCamouflaged) {
            baseVisibility *= 0.5f
        }

        return baseVisibility.coerceIn(0f, 100f)
    }

    private fun calculateDetectionStatus(progress: Float): DetectionStatus {
        return when {
            progress >= 95f -> DetectionStatus.COMPROMISED
            progress >= 75f -> DetectionStatus.ALERTED
            progress >= 50f -> DetectionStatus.DETECTED
            progress >= 20f -> DetectionStatus.SUSPECTED
            else -> DetectionStatus.HIDDEN
        }
    }

    private fun recalculateStealthState(state: StealthState): StealthState {
        val visLevel = calculateVisibilityLevel(state)
        val status = calculateDetectionStatus(state.detectionProgress)
        val msg = buildStatusMessage(visLevel, state.noiseLevel, status, state.isInCover, state.isInvisible)

        return state.copy(
            visibilityLevel = visLevel,
            detectionStatus = status,
            statusMessage = msg
        )
    }

    private fun buildStatusMessage(
        visibility: Float,
        noise: Float,
        status: DetectionStatus,
        inCover: Boolean,
        invisible: Boolean
    ): String {
        return when {
            invisible -> "Cloaked (Invisible)"
            status == DetectionStatus.COMPROMISED -> "COMPROMISED! Enemies engaging!"
            status == DetectionStatus.ALERTED -> "Alert! Enemies actively hunting!"
            status == DetectionStatus.DETECTED -> "Spotted! Enemies investigating position"
            status == DetectionStatus.SUSPECTED -> "Suspicious noise or movement detected"
            inCover -> "Hidden in cover"
            visibility < 25f -> "Concealed in deep shadow"
            visibility < 50f -> "Low visibility profile"
            noise > 70f -> "Making heavy noise!"
            else -> "Exposed in light"
        }
    }
}
