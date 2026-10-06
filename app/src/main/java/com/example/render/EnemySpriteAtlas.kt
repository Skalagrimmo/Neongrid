package com.example.render

import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.SpriteAnimation

/**
 * Presentation contract for the four legacy enemy sheets.
 *
 * Frame numbers remain unset until the original source sheets are imported;
 * gameplay therefore stays renderable through the existing primitive fallback.
 */
object EnemySpriteAtlas {
    const val DEFAULT_COLUMNS = 8
    const val DEFAULT_ROWS = 8

    fun animationFor(enemy: Enemy): SpriteAnimation {
        return when {
            enemy.isDead -> SpriteAnimation.DEATH
            enemy.alertState == AlertState.ALERTED -> SpriteAnimation.CONTEXT_ACTION
            enemy.alertState == AlertState.SUSPICIOUS -> SpriteAnimation.WALK
            else -> SpriteAnimation.IDLE
        }
    }

    fun profileIdFor(enemy: Enemy): String = EnemyVisualVariantResolver.resolve(enemy)

    /** No binary atlas is committed until the source sheets are available. */
    fun isProductionReady(): Boolean = false
}