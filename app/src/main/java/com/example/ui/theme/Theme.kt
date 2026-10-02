package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightModernColorScheme = lightColorScheme(
    primary = ModernPrimary,
    onPrimary = ModernOnPrimary,
    primaryContainer = ModernPrimaryContainer,
    onPrimaryContainer = ModernOnPrimaryContainer,
    secondary = ModernSecondary,
    onSecondary = ModernOnSecondary,
    secondaryContainer = ModernSecondaryContainer,
    onSecondaryContainer = ModernOnSecondaryContainer,
    tertiary = ModernTertiary,
    onTertiary = ModernOnTertiary,
    tertiaryContainer = ModernTertiaryContainer,
    onTertiaryContainer = ModernOnTertiaryContainer,
    background = ModernBackground,
    onBackground = ModernOnBackground,
    surface = ModernSurface,
    onSurface = ModernOnSurface,
    surfaceVariant = ModernSurfaceVariant,
    onSurfaceVariant = ModernOnSurfaceVariant,
    outline = ModernOutline,
    outlineVariant = ModernOutlineVariant,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F5F9)
)

private val DarkModernColorScheme = darkColorScheme(
    primary = ModernDarkPrimary,
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D69),
    onPrimaryContainer = Color(0xFFC2E8FF),
    background = ModernDarkBackground,
    surface = ModernDarkSurface,
    surfaceVariant = ModernDarkSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Emphasize bright and modern aesthetic
    dynamicColor: Boolean = false, // Keep intentional bright theme consistent
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkModernColorScheme else LightModernColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
