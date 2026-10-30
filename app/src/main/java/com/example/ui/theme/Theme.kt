package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VrDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = AmberNeon,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF633F00),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = CardboardBrown,
    onTertiary = Color.White,
    background = VrBackground,
    onBackground = TextPrimary,
    surface = VrSurface,
    onSurface = TextPrimary,
    surfaceVariant = VrSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = VrBorder
)

private val VrLightColorScheme = VrDarkColorScheme // VR is best in dark mode to prevent lens glare

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent VR aesthetics
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VrDarkColorScheme,
        typography = Typography,
        content = content
    )
}
