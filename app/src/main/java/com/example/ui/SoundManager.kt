package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Singleton SoundManager providing low-latency synthesized audio feedback for game events,
 * including stealth detection alerts, skill activations, and menu interactions.
 */
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    /** Global mute toggle for all audio playback */
    var isMuted: Boolean = false

    private const val DEFAULT_SAMPLE_RATE = 22050

    /**
     * Synthesizes and plays a procedural audio clip asynchronously using AudioTrack.
     */
    private fun playProceduralSound(durationMs: Int, generate: (Int, Int) -> Short) {
        if (isMuted) return
        scope.launch {
            try {
                val numSamples = (DEFAULT_SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@launch

                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    samples[i] = generate(i, numSamples)
                }

                val bufferSize = numSamples * 2 // 16-bit Mono (2 bytes per sample)

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(DEFAULT_SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                audioTrack.write(samples, 0, numSamples)

                delay(durationMs + 40L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {
                    // Ignore release errors after stopping
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================
    // 1. STEALTH DETECTION ALERTS
    // ==========================================

    /**
     * Urgent high-pitched alarm pulse when an enemy fully spots or alerts to the player.
     * @param intensity Controls pitch and urgency (0.5 to 1.5)
     */
    fun playStealthDetectionAlert(intensity: Float = 1.0f) {
        val duration = (340 * intensity.coerceIn(0.6f, 1.4f)).toInt()
        playProceduralSound(duration) { i, total ->
            val t = i.toFloat() / total
            // Rapid double-beep modulation
            val pulse = if ((t * 10).toInt() % 2 == 0) 1.0f else 0.1f
            val baseFreq = (1200.0 + 400.0 * sin(t * PI.toFloat() * 3.0)) * intensity
            val phase = 2.0 * PI * baseFreq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val wave = if (sin(phase) > 0) 0.8 else -0.8 // Square pulse for cyber alarm feel
            val envelope = (1.0f - t * 0.5f)
            val sample = (wave * pulse * envelope * 0.45f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Subtle warning pulse played when detection meter rises above warning threshold.
     */
    fun playStealthWarning() {
        playProceduralSound(180) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val freq = 780.0 + 120.0 * sin(t * PI.toFloat() * 2.0)
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.28f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    // ==========================================
    // 2. SKILL ACTIVATIONS & UNLOCKS
    // ==========================================

    /**
     * Energetic sci-fi pulse chime when activating an active skill or ability (Overclock, Nano Shield, EMP, etc.).
     */
    fun playSkillActivation(skillName: String = "") {
        playProceduralSound(360) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            // Rising resonant synth sweep with harmonic overtone
            val freq1 = 440.0 + 880.0 * t
            val freq2 = freq1 * 1.5 // Perfect fifth harmonic
            val phase1 = 2.0 * PI * freq1 * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val phase2 = 2.0 * PI * freq2 * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val synth = sin(phase1) * 0.7 + sin(phase2) * 0.3
            val sample = (synth * envelope * 0.50f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Ascending audio chime when purchasing or unlocking a skill node in the Skill Tree graph.
     */
    fun playSkillUnlock() {
        playProceduralSound(420) { i, total ->
            val t = i.toFloat() / total
            val envelope = if (t < 0.2f) t / 0.2f else (1.0f - (t - 0.2f) / 0.8f)
            val notes = intArrayOf(523, 659, 784, 1047) // C E G C major arpeggio
            val noteIdx = (t * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx].toDouble()
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.45f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    // ==========================================
    // 3. MENU & UI INTERACTIONS
    // ==========================================

    /**
     * Crisp, subtle mechanical UI tap sound for buttons, tabs, and loadout actions.
     */
    fun playMenuClick() {
        playProceduralSound(45) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t) * (1.0f - t) // Fast decay
            val freq = 1800.0 - 600.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val clickNoise = (Random.nextFloat() * 2f - 1f) * 0.1f * envelope
            val tone = sin(phase) * 0.9 + clickNoise
            val sample = (tone * envelope * 0.25f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Soft hover tone when scrolling or focusing UI elements.
     */
    fun playMenuHover() {
        playProceduralSound(30) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t)
            val freq = 2200.0
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.15f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Cyber blip when opening a full screen menu or overlay.
     */
    fun playMenuOpen() {
        playProceduralSound(160) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val freq = 600.0 + 800.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.35f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Subtle descending blip when closing a menu or returning to game.
     */
    fun playMenuClose() {
        playProceduralSound(140) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t)
            val freq = 1200.0 - 600.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.30f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    // ==========================================
    // 4. GAMEPLAY SOUND EFFECTS
    // ==========================================

    fun playFootstep(sneaking: Boolean) {
        val duration = if (sneaking) 45 else 80
        val volumeScale = if (sneaking) 0.12f else 0.40f
        playProceduralSound(duration) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t) * (1.0f - t)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.18f
            val baseTone = sin(2.0 * PI * 140.0 * (i.toDouble() / DEFAULT_SAMPLE_RATE)) * 0.15
            val sample = ((noise + baseTone) * envelope * 32767 * volumeScale).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playAttack() {
        playProceduralSound(220) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat()) * (1.0f - t)
            val freq = 1900.0 - 1500.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val tone = sin(phase)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.08f * (1.0f - t)
            val sample = ((tone + noise) * envelope * 0.65f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playBackstab() {
        playProceduralSound(380) { i, total ->
            val t = i.toFloat() / total
            val envelope = 1.0f - t
            val freq = 3200.0 - 2800.0 * t
            val vibrato = 1.0 + 0.2 * sin(2.0 * PI * 35.0 * t)
            val phase = 2.0 * PI * (freq * vibrato) * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val tone = sin(phase)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f * (1.0f - t)
            val sample = ((tone + noise) * envelope * 0.75f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playLaser() {
        playProceduralSound(130) { i, total ->
            val t = i.toFloat() / total
            val envelope = 1.0f - t
            val freq = 2400.0 - 1800.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val wave = if (sin(phase) > 0) 1.0 else -1.0
            val sample = (wave * envelope * 0.30f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playLadder(ascending: Boolean) {
        playProceduralSound(320) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val notes = if (ascending) intArrayOf(440, 554, 659, 880) else intArrayOf(880, 659, 554, 440)
            val noteIndex = (t * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex].toDouble()
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.40f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playStealthToggle(isActive: Boolean) {
        playProceduralSound(280) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val freq = if (isActive) 250.0 + 850.0 * t else 1100.0 - 850.0 * t
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val modulation = 0.08 * sin(2.0 * PI * 18.0 * t)
            val wave = sin(phase + modulation)
            val sample = (wave * envelope * 0.42f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playInteract() {
        playProceduralSound(180) { i, total ->
            val t = i.toFloat() / total
            val envelope = if (t < 0.5f) (1.0f - t * 2) else (1.0f - (t - 0.5f) * 2)
            val freq = if (t < 0.5f) 980.0 else 1470.0
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.35f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playLevelUp() {
        playProceduralSound(550) { i, total ->
            val t = i.toFloat() / total
            val envelope = if (t < 0.15f) (t / 0.15f) else (1.0f - t)
            val notes = intArrayOf(523, 659, 784, 1047, 1318)
            val noteIndex = (t * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex].toDouble()
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.48f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    fun playAlert() {
        playStealthDetectionAlert(1.0f)
    }

    fun playCreditLoot() {
        playProceduralSound(160) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t) * (1.0f - t)
            val freq = 1600.0 + 600.0 * sin(t * PI.toFloat())
            val phase = 2.0 * PI * freq * (i.toDouble() / DEFAULT_SAMPLE_RATE)
            val sample = (sin(phase) * envelope * 0.30f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }
}
