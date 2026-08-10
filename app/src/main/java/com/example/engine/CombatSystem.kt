package com.example.engine

import com.example.model.*
import com.example.ui.AudioManager
import com.example.ui.SoundManager
import kotlin.math.*

class CombatSystem(private val levelManager: LevelManager) {

    interface CombatLogListener {
        fun onLog(message: String)
        fun onLevelUp()
        fun onGameOver()
        fun onGameWon()
    }

    var logListener: CombatLogListener? = null

    private fun log(msg: String) {
        logListener?.onLog(msg)
    }

    fun processProjectiles(
        projectiles: List<Pair<Point3D, Point3D>>,
        enemies: List<Enemy>,
        player: Player,
        currentZLevel: Int,
        dt: Float
    ): List<Pair<Point3D, Point3D>> {
        val nextProj = mutableListOf<Pair<Point3D, Point3D>>()
        for (p in projectiles) {
            val pos = p.first
            val vel = p.second
            val nextX = pos.x + vel.x * 20f * dt
            val nextY = pos.y + vel.y * 20f * dt
            val nextPos = Point3D(nextX, nextY, pos.z)

            val map = levelManager.getLevelMap(currentZLevel) ?: continue
            val tile = map.getTile(nextX.toInt(), nextY.toInt())
            if (tile == TileType.WALL || tile == TileType.EMPTY) {
                detonateExplosionAt(nextPos, enemies, player)
                continue
            }

            var hit = false
            for (enemy in enemies) {
                if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel && enemy.pos.distanceTo(nextPos) < 1.0f) {
                    val dmg = player.getDamage()
                    enemy.health -= dmg
                    enemy.alertState = AlertState.ALERTED
                    enemy.lastKnownPlayerPos = player.pos.copy()
                    log("PROJECTILE HIT ${enemy.name}: -${dmg.toInt()}HP")
                    hit = true
                    if (enemy.health <= 0) {
                        enemy.isDead = true
                        player.xp += 30
                        player.credits += 40
                        checkPlayerLevelUp(player)
                        log("TARGET ELIMINATED: +40C +30XP")
                    }
                    break
                }
            }

            if (!hit) {
                nextProj.add(Pair(nextPos, vel))
            }
        }
        return nextProj
    }

    fun executeAttack(
        player: Player,
        enemies: List<Enemy>,
        currentZLevel: Int,
        lastMoveX: Float,
        lastMoveY: Float,
        activeProjectiles: MutableList<Pair<Point3D, Point3D>>,
        noiseRipples: MutableList<NoiseRipple>
    ) {
        val cost = 12f
        if (player.energy < cost) {
            log("INSUFFICIENT ENERGY CORE POWER")
            return
        }

        player.energy = (player.energy - cost).coerceAtLeast(0f)

        val noiseRadius = if (player.equippedWeapon.id == "plasma_carbine") 15f else 3f
        noiseRipples.add(NoiseRipple(player.pos.copy(), 0.5f, noiseRadius))

        if (player.equippedWeapon.id == "plasma_carbine") {
            var dirX = lastMoveX
            var dirY = lastMoveY

            val nearest = enemies.filter { !it.isDead && it.pos.z.toInt() == currentZLevel }.minByOrNull { it.pos.distanceTo(player.pos) }
            if (nearest != null) {
                val dx = nearest.pos.x - player.pos.x
                val dy = nearest.pos.y - player.pos.y
                val d = sqrt(dx * dx + dy * dy)
                if (d > 0.1f) {
                    dirX = dx / d
                    dirY = dy / d
                }
            }

            activeProjectiles.add(Pair(player.pos.copy(), Point3D(dirX, dirY, 0f)))
            AudioManager.playLaser()
            log("PLASMA CHARGE FIRED")
        } else {
            var hitAny = false
            var triggeredBackstab = false
            for (enemy in enemies) {
                if (enemy.isDead || enemy.pos.z.toInt() != currentZLevel) continue

                val dist = enemy.pos.distanceTo(player.pos)
                if (dist < 1.6f) {
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
                        triggeredBackstab = true
                        log("CRITICAL SILENT BACKSTAB! -${dmg.toInt()}HP")
                    } else {
                        log("SWIPE HIT ${enemy.name}: -${dmg.toInt()}HP")
                    }

                    if (enemy.health <= 0) {
                        enemy.isDead = true
                        player.xp += 30
                        player.credits += 40
                        checkPlayerLevelUp(player)
                        AudioManager.playCreditLoot()
                        log("TARGET ELIMINATED: +40C +30XP")
                    }
                }
            }
            if (hitAny) {
                if (triggeredBackstab) {
                    AudioManager.playBackstab()
                } else {
                    AudioManager.playAttack()
                }
            } else {
                AudioManager.playAttack()
                log("SWIPE MELEE ATTACK: MISSED")
            }
        }
    }

    fun detonateExplosionAt(pos: Point3D, enemies: List<Enemy>, player: Player) {
        val radius = 2.0f
        log("PLASMA BARREL DETONATED!")
        for (enemy in enemies) {
            if (!enemy.isDead && enemy.pos.z.toInt() == pos.z.toInt() && enemy.pos.distanceTo(pos) <= radius) {
                enemy.health -= 80f
                enemy.alertState = AlertState.ALERTED
                enemy.lastKnownPlayerPos = player.pos.copy()
                log("${enemy.name} SPLASHED: -80HP")
                if (enemy.health <= 0) {
                    enemy.isDead = true
                    player.xp += 30
                    player.credits += 40
                    checkPlayerLevelUp(player)
                }
            }
        }

        if (player.pos.z.toInt() == pos.z.toInt() && player.pos.distanceTo(pos) <= radius) {
            player.health = (player.health - 40f).coerceAtLeast(0f)
            log("DANGER! SPLASH DAMAGE DEALT: -40HP")
            if (player.health <= 0) {
                logListener?.onGameOver()
            }
        }

        val map = levelManager.getLevelMap(pos.z.toInt())
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

    fun triggerActiveSkill(
        skillId: String,
        player: Player,
        enemies: List<Enemy>,
        currentZLevel: Int,
        lastMoveX: Float,
        lastMoveY: Float
    ) {
        if (!player.unlockedSkills.contains(skillId)) {
            log("SKILL LOCKED IN COGNITIVE MATRIX")
            return
        }

        SoundManager.playSkillActivation(skillId)

        when (skillId) {
            "ronin_crit" -> {
                val cost = 20f
                if (player.energy < cost) {
                    log("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                val map = levelManager.getLevelMap(currentZLevel) ?: return
                val oldX = player.pos.x
                val oldY = player.pos.y
                val targetX = (oldX + lastMoveX * 3.2f).coerceIn(1f, 18f)
                val targetY = (oldY + lastMoveY * 3.2f).coerceIn(1f, 18f)

                if (map.getTile(targetX.toInt(), targetY.toInt()).isWalkable) {
                    player.pos.x = targetX
                    player.pos.y = targetY
                    AudioManager.playBackstab()
                    log("KAZE DASH ACTIVATED: CRITICAL STRIKE LOADED")
                    
                    val midX = (oldX + targetX) / 2f
                    val midY = (oldY + targetY) / 2f
                    for (enemy in enemies) {
                        if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                            val d1 = enemy.pos.distanceTo(Point3D(oldX, oldY, currentZLevel.toFloat()))
                            val d2 = enemy.pos.distanceTo(Point3D(midX, midY, currentZLevel.toFloat()))
                            val d3 = enemy.pos.distanceTo(Point3D(targetX, targetY, currentZLevel.toFloat()))
                            if (d1 < 1.6f || d2 < 1.6f || d3 < 1.6f) {
                                val dmg = player.getDamage() * 2f
                                enemy.health -= dmg
                                enemy.alertState = AlertState.ALERTED
                                log("DASHED THROUGH ${enemy.name}: -${dmg.toInt()}HP")
                                if (enemy.health <= 0) {
                                    enemy.isDead = true
                                    player.xp += 30
                                    player.credits += 40
                                    checkPlayerLevelUp(player)
                                }
                            }
                        }
                    }
                }
            }
            "tech_ultimate" -> {
                val cost = 40f
                if (player.energy < cost) {
                    log("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                val map = levelManager.getLevelMap(currentZLevel) ?: return
                for (x in 0 until map.width) {
                    for (y in 0 until map.height) {
                        if (map.getTile(x, y) == TileType.LASER_GRID) {
                            map.setTile(x, y, TileType.FLOOR)
                        }
                    }
                }
                for (enemy in enemies) {
                    if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel) {
                        enemy.alertState = AlertState.SUSPICIOUS
                        enemy.alertMeter = 10f
                        enemy.attackCooldown = 200
                    }
                }
                AudioManager.playAlert()
                log("EMP COMPLETED: ALL LOCAL GATE LATTICES DESTROYED")
            }
            "ghost_smoke" -> {
                val cost = 15f
                if (player.energy < cost) {
                    log("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                for (enemy in enemies) {
                    if (!enemy.isDead && enemy.pos.z.toInt() == currentZLevel && enemy.pos.distanceTo(player.pos) < 4.0f) {
                        enemy.alertState = AlertState.PATROLLING
                        enemy.alertMeter = 0f
                        enemy.lastKnownPlayerPos = null
                    }
                }
                AudioManager.playStealthToggle(isActive = false)
                log("CHAFF BOMB: LOCAL SENTRY MATRIX RE-DAMPENED")
            }
            "ghost_ultimate" -> {
                val cost = 30f
                if (player.energy < cost) {
                    log("INSUFFICIENT ENERGY CORE POWER")
                    return
                }
                player.energy -= cost
                player.isInvisible = true
                player.invisibleTimer = 10.0f
                AudioManager.playStealthToggle(isActive = true)
                log("PHANTOM MATRIX INVISIBILITY CLOAK DEPLOYED")
            }
        }
    }

    private fun checkPlayerLevelUp(player: Player) {
        val xpNeeded = player.level * 100
        if (player.xp >= xpNeeded) {
            player.xp -= xpNeeded
            player.level++
            player.skillPoints += 2
            AudioManager.playLevelUp()
            log("CYBER SYSTEM UPGRADED: LEVEL ${player.level}! +2 SP")
            logListener?.onLevelUp()
        }
    }
}
