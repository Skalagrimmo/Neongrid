package com.example.render

import com.example.model.Enemy

/** Presentation-only mapping from the gameplay enemy type to a reusable visual profile. */
object EnemyVisualVariantResolver {
    fun resolve(enemy: Enemy): String = when {
        enemy.type.equals("Boss", ignoreCase = true) -> "heavy_elite"
        enemy.type.equals("SentryDrone", ignoreCase = true) -> "stalker_beast"
        enemy.type.equals("Sentry", ignoreCase = true) -> "hooded_operator"
        enemy.type.equals("Syntrob", ignoreCase = true) -> "heavy_elite"
        enemy.type.equals("Mutant", ignoreCase = true) -> "sewer_mutant"
        else -> "heavy_elite"
    }
}

data class EnemySpritePresentation(
    val variantId: String,
    val fallbackRadius: Float,
    val runScale: Float
)

object EnemySpritePresentationResolver {
    fun resolve(enemy: Enemy): EnemySpritePresentation {
        return when (val variant = EnemyVisualVariantResolver.resolve(enemy)) {
            "sewer_mutant" -> EnemySpritePresentation(variant, 14f, 1.05f)
            "stalker_beast" -> EnemySpritePresentation(variant, 11f, 1.20f)
            "hooded_operator" -> EnemySpritePresentation(variant, 12f, 1.10f)
            else -> EnemySpritePresentation(variant, if (enemy.type == "Boss") 20f else 13f, 1.0f)
        }
    }
}