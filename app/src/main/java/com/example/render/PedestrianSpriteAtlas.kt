package com.example.render

import com.example.model.CivilianActivity
import com.example.model.CivilianNpc

object PedestrianSpriteAtlas {
    const val columns = 8
    const val rows = 4
    const val cellWidth = 64
    const val cellHeight = 88
    const val spriteScale = 1.0f

    private val idle = intArrayOf(0, 1, 2, 3)
    private val walk = intArrayOf(9, 10, 11, 12)
    private val chat = intArrayOf(18, 19)
    private val gesture = intArrayOf(20, 21)

    fun frameFor(npc: CivilianNpc): Int {
        val variant = variantIndex(npc.visualVariantId)
        return when (npc.activity) {
            CivilianActivity.WALKING ->
                walk[(npc.animationTime * 8f).toInt().mod(walk.size)]
            CivilianActivity.SOCIALIZE ->
                chat[(npc.animationTime * 2f).toInt().mod(chat.size)]
            CivilianActivity.GESTURE ->
                gesture[(npc.animationTime * 2f).toInt().mod(gesture.size)]
            CivilianActivity.HURT -> 22
            CivilianActivity.DEAD -> 23
            CivilianActivity.DOWNED -> 24
            CivilianActivity.VENDOR -> 25
            CivilianActivity.IDLE -> idle[variant % idle.size]
        }
    }

    private fun variantIndex(id: String): Int {
        val suffix = id.substringAfterLast('_', "0").toIntOrNull() ?: 0
        return suffix.coerceAtLeast(0)
    }
}
