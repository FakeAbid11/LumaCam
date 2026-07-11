package com.lumacam.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "luma_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val CLOUD_AI = booleanPreferencesKey("cloud_ai")
    }

    val privacyMode: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.PRIVACY_MODE] ?: false }

    val cloudAiEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.CLOUD_AI] ?: false }

    suspend fun setPrivacyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PRIVACY_MODE] = enabled }
    }

    suspend fun setCloudAi(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CLOUD_AI] = enabled }
    }
}
