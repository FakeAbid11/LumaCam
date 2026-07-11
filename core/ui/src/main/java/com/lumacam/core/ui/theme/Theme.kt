package com.lumacam.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * LumaCam theme. Dark is the default premium experience (PRD: full dark theme;
 * light optional). The accent is intentionally NOT wired into the generic M3
 * [androidx.compose.material3.ColorScheme] roles so it stays reserved for AI
 * states; AI surfaces reference [LumaAccent] directly.
 */
private val LumaDarkColorScheme = darkColorScheme(
    primary = LumaWhite,
    onPrimary = LumaBlack,
    secondary = LumaGray300,
    onSecondary = LumaBlack,
    tertiary = LumaGray300,
    background = LumaBlack,
    onBackground = LumaWhite,
    surface = LumaSurface,
    onSurface = LumaWhite,
    surfaceVariant = LumaSurfaceElevated,
    onSurfaceVariant = LumaGray300,
    outline = LumaGray700
)

private val LumaLightColorScheme = lightColorScheme(
    primary = LumaLightOnSurface,
    onPrimary = LumaWhite,
    secondary = LumaGray500,
    background = LumaLightBackground,
    onBackground = LumaLightOnSurface,
    surface = LumaLightSurface,
    onSurface = LumaLightOnSurface
)

@Composable
fun LumaCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LumaDarkColorScheme else LumaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumaTypography,
        shapes = LumaShapes,
        content = content
    )
}
