package com.example.render

import android.content.res.AssetManager
import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.SpriteAnimation
import kotlin.math.cos

/**
 * Runtime frame selection for the four supplied enemy sheets.
 * The simulation remains authoritative; this class only translates state to art.
 */
object EnemySpriteAtlas {
    const val columns = 6
    const val rows = 6
    const val cellWidth = 224
    const val cellHeight = 176

    private val idle = intArrayOf(0, 1, 2, 3)
    private val walk = intArrayOf(4, 5, 6, 7)

    private val profilePaths = mapOf(
        "heavy_elite" to "sprites/enemies/heavy_elite_atlas.webp",
        "sewer_mutant" to "sprites/enemies/sewer_mutant_atlas.webp",
        "stalker_beast" to "sprites/enemies/stalker_beast_atlas.webp",
        "hooded_operator" to "sprites/enemies/hooded_operator_atlas.webp"
    )

    private val attack1 = intArrayOf(18, 19, 20)
    private val attack2ByProfile = mapOf(
        "heavy_elite" to intArrayOf(21, 22, 23, 24),
        "sewer_mutant" to intArrayOf(21, 22, 23),
        "stalker_beast" to intArrayOf(21, 22, 23),
        "hooded_operator" to intArrayOf(21, 22, 23)
    )

    fun profileIds(): Set<String> = profilePaths.keys

    fun profileIdFor(enemy: Enemy): String = EnemyVisualVariantResolver.resolve(enemy)

    fun assetPathFor(profileId: String): String? = profilePaths[profileId]

    fun areAssetsAvailable(assets: AssetManager): Boolean {
        return profilePaths.values.all { path ->
            runCatching { assets.open(path).use { } }.isSuccess
        }
    }

    fun animationFor(enemy: Enemy): SpriteAnimation {
        return when {
            enemy.isDead -> SpriteAnimation.DEATH
            enemy.attackCooldown > 0 -> SpriteAnimation.CONTEXT_ACTION
            enemy.isSearching -> SpriteAnimation.CONTEXT_ACTION
            enemy.currentPath.isNotEmpty() && enemy.pathIndex < enemy.currentPath.lastIndex -> SpriteAnimation.WALK
            enemy.alertState == AlertState.SUSPICIOUS -> SpriteAnimation.WALK
            else -> SpriteAnimation.IDLE
        }
    }

    fun frameFor(enemy: Enemy, animationClock: Float): Int {
        val profile = profileIdFor(enemy)
        val animation = animationFor(enemy)
        val clip = when (animation) {
            SpriteAnimation.DEATH -> intArrayOf(frameCount(profile) - 2)
            SpriteAnimation.FALL -> intArrayOf(frameCount(profile) - 1)
            SpriteAnimation.HURT -> intArrayOf(frameCount(profile) - 3)
            SpriteAnimation.WALK -> walk
            SpriteAnimation.CONTEXT_ACTION -> if (enemy.attackCooldown > 0) {
                attack1
            } else {
                contextClip(profile)
            }
            SpriteAnimation.IDLE -> idle
            else -> idle
        }
        return clip[(animationClock * 8f).toInt().mod(clip.size)]
    }

    fun flipX(enemy: Enemy): Boolean = cos(enemy.directionAngle) < 0f

    fun widthFor(enemy: Enemy): Float {
        return when (profileIdFor(enemy)) {
            "stalker_beast" -> 150f
            "sewer_mutant" -> 120f
            "hooded_operator" -> 108f
            else -> 116f
        }
    }

    fun heightFor(enemy: Enemy): Float {
        return when (profileIdFor(enemy)) {
            "stalker_beast" -> 138f
            else -> 148f
        }
    }

    private fun frameCount(profile: String): Int = when (profile) {
        "heavy_elite", "hooded_operator" -> 34
        "sewer_mutant" -> 33
        "stalker_beast" -> 32
        else -> 0
    }

    private fun contextClip(profile: String): IntArray {
        val first = when (profile) {
            "heavy_elite" -> 25
            "sewer_mutant", "stalker_beast" -> 24
            "hooded_operator" -> 24
            else -> 0
        }
        val last = frameCount(profile) - 4
        return if (last >= first) IntArray(last - first + 1) { first + it } else idle
    }
}
