package com.lumacam.core.camera

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Saves captured media to app-specific external storage
 * (getExternalFilesDir). This requires NO storage permission even on API 24+.
 */
class MediaSaver @Inject constructor(@ApplicationContext private val context: Context) {

    private val photoDir: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "LumaCam")
            .also { it.mkdirs() }

    private val videoDir: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "LumaCam")
            .also { it.mkdirs() }

    fun newPhotoFile(): File = File(photoDir, buildMediaFileName("LumaCam", "jpg", Date()))

    fun newVideoFile(): File = File(videoDir, buildMediaFileName("LumaCam", "mp4", Date()))
}

/** Pure, testable file-name builder (no Android dependencies). */
fun buildMediaFileName(prefix: String, extension: String, date: Date): String {
    val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return "$prefix_${fmt.format(date)}.$extension"
}
