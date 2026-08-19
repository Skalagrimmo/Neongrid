package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CustomLoadoutEntity
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.PlayerSaveStateMapper
import com.example.engine.CombatSystem
import com.example.engine.LevelManager
import com.example.engine.MovementSystem
import com.example.engine.StealthAiSystem
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GameDatabase.getDatabase(application)
    private val repository = GameRepository(db.saveStateDao(), db.customLoadoutDao(), db.inventoryItemDao())

    val customLoadouts: StateFlow<List<CustomLoadoutEntity>> = repository.allLoadouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.allLoadouts.collect { loadouts ->
                if (loadouts.isEmpty()) {
                    seedDefaultLoadouts()
                }
            }
        }
    }

    val mapSize = 35

    // Modular Systems
    val levelManager = LevelManager(mapSize)
    val movementSystem = MovementSystem(levelManager)
    val stealthAiSystem = StealthAiSystem()
    val combatSystem = CombatSystem(levelManager)

    // Game Screens State
    enum class Screen {
        MENU,
        PLAY,
        SKILL_TREE,
        LOADOUT,
        CONTROLS,
        NATIVE_ISO_CANVAS
    }

    var currentScreen by mutableStateOf(Screen.MENU)
        private set

    // Low-Spec Performance Optimization Mode (2012-2013 4-Core Mobile Preset)
    var isLowSpecPerformanceMode by mutableStateOf(true)
        private set

    // Game Boy Color Graphics & Palette Quality State
    var gbcGraphicsSettings by mutableStateOf(GbcGraphicsSettings())
        private set

    fun togglePerformanceMode() {
        isLowSpecPerformanceMode = !isLowSpecPerformanceMode
        logToConsole(if (isLowSpecPerformanceMode) "PERF MODE: 2012-2013 4-CORE OPTIMIZATION ENABLED" else "PERF MODE: 60FPS FULL SHADER ENGAGED")
    }

    fun setGbcPalette(palette: GbcPalette) {
        gbcGraphicsSettings = gbcGraphicsSettings.copy(palette = palette)
        logToConsole("GBC PALETTE: SWITCHED TO [${palette.displayName.uppercase()}]")
    }

    fun toggleGbcScanlines() {
        gbcGraphicsSettings = gbcGraphicsSettings.copy(isScanlinesEnabled = !gbcGraphicsSettings.isScanlinesEnabled)
        logToConsole("GBC DISPLAY: CRT SCANLINES [${if (gbcGraphicsSettings.isScanlinesEnabled) "ENABLED" else "DISABLED"}]")
    }

    fun toggleGbcDither() {
        gbcGraphicsSettings = gbcGraphicsSettings.copy(isPixelDitherEnabled = !gbcGraphicsSettings.isPixelDitherEnabled)
        logToConsole("GBC GRAPHICS: PIXEL DITHER [${if (gbcGraphicsSettings.isPixelDitherEnabled) "ACTIVE" else "OFF"}]")
    }

    fun toggleGbcPixelOutlines() {
        gbcGraphicsSettings = gbcGraphicsSettings.copy(isPixelOutlineEnabled = !gbcGraphicsSettings.isPixelOutlineEnabled)
        logToConsole("GBC GRAPHICS: PIXEL OUTLINES [${if (gbcGraphicsSettings.isPixelOutlineEnabled) "ACTIVE" else "OFF"}]")
    }

    fun toggleCelShading() {
        val newEnabled = !gbcGraphicsSettings.isCelShadingEnabled
        gbcGraphicsSettings = gbcGraphicsSettings.copy(
            isCelShadingEnabled = newEnabled,
            celShadingSettings = gbcGraphicsSettings.celShadingSettings.copy(isEnabled = newEnabled)
        )
        logToConsole("CEL-SHADING SYSTEM: [${if (newEnabled) "ACTIVE" else "DISABLED"}]")
    }

    fun setCelShadingBands(bands: Int) {
        val newSettings = gbcGraphicsSettings.celShadingSettings.copy(bands = bands)
        gbcGraphicsSettings = gbcGraphicsSettings.copy(celShadingSettings = newSettings)
        logToConsole("CEL-SHADING BANDS: SET TO $bands-BAND QUANTIZATION")
    }

    fun setCelInkOutlineThickness(thickness: Float) {
        val newSettings = gbcGraphicsSettings.celShadingSettings.copy(inkOutlineThickness = thickness)
        gbcGraphicsSettings = gbcGraphicsSettings.copy(celShadingSettings = newSettings)
        logToConsole("CEL-SHADING INK OUTLINE: SET TO ${thickness}PX")
    }

    // Active Game Entities & State
    var player by mutableStateOf(Player())
        private set

    var exploredTiles by mutableStateOf(mutableMapOf<Int, Set<String>>())
        private set

    private var lastExploredGridX = -1
    private var lastExploredGridY = -1
    private var lastExploredZ = -1

    var enemies = mutableListOf<Enemy>()
        private set

    var noiseRipples = mutableListOf<NoiseRipple>()
        private set

    val gameLevels: Map<Int, GameLevelMap>
        get() = levelManager.gameLevels

    var currentZLevel by mutableStateOf(1)
        private set

    var currentScore by mutableStateOf(0)
        private set

    var isGameOver by mutableStateOf(false)
        private set

    var isGameWon by mutableStateOf(false)
        private set

    var isHackingActive by mutableStateOf(false)
        private set

    var isTacticalOverlayActive by mutableStateOf(false)
        private set

    var activeGridAction by mutableStateOf(GridActionType.NONE)
        private set

    var hoveredTargetTile by mutableStateOf<GridPos?>(null)
        private set

    var isThreatZoneOverlayVisible by mutableStateOf(true)
        private set

    var isVisionConeOverlayVisible by mutableStateOf(true)
        private set

    var isMovementRangeOverlayVisible by mutableStateOf(true)
        private set

    var hackTerminalPos by mutableStateOf<GridPos?>(null)
        private set

    var hackProgress by mutableStateOf(0f)
        private set

    var consoleLogs = mutableListOf<String>()
        private set

    var skillNodes by mutableStateOf(SkillNode.getSkillTree())
        private set

    var activeProjectiles = mutableListOf<Pair<Point3D, Point3D>>()
        private set

    var gameTick by mutableStateOf(0L)
        private set

    private var gameLoopJob: Job? = null

    var lastMoveX: Float = 1.0f
        private set
    var lastMoveY: Float = 0.0f
        private set

    init {
        stealthAiSystem.logListener = object : StealthAiSystem.AiLogListener {
            override fun onLog(message: String) {
                logToConsole(message)
            }
        }

        combatSystem.logListener = object : CombatSystem.CombatLogListener {
            override fun onLog(message: String) {
                logToConsole(message)
            }

            override fun onLevelUp() {
                saveGameProgress()
            }

            override fun onGameOver() {
                isGameOver = true
                stopGameLoop()
                logToConsole("CRITICAL COLLAPSE: PLAYER DECEASED")
            }

            override fun onGameWon() {
                isGameWon = true
                stopGameLoop()
                logToConsole("MISSION ACCOMPLISHED!")
            }
        }

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
                    val restored = PlayerSaveStateMapper.restore(it, player)
                    currentZLevel = restored.currentZLevel
                    player = restored.player
                    skillNodes = restored.skillNodes
                    exploredTiles = restored.exploredTiles
                    updateExplorationAtPlayer(force = true)

                    logToConsole("SAVE RESTORED: CLVL ${it.level} | POS (${it.playerPosX.toInt()},${it.playerPosY.toInt()},Z=${it.currentZLevel})")
                }
            }
        }
    }

    fun saveGameProgress() {
        viewModelScope.launch {
            val save = PlayerSaveStateMapper.toSaveState(
                player = player,
                currentZLevel = currentZLevel,
                currentScore = currentScore,
                exploredTiles = exploredTiles
            )
            repository.saveGame(save)
            logToConsole("ROOM DB PERSISTED: POS & QUEST & INVENTORY")
        }
    }

    fun resetSaveData() {
        viewModelScope.launch {
            repository.clearSave()
            player = Player()
            skillNodes = SkillNode.getSkillTree()
            exploredTiles = mutableMapOf()
            lastExploredGridX = -1
            lastExploredGridY = -1
            lastExploredZ = -1
            saveGameProgress()
            resetGameEntities()
            logToConsole("ROOM DB SAVES RESET")
        }
    }

    fun logToConsole(message: String) {
        if (consoleLogs.size > 8) {
            consoleLogs.removeAt(0)
        }
        consoleLogs.add(message)
    }

    fun resetGameEntities() {
        isGameOver = false
        isGameWon = false
        currentZLevel = 1
        currentScore = 0
        isHackingActive = false
        enemies.clear()
        noiseRipples.clear()
        activeProjectiles.clear()
        levelManager.generateLevels()

        player.pos = Point3D(2f, 2f, 1f)
        player.health = 100f + player.equippedCore.statBoostHealth
        player.maxHealth = 100f + player.equippedCore.statBoostHealth
        player.energy = 80f + player.equippedCore.statBoostEnergy
        player.maxEnergy = 80f + player.equippedCore.statBoostEnergy

        spawnEnemiesForLevel()
        logToConsole("ENTITIES REPOPULATED ON GRID")
    }

    private fun spawnEnemiesForLevel() {
        enemies.clear()

        // Z=0 Sewers
        enemies.add(
            Enemy(
                id = "drone_0", name = "Drone SENTROY_0",
                pos = Point3D(5f, 5f, 0f), type = "SentryDrone", health = 30f, maxHealth = 30f,
                patrolRoute = listOf(Point3D(5f, 5f, 0f), Point3D(5f, 25f, 0f), Point3D(25f, 25f, 0f), Point3D(25f, 5f, 0f))
            )
        )
        enemies.add(
            Enemy(
                id = "drone_1", name = "Drone SENTROY_1",
                pos = Point3D(25f, 8f, 0f), type = "SentryDrone", health = 30f, maxHealth = 30f,
                patrolRoute = listOf(Point3D(25f, 8f, 0f), Point3D(25f, 30f, 0f), Point3D(9f, 30f, 0f))
            )
        )

        // Z=1 Main Street
        enemies.add(
            Enemy(
                id = "sentry_0", name = "Guardsman SYNTROB_0",
                pos = Point3D(12f, 6f, 1f), type = "Syntrob", health = 60f, maxHealth = 60f,
                patrolRoute = listOf(Point3D(12f, 6f, 1f), Point3D(20f, 6f, 1f), Point3D(20f, 15f, 1f), Point3D(12f, 15f, 1f))
            )
        )
        enemies.add(
            Enemy(
                id = "sentry_1", name = "Guardsman SYNTROB_1",
                pos = Point3D(12f, 25f, 1f), type = "Syntrob", health = 60f, maxHealth = 60f,
                patrolRoute = listOf(Point3D(12f, 25f, 1f), Point3D(20f, 25f, 1f), Point3D(20f, 12f, 1f))
            )
        )

        // Z=2 Mezzanine
        enemies.add(
            Enemy(
                id = "sentry_2", name = "Commando SYNTROB_2",
                pos = Point3D(5f, 5f, 2f), type = "Syntrob", health = 80f, maxHealth = 80f,
                patrolRoute = listOf(Point3D(5f, 5f, 2f), Point3D(8f, 5f, 2f), Point3D(8f, 8f, 2f), Point3D(5f, 8f, 2f))
            )
        )
        enemies.add(
            Enemy(
                id = "sentry_3", name = "Elite WATCHER_3",
                pos = Point3D(25f, 5f, 2f), type = "Sentry", health = 50f, maxHealth = 50f,
                patrolRoute = listOf(Point3D(25f, 5f, 2f), Point3D(25f, 9f, 2f), Point3D(28f, 9f, 2f), Point3D(28f, 5f, 2f))
            )
        )

        // Z=3 Sky Gateway Boss
        enemies.add(
            Enemy(
                id = "boss_heavy", name = "Mech SYNTROY_HEAVY",
                pos = Point3D(20f, 20f, 3f), type = "Boss", health = 250f, maxHealth = 250f,
                patrolRoute = listOf(Point3D(20f, 20f, 3f), Point3D(20f, 8f, 3f), Point3D(8f, 8f, 3f), Point3D(8f, 20f, 3f))
            )
        )
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
                gameTick++
                val targetDelay = if (isLowSpecPerformanceMode) 33L else 16L
                delay(targetDelay)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    private fun updateGameEntities(dt: Float) {
        if (isGameOver || isGameWon) return

        // 1. Process Active Projectiles
        activeProjectiles = combatSystem.processProjectiles(activeProjectiles, enemies, player, currentZLevel, dt).toMutableList()

        // 2. Process Noise Ripples
        val nextRipples = mutableListOf<NoiseRipple>()
        for (ripple in noiseRipples) {
            ripple.radius += ripple.speed * 10f * dt
            if (ripple.radius < ripple.maxRadius) {
                nextRipples.add(ripple)
            }
        }
        noiseRipples = nextRipples

        // 3. Process Invisibility
        if (player.isInvisible) {
            player.invisibleTimer -= dt
            if (player.invisibleTimer <= 0) {
                player.isInvisible = false
                logToConsole("PHANTOM MATRIX CLOAK DEACTIVATED")
            }
        }

        // 4. Update Enemy Behaviors & Stealth AI
        val currentLevelMap = levelManager.getLevelMap(currentZLevel)
        stealthAiSystem.updateEnemies(enemies, player, currentZLevel, noiseRipples, dt, currentLevelMap) { damage ->
            player.health = (player.health - damage).coerceAtLeast(0f)
            if (player.health <= 0f) {
                isGameOver = true
                stopGameLoop()
                logToConsole("CRITICAL COLLAPSE: PLAYER DECEASED")
            }
        }

        // 5. Active Hacking
        if (isHackingActive) {
            hackProgress += dt * 0.35f
            if (hackProgress >= 1f) {
                isHackingActive = false
                hackProgress = 0f
                val hPos = hackTerminalPos
                if (hPos != null) {
                    val map = levelManager.getLevelMap(currentZLevel)
                    if (map != null) {
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

                    // Update Active Quest Progress
                    if (!player.quest.isCompleted) {
                        val newProgress = (player.quest.currentProgress + 1).coerceAtMost(player.quest.targetCount)
                        val completed = newProgress >= player.quest.targetCount
                        player.quest = player.quest.copy(
                            currentProgress = newProgress,
                            isCompleted = completed
                        )
                        logToConsole("ACTIVE QUEST OBJECTIVE: ${player.quest.title} ($newProgress/${player.quest.targetCount})")
                        if (completed) {
                            player.credits += 150
                            player.xp += 200
                            logToConsole("QUEST COMPLETED! REWARD: +150 CREDITS, +200 XP")
                        }
                    }
                    saveGameProgress()
                }
            }
        }

        // 6. Energy Regeneration
        if (player.energy < player.maxEnergy) {
            player.energy = (player.energy + 4.0f * dt).coerceAtMost(player.maxEnergy)
        }

        player = player.copy()
    }

    fun movePlayer(dx: Float, dy: Float) {
        if (isGameOver || isGameWon || isHackingActive) return

        val res = movementSystem.processPlayerMove(player, currentZLevel, dx, dy, noiseRipples)
        if (res.moved) {
            lastMoveX = res.lastMoveX
            lastMoveY = res.lastMoveY

            if (res.fallDamageTriggered) {
                player.health = (player.health - 15f).coerceAtLeast(0f)
                logToConsole("FALL DETECTED FROM ELEVATION LAYER! DAMAGE RECEIVED (-15HP)")
                if (player.health <= 0) {
                    isGameOver = true
                    stopGameLoop()
                }
            }
            if (res.newZLevel != null) {
                currentZLevel = res.newZLevel!!
            }
            updateExplorationAtPlayer()
            saveGameProgress()
        }
    }

    fun toggleSneak() {
        player.isSneaking = !player.isSneaking
        AudioManager.playStealthToggle(player.isSneaking)
        logToConsole("STEALTH INGRESS: ${if (player.isSneaking) "ACTIVE (QUIET)" else "INACTIVE (LOUD)"}")
    }

    // Commandos-Style Visibility & Noise Detection Engine API
    fun triggerNoiseEvent(pos: Point3D, maxRadius: Float = 6.0f, speed: Float = 0.25f) {
        val ripple = NoiseRipple(pos = pos.copy(), radius = 0.5f, maxRadius = maxRadius, speed = speed)
        noiseRipples.add(ripple)
        logToConsole("NOISE ENGINE: SOUND WAVE EMITTED [R=${maxRadius.toInt()}m]")
    }

    fun isPlayerSpotted(): Boolean {
        return enemies.any { it.pos.z.toInt() == currentZLevel && !it.isDead && it.alertState == AlertState.ALERTED }
    }

    fun getHighestAlertState(): AlertState {
        val activeOnLevel = enemies.filter { it.pos.z.toInt() == currentZLevel && !it.isDead }
        if (activeOnLevel.any { it.alertState == AlertState.ALERTED }) return AlertState.ALERTED
        if (activeOnLevel.any { it.alertState == AlertState.SUSPICIOUS }) return AlertState.SUSPICIOUS
        return AlertState.PATROLLING
    }

    fun calculatePlayerNoiseOutput(): Float {
        if (player.isInvisible) return 0f
        if (player.isSneaking) return 0.2f
        val systemDampening = if (player.equippedSystem.id == "quiet_soles") 0.35f else 1.0f
        return 1.0f * systemDampening
    }

    fun toggleTacticalOverlay() {
        isTacticalOverlayActive = !isTacticalOverlayActive
        AudioManager.playInteract()
        logToConsole("TACTICAL OVERLAY: ${if (isTacticalOverlayActive) "ACTIVE" else "INACTIVE"}")
    }

    fun setGridAction(action: GridActionType) {
        if (activeGridAction == action) {
            activeGridAction = GridActionType.NONE
            logToConsole("ACTION DESELECTED")
        } else {
            activeGridAction = action
            isTacticalOverlayActive = true
            AudioManager.playInteract()
            logToConsole("GRID ACTION INITIATED: ${action.displayName}")
        }
    }

    fun clearAction() {
        activeGridAction = GridActionType.NONE
        hoveredTargetTile = null
    }

    fun setHoveredTile(pos: GridPos?) {
        hoveredTargetTile = pos
    }

    fun toggleThreatZoneOverlay() {
        isThreatZoneOverlayVisible = !isThreatZoneOverlayVisible
        AudioManager.playInteract()
        logToConsole("THREAT ZONES: ${if (isThreatZoneOverlayVisible) "VISIBLE" else "HIDDEN"}")
    }

    fun toggleVisionConeOverlay() {
        isVisionConeOverlayVisible = !isVisionConeOverlayVisible
        AudioManager.playInteract()
        logToConsole("VISION CONES: ${if (isVisionConeOverlayVisible) "VISIBLE" else "HIDDEN"}")
    }

    fun toggleMovementRangeOverlay() {
        isMovementRangeOverlayVisible = !isMovementRangeOverlayVisible
        AudioManager.playInteract()
        logToConsole("MOVEMENT RANGE: ${if (isMovementRangeOverlayVisible) "VISIBLE" else "HIDDEN"}")
    }

    fun executeSelectedGridAction(target: GridPos) {
        when (activeGridAction) {
            GridActionType.MOVE -> {
                val dx = target.x - player.pos.x
                val dy = target.y - player.pos.y
                movePlayer(dx, dy)
                logToConsole("GRID MOVE EXECUTED -> (${target.x}, ${target.y})")
            }
            GridActionType.ATTACK -> {
                executeAttack()
            }
            GridActionType.HACK -> {
                executeInteract()
            }
            GridActionType.SKILL -> {
                val firstSkill = player.unlockedSkills.firstOrNull() ?: "ronin_base"
                triggerActiveSkill(firstSkill)
            }
            GridActionType.NONE -> {}
        }
        activeGridAction = GridActionType.NONE
        hoveredTargetTile = null
    }

    fun executeAttack() {
        if (isGameOver || isGameWon || isHackingActive) return
        combatSystem.executeAttack(player, enemies, currentZLevel, lastMoveX, lastMoveY, activeProjectiles, noiseRipples)
    }

    fun executeInteract() {
        if (isGameOver || isGameWon || isHackingActive) return

        val map = levelManager.getLevelMap(currentZLevel) ?: return
        val currentTile = map.getTile(player.pos.x.toInt(), player.pos.y.toInt())

        if (currentTile == TileType.LADDER_UP) {
            if (currentZLevel < 3) {
                currentZLevel++
                player.pos.z = currentZLevel.toFloat()
                AudioManager.playLadder(ascending = true)
                logToConsole("ASCENDING SHAFT TO LEVEL Z=$currentZLevel")
                updateExplorationAtPlayer()
                return
            }
        } else if (currentTile == TileType.LADDER_DOWN) {
            if (currentZLevel > 0) {
                currentZLevel--
                player.pos.z = currentZLevel.toFloat()
                AudioManager.playLadder(ascending = false)
                logToConsole("DESCENDING SHAFT TO LEVEL Z=$currentZLevel")
                updateExplorationAtPlayer()
                return
            }
        }

        val px = player.pos.x.toInt()
        val py = player.pos.y.toInt()

        for (dx in -1..1) {
            for (dy in -1..1) {
                val tx = px + dx
                val ty = py + dy
                val tile = map.getTile(tx, ty)

                if (tile == TileType.TERMINAL) {
                    isHackingActive = true
                    hackProgress = 0f
                    hackTerminalPos = GridPos(tx, ty, currentZLevel)
                    AudioManager.playInteract()
                    logToConsole("UPLINK DOCKED. DECRYPTING NODE ENCRYPTION...")
                    return
                } else if (tile == TileType.BARREL_EXPLOSIVE) {
                    if (player.unlockedSkills.contains("tech_base")) {
                        map.setTile(tx, ty, TileType.FLOOR)
                        combatSystem.detonateExplosionAt(Point3D(tx.toFloat(), ty.toFloat(), currentZLevel.toFloat()), enemies, player)
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

    fun triggerActiveSkill(skillId: String) {
        combatSystem.triggerActiveSkill(skillId, player, enemies, currentZLevel, lastMoveX, lastMoveY)
    }

    fun buySkill(skillId: String) {
        val node = skillNodes.find { it.id == skillId } ?: return
        if (node.isUnlocked) return

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

    fun grantSkillPoint(amount: Int = 1) {
        player = player.copy(skillPoints = player.skillPoints + amount)
        AudioManager.playLevelUp()
        logToConsole("SIMULATION: GRANTED +$amount SKILL POINTS")
        saveGameProgress()
    }

    fun respecSkills() {
        val totalRefund = skillNodes.filter { it.isUnlocked }.sumOf { it.costPoints }
        player = player.copy(
            skillPoints = player.skillPoints + totalRefund,
            unlockedSkills = emptySet()
        )
        skillNodes = skillNodes.map { it.copy(isUnlocked = false) }
        AudioManager.playInteract()
        logToConsole("NEURAL MATRIX RESPEC: REFUNDED +$totalRefund SP")
        saveGameProgress()
    }

    fun equipItem(item: EquipmentItem) {
        var nextPlayer = when (item.type) {
            EquipmentType.WEAPON -> player.copy(equippedWeapon = item)
            EquipmentType.CORE -> player.copy(equippedCore = item)
            EquipmentType.SYSTEM -> player.copy(equippedSystem = item)
            else -> player.copy(equippedSystem = item)
        }

        val totalHealthBoost = nextPlayer.equippedWeapon.statBoostHealth + nextPlayer.equippedCore.statBoostHealth + nextPlayer.equippedSystem.statBoostHealth
        val totalEnergyBoost = nextPlayer.equippedWeapon.statBoostEnergy + nextPlayer.equippedCore.statBoostEnergy + nextPlayer.equippedSystem.statBoostEnergy

        val newMaxHealth = 100f + totalHealthBoost
        val newMaxEnergy = 80f + totalEnergyBoost

        nextPlayer.maxHealth = newMaxHealth
        nextPlayer.maxEnergy = newMaxEnergy
        nextPlayer.health = nextPlayer.health.coerceAtMost(newMaxHealth)
        nextPlayer.energy = nextPlayer.energy.coerceAtMost(newMaxEnergy)

        player = nextPlayer
        logToConsole("LOADOUT UPDATED: EQUIPPED ${item.name}")
        saveGameProgress()
    }

    fun buyEquipmentItem(item: EquipmentItem) {
        if (player.credits >= item.costCredits) {
            val updatedOwned = player.inventory.ownedEquipmentIds + item.id
            player.credits -= item.costCredits
            player.inventory = player.inventory.copy(ownedEquipmentIds = updatedOwned)
            equipItem(item)
            logToConsole("PURCHASED ${item.name} FOR ${item.costCredits}C")
            saveGameProgress()
        } else {
            logToConsole("INSUFFICIENT CREDITS")
        }
    }

    fun buyHealthPack() {
        val cost = 50
        if (player.credits >= cost) {
            player.credits -= cost
            val nextPacks = player.inventory.healthPacks + 1
            player.inventory = player.inventory.copy(healthPacks = nextPacks)
            logToConsole("PURCHASED MED-PACK (+1). TOTAL: $nextPacks")
            saveGameProgress()
        } else {
            logToConsole("INSUFFICIENT CREDITS (NEEDS 50C)")
        }
    }

    fun buyEnergyCell() {
        val cost = 50
        if (player.credits >= cost) {
            player.credits -= cost
            val nextCells = player.inventory.energyCells + 1
            player.inventory = player.inventory.copy(energyCells = nextCells)
            logToConsole("PURCHASED E-CELL (+1). TOTAL: $nextCells")
            saveGameProgress()
        } else {
            logToConsole("INSUFFICIENT CREDITS (NEEDS 50C)")
        }
    }

    fun useHealthPack() {
        if (player.inventory.healthPacks > 0 && player.health < player.maxHealth) {
            val nextPacks = player.inventory.healthPacks - 1
            player.inventory = player.inventory.copy(healthPacks = nextPacks)
            player.health = (player.health + 40f).coerceAtMost(player.maxHealth)
            logToConsole("MED-PACK DEPLOYED: +40 HP (REMAINING: $nextPacks)")
            saveGameProgress()
        } else if (player.inventory.healthPacks == 0) {
            logToConsole("NO MED-PACKS AVAILABLE IN INVENTORY")
        }
    }

    fun useEnergyCell() {
        if (player.inventory.energyCells > 0 && player.energy < player.maxEnergy) {
            val nextCells = player.inventory.energyCells - 1
            player.inventory = player.inventory.copy(energyCells = nextCells)
            player.energy = (player.energy + 40f).coerceAtMost(player.maxEnergy)
            logToConsole("E-CELL INJECTED: +40 EP (REMAINING: $nextCells)")
            saveGameProgress()
        } else if (player.inventory.energyCells == 0) {
            logToConsole("NO E-CELLS AVAILABLE IN INVENTORY")
        }
    }

    fun updateExplorationAtPlayer(force: Boolean = false) {
        val (updated, changed, triple) = levelManager.updateExploration(
            player.pos,
            currentZLevel,
            exploredTiles,
            lastExploredGridX,
            lastExploredGridY,
            lastExploredZ,
            force
        )

        if (changed) {
            exploredTiles = updated.toMutableMap()
            lastExploredGridX = triple.first
            lastExploredGridY = triple.second
            lastExploredZ = triple.third
            saveGameProgress()
        }
    }

    fun saveCurrentLoadoutAsCustom(loadoutName: String) {
        val nameToUse = if (loadoutName.isBlank()) "PRESET #${(System.currentTimeMillis() % 1000)}" else loadoutName
        val derivedClass = when {
            player.unlockedSkills.contains("ronin_base") -> "CYBER_RONIN"
            player.unlockedSkills.contains("tech_base") -> "TECH_NECROMANCER"
            player.unlockedSkills.contains("ghost_base") -> "GHOST_INFILTRATOR"
            else -> "CYBER_RONIN"
        }

        viewModelScope.launch {
            repository.saveCustomLoadout(
                CustomLoadoutEntity(
                    name = nameToUse,
                    weaponId = player.equippedWeapon.id,
                    weaponName = player.equippedWeapon.name,
                    coreId = player.equippedCore.id,
                    coreName = player.equippedCore.name,
                    systemId = player.equippedSystem.id,
                    systemName = player.equippedSystem.name,
                    characterClass = derivedClass
                )
            )
            AudioManager.playInteract()
            logToConsole("ROOM DB: SAVED CUSTOM LOADOUT '$nameToUse'")
        }
    }

    fun applyCustomLoadout(loadout: CustomLoadoutEntity) {
        val weapon = EquipmentItem.ALL_ITEMS.find { it.id == loadout.weaponId } ?: player.equippedWeapon
        val core = EquipmentItem.ALL_ITEMS.find { it.id == loadout.coreId } ?: player.equippedCore
        val system = EquipmentItem.ALL_ITEMS.find { it.id == loadout.systemId } ?: player.equippedSystem

        equipItem(weapon)
        equipItem(core)
        equipItem(system)

        AudioManager.playInteract()
        logToConsole("ROOM DB: LOADED PRESET '${loadout.name}'")
    }

    fun deleteCustomLoadout(loadoutId: Int) {
        viewModelScope.launch {
            repository.deleteCustomLoadout(loadoutId)
            AudioManager.playInteract()
            logToConsole("ROOM DB: REMOVED LOADOUT PRESET #$loadoutId")
        }
    }

    private fun seedDefaultLoadouts() {
        viewModelScope.launch {
            repository.saveCustomLoadout(
                CustomLoadoutEntity(
                    name = "RONIN MELEE STRIKER",
                    weaponId = "nano_blade",
                    weaponName = "Nano-Edge Katana",
                    coreId = "force_shield",
                    coreName = "Hard-Light Kinetic Barrier",
                    systemId = "targeting_chip",
                    systemName = "Ocular Combat Predictor",
                    characterClass = "CYBER_RONIN"
                )
            )
            repository.saveCustomLoadout(
                CustomLoadoutEntity(
                    name = "TECH NECRO OVERLOAD",
                    weaponId = "plasma_rifle",
                    weaponName = "Hyperion Plasma Launcher",
                    coreId = "overclock_core",
                    coreName = "Thermal Overclock Core",
                    systemId = "drone_controller",
                    systemName = "Autonomous Sentry Link",
                    characterClass = "TECH_NECROMANCER"
                )
            )
            repository.saveCustomLoadout(
                CustomLoadoutEntity(
                    name = "GHOST INFILTRATOR",
                    weaponId = "vibro_dagger",
                    weaponName = "High-Frequency Vibro-Dagger",
                    coreId = "stealth_cloak",
                    coreName = "Phase-Shift Stealth Field",
                    systemId = "stealth_dampener",
                    systemName = "Acoustic Noise Suppressor",
                    characterClass = "GHOST_INFILTRATOR"
                )
            )
        }
    }
}
