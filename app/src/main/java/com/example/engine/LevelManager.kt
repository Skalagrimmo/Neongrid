package com.example.engine

import com.example.model.*

class LevelManager(val mapSize: Int = 35) {

    val gameLevels = mutableMapOf<Int, GameLevelMap>()
    val elevationGrid: IsoMultiLevelGrid = IsoMultiLevelGrid.createSampleTacticalGrid(mapSize, mapSize, 4)

    init {
        generateLevels()
    }

    fun getLevelMap(zLevel: Int): GameLevelMap? = gameLevels[zLevel]

    fun generateLevels() {
        gameLevels.clear()
        val size = mapSize

        // Z=0 Sewer Grid (Flooded concrete pathways, water drainage channels, columns, ladders to Z=1)
        val sewerGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x == 5 || x == 9 || x == 10 || x == 22 || x == 25 || x == 30 || 
                    y == 5 || y == 15 || y == 25 || y == 30 || 
                    (x in 4..12 && y in 4..12) || (x in 20..28 && y in 20..28)) {
                    sewerGrid[x][y] = TileType.FLOOR
                }
            }
        }
        sewerGrid[5][5] = TileType.LADDER_UP   // Rises into Biotech Lab
        sewerGrid[2][25] = TileType.LADDER_UP  // Rises into Left Street pavement
        sewerGrid[30][25] = TileType.LADDER_UP // Rises into Right Street pavement

        sewerGrid[4][5] = TileType.BARREL_EXPLOSIVE
        sewerGrid[11][15] = TileType.BARREL_EXPLOSIVE
        sewerGrid[13][15] = TileType.BARREL_EXPLOSIVE
        sewerGrid[21][25] = TileType.BARREL_EXPLOSIVE
        
        gameLevels[0] = GameLevelMap(0, "Z=0 SEWER CANALS", size, size, sewerGrid)

        // Z=1 Main Street Grid
        val mainGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x in 1..(size - 2) && y in 1..(size - 2)) {
                    mainGrid[x][y] = TileType.FLOOR
                }
                if (x == 17 || y == 17) {
                    mainGrid[x][y] = TileType.GRID_ROAD
                }
            }
        }

        // BUILDING 1: Biotech Lab
        for (x in 3..9) {
            for (y in 3..9) {
                if (x == 3 || x == 9 || y == 3 || y == 9) {
                    mainGrid[x][y] = TileType.WALL
                } else {
                    mainGrid[x][y] = TileType.FLOOR
                }
            }
        }
        mainGrid[9][6] = TileType.LASER_GRID
        mainGrid[10][6] = TileType.TERMINAL

        // BUILDING 2: Security HQ
        for (x in 22..29) {
            for (y in 3..10) {
                if (x == 22 || x == 29 || y == 3 || y == 10) {
                    mainGrid[x][y] = TileType.WALL
                } else {
                    mainGrid[x][y] = TileType.FLOOR
                }
            }
        }
        mainGrid[22][6] = TileType.LASER_GRID
        mainGrid[21][6] = TileType.TERMINAL

        // BUILDING 3: Power Grid Station
        for (x in 22..29) {
            for (y in 22..29) {
                if (x == 22 || x == 29 || y == 22 || y == 29) {
                    mainGrid[x][y] = TileType.WALL
                } else {
                    mainGrid[x][y] = TileType.FLOOR
                }
            }
        }
        mainGrid[22][25] = TileType.LASER_GRID
        mainGrid[21][25] = TileType.TERMINAL

        // Ladders DOWN
        mainGrid[5][5] = TileType.LADDER_DOWN
        mainGrid[2][25] = TileType.LADDER_DOWN
        mainGrid[30][25] = TileType.LADDER_DOWN

        // Ladders UP
        mainGrid[4][8] = TileType.LADDER_UP
        mainGrid[25][4] = TileType.LADDER_UP
        mainGrid[25][25] = TileType.LADDER_UP
        mainGrid[15][15] = TileType.LADDER_UP

        mainGrid[16][16] = TileType.BARREL_EXPLOSIVE
        mainGrid[25][15] = TileType.BARREL_EXPLOSIVE
        mainGrid[15][25] = TileType.BARREL_EXPLOSIVE

        gameLevels[1] = GameLevelMap(1, "Z=1 MAIN STREET", size, size, mainGrid)

        // Z=2 Mezzanine
        val mezGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x in 3..9 && y in 3..9) {
                    mezGrid[x][y] = TileType.FLOOR
                }
                if (x in 22..29 && y in 3..10) {
                    mezGrid[x][y] = TileType.FLOOR
                }
                if (x in 22..29 && y in 22..29) {
                    mezGrid[x][y] = TileType.FLOOR
                }
                if (y == 6 && x in 10..21) {
                    mezGrid[x][y] = TileType.GRID_ROAD
                }
                if (x == 15 && y in 7..14) {
                    mezGrid[x][y] = TileType.GRID_ROAD
                }
            }
        }
        
        mezGrid[4][8] = TileType.LADDER_DOWN
        mezGrid[25][4] = TileType.LADDER_DOWN
        mezGrid[25][25] = TileType.LADDER_DOWN
        mezGrid[15][15] = TileType.LADDER_DOWN

        mezGrid[6][6] = TileType.LADDER_UP

        mezGrid[15][12] = TileType.LASER_GRID
        mezGrid[14][12] = TileType.TERMINAL

        mezGrid[5][5] = TileType.BARREL_EXPLOSIVE

        gameLevels[2] = GameLevelMap(2, "Z=2 MEZZANINE DECK", size, size, mezGrid)

        // Z=3 Sky Gateway
        val skyGrid = Array(size) { Array(size) { TileType.WALL } }
        for (x in 0 until size) {
            for (y in 0 until size) {
                if (x in 5..30 && y in 5..30) {
                    skyGrid[x][y] = TileType.FLOOR
                }
                if ((x in 7..9 && y in 5..6) || (x in 26..28 && y in 5..6)) {
                    skyGrid[x][y] = TileType.EMPTY
                }
            }
        }
        skyGrid[6][6] = TileType.LADDER_DOWN

        skyGrid[17][17] = TileType.EXIT_PORTAL

        skyGrid[17][16] = TileType.LASER_GRID
        skyGrid[17][18] = TileType.LASER_GRID

        skyGrid[12][12] = TileType.TERMINAL
        skyGrid[22][22] = TileType.TERMINAL

        skyGrid[22][8] = TileType.BARREL_EXPLOSIVE
        skyGrid[8][22] = TileType.BARREL_EXPLOSIVE

        gameLevels[3] = GameLevelMap(3, "Z=3 SKY PORTAL APEX", size, size, skyGrid)
    }

    fun updateExploration(
        playerPos: Point3D,
        currentZ: Int,
        exploredTiles: Map<Int, Set<String>>,
        lastExploredGridX: Int,
        lastExploredGridY: Int,
        lastExploredZ: Int,
        force: Boolean = false
    ): Triple<Map<Int, Set<String>>, Boolean, Triple<Int, Int, Int>> {
        val px = playerPos.x.toInt()
        val py = playerPos.y.toInt()

        if (!force && px == lastExploredGridX && py == lastExploredGridY && currentZ == lastExploredZ) {
            return Triple(exploredTiles, false, Triple(lastExploredGridX, lastExploredGridY, lastExploredZ))
        }

        val radius = 5
        val currentSet = exploredTiles[currentZ]?.toMutableSet() ?: mutableSetOf()
        var changed = false

        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                if (dx * dx + dy * dy <= radius * radius) {
                    val tx = px + dx
                    val ty = py + dy
                    if (tx in 0 until mapSize && ty in 0 until mapSize) {
                        if (currentSet.add("$tx,$ty")) {
                            changed = true
                        }
                    }
                }
            }
        }

        if (changed) {
            val updatedMap = exploredTiles.toMutableMap()
            updatedMap[currentZ] = currentSet
            return Triple(updatedMap, true, Triple(px, py, currentZ))
        }

        return Triple(exploredTiles, false, Triple(px, py, currentZ))
    }
}
