package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.PlayerSaveState
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GameDatabase.getDatabase(application)
    private val repository = GameRepository(db.saveStateDao())

    // Game Screens State
    enum class Screen {
        MENU,
        PLAY,
        SKILL_TREE,
        LOADOUT,
        CONTROLS
    }

    var currentScreen by mutableStateOf(Screen.MENU)
        private set

    // Active Game Entities & State
    var player by mutableStateOf(Player())
        private set

    var enemies = mutableListOf<Enemy>()
        private set

    var noiseRipples = mutableListOf<NoiseRipple>()
        private set

    // Levels Data Structure
    var gameLevels = mutableMapOf<Int, GameLevelMap>()
        private set

    var currentZLevel by mutableStateOf(1) // Starts at Z=1 (Main Street)
        private set

    var currentScore by mutableStateOf(0)
        private set

    var isGameOver by mutableStateOf(false)
        private set

    var isGameWon by mutableStateOf(false)
        private set

    var isHackingActive by mutableStateOf(false)
        private set

    var hackTerminalPos by mutableStateOf<GridPos?>(null)
        private set

    var hackProgress by mutableStateOf(0f)
        private set

    // Debug Dev Console Logs (Matching the visual style in the image)
    var consoleLogs = mutableListOf<String>()
        private set

    // Dynamic Skill Trees (Can be unlocked using skill points)
    var skillNodes by mutableStateOf(SkillNode.getSkillTree())
        private set

    // Active Combat Orbs (Ranged shots)
    var activeProjectiles = mutableListOf<Pair<Point3D, Point3D>>() // Pair of <Position, Velocity>
    private set

    // Game loop tick state for optimized Jetpack Compose recomposition
    var gameTick by mutableStateOf(0L)
        private set

    // Game Loop Job
    private var gameLoopJob: Job? = null

    init {
        generateLevels()
        resetGameEntities()
        observeSaveState()
        logToConsole("DEV-CONSOLE INITIALIZED")
        logToConsole("LEVEL 1 SECTOR_GIBSON LOADED")
    }

    fun changeScreen(screen: Screen) {
        currentScreen = screen
        if (screen == Screen.PLAY && gameLoopJob == null) {
            startGameLoop()
        } else if (screen != Screen.PLAY) {
            stopGameLoop()
        }
    }

    private fun observeSaveState() {
        viewModelScope.launch {
            repository.saveState.collect { save ->
                save?.let {
                    // Map save state into our memory objects
                    val unlockedSet = it.unlockedSkillIdsString.split(",").filter { id -> id.isNotEmpty() }.toSet()
                    val equippedWeapon = EquipmentItem.ALL_ITEMS.find { item -> item.id == it.equippedWeaponId } ?: EquipmentItem.DEFAULT_WEAPON
                    val equippedCore = EquipmentItem.ALL_ITEMS.find { item -> item.id == it.equippedCoreId } ?: EquipmentItem.DEFAULT_CORE
                    val equippedSystem = EquipmentItem.ALL_ITEMS.find { item -> item.id == it.equippedSystemId } ?: EquipmentItem.DEFAULT_SYSTEM

                    player = player.copy(
                        level = it.level,
                        xp = it.xp,
                        skillPoints = it.skillPoints,
                        credits = it.credits,
                        equippedWeapon = equippedWeapon,
                        equippedCore = equippedCore,
                        equippedSystem = equippedSystem,
                        unlockedSkills = unlockedSet
                    )

                    // Mark skills as unlocked in the tree
                    skillNodes = skillNodes.map { node ->
                        node.copy(isUnlocked = unlockedSet.contains(node.id))
                    }

                    logToConsole("SAVE STATE RESTORED: CLVL ${it.level}")
                }
            }
        }
    }

    fun saveGameProgress() {
        viewModelScope.launch {
            val save = PlayerSaveState(
                level = player.level,
                xp = player.xp,
                skillPoints = player.skillPoints,
                credits = player.credits,
                equippedWeaponId = player.equippedWeapon.id,
                equippedCoreId = player.equippedCore.id,
                equippedSystemId = player.equippedSystem.id,
                unlockedSkillIdsString = player.unlockedSkills.joinToString(","),
                highScore = max(currentScore, player.credits),
                highestZLevelCleared = max(currentZLevel, 1)
            )
            repository.saveGame(save)
            logToConsole("SYNCING WITH LOCAL SQLITE DB: DONE")
        }
    }

    fun resetSaveData() {
        viewModelScope.launch {
            repository.clearSave()
            player = Player()
            skillNodes = SkillNode.getSkillTree()
            saveGameProgress()
            resetGameEntities()
            logToConsole("SQLITE DATABASE SAVES WIPED")
        }
    }

    private fun logToConsole(message: String) {
        if (consoleLogs.size > 8) {
            consoleLogs.removeAt(0)
        }
        consoleLogs.add(message)
    }

    // Reset All levels & dynamic entities
    fun resetGameEntities() {
        isGameOver = false
        isGameWon = false
        currentZLevel = 1
        currentScore = 0
        isHackingActive = false
        enemies.clear()
        noiseRipples.clear()
        activeProjectiles.clear()
        generateLevels()

        // Place Player
        player.pos = Point3D(2f, 2f, 1f)
        player.health = 100f + player.equippedCore.statBoostHealth
        player.maxHealth = 100f + player.equippedCore.statBoostHealth
        player.energy = 80f + player.equippedCore.statBoostEnergy
        player.maxEnergy = 80f + player.equippedCore.statBoostEnergy

        // Populate Enemies for Z=0, Z=1, Z=2, Z=3
        spawnEnemiesForLevel()
        logToConsole("ENTITIES REPOPULATED ON GRID")
    }

    private fun spawnEnemiesForLevel() {
        enemies.clear()

        // Z=0 (Sewer) Enemies: Drones, patrolling narrow paths
        enemies.add(
            Enemy(
                id = "drone_0", name = "Drone SENTROY_0",
                pos = Point3D(5f, 4f, 0f), type = "SentryDrone", health = 30f, maxHealth = 30f,
                patrolRoute = listOf(Point3D(5f, 4f, 0f), Point3D(5f, 12f, 0f), Point3D(12f, 12f, 0f), Point3D(12f, 4f, 0f))
            )
        )
        enemies.add(
            Enemy(
                id = "drone_1", name = "Drone SENTROY_1",
                pos = Point3D(14f, 6f, 0f), type = "SentryDrone", health = 30f, maxHealth = 30f,
                patrolRoute = listOf(Point3D(14f, 6f, 0f), Point3D(14f, 15f, 0f), Point3D(8f, 15f, 0f))
            )
        )

        // Z=1 (Main Street) Enemies: Sentry guards patrolling in blocks
        enemies.add(
            Enemy(
                id = "sentry_0", name = "Guardsman SYNTROB_0",
                pos = Point3D(8f, 5f, 1f), type = "Syntrob", health = 60f, maxHealth = 60f,
                patrolRoute = listOf(Point3D(8f, 5f, 1f), Point3D(15f, 5f, 1f), Point3D(15f, 10f, 1f), Point3D(8f, 10f, 1f))
            )
        )
        enemies.add(
            Enemy(
                id = "sentry_1", name = "Guardsman SYNTROB_1",
                pos = Point3D(5f, 14f, 1f), type = "Syntrob", health = 60f, maxHealth = 60f,
                patrolRoute = listOf(Point3D(5f, 14f, 1f), Point3D(12f, 14f, 1f), Point3D(12f, 8f, 1f))
            )
        )

        // Z=2 (Mezzanine) Enemies: Advanced sentries & watchers
        enemies.add(
            Enemy(
                id = "sentry_2", name = "Commando SYNTROB_2",
                pos = Point3D(4f, 8f, 2f), type = "Syntrob", health = 80f, maxHealth = 80f,
                patrolRoute = listOf(Point3D(4f, 8f, 2f), Point3D(4f, 15f, 2f), Point3D(14f, 15f, 2f), Point3D(14f, 8f, 2f))
            )
        )
        enemies.add(
            Enemy(
                id = "sentry_3", name = "Elite WATCHER_3",
                pos = Point3D(10f, 3f, 2f), type = "Sentry", health = 50f, maxHealth = 50f,
                patrolRoute = listOf(Point3D(10f, 3f, 2f), Point3D(16f, 3f, 2f), Point3D(16f, 7f, 2f))
            )
        )

        // Z=3 (Sky Gateway) Boss
        enemies.add(
            Enemy(
                id = "boss_heavy", name = "Mech SYNTROY_HEAVY",
                pos = Point3D(12f, 12f, 3f), type = "Boss", health = 250f, maxHealth = 250f,
                patrolRoute = listOf(Point3D(12f, 12f, 3f), Point3D(12f, 5f, 3f), Point3D(5f, 5f, 3f), Point3D(5f, 12f, 3f))
            )
        )
    }

    private fun generateLevels() {
        gameLevels.clear()
        val size = 20

        // Z=0 Sewer Grid (Flooded pathways, columns, ladders to Z=1)
        val sewerGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Large grid paths
                if (x == 2 || x == 5 || x == 12 || x == 14 || y == 4 || y == 8 || y == 12 || y == 15) {
                    sewerGrid[x][y] = TileType.FLOOR
                }
            }
        }
        sewerGrid[5][4] = TileType.LADDER_UP // Ascent to Main
        sewerGrid[12][12] = TileType.LADDER_UP
        sewerGrid[14][15] = TileType.LADDER_UP
        sewerGrid[3][4] = TileType.BARREL_EXPLOSIVE
        sewerGrid[11][12] = TileType.BARREL_EXPLOSIVE
        gameLevels[0] = GameLevelMap(0, "Z=0 SEWER", size, size, sewerGrid)

        // Z=1 Main Street Grid (Glowing roads, laser gates, terminals)
        val mainGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x in 1..18 && y in 1..18) {
                    mainGrid[x][y] = TileType.FLOOR
                }
                // Roads in center
                if (x == 10 || y == 10) {
                    mainGrid[x][y] = TileType.GRID_ROAD
                }
            }
        }
        // Add obstacles and walls
        for (i in 4..7) {
            mainGrid[i][6] = TileType.WALL
            mainGrid[13][i] = TileType.WALL
        }
        // Ladders down to Sewer and up to Mezzanine
        mainGrid[5][4] = TileType.LADDER_DOWN
        mainGrid[12][12] = TileType.LADDER_DOWN
        mainGrid[14][15] = TileType.LADDER_DOWN

        mainGrid[15][2] = TileType.LADDER_UP // Goes to Z=2
        mainGrid[3][16] = TileType.LADDER_UP

        // Laser grids & Terminals
        mainGrid[10][6] = TileType.LASER_GRID
        mainGrid[9][6] = TileType.TERMINAL // Hacking this disables the laser grid at 10,6
        mainGrid[14][10] = TileType.LASER_GRID
        mainGrid[14][9] = TileType.TERMINAL

        mainGrid[8][15] = TileType.BARREL_EXPLOSIVE
        mainGrid[15][15] = TileType.BARREL_EXPLOSIVE

        gameLevels[1] = GameLevelMap(1, "Z=1 MAIN STREET", size, size, mainGrid)

        // Z=2 Mezzanine (High structures, bridges)
        val mezGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                // Multiple platforms with narrow bridges
                if ((x in 2..7 && y in 2..8) || (x in 12..18 && y in 2..8) || (x in 2..18 && y in 12..18)) {
                    mezGrid[x][y] = TileType.FLOOR
                }
                // Bridges
                if (y == 5 && x in 8..11) {
                    mezGrid[x][y] = TileType.GRID_ROAD
                }
                if (x == 5 && y in 9..11) {
                    mezGrid[x][y] = TileType.GRID_ROAD
                }
                if (x == 15 && y in 9..11) {
                    mezGrid[x][y] = TileType.GRID_ROAD
                }
            }
        }
        // Ladders down
        mezGrid[15][2] = TileType.LADDER_DOWN
        mezGrid[3][16] = TileType.LADDER_DOWN
        // Ladder up to Z=3 Roof
        mezGrid[14][14] = TileType.LADDER_UP

        mezGrid[6][12] = TileType.LASER_GRID
        mezGrid[5][12] = TileType.TERMINAL
        mezGrid[4][4] = TileType.BARREL_EXPLOSIVE

        gameLevels[2] = GameLevelMap(2, "Z=2 MEZZANINE", size, size, mezGrid)

        // Z=3 Sky Gateway
        val skyGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x in 3..16 && y in 3..16) {
                    skyGrid[x][y] = TileType.FLOOR
                }
            }
        }
        skyGrid[14][14] = TileType.LADDER_DOWN
        // Exit Uplink Receiver
        skyGrid[10][10] = TileType.EXIT_PORTAL

        skyGrid[10][12] = TileType.LASER_GRID
        skyGrid[9][12] = TileType.TERMINAL

        skyGrid[13][5] = TileType.BARREL_EXPLOSIVE
        skyGrid[5][13] = TileType.BARREL_EXPLOSIVE

        gameLevels[3] = GameLevelMap(3, "Z=3 SKY GATEWAY", size, size, skyGrid)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch(Dispatchers.Main.immediate) {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                updateGameEntities(deltaTime)
                gameTick++ // Trigger Compose state read recomposition

                delay(16) // Target ~60 fps
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // Core Game Update Loop Logic
    private fun updateGameEntities(dt: Float) {
        if (isGameOver || isGameWon) return

        // 1. Process Active Projectiles
        val nextProj = mutableListOf<Pair<Point3D, Point3D>>()
        for (p in activeProjectiles) {
            val pos = p.first
            val vel = p.second
            val nextX = pos.x + vel.x * 20f * dt
            val nextY = pos.y + vel.y * 20f * dt

            val nextPos = Point3D(nextX, nextY, pos.z)
            // Check wall collision or bounds
            val map = gameLevels[currentZLevel] ?: continue
            val tile = map.getTile(nextX.toInt(), nextY.toInt())
            if (tile == TileType.WALL || tile == TileType.EMPTY) {
                // Hits a wall - detonate if close to explosive barrels or terminals
                detonateExplosionAt(nextPos)
                continue
            }

            // Check enemy collision
            var hit = false
            for (enemy in enemies) {
                if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel.toInt() && enemy.pos.distanceTo(nextPos) < 1.0f) {
                    // Deal damage
                    val dmg = player.getDamage()
                    enemy.health -= dmg
                    enemy.alertState = AlertState.ALERTED
                    enemy.lastKnownPlayerPos = player.pos.copy()
                    logToConsole("PROJECTILE HIT ${enemy.name}: -${dmg.toInt()}HP")
                    hit = true
                    if (enemy.health <= 0) {
                        enemy.isDead = true
                        player.xp += 30
                        player.credits += 40
                        checkPlayerLevelUp()
                        logToConsole("TARGET ELIMINATED: +40C +30XP")
                    }
                    break
                }
            }

            if (!hit) {
                nextProj.add(Pair(nextPos, vel))
            }
        }
        activeProjectiles = nextProj

        // 2. Process Noise Ripples
        val nextRipples = mutableListOf<NoiseRipple>()
        for (ripple in noiseRipples) {
            ripple.radius += ripple.speed * 10f * dt
            if (ripple.radius < ripple.maxRadius) {
                nextRipples.add(ripple)
            }
        }
        noiseRipples = nextRipples

        // 3. Process Invisibility Timer
        if (player.isInvisible) {
            player.invisibleTimer -= dt
            if (player.invisibleTimer <= 0) {
                player.isInvisible = false
                logToConsole("PHANTOM MATRIX CLOAK DEACTIVATED")
            }
        }

        // 4. Update Enemy Behaviors (Commandos-Style Patrolling & Alerts)
        for (enemy in enemies) {
            if (enemy.isDead) continue

            // Cooldowns
            if (enemy.attackCooldown > 0) enemy.attackCooldown--

            // Enemy must be on same Z level to see player
            if (enemy.pos.z.toInt() == currentZLevel) {
                val dist = enemy.pos.distanceTo(player.pos)
                val isPlayerVisible = checkPlayerVisibility(enemy, dist)

                if (isPlayerVisible) {
                    // Vision Alert Meter goes up
                    val rate = when (enemy.alertState) {
                        AlertState.PATROLLING -> 45f
                        AlertState.SUSPICIOUS -> 75f
                        AlertState.ALERTED -> 120f
                    }
                    // Stealth factor lowers the accumulation rate
                    val factor = player.getStealthFactor()
                    enemy.alertMeter = (enemy.alertMeter + rate * factor * dt).coerceAtMost(100f)

                    if (enemy.alertMeter >= 100f && enemy.alertState != AlertState.ALERTED) {
                        enemy.alertState = AlertState.ALERTED
                        logToConsole("ALERT! ${enemy.name} ENGAGED")
                    }
                    enemy.lastKnownPlayerPos = player.pos.copy()
                } else {
                    // Decay alert meter
                    if (enemy.alertState != AlertState.ALERTED) {
                        enemy.alertMeter = (enemy.alertMeter - 15f * dt).coerceAtLeast(0f)
                        if (enemy.alertMeter <= 0f && enemy.alertState == AlertState.SUSPICIOUS) {
                            enemy.alertState = AlertState.PATROLLING
                        }
                    } else {
                        // Alerted but lost player - counts down suspicion timer
                        enemy.suspicionTimer += dt
                        if (enemy.suspicionTimer > 5.0f) {
                            enemy.alertState = AlertState.SUSPICIOUS
                            enemy.alertMeter = 50f
                            enemy.suspicionTimer = 0f
                            logToConsole("${enemy.name} LOST TARGET. SEARCHING...")
                        }
                    }
                }

                // Noise detection
                for (ripple in noiseRipples) {
                    if (ripple.pos.z.toInt() == enemy.pos.z.toInt()) {
                        val dToRipple = enemy.pos.distanceTo(ripple.pos)
                        if (dToRipple <= ripple.radius && enemy.alertState != AlertState.ALERTED) {
                            enemy.alertState = AlertState.SUSPICIOUS
                            enemy.alertMeter = max(enemy.alertMeter, 40f)
                            enemy.lastKnownPlayerPos = ripple.pos.copy()
                            logToConsole("${enemy.name} HEARD SOUND RIPPLE")
                        }
                    }
                }

                // Combat attacking
                if (enemy.alertState == AlertState.ALERTED && dist < 1.5f && enemy.attackCooldown <= 0) {
                    // Attack Player!
                    val damage = 15f
                    player.health = (player.health - damage).coerceAtLeast(0f)
                    enemy.attackCooldown = 50 // frames
                    logToConsole("DAMAGE! MELEE HIT FROM ${enemy.name}: -15HP")

                    if (player.health <= 0f) {
                        isGameOver = true
                        stopGameLoop()
                        logToConsole("CRITICAL COLLAPSE: PLAYER DECEASED")
                    }
                } else if (enemy.alertState == AlertState.ALERTED && dist in 1.5f..6.5f && enemy.attackCooldown <= 0 && enemy.type == "SentryDrone") {
                    // Ranged blast from drone
                    player.health = (player.health - 10f).coerceAtLeast(0f)
                    enemy.attackCooldown = 75
                    logToConsole("DAMAGE! PULSE SHOT BY ${enemy.name}: -10HP")

                    if (player.health <= 0f) {
                        isGameOver = true
                        stopGameLoop()
                        logToConsole("CRITICAL COLLAPSE: PLAYER DECEASED")
                    }
                }
            }

            // Enemy Patrolling / Movement
            updateEnemyMovement(enemy, dt)
        }

        // 5. Update active hacking state if hacking
        if (isHackingActive) {
            hackProgress += dt * 0.35f
            if (hackProgress >= 1f) {
                isHackingActive = false
                hackProgress = 0f
                val hPos = hackTerminalPos
                if (hPos != null) {
                    // Disable laser grid nearby!
                    val map = gameLevels[currentZLevel]
                    if (map != null) {
                        // Look for adjacent Laser Grids and change to Floor
                        for (dx in -1..1) {
                            for (dy in -1..1) {
                                val tx = hPos.x + dx
                                val ty = hPos.y + dy
                                if (map.getTile(tx, ty) == TileType.LASER_GRID) {
                                    map.setTile(tx, ty, TileType.FLOOR)
                                    player.credits += 25
                                    currentScore += 100
                                    logToConsole("LASER GATE OFF: SYSTEM OVERRIDDEN")
                                    logToConsole("CREDITS COLLECTED: +25C")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPlayerVisibility(enemy: Enemy, dist: Float): Boolean {
        if (player.isInvisible) return false
        val visionRange = enemy.getVisionRange()
        if (dist > visionRange) return false

        // Compute angle to player
        val dx = player.pos.x - enemy.pos.x
        val dy = player.pos.y - enemy.pos.y
        val angleToPlayer = atan2(dy, dx)

        // Find angle difference
        var diff = abs(angleToPlayer - enemy.directionAngle)
        while (diff > PI) diff = (2 * PI - diff).toFloat()

        return diff <= enemy.getVisionConeAngle() / 2f
    }

    private fun updateEnemyMovement(enemy: Enemy, dt: Float) {
        val speed = when (enemy.alertState) {
            AlertState.PATROLLING -> 1.0f
            AlertState.SUSPICIOUS -> 1.8f
            AlertState.ALERTED -> 2.6f
        }

        val target = when (enemy.alertState) {
            AlertState.ALERTED -> {
                // Chase player if on same level
                if (player.pos.z.toInt() == enemy.pos.z.toInt() && !player.isInvisible) {
                    player.pos
                } else {
                    enemy.lastKnownPlayerPos ?: (enemy.patrolRoute.getOrNull(enemy.patrolIndex) ?: enemy.pos)
                }
            }
            AlertState.SUSPICIOUS -> {
                // Move to investigate sound or last seen pos
                enemy.lastKnownPlayerPos ?: (enemy.patrolRoute.getOrNull(enemy.patrolIndex) ?: enemy.pos)
            }
            AlertState.PATROLLING -> {
                if (enemy.patrolRoute.isNotEmpty()) {
                    enemy.patrolRoute[enemy.patrolIndex]
                } else {
                    enemy.pos
                }
            }
        }

        val dx = target.x - enemy.pos.x
        val dy = target.y - enemy.pos.y
        val d = sqrt(dx * dx + dy * dy)

        if (d > 0.1f) {
            // Move towards target
            enemy.pos.x += (dx / d) * speed * dt
            enemy.pos.y += (dy / d) * speed * dt
            enemy.directionAngle = atan2(dy, dx)
        } else if (enemy.alertState == AlertState.PATROLLING && enemy.patrolRoute.isNotEmpty()) {
            // Cycle patrol point
            enemy.patrolIndex = (enemy.patrolIndex + 1) % enemy.patrolRoute.size
        } else if (enemy.alertState == AlertState.SUSPICIOUS) {
            // Arrived at sound/investigation point, wait and decay alert
            enemy.alertState = AlertState.PATROLLING
            enemy.alertMeter = 0f
            logToConsole("${enemy.name} THREAT MINIMIZED. RESUMING PATROL")
        }
    }

    private fun detonateExplosionAt(pos: Point3D) {
        val radius = 2.0f
        logToConsole("PLASMA BARREL DETONATED!")
        // Damage enemies
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == pos.z.toInt() && enemy.pos.distanceTo(pos) <= radius) {
                enemy.health -= 80f
                enemy.alertState = AlertState.ALERTED
                enemy.lastKnownPlayerPos = player.pos.copy()
                logToConsole("${enemy.name} SPLASHED: -80HP")
                if (enemy.health <= 0) {
                    enemy.isDead = true
                    player.xp += 30
                    player.credits += 40
                    checkPlayerLevelUp()
                }
            }
        }

        // Damage Player
        if (player.pos.z.toInt() == pos.z.toInt() && player.pos.distanceTo(pos) <= radius) {
            player.health = (player.health - 40f).coerceAtLeast(0f)
            logToConsole("DANGER! SPLASH DAMAGE DEALT: -40HP")
            if (player.health <= 0) {
                isGameOver = true
                stopGameLoop()
            }
        }

        // Clear laser gates or barrels adjacent
        val map = gameLevels[pos.z.toInt()]
        if (map != null) {
            val gx = pos.x.toInt()
            val gy = pos.y.toInt()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val tx = gx + dx
                    val ty = gy + dy
                    if (map.getTile(tx, ty) == TileType.LASER_GRID || map.getTile(tx, ty) == TileType.BARREL_EXPLOSIVE) {
                        map.setTile(tx, ty, TileType.FLOOR)
                    }
                }
            }
        }
    }

    private fun checkPlayerLevelUp() {
        val xpNeeded = player.level * 100
        if (player.xp >= xpNeeded) {
            player.xp -= xpNeeded
            player.level++
            player.skillPoints += 2
            logToConsole("CYBER SYSTEM UPGRADED: LEVEL ${player.level}! +2 SP")
            saveGameProgress()
        }
    }

    // --- CONTROLLER ACTIONS ---

    fun movePlayer(dx: Float, dy: Float) {
        if (isGameOver || isGameWon || isHackingActive) return

        val speed = player.getSpeed()
        val nextX = player.pos.x + dx * speed
        val nextY = player.pos.y + dy * speed

        val map = gameLevels[currentZLevel] ?: return

        // Simple grid collision check
        val tile = map.getTile(nextX.toInt(), nextY.toInt())
        if (tile.isWalkable) {
            player.pos.x = nextX
            player.pos.y = nextY

            // Special actions for triggers
            if (tile == TileType.LASER_GRID) {
                player.health = (player.health - 25f).coerceAtLeast(0f)
                logToConsole("LASER TRIGGERED ALARM! -25HP")
                // Sound sound waves instantly triggering alerts
                triggerSoundRipple(player.pos, 12f)
                if (player.health <= 0) {
                    isGameOver = true
                    stopGameLoop()
                }
            }

            // Sound ripple creation if running
            if (!player.isSneaking && !player.isInvisible) {
                val soundRadius = if (player.equippedSystem.id == "quiet_soles") 2.5f else 5.5f
                if (noiseRipples.isEmpty() || noiseRipples.last().pos.distanceTo(player.pos) > 2.0f) {
                    triggerSoundRipple(player.pos, soundRadius)
                }
            }
        }
    }

    fun triggerSoundRipple(pos: Point3D, maxRadius: Float) {
        noiseRipples.add(NoiseRipple(pos.copy(), 0.5f, maxRadius))
    }

    fun toggleSneak() {
        player.isSneaking = !player.isSneaking
        logToConsole("STEALTH INGRESS: ${if (player.isSneaking) "ACTIVE (QUIET)" else "INACTIVE (LOUD)"}")
    }

    fun executeAttack() {
        if (isGameOver || isGameWon || isHackingActive) return

        val cost = 12f
        if (player.energy < cost) {
            logToConsole("INSUFFICIENT ENERGY CORE POWER")
            return
        }

        player.energy = (player.energy - cost).coerceAtLeast(0f)

        // Trigger noise
        val noiseRadius = if (player.equippedWeapon.id == "plasma_carbine") 15f else 3f
        triggerSoundRipple(player.pos, noiseRadius)

        // Ranged Weapons shoot orbs
        if (player.equippedWeapon.id == "plasma_carbine") {
            // Fire projectile in direction player is looking/moving
            val angle = player.getSpeed() // simple look dir approximation based on speed/last movement
            val targetDirX = cos(0f) // default to right for now, or based on last joystick input
            val targetDirY = sin(0f)

            // Let's shoot in 4 directions based on joystick/movement, or closest enemy!
            var dirX = 1f
            var dirY = 0f

            val nearest = enemies.filter { !it.isDead && it.pos.z.toInt() == currentZLevel }.minByOrNull { it.pos.distanceTo(player.pos) }
            if (nearest != null) {
                val dx = nearest.pos.x - player.pos.x
                val dy = nearest.pos.y - player.pos.y
                val d = sqrt(dx*dx + dy*dy)
                if (d > 0.1f) {
                    dirX = dx / d
                    dirY = dy / d
                }
            }

            activeProjectiles.add(Pair(player.pos.copy(), Point3D(dirX, dirY, 0f)))
            logToConsole("PLASMA CHARGE FIRED")
        } else {
            // Melee Swipe
            var hitAny = false
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != currentZLevel) continue

                val dist = enemy.pos.distanceTo(player.pos)
                if (dist < 1.6f) {
                    // Check if Backstab
                    val angleDiff = abs(enemy.directionAngle - atan2(enemy.pos.y - player.pos.y, enemy.pos.x - player.pos.x))
                    val isBackstab = player.isSneaking && (angleDiff < 1.0f || angleDiff > 2 * PI - 1.0f)
                    val mult = if (isBackstab) {
                        if (player.unlockedSkills.contains("ghost_backstab")) 5f else 3f
                    } else 1f

                    val dmg = player.getDamage() * mult
                    enemy.health -= dmg
                    enemy.alertState = AlertState.ALERTED
                    enemy.lastKnownPlayerPos = player.pos.copy()

                    hitAny = true
                    if (isBackstab) {
                        logToConsole("CRITICAL SILENT BACKSTAB! -${dmg.toInt()}HP")
                    } else {
                        logToConsole("SWIPE HIT ${enemy.name}: -${dmg.toInt()}HP")
                    }

                    if (enemy.health <= 0) {
                        enemy.isDead = true
                        player.xp += 30
                        player.credits += 40
                        checkPlayerLevelUp()
                        logToConsole("TARGET ELIMINATED: +40C +30XP")
                    }
                }
            }
            if (!hitAny) {
                logToConsole("SWIPE MELEE ATTACK: MISSED")
            }
        }
    }

    // Interactive Action button (climb ladder, hack terminal, trigger explosion)
    fun executeInteract() {
        if (isGameOver || isGameWon || isHackingActive) return

        val map = gameLevels[currentZLevel] ?: return
        val currentTile = map.getTile(player.pos.x.toInt(), player.pos.y.toInt())

        // 1. Check ladders
        if (currentTile == TileType.LADDER_UP) {
            if (currentZLevel < 3) {
                currentZLevel++
                player.pos.z = currentZLevel.toFloat()
                logToConsole("ASCENDING SHAFT TO LEVEL Z=$currentZLevel")
                // Reposition player nicely on new level ladder
                return
            }
        } else if (currentTile == TileType.LADDER_DOWN) {
            if (currentZLevel > 0) {
                currentZLevel--
                player.pos.z = currentZLevel.toFloat()
                logToConsole("DESCENDING SHAFT TO LEVEL Z=$currentZLevel")
                return
            }
        }

        // 2. Check Adjacent terminals to hack or barrels to detonate
        val px = player.pos.x.toInt()
        val py = player.pos.y.toInt()

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = px + dx
                val ty = py + dy
                val tile = map.getTile(tx, ty)

                if (tile == TileType.TERMINAL) {
                    // Start hacking!
                    isHackingActive = true
                    hackProgress = 0f
                    hackTerminalPos = GridPos(tx, ty, currentZLevel)
                    logToConsole("UPLINK DOCKED. DECRYPTING NODE ENCRYPTION...")
                    return
                } else if (tile == TileType.BARREL_EXPLOSIVE) {
                    // Manual detonating trigger from skill tree or direct overload hack
                    if (player.unlockedSkills.contains("tech_base")) {
                        map.setTile(tx, ty, TileType.FLOOR)
                        detonateExplosionAt(Point3D(tx.toFloat(), ty.toFloat(), currentZLevel.toFloat()))
                        return
                    }
                } else if (tile == TileType.EXIT_PORTAL && currentZLevel == 3) {
                    isGameWon = true
                    stopGameLoop()
                    player.credits += 200
                    currentScore += 1000
                    logToConsole("MISSION ACCOMPLISHED! ALL DATA EXTRACTED")
                    saveGameProgress()
                    return
                }
            }
        }

        logToConsole("NO INTERACTIVE TERMINALS OR SHAFTS IN RANGE")
    }

    // Active Skill usage from Skill Tree
    fun triggerActiveSkill(skillId: String) {
        if (!player.unlockedSkills.contains(skillId)) {
            logToConsole("SKILL LOCKED IN COGNITIVE MATRIX")
            return
        }

        when (skillId) {
            "ronin_crit" -> { // Ronin Dash
                val cost = 20f
                if (player.energy < cost) {
                    logToConsole("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                // Teleport forward 3 steps if walkable
                val map = gameLevels[currentZLevel] ?: return
                // simple dash direction calculation
                val targetX = (player.pos.x + 3f).coerceIn(1f, 18f)
                val targetY = player.pos.y

                if (map.getTile(targetX.toInt(), targetY.toInt()).isWalkable) {
                    player.pos.x = targetX
                    logToConsole("KAZE DASH ACTIVATED: CRITICAL STRIKE LOADED")
                    // Damage any enemies passed through
                    for (enemy in enemies) {
                        if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel && enemy.pos.x in player.pos.x..targetX && abs(enemy.pos.y - targetY) < 1.0f) {
                            val dmg = player.getDamage() * 2f
                            enemy.health -= dmg
                            enemy.alertState = AlertState.ALERTED
                            logToConsole("DASHED THROUGH ${enemy.name}: -${dmg.toInt()}HP")
                            if (enemy.health <= 0) {
                                enemy.isDead = true
                                player.xp += 30
                                player.credits += 40
                                checkPlayerLevelUp()
                            }
                        }
                    }
                }
            }
            "tech_ultimate" -> { // EMP Reboot
                val cost = 40f
                if (player.energy < cost) {
                    logToConsole("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                val map = gameLevels[currentZLevel] ?: return
                // Clear all laser gates in current map
                for (x in 0 until map.width) {
                    for (y in 0 until map.height) {
                        if (map.getTile(x, y) == TileType.LASER_GRID) {
                            map.setTile(x, y, TileType.FLOOR)
                        }
                    }
                }
                // Stun all enemies on this Z level for 8s
                for (enemy in enemies) {
                    if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                        enemy.alertState = AlertState.SUSPICIOUS
                        enemy.alertMeter = 10f
                        enemy.attackCooldown = 200 // massive stun cooldown
                    }
                }
                logToConsole("EMP COMPLETED: ALL LOCAL GATE LATTICES DESTROYED")
            }
            "ghost_smoke" -> { // Smoke Bomb
                val cost = 15f
                if (player.energy < cost) {
                    logToConsole("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                // Reset alert on nearby enemies
                for (enemy in enemies) {
                    if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel && enemy.pos.distanceTo(player.pos) < 4.0f) {
                        enemy.alertState = AlertState.PATROLLING
                        enemy.alertMeter = 0f
                        enemy.lastKnownPlayerPos = null
                    }
                }
                logToConsole("CHAFF BOMB: LOCAL SENTRY MATRIX RE-DAMPENED")
            }
            "ghost_ultimate" -> { // Phantom Matrix (Invisibility)
                val cost = 30f
                if (player.energy < cost) {
                    logToConsole("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                player.isInvisible = true
                player.invisibleTimer = 10.0f
                logToConsole("PHANTOM MATRIX INVISIBILITY CLOAK DEPLOYED")
            }
            else -> {
                logToConsole("SKILL HAS PASSIVE MATRIX EFFECTS")
            }
        }
    }

    // Purchase & Unlock Skill Node
    fun buySkill(skillId: String) {
        val node = skillNodes.find { it.id == skillId } ?: return
        if (node.isUnlocked) return

        // Check parents
        val parentsUnlocked = node.parents.all { pId ->
            player.unlockedSkills.contains(pId)
        }
        if (!parentsUnlocked && node.parents.isNotEmpty()) {
            logToConsole("REQUIRE PRE-REQUISITE DIRECTIVE UNLOCKED")
            return
        }

        if (player.skillPoints >= node.costPoints) {
            player.skillPoints -= node.costPoints
            val updatedSkills = player.unlockedSkills.toMutableSet()
            updatedSkills.add(skillId)
            player = player.copy(unlockedSkills = updatedSkills)

            skillNodes = skillNodes.map {
                if (it.id == skillId) it.copy(isUnlocked = true) else it
            }

            logToConsole("UNLOCK COGNITIVE DIRECTIVE: ${node.name}")
            saveGameProgress()
        } else {
            logToConsole("INSUFFICIENT SKILL CORE POINTS")
        }
    }

    // Customize loadouts
    fun equipItem(item: EquipmentItem) {
        player = when (item.type) {
            EquipmentType.WEAPON -> player.copy(equippedWeapon = item)
            EquipmentType.CORE -> player.copy(equippedCore = item)
            EquipmentType.SYSTEM -> player.copy(equippedSystem = item)
        }
        logToConsole("LOADOUT UPDATED: EQUIPPED ${item.name}")
        saveGameProgress()
    }

    fun buyEquipmentItem(item: EquipmentItem) {
        if (player.credits >= item.costCredits) {
            player.credits -= item.costCredits
            equipItem(item)
            logToConsole("PURCHASED ${item.name} FOR ${item.costCredits}C")
            saveGameProgress()
        } else {
            logToConsole("INSUFFICIENT CREDITS")
        }
    }
}
