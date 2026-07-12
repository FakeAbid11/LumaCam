package com.lumacam.core.camera

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Saves captured media to the system gallery via the MediaStore API (scoped
 * storage, API 29+). Photos and videos are inserted as pending rows under
 * `Pictures/LumaCam` / `Movies/LumaCam`; [IS_PENDING] is cleared once the write
 * finishes so the system Gallery indexes them automatically — no storage
 * permission and no reliance on the app-private, never-scanned
 * `getExternalFilesDir()` directory.
 *
 * Pre-API 29 devices use the same MediaStore insert path (the legacy insert still
 * works there); `RELATIVE_PATH`/`IS_PENDING` are simply omitted on those versions.
 */
class MediaSaver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val resolver = context.contentResolver

    /** A private temp file for the actual CameraX photo write (before publish). */
    fun newPhotoTempFile(): File =
        File(context.cacheDir, buildMediaFileName("LumaCam", "jpg", Date()))

    /** Inserts a pending photo row and returns its content [Uri]. */
    fun createPhotoUri(): Uri =
        insertMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_PICTURES, "image/jpeg", "jpg").first

    /** Inserts a pending video row and returns its [Uri] + the [ContentValues] to write with. */
    fun createVideoUri(): Pair<Uri, ContentValues> =
        insertMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Environment.DIRECTORY_MOVIES, "video/mp4", "mp4")

    /**
     * Copies a finished temp photo file into its MediaStore [uri], clears the
     * pending flag, and deletes the temp file.
     */
    fun publishPhoto(uri: Uri, tempFile: File) {
        try {
            resolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { it.copyTo(out) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "publish photo failed", e)
        }
        finalize(uri)
        runCatching { tempFile.delete() }
    }

    /** Clears [MediaStore.MediaColumns.IS_PENDING] once a write has completed. */
    fun finalize(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        try {
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "finalize failed", e)
        }
    }

    /** Removes a pending row on a failed/aborted capture. */
    fun discard(uri: Uri) {
        try {
            resolver.delete(uri, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "discard failed", e)
        }
    }

    private fun insertMedia(
        collection: Uri,
        dir: String,
        mime: String,
        ext: String
    ): Pair<Uri, ContentValues> {
        val name = buildMediaFileName("LumaCam", ext, Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$dir/LumaCam")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore insert failed for $collection")
        return uri to values
    }

    private companion object {
        const val TAG = "MediaSaver"
    }
}

/** Pure, testable file-name builder (no Android dependencies). */
fun buildMediaFileName(prefix: String, extension: String, date: Date): String {
    val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return "${prefix}_${fmt.format(date)}.$extension"
}
