package com.example.ui

/**
 * Backward-compatible delegation wrapper to [SoundManager].
 */
object AudioManager {
    var isMuted: Boolean
        get() = SoundManager.isMuted
        set(value) { SoundManager.isMuted = value }

    fun playFootstep(sneaking: Boolean) = SoundManager.playFootstep(sneaking)
    fun playAttack() = SoundManager.playAttack()
    fun playBackstab() = SoundManager.playBackstab()
    fun playLaser() = SoundManager.playLaser()
    fun playLadder(ascending: Boolean) = SoundManager.playLadder(ascending)
    fun playStealthToggle(isActive: Boolean) = SoundManager.playStealthToggle(isActive)
    fun playInteract() = SoundManager.playInteract()
    fun playLevelUp() = SoundManager.playLevelUp()
    fun playAlert() = SoundManager.playStealthDetectionAlert(1.0f)
    fun playCreditLoot() = SoundManager.playCreditLoot()
}

