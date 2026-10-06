package com.example.model

/**
 * Presentation-only animation capabilities for a character sprite profile.
 *
 * The gameplay state machine remains authoritative; these values describe only
 * what the renderer may ask a visual profile to display.
 */
enum class SpriteAnimation {
    IDLE,
    WALK,
    CONTEXT_ACTION,
    GAZE_CHAT,
    GESTURE,
    VENDOR,
    INTERACTION_LOOP,
    HURT,
    DEATH,
    FALL
}

enum class VisualProfileRole {
    CIVILIAN,
    VENDOR,
    ENEMY,
    FALLBACK
}

data class VisualAssetProfile(
    val id: String,
    val role: VisualProfileRole,
    val animations: Set<SpriteAnimation>,
    val sourceSheetId: String? = null
) {
    fun supports(animation: SpriteAnimation): Boolean = animation in animations
}

/**
 * Central registry for legacy/reference sprite profiles.
 *
 * This registry intentionally stores metadata only. It does not encode AI,
 * faction, dialogue, economy, or combat rules, and it does not require the
 * corresponding image asset to be present.
 */
object VisualAssetCatalog {
    const val FALLBACK_PROFILE_ID = "fallback_placeholder"

    val profiles: List<VisualAssetProfile> = listOf(
        VisualAssetProfile(
            id = "civilian_01",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_02",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_03",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_04",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_05",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_06",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "civilian_07",
            role = VisualProfileRole.CIVILIAN,
            sourceSheetId = "pedestrian_civilian",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.GAZE_CHAT,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "vendor_01",
            role = VisualProfileRole.VENDOR,
            sourceSheetId = "pedestrian_vendor",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.VENDOR,
                SpriteAnimation.INTERACTION_LOOP,
                SpriteAnimation.GESTURE,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "heavy_elite",
            role = VisualProfileRole.ENEMY,
            sourceSheetId = "legacy_enemy_heavy_elite",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "sewer_mutant",
            role = VisualProfileRole.ENEMY,
            sourceSheetId = "legacy_enemy_sewer_mutant",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "stalker_beast",
            role = VisualProfileRole.ENEMY,
            sourceSheetId = "legacy_enemy_stalker_beast",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = "hooded_operator",
            role = VisualProfileRole.ENEMY,
            sourceSheetId = "legacy_enemy_hooded_operator",
            animations = setOf(
                SpriteAnimation.IDLE,
                SpriteAnimation.WALK,
                SpriteAnimation.CONTEXT_ACTION,
                SpriteAnimation.HURT,
                SpriteAnimation.DEATH,
                SpriteAnimation.FALL
            )
        ),
        VisualAssetProfile(
            id = FALLBACK_PROFILE_ID,
            role = VisualProfileRole.FALLBACK,
            animations = setOf(SpriteAnimation.IDLE),
            sourceSheetId = null
        )
    )

    private val byId: Map<String, VisualAssetProfile> = profiles.associateBy(VisualAssetProfile::id)

    val civilianProfiles: List<VisualAssetProfile>
        get() = profiles.filter { it.role == VisualProfileRole.CIVILIAN }

    val vendorProfiles: List<VisualAssetProfile>
        get() = profiles.filter { it.role == VisualProfileRole.VENDOR }

    val enemyProfiles: List<VisualAssetProfile>
        get() = profiles.filter { it.role == VisualProfileRole.ENEMY }

    fun find(id: String): VisualAssetProfile? = byId[id]

    fun resolve(id: String?): VisualAssetProfile {
        return id?.let(byId::get) ?: byId.getValue(FALLBACK_PROFILE_ID)
    }
}
