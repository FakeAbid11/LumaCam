package com.lumacam.app.data

import android.content.Context
import android.content.SharedPreferences
import com.lumacam.feature.ai.local.ActiveLocalModel
import com.lumacam.feature.ai.local.LocalModelCatalog
import com.lumacam.feature.ai.local.LocalModelSpec

/**
 * Single source of truth for local model state (PRD §4 Tier 3): the curated
 * catalog, which models are downloaded, and which one is the active selection.
 *
 * The active-model id is persisted in plain [SharedPreferences] (it's not a secret,
 * unlike Cloud AI keys). Download files live in [LocalModelStorage].
 */
class LocalModelRepository(
    context: Context,
    val storage: LocalModelStorage
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The curated list of supported models. */
    val catalog: List<LocalModelSpec> = LocalModelCatalog.models

    /** Id of the currently-selected model, or null if none selected. */
    var activeModelId: String?
        get() = prefs.getString(KEY_ACTIVE, null)
        set(value) = prefs.edit().apply {
            if (value == null) remove(KEY_ACTIVE) else putString(KEY_ACTIVE, value)
        }.apply()

    fun isDownloaded(spec: LocalModelSpec): Boolean = storage.isDownloaded(spec)

    fun availableBytes(): Long = storage.availableBytes()

    /** Selects [spec] as active. No-op guard: only meaningful if downloaded. */
    fun select(spec: LocalModelSpec) {
        activeModelId = spec.id
    }

    /**
     * Deletes [spec]'s files. If it was the active model, clears the selection so
     * Local AI mode correctly reports "no model" instead of pointing at a gone file.
     */
    fun delete(spec: LocalModelSpec): Boolean {
        val removed = storage.delete(spec)
        if (activeModelId == spec.id) activeModelId = null
        return removed
    }

    /**
     * The active model as an [ActiveLocalModel] for the provider — only when a
     * model is selected AND its file is present on disk; otherwise null.
     */
    fun activeModel(): ActiveLocalModel? {
        val spec = LocalModelCatalog.findById(activeModelId) ?: return null
        if (!storage.isDownloaded(spec)) return null
        return ActiveLocalModel(spec = spec, filePath = storage.fileFor(spec).absolutePath)
    }

    private companion object {
        const val PREFS_NAME = "luma_local_ai"
        const val KEY_ACTIVE = "active_model_id"
    }
}
