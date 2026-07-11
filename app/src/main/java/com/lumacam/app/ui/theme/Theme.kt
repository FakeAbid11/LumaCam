package com.lumacam.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LumaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    secondary = Color(0xFFC7C7D1),
    tertiary = Color(0xFFE8C7A1),
    background = Color(0xFF0E0E12),
    surface = Color(0xFF16161C)
)

private val LumaLightColorScheme = lightColorScheme(
    primary = Color(0xFF3B5BA5),
    secondary = Color(0xFF5A5A72),
    tertiary = Color(0xFF8A5A2B),
    background = Color(0xFFF8F8FC),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun LumaCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LumaDarkColorScheme else LumaLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
