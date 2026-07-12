package com.lumacam.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens for the camera-facing UI. Groups the existing loose
 * palette values so in-scope screens reference intent rather than raw hex.
 *
 * The base accent [LumaAccent] intentionally remains a top-level `val` in
 * [Color.kt] for backward compatibility with the Settings / Cloud AI / Local AI /
 * Benchmark screens, which are intentionally kept on standard Material 3.
 */
object LumaColors {
    /** Primary AI accent (electric blue). Mirrors [LumaAccent]. */
    val accent = LumaAccent

    /** Foreground drawn on top of [accent] (e.g. action-button label). */
    val onAccent = Color.Black

    /** Solid black chrome behind the floating preview card. */
    val chromeBlack = Color.Black

    /** Horizon/level line color when the frame is level. */
    val horizonLevel = Color.White

    /** Horizon/level line color while off-level (warning yellow). */
    val horizonOffLevel = Color(0xFFFFD60A)

    /** Fallback solid scrim (kept for non-Brush contexts). */
    val scrimSolid = Color(0xCC121218)
}
