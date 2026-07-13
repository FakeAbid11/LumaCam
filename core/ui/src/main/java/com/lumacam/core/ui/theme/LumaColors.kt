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

    /** Standard semi-transparent black fill for floating camera chrome pills. */
    val chromePill = Color(0x66000000)

    /** Lighter semi-transparent fill for low-emphasis camera chrome chips. */
    val chromePillSoft = Color(0x33000000)

    /** Faint outline used on the floating preview card and gallery thumbnail. */
    val chromeBorder = Color(0x22FFFFFF)

    /** Soft semi-transparent fill (e.g. gallery thumbnail background). */
    val chromeScrim = Color(0x33FFFFFF)

    /** Medium semi-transparent fill (e.g. gallery thumbnail border). */
    val chromeScrimMedium = Color(0x55FFFFFF)

    /** Muted foreground for disabled/inactive chrome icons. */
    val chromeMuted = Color(0x88FFFFFF)

    /** Inactive track color for the zoom slider. */
    val sliderTrackInactive = Color(0x66FFFFFF)

    /** Red dot used by the recording indicator. */
    val recIndicator = Color(0xFFFF3B30)
}
