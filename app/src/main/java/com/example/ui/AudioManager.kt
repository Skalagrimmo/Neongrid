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

object AudioManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    var isMuted = false

    private fun playProceduralSound(durationMs: Int, generate: (Int, Int) -> Short) {
        if (isMuted) return
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@launch

                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    samples[i] = generate(i, numSamples)
                }

                val bufferSize = numSamples * 2 // 16-bit Mono (2 bytes/sample)

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
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                audioTrack.write(samples, 0, numSamples)

                // Wait for playback to complete, then stop and release resources safely
                delay(durationMs + 50L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Suppress post-playback release errors
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Light cyber-click footstep sound.
     * Generates a soft mechanical impact, quieter when sneaking.
     */
    fun playFootstep(sneaking: Boolean) {
        val duration = if (sneaking) 45 else 80
        val volumeScale = if (sneaking) 0.12f else 0.40f
        playProceduralSound(duration) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t) * (1.0f - t) // rapid decay
            val noise = (Random.nextFloat() * 2f - 1f) * 0.18f
            val baseTone = sin(2.0 * PI * 140.0 * (i.toDouble() / 22050.0)) * 0.15
            val sample = ((noise + baseTone) * envelope * 32767 * volumeScale).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Sweeping cyber-blade attack slash sound.
     */
    fun playAttack() {
        playProceduralSound(220) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat()) * (1.0f - t)
            val freq = 1900.0 - 1500.0 * t // Linear downward sweep
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val tone = sin(phase)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.08f * (1.0f - t)
            val sample = ((tone + noise) * envelope * 0.65f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Devastating high-voltage critical backstab sound.
     */
    fun playBackstab() {
        playProceduralSound(380) { i, total ->
            val t = i.toFloat() / total
            val envelope = 1.0f - t
            val freq = 3200.0 - 2800.0 * t
            val vibrato = 1.0 + 0.2 * sin(2.0 * PI * 35.0 * t) // cyber phase vibrato
            val phase = 2.0 * PI * (freq * vibrato) * (i.toDouble() / 22050.0)
            val tone = sin(phase)
            val noise = (Random.nextFloat() * 2f - 1f) * 0.3f * (1.0f - t) // electrical burn noise
            val sample = ((tone + noise) * envelope * 0.75f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Sentry automatic laser blaster shot sound.
     */
    fun playLaser() {
        playProceduralSound(130) { i, total ->
            val t = i.toFloat() / total
            val envelope = 1.0f - t
            val freq = 2400.0 - 1800.0 * t // fast chirp sweep
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            // Square wave generator for a raspy laser grid sound
            val wave = if (sin(phase) > 0) 1.0 else -1.0
            val sample = (wave * envelope * 0.30f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Musical ascending/descending chime for ascending or descending vertical shaft ladders.
     */
    fun playLadder(ascending: Boolean) {
        playProceduralSound(320) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val notes = if (ascending) {
                intArrayOf(440, 554, 659, 880) // A Major Upward Chimes
            } else {
                intArrayOf(880, 659, 554, 440) // Downward Chimes
            }
            val noteIndex = (t * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex].toDouble()
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val sample = (sin(phase) * envelope * 0.40f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Cool rising or falling resonance sweep when active cloak (Stealth mode) is toggled.
     */
    fun playStealthToggle(isActive: Boolean) {
        playProceduralSound(280) { i, total ->
            val t = i.toFloat() / total
            val envelope = sin(t * PI.toFloat())
            val freq = if (isActive) {
                250.0 + 850.0 * t // Sci-fi fade in / engage
            } else {
                1100.0 - 850.0 * t // Fade out / disengage
            }
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val modulation = 0.08 * sin(2.0 * PI * 18.0 * t) // holographic phase modulation
            val wave = sin(phase + modulation)
            val sample = (wave * envelope * 0.42f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Interaction sound for terminals, chest/barrel hacking, or unlocking door locks.
     */
    fun playInteract() {
        playProceduralSound(180) { i, total ->
            val t = i.toFloat() / total
            val envelope = if (t < 0.5f) (1.0f - t * 2) else (1.0f - (t - 0.5f) * 2)
            val freq = if (t < 0.5f) 980.0 else 1470.0 // clean mechanical digital fourth
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val sample = (sin(phase) * envelope * 0.35f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Majestic, triumphant upward progression scale for leveling up or clearing levels.
     */
    fun playLevelUp() {
        playProceduralSound(550) { i, total ->
            val t = i.toFloat() / total
            val envelope = if (t < 0.15f) (t / 0.15f) else (1.0f - t)
            val notes = intArrayOf(523, 659, 784, 1047, 1318) // Pentatonic upward
            val noteIndex = (t * notes.size).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex].toDouble()
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val sample = (sin(phase) * envelope * 0.48f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Alert alarm (beep beep) when security drones detect player or hazard is tripped.
     */
    fun playAlert() {
        playProceduralSound(380) { i, total ->
            val t = i.toFloat() / total
            val pulse = if ((t * 8).toInt() % 2 == 0) 1.0f else 0.0f
            val freq = 1150.0
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val sample = (sin(phase) * pulse * 0.40f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }

    /**
     * Metallic coin slot-machine chime when picking up carbon/credits.
     */
    fun playCreditLoot() {
        playProceduralSound(160) { i, total ->
            val t = i.toFloat() / total
            val envelope = (1.0f - t) * (1.0f - t)
            val freq = 1600.0 + 600.0 * sin(t * PI.toFloat())
            val phase = 2.0 * PI * freq * (i.toDouble() / 22050.0)
            val sample = (sin(phase) * envelope * 0.30f * 32767).toInt()
            sample.coerceIn(-32768, 32767).toShort()
        }
    }
}
