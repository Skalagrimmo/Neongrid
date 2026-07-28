package com.example.model

import androidx.compose.ui.graphics.Color

enum class GbcPalette(
    val displayName: String,
    val bgDark: Color,
    val floorPrimary: Color,
    val floorSecondary: Color,
    val wallPrimary: Color,
    val wallTop: Color,
    val wallAccent: Color,
    val playerBody: Color,
    val playerVisor: Color,
    val enemyPatrol: Color,
    val enemySuspicious: Color,
    val enemyAlert: Color,
    val terminalColor: Color,
    val gridOutline: Color
) {
    CLASSIC_GBC(
        displayName = "GBC Authentic (Nintendo 1998)",
        bgDark = Color(0xFF0F1B1B),
        floorPrimary = Color(0xFF1B4D3E),
        floorSecondary = Color(0xFF10332B),
        wallPrimary = Color(0xFF152A38),
        wallTop = Color(0xFF223E52),
        wallAccent = Color(0xFF00FF99),
        playerBody = Color(0xFF3366FF),
        playerVisor = Color(0xFFFFB700),
        enemyPatrol = Color(0xFF70FF00),
        enemySuspicious = Color(0xFFFFB700),
        enemyAlert = Color(0xFFFF2244),
        terminalColor = Color(0xFFFFB700),
        gridOutline = Color(0xFF050B0B)
    ),
    CYBER_8BIT(
        displayName = "GBC Neon Cyberpunk",
        bgDark = Color(0xFF08061A),
        floorPrimary = Color(0xFF18103A),
        floorSecondary = Color(0xFF120B2E),
        wallPrimary = Color(0xFF2D124D),
        wallTop = Color(0xFF3F196B),
        wallAccent = Color(0xFFFF00A0),
        playerBody = Color(0xFF00F0FF),
        playerVisor = Color(0xFFFF00CC),
        enemyPatrol = Color(0xFFFFE600),
        enemySuspicious = Color(0xFFFF9900),
        enemyAlert = Color(0xFFFF0055),
        terminalColor = Color(0xFFFF00CC),
        gridOutline = Color(0xFF03010C)
    ),
    POCKET_DMG(
        displayName = "Game Boy Pocket (Olive LCD)",
        bgDark = Color(0xFF0F380F),
        floorPrimary = Color(0xFF306230),
        floorSecondary = Color(0xFF1E4B1E),
        wallPrimary = Color(0xFF0F380F),
        wallTop = Color(0xFF306230),
        wallAccent = Color(0xFF8BAC0F),
        playerBody = Color(0xFF9BBC0F),
        playerVisor = Color(0xFF0F380F),
        enemyPatrol = Color(0xFF8BAC0F),
        enemySuspicious = Color(0xFF8BAC0F),
        enemyAlert = Color(0xFF0F380F),
        terminalColor = Color(0xFF9BBC0F),
        gridOutline = Color(0xFF081E08)
    ),
    RETRO_ARCADE(
        displayName = "GBC Arcade 1999",
        bgDark = Color(0xFF0C1021),
        floorPrimary = Color(0xFF163E4D),
        floorSecondary = Color(0xFF0F2B36),
        wallPrimary = Color(0xFF381B3E),
        wallTop = Color(0xFF4D2554),
        wallAccent = Color(0xFFFFC700),
        playerBody = Color(0xFFFF5555),
        playerVisor = Color(0xFF00E5FF),
        enemyPatrol = Color(0xFF00E5FF),
        enemySuspicious = Color(0xFFFFAA00),
        enemyAlert = Color(0xFFFF1A1A),
        terminalColor = Color(0xFFFFAA00),
        gridOutline = Color(0xFF04060D)
    )
}

data class GbcGraphicsSettings(
    val palette: GbcPalette = GbcPalette.CLASSIC_GBC,
    val isPixelOutlineEnabled: Boolean = true,
    val isPixelDitherEnabled: Boolean = true,
    val isScanlinesEnabled: Boolean = true,
    val isPixelSnappingEnabled: Boolean = false
)
