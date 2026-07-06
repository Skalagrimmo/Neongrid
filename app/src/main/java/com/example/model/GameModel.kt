package com.example.model

import androidx.compose.ui.graphics.Color

// Smooth floating point position in isometric space
data class Point3D(
    var x: Float,
    var y: Float,
    var z: Float // Z level: 0 (Sewer), 1 (Main), 2 (Upper), 3 (Upper Roof)
) {
    fun distanceTo(other: Point3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = (z - other.z) * 2.0f // Scale Z difference slightly for isometric verticality
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}

// Grid-aligned cell coordinate
data class GridPos(val x: Int, val y: Int, val z: Int)

// Game Equipment Categories
enum class EquipmentType {
    WEAPON,   // Nano-Blade, Plasma Carbine, Monofilament Whip
    CORE,     // Ghost Cloak, Force Shield, Overclocker
    SYSTEM    // Dash Boosters, Quiet Soles, Targeting Chip
}

// Represent an equippable weapon/gear
data class EquipmentItem(
    val id: String,
    val name: String,
    val type: EquipmentType,
    val description: String,
    val statBoostHealth: Float = 0f,
    val statBoostEnergy: Float = 0f,
    val statBoostDamage: Float = 0f,
    val statBoostSpeed: Float = 0f,
    val statBoostStealth: Float = 0f, // Reduces enemy vision fill rate
    val energyCost: Float = 0f,
    val costCredits: Int = 100,
    val color: Long = 0xFF00FF00 // Hex Color representation
) {
    companion object {
        val DEFAULT_WEAPON = EquipmentItem(
            id = "nano_blade",
            name = "Mono Nano-Blade",
            type = EquipmentType.WEAPON,
            description = "High-frequency slicing blade. Silent and deadly.",
            statBoostDamage = 35f,
            statBoostSpeed = 0.5f,
            statBoostStealth = 20f,
            costCredits = 0,
            color = 0xFF00F0FF // Cyan
        )
        val DEFAULT_CORE = EquipmentItem(
            id = "force_shield",
            name = "Aegis Core V1",
            type = EquipmentType.CORE,
            description = "Deploys a kinetic barrier protecting the host.",
            statBoostHealth = 50f,
            statBoostEnergy = 10f,
            costCredits = 0,
            color = 0xFF00FF66 // Green
        )
        val DEFAULT_SYSTEM = EquipmentItem(
            id = "targeting_chip",
            name = "Apex System Link",
            type = EquipmentType.SYSTEM,
            description = "Direct brain interface that enhances motor reflexes.",
            statBoostSpeed = 0.2f,
            costCredits = 0,
            color = 0xFFFF9900 // Orange
        )

        val ALL_ITEMS = listOf(
            DEFAULT_WEAPON,
            DEFAULT_CORE,
            DEFAULT_SYSTEM,
            // Weapons
            EquipmentItem(
                id = "plasma_carbine",
                name = "Heavy Plasma Carbine",
                type = EquipmentType.WEAPON,
                description = "Rapid fire plasma rifle. Loud, but extremely high damage output.",
                statBoostDamage = 60f,
                statBoostSpeed = -0.3f,
                statBoostStealth = -15f,
                costCredits = 300,
                color = 0xFFFF3366 // Bright Red-Pink
            ),
            EquipmentItem(
                id = "neuro_whip",
                name = "Neuro-Whip",
                type = EquipmentType.WEAPON,
                description = "Discharges EMP arcs. High range melee and short-circuits targets.",
                statBoostDamage = 40f,
                statBoostEnergy = 20f,
                costCredits = 450,
                color = 0xFFBD00FF // Purple
            ),
            // Cores
            EquipmentItem(
                id = "ghost_cloak",
                name = "Ghost Cloak Module",
                type = EquipmentType.CORE,
                description = "Reduces detection rate by 60% and increases critical strike chance.",
                statBoostStealth = 60f,
                statBoostSpeed = 0.2f,
                costCredits = 400,
                color = 0xFF00F0FF // Cyan
            ),
            EquipmentItem(
                id = "overclock_core",
                name = "Overclock Injector",
                type = EquipmentType.CORE,
                description = "Increases damage and speed, but drains energy continuously during combat.",
                statBoostDamage = 25f,
                statBoostSpeed = 0.8f,
                statBoostEnergy = -15f,
                costCredits = 500,
                color = 0xFFFFCC00 // Amber
            ),
            // Systems
            EquipmentItem(
                id = "quiet_soles",
                name = "Quiet Soles Dampener",
                type = EquipmentType.SYSTEM,
                description = "Absorbs vibration when running, making sound waves 75% smaller.",
                statBoostStealth = 35f,
                statBoostSpeed = 0.1f,
                costCredits = 250,
                color = 0xFF00FFCC // Teal
            ),
            EquipmentItem(
                id = "dash_boosters",
                name = "Hydra Dash Boosters",
                type = EquipmentType.SYSTEM,
                description = "Pneumatic implants that increase dash speed and recovery rate.",
                statBoostSpeed = 0.6f,
                statBoostEnergy = 15f,
                costCredits = 350,
                color = 0xFFFF0055 // Rose
            )
        )
    }
}

// Multi-class Skill Tree Node
enum class CharacterClass {
    CYBER_RONIN,       // Melee, Criticals, Agility
    TECH_NECROMANCER,  // Terminals, Barrels, Traps
    GHOST_INFILTRATOR  // Invisible, Soundless, Backstabs
}

data class SkillNode(
    val id: String,
    val name: String,
    val characterClass: CharacterClass,
    val description: String,
    val costPoints: Int = 1,
    val isUnlocked: Boolean = false,
    val parents: List<String> = emptyList(), // Pre-requisite node IDs
    val statModifier: (Player) -> Unit = {} // Hook to modify player stats
) {
    companion object {
        fun getSkillTree(): List<SkillNode> {
            return listOf(
                // --- CYBER-RONIN TREE ---
                SkillNode(
                    id = "ronin_base",
                    name = "Ronin Edge",
                    characterClass = CharacterClass.CYBER_RONIN,
                    description = "Increases melee damage by +10%. Unlock further blade arts.",
                    parents = emptyList()
                ),
                SkillNode(
                    id = "ronin_speed",
                    name = "Kaze Swiftness",
                    characterClass = CharacterClass.CYBER_RONIN,
                    description = "Passive speed increased by +15%.",
                    parents = listOf("ronin_base")
                ),
                SkillNode(
                    id = "ronin_crit",
                    name = "Zanshin Strike",
                    characterClass = CharacterClass.CYBER_RONIN,
                    description = "Unlocks Ronin Dash attack: deals triple damage if striking from behind.",
                    parents = listOf("ronin_base")
                ),
                SkillNode(
                    id = "ronin_ultimate",
                    name = "Blade Tempest",
                    characterClass = CharacterClass.CYBER_RONIN,
                    description = "Releases a deadly sweep that damages all surrounding targets in Z-range.",
                    parents = listOf("ronin_speed", "ronin_crit")
                ),

                // --- TECH-NECROMANCER TREE ---
                SkillNode(
                    id = "tech_base",
                    name = "Proxy Ingress",
                    characterClass = CharacterClass.TECH_NECROMANCER,
                    description = "Allows remote hacking of security nodes and explosive barrels from +2 range.",
                    parents = emptyList()
                ),
                SkillNode(
                    id = "tech_shrapnel",
                    name = "Overload Trigger",
                    characterClass = CharacterClass.TECH_NECROMANCER,
                    description = "Exploded plasma barrels deal +50% damage and disable lasers for 8s.",
                    parents = listOf("tech_base")
                ),
                SkillNode(
                    id = "tech_drone",
                    name = "Sentry Drone Buddy",
                    characterClass = CharacterClass.TECH_NECROMANCER,
                    description = "Summons a small automated flying drone to zap and shock near-by targets.",
                    parents = listOf("tech_base")
                ),
                SkillNode(
                    id = "tech_ultimate",
                    name = "System Reboot EMP",
                    characterClass = CharacterClass.TECH_NECROMANCER,
                    description = "Triggers a full EMP field disabling all cameras and lasers on the level.",
                    parents = listOf("tech_shrapnel", "tech_drone")
                ),

                // --- GHOST-INFILTRATOR TREE ---
                SkillNode(
                    id = "ghost_base",
                    name = "Shadow Dampener",
                    characterClass = CharacterClass.GHOST_INFILTRATOR,
                    description = "Sound waves produced by walking are cut in half. Sneak speed +20%.",
                    parents = emptyList()
                ),
                SkillNode(
                    id = "ghost_smoke",
                    name = "Chaff Discharger",
                    characterClass = CharacterClass.GHOST_INFILTRATOR,
                    description = "Deploys an instant smokescreen, resetting nearby alert meters and blinding sentries.",
                    parents = listOf("ghost_base")
                ),
                SkillNode(
                    id = "ghost_backstab",
                    name = "Assassinate",
                    characterClass = CharacterClass.GHOST_INFILTRATOR,
                    description = "Sneak attack multiplier increased from 2x to 5x damage.",
                    parents = listOf("ghost_base")
                ),
                SkillNode(
                    id = "ghost_ultimate",
                    name = "Phantom Matrix",
                    characterClass = CharacterClass.GHOST_INFILTRATOR,
                    description = "Active invisibility for 10s. Energy costs are reduced by 40%.",
                    parents = listOf("ghost_smoke", "ghost_backstab")
                )
            )
        }
    }
}

// Tile Types in our Isometric Map
enum class TileType(val symbol: String, val color: Long, val isWalkable: Boolean = true) {
    EMPTY("Empty Space", 0xFF0E1317, false),
    FLOOR("Main Cyberpath", 0xFF1B2329, true),
    GRID_ROAD("Main Tech-Road", 0xFF142C33, true), // Cyan glowing roads
    WALL("Structure Grid", 0xFF1C2224, false),
    LADDER_UP("Ascending Shaft", 0xFFE65C00, true), // Goes up a level
    LADDER_DOWN("Descending Shaft", 0xFFB34700, true), // Goes down a level
    BARREL_EXPLOSIVE("Plasma Storage", 0xFFFF3330, false), // Hackable explosion
    TERMINAL("Console Uplink", 0xFF00FFCC, false), // Hacking interface
    LASER_GRID("Active Laser Gate", 0xFFFF0055, false), // Drains health/sounds alarm
    SECURITY_CAMERA("Sentry Lens", 0xFF00F0FF, false), // Slowly scans and alerts
    EXIT_PORTAL("Uplink Receiver", 0xFF00FF66, true) // Goal of the run
}

// Single Level Grid Map
class GameLevelMap(
    val zLevel: Int,
    val name: String,
    val width: Int,
    val height: Int,
    val grid: Array<Array<TileType>>
) {
    fun getTile(x: Int, y: Int): TileType {
        if (x !in 0 until width || y !in 0 until height) return TileType.EMPTY
        return grid[x][y]
    }
    fun setTile(x: Int, y: Int, tile: TileType) {
        if (x in 0 until width && y in 0 until height) {
            grid[x][y] = tile
        }
    }
}

// Active Game States
enum class GamePlayState {
    STEALTH_RT, // Commandos style, enemies are unaware, sneaky tactics
    COMBAT_RT   // Combat engagement, alerted enemies
}

// Enemy Alert States
enum class AlertState {
    PATROLLING,  // Vision cone is Green
    SUSPICIOUS,  // Vision cone is Orange, moves to investigation point
    ALERTED      // Vision cone is Red, attacks player
}

// Player state model
data class Player(
    var pos: Point3D = Point3D(2f, 2f, 1f), // Starts on Z=1 (Main Street)
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var energy: Float = 80f,
    var maxEnergy: Float = 80f,
    var xp: Int = 0,
    var level: Int = 1,
    var skillPoints: Int = 2,
    var credits: Int = 120,
    var isSneaking: Boolean = false,
    var isInvisible: Boolean = false,
    var invisibleTimer: Float = 0f, // counts down
    var equippedWeapon: EquipmentItem = EquipmentItem.DEFAULT_WEAPON,
    var equippedCore: EquipmentItem = EquipmentItem.DEFAULT_CORE,
    var equippedSystem: EquipmentItem = EquipmentItem.DEFAULT_SYSTEM,
    var unlockedSkills: Set<String> = setOf("ronin_base")
) {
    fun getSpeed(): Float {
        val base = if (isSneaking) 0.08f else 0.16f
        val boost = equippedWeapon.statBoostSpeed + equippedCore.statBoostSpeed + equippedSystem.statBoostSpeed
        return (base + boost * 0.05f).coerceIn(0.04f, 0.25f)
    }

    fun getStealthFactor(): Float {
        val base = if (isSneaking) 0.4f else 1.0f
        val boost = equippedWeapon.statBoostStealth + equippedCore.statBoostStealth + equippedSystem.statBoostStealth
        // boost reduces visibility. E.g. 50 boost means half visibility
        val multiplier = (1f - boost / 100f).coerceIn(0.1f, 1.5f)
        return if (isInvisible) 0.0f else base * multiplier
    }

    fun getDamage(): Float {
        val base = 20f
        val boost = equippedWeapon.statBoostDamage + equippedCore.statBoostDamage + equippedSystem.statBoostDamage
        return base + boost
    }
}

// AI Enemy state model
data class Enemy(
    val id: String,
    val name: String,
    var pos: Point3D,
    val type: String = "Syntrob", // "Sentry", "Syntrob", "SentryDrone"
    var health: Float = 50f,
    val maxHealth: Float = 50f,
    var directionAngle: Float = 0f, // Radians, pointing where they look
    val patrolRoute: List<Point3D> = emptyList(),
    var patrolIndex: Int = 0,
    var alertState: AlertState = AlertState.PATROLLING,
    var alertMeter: Float = 0f, // 0 to 100. At 100, triggers ALERTED
    var lastKnownPlayerPos: Point3D? = null,
    var suspicionTimer: Float = 0f, // Investigations countdown
    var attackCooldown: Int = 0,
    var isDead: Boolean = false
) {
    fun getVisionConeAngle(): Float = when (alertState) {
        AlertState.PATROLLING -> 1.05f // ~60 degrees
        AlertState.SUSPICIOUS -> 1.4f // ~80 degrees
        AlertState.ALERTED -> 1.8f // ~100 degrees
    }

    fun getVisionRange(): Float = when (alertState) {
        AlertState.PATROLLING -> 5.5f
        AlertState.SUSPICIOUS -> 7.0f
        AlertState.ALERTED -> 8.5f
    }
}

// Noise ripple generated in the grid
data class NoiseRipple(
    val pos: Point3D,
    var radius: Float,
    val maxRadius: Float,
    val speed: Float = 0.2f
)
