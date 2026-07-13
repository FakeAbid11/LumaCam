package com.lumacam.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.lumacam.feature.ai.cloud.CloudAiConfig
import com.lumacam.feature.ai.cloud.CloudProviderType

/**
 * Encrypted-at-rest storage for Cloud AI credentials (PRD §6 Settings). API keys
 * are entered by the user at runtime and stored via [EncryptedSharedPreferences] —
 * never in plaintext, never bundled in the app or read from build config.
 *
 * Per-provider keys/base-URLs/models are kept separately so a user can configure
 * several providers and switch between them.
 */
class CloudAiKeyStore(context: Context) : CloudAiCredentials {

    private val prefs: SharedPreferences = run {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override var selectedProvider: CloudProviderType
        get() = prefs.getString(KEY_SELECTED, null)
            ?.let { runCatching { CloudProviderType.valueOf(it) }.getOrNull() }
            ?: CloudProviderType.GEMINI
        set(value) = prefs.edit().putString(KEY_SELECTED, value.name).apply()

    fun getApiKey(type: CloudProviderType): String =
        prefs.getString(keyFor(PREFIX_KEY, type), "").orEmpty()

    fun setApiKey(type: CloudProviderType, value: String) {
        prefs.edit().putString(keyFor(PREFIX_KEY, type), value.trim()).apply()
    }

    fun getBaseUrl(type: CloudProviderType): String =
        prefs.getString(keyFor(PREFIX_BASE_URL, type), "").orEmpty()

    fun setBaseUrl(type: CloudProviderType, value: String) {
        prefs.edit().putString(keyFor(PREFIX_BASE_URL, type), value.trim()).apply()
    }

    fun getModel(type: CloudProviderType): String =
        prefs.getString(keyFor(PREFIX_MODEL, type), "").orEmpty()

    fun setModel(type: CloudProviderType, value: String) {
        prefs.edit().putString(keyFor(PREFIX_MODEL, type), value.trim()).apply()
    }

    override fun hasApiKey(type: CloudProviderType): Boolean = getApiKey(type).isNotBlank()

    /** Assembles a [CloudAiConfig] for [type] from the stored values. */
    fun buildConfig(type: CloudProviderType): CloudAiConfig = CloudAiConfig(
        type = type,
        apiKey = getApiKey(type),
        baseUrl = getBaseUrl(type),
        model = getModel(type)
    )

    private fun keyFor(prefix: String, type: CloudProviderType) = "$prefix${type.name}"

    private companion object {
        const val FILE_NAME = "luma_cloud_ai_secure"
        const val KEY_SELECTED = "selected_provider"
        const val PREFIX_KEY = "api_key_"
        const val PREFIX_BASE_URL = "base_url_"
        const val PREFIX_MODEL = "model_"
    }
}
