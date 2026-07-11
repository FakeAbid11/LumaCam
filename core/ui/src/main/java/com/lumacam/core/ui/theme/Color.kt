package com.lumacam.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * LumaCam palette (PRD §3 design direction): a disciplined black / white / gray
 * base with a single accent reserved for AI-related states. Ordinary chrome must
 * stay neutral so the accent reads as "this is AI".
 */

// Neutrals
val LumaBlack = Color(0xFF000000)
val LumaSurface = Color(0xFF0E0E12)
val LumaSurfaceElevated = Color(0xFF16161C)
val LumaSurfaceHigh = Color(0xFF202028)
val LumaScrim = Color(0x99000000)

val LumaWhite = Color(0xFFFFFFFF)
val LumaGray100 = Color(0xFFECECF0)
val LumaGray300 = Color(0xFFC7C7D1)
val LumaGray500 = Color(0xFF8A8A96)
val LumaGray700 = Color(0xFF44444C)

// Single AI accent — reserved for AI states only (Smart indicator, hints, AI sheet).
val LumaAccent = Color(0xFF3A6FF8)
val LumaAccentMuted = Color(0xFF24356B)

// Light-theme neutrals (light theme optional per PRD).
val LumaLightBackground = Color(0xFFF8F8FC)
val LumaLightSurface = Color(0xFFFFFFFF)
val LumaLightOnSurface = Color(0xFF16161C)
