package com.lumacam.app.data

import android.content.Context
import com.lumacam.feature.ai.local.LocalModelSpec
import java.io.File

/**
 * Manages on-disk model files (PRD §4 Tier 3). Files live in the app-specific
 * external files directory (`Context.getExternalFilesDir`), which is:
 *  - not bundled in the APK,
 *  - private to this app (other apps can't read it),
 *  - automatically removed on uninstall.
 *
 * A download-in-progress uses a temporary `.part` file so a half-written model is
 * never mistaken for a complete one, and partial bytes can be resumed.
 */
class LocalModelStorage(private val context: Context) {

    /** The directory holding downloaded models, created on demand. */
    fun modelsDir(): File {
        val dir = File(context.getExternalFilesDir(null), MODELS_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Final path for a fully-downloaded [spec]. */
    fun fileFor(spec: LocalModelSpec): File = File(modelsDir(), spec.fileName)

    /** Temporary path used while [spec] is downloading. */
    fun partialFileFor(spec: LocalModelSpec): File = File(modelsDir(), spec.fileName + PART_SUFFIX)

    /** True when [spec] is fully downloaded and present. */
    fun isDownloaded(spec: LocalModelSpec): Boolean = fileFor(spec).let { it.exists() && it.length() > 0 }

    /** Size on disk of a downloaded [spec], or 0 when absent. */
    fun sizeOnDisk(spec: LocalModelSpec): Long = fileFor(spec).takeIf { it.exists() }?.length() ?: 0L

    /** Bytes already written to the partial file for [spec] (for resume), or 0. */
    fun partialBytes(spec: LocalModelSpec): Long =
        partialFileFor(spec).takeIf { it.exists() }?.length() ?: 0L

    /** Deletes the final and partial files for [spec]; returns true if anything was removed. */
    fun delete(spec: LocalModelSpec): Boolean {
        val a = fileFor(spec).let { it.exists() && it.delete() }
        val b = partialFileFor(spec).let { it.exists() && it.delete() }
        return a || b
    }

    /** Free space (bytes) available in the models directory's volume. */
    fun availableBytes(): Long = runCatching { modelsDir().usableSpace }.getOrDefault(0L)
}

private const val MODELS_SUBDIR = "models"
private const val PART_SUFFIX = ".part"
