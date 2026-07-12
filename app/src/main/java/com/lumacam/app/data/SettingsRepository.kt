package com.lumacam.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val CLOUD_AI = booleanPreferencesKey("cloud_ai")
        val FILM_PRESET = stringPreferencesKey("film_preset")
        val FILM_PREVIEW_FILTER = booleanPreferencesKey("film_preview_filter")
    }

    val privacyMode: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PRIVACY_MODE] ?: false }

    val cloudAiEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.CLOUD_AI] ?: false }

    /** Selected film preset id, or null when nothing has been chosen yet. */
    val filmPresetId: Flow<String?> =
        context.dataStore.data.map { it[Keys.FILM_PRESET] }

    /** Live preview filtering; null when unset so a device-tier default can apply. */
    val filmPreviewFilter: Flow<Boolean?> =
        context.dataStore.data.map { it[Keys.FILM_PREVIEW_FILTER] }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_MODE] = enabled }
    }

    suspend fun setCloudAi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOUD_AI] = enabled }
    }

    suspend fun setFilmPreset(id: String) {
        context.dataStore.edit { it[Keys.FILM_PRESET] = id }
    }

    suspend fun setFilmPreviewFilter(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FILM_PREVIEW_FILTER] = enabled }
    }
}
