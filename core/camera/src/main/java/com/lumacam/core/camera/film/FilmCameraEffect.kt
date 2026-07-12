package com.lumacam.core.camera.film

import androidx.camera.core.CameraEffect
import androidx.core.util.Consumer

/**
 * Binds the [FilmSurfaceProcessor] into the CameraX pipeline as a [CameraEffect].
 * The [targets] bitmask selects which streams are filtered — always
 * [CameraEffect.VIDEO_CAPTURE] (baked video), plus [CameraEffect.PREVIEW] when live
 * preview filtering is enabled. Still photos are baked separately by
 * [FilmPhotoBaker], so [CameraEffect.IMAGE_CAPTURE] is intentionally not targeted.
 */
internal class FilmCameraEffect(
    processor: FilmSurfaceProcessor,
    targets: Int,
    errorListener: Consumer<Throwable>
) : CameraEffect(targets, processor.glExecutor, processor, errorListener)
