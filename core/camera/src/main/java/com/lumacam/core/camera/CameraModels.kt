package com.lumacam.core.camera

import androidx.camera.core.ImageCapture

enum class LensFacing {
    BACK, FRONT
}

object FlashMode {
    const val OFF = ImageCapture.FLASH_MODE_OFF
    const val ON = ImageCapture.FLASH_MODE_ON
    const val AUTO = ImageCapture.FLASH_MODE_AUTO
}

data class ZoomState(
    val zoomRatio: Float,
    val minZoomRatio: Float,
    val maxZoomRatio: Float
)
