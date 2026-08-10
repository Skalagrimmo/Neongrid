package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Custom Extra Accent Colors for the 'NeonGrid' aesthetic.
 */
@Immutable
data class NeonGridExtraColors(
    val neonCyan: Color = CyberNeonCyan,
    val neonMagenta: Color = CyberNeonMagenta,
    val acidGreen: Color = CyberAcidGreen,
    val neonPurple: Color = CyberNeonPurple,
    val neonAmber: Color = CyberNeonAmber,
    val neonRed: Color = CyberNeonRed,
    val gridLine: Color = Color(0x3300F0FF),
    val bgVoid: Color = CyberBgVoid,
    val surfaceContainer: Color = CyberSurfaceContainer
)

val LocalNeonGridExtraColors = staticCompositionLocalOf { NeonGridExtraColors() }

/**
 * Material 3 Dark Color Scheme tailored specifically for the 'NeonGrid' aesthetic.
 */
private val NeonGridDarkColorScheme = darkColorScheme(
    primary = CyberNeonCyan,
    onPrimary = CyberBgVoid,
    primaryContainer = CyberDarkCyan,
    onPrimaryContainer = CyberLightCyan,
    secondary = CyberNeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = CyberDarkMagenta,
    onSecondaryContainer = CyberLightMagenta,
    tertiary = CyberAcidGreen,
    onTertiary = CyberBgVoid,
    tertiaryContainer = CyberDarkAcidGreen,
    onTertiaryContainer = CyberAcidGreen,
    error = CyberNeonRed,
    onError = Color.White,
    background = CyberBgVoid,
    onBackground = CyberTextHigh,
    surface = CyberSurfaceDark,
    onSurface = CyberTextHigh,
    surfaceVariant = CyberSurfaceContainer,
    onSurfaceVariant = CyberTextMedium,
    outline = CyberOutlineNeon,
    outlineVariant = CyberOutlineMuted
)

private val NeonGridLightColorScheme = NeonGridDarkColorScheme // Standardized dark neon-grid theme

/**
 * Custom Material 3 Theme for the NeonGrid application.
 */
@Composable
fun NeonGridTheme(
    darkTheme: Boolean = true, // Default to dark for neon-grid theme
    dynamicColor: Boolean = false, // Disable dynamic color overrides to preserve neon cyberpunk aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NeonGridDarkColorScheme
        else -> NeonGridLightColorScheme
    }

    val extraColors = NeonGridExtraColors()

    CompositionLocalProvider(LocalNeonGridExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Alias object to easily access custom NeonGrid theme tokens from Composables.
 */
object NeonGridTheme {
    val extraColors: NeonGridExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNeonGridExtraColors.current
}

/**
 * Backward compatible wrapper for MyApplicationTheme.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NeonGridTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}


