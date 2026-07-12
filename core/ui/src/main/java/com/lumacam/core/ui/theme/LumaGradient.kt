package com.lumacam.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared gradient brushes for the Doka-Cam-inspired visual language. Used
 * specifically for AI-active states (shutter ring, subject/aim ring, AI-active
 * icon tint) and the translucent scrim behind guidance text bubbles.
 */
object LumaGradient {
    /** AI-active accent sweep: blue -> pink -> gold. */
    val aiAccent: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF3A6FF8), // blue
            Color(0xFFFF4D8D), // pink
            Color(0xFFFFC36B) // gold
        )
    )

    /** Dark translucent scrim with a subtle blue/violet tint (not flat black). */
    val scrim: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xE6121218),
            Color(0x991A1530)
        )
    )
}
