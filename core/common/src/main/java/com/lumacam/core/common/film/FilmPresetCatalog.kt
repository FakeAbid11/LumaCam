package com.lumacam.core.common.film

/**
 * The curated set of film-simulation presets (LumaCam Film Camera Engine, per PRD).
 * Pure data — the GL renderer and the preset picker both consume this catalog.
 *
 * Parameters are hand-tuned starting points (all normalized `0f..1f`, temperature
 * `0.5f` = neutral) and are meant to be adjusted after on-device review.
 */
object FilmPresetCatalog {

    /** No effect — the camera's native look (identity passthrough). */
    val Original: FilmPreset = FilmPreset(
        id = FilmPreset.ORIGINAL_ID,
        name = "Original",
        description = "No film effect — your camera's native look.",
        colorMatrix = FilmPreset.identityMatrix(),
        grainIntensity = 0f,
        halationIntensity = 0f,
        bloomIntensity = 0f,
        vignetteIntensity = 0f,
        chromaticAberration = 0f,
        scanlineIntensity = 0f,
        softness = 0f,
        temperature = 0.5f
    )

    /** Disposable: flat colors, mild grain, slight vignette, warm cast. */
    val Disposable: FilmPreset = FilmPreset(
        id = "disposable",
        name = "Disposable",
        description = "Flat colors, warm cast, mild grain — a single-use film look.",
        colorMatrix = FilmColorMatrix.compose(
            FilmColorMatrix.saturation(0.9f),
            FilmColorMatrix.contrast(0.9f),
            FilmColorMatrix.channelScale(rGain = 1.05f, bGain = 0.95f, rOffset = 6f)
        ),
        grainIntensity = 0.35f,
        halationIntensity = 0.15f,
        bloomIntensity = 0.1f,
        vignetteIntensity = 0.3f,
        chromaticAberration = 0f,
        scanlineIntensity = 0f,
        softness = 0.1f,
        temperature = 0.62f
    )

    /** CCD: early-2000s digital camera — slightly oversaturated, cooler shadows. */
    val Ccd: FilmPreset = FilmPreset(
        id = "ccd",
        name = "CCD",
        description = "Early-2000s digicam — punchy, oversaturated, cool shadows.",
        colorMatrix = FilmColorMatrix.compose(
            FilmColorMatrix.saturation(1.2f),
            FilmColorMatrix.contrast(1.1f),
            FilmColorMatrix.channelScale(bGain = 1.05f, bOffset = 4f)
        ),
        grainIntensity = 0.1f,
        halationIntensity = 0.05f,
        bloomIntensity = 0.15f,
        vignetteIntensity = 0.15f,
        chromaticAberration = 0.05f,
        scanlineIntensity = 0f,
        softness = 0f,
        temperature = 0.44f
    )

    /** MiniDV: camcorder look — soft focus, mild interlacing, muted color. */
    val MiniDv: FilmPreset = FilmPreset(
        id = "minidv",
        name = "MiniDV",
        description = "Camcorder feel — soft focus, muted color, mild interlacing.",
        colorMatrix = FilmColorMatrix.compose(
            FilmColorMatrix.saturation(0.8f),
            FilmColorMatrix.contrast(0.95f)
        ),
        grainIntensity = 0.15f,
        halationIntensity = 0.05f,
        bloomIntensity = 0.1f,
        vignetteIntensity = 0.1f,
        chromaticAberration = 0.05f,
        scanlineIntensity = 0.2f,
        softness = 0.5f,
        temperature = 0.5f
    )

    /** VHS: heavy grain/noise, color bleed, chromatic aberration, scanlines. */
    val Vhs: FilmPreset = FilmPreset(
        id = "vhs",
        name = "VHS",
        description = "Heavy grain, color bleed, chroma fringing and scanlines.",
        colorMatrix = FilmColorMatrix.compose(
            FilmColorMatrix.saturation(1.1f),
            FilmColorMatrix.contrast(0.9f),
            FilmColorMatrix.channelScale(rGain = 1.04f, gGain = 0.99f)
        ),
        grainIntensity = 0.7f,
        halationIntensity = 0.25f,
        bloomIntensity = 0.2f,
        vignetteIntensity = 0.35f,
        chromaticAberration = 0.5f,
        scanlineIntensity = 0.6f,
        softness = 0.4f,
        temperature = 0.52f
    )

    /** All presets in display order (Original first). */
    val presets: List<FilmPreset> = listOf(Original, Disposable, Ccd, MiniDv, Vhs)

    /** The default selection when nothing is persisted. */
    val default: FilmPreset get() = Original

    /** Returns the preset with the given [id], or [default] if unknown/null. */
    fun byId(id: String?): FilmPreset = presets.firstOrNull { it.id == id } ?: default
}
