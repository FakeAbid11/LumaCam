package com.lumacam.app.data

import com.lumacam.feature.ai.cloud.CloudProviderType

/**
 * Minimal read-only view of Cloud AI credential state needed to decide whether
 * Cloud AI can be offered/selected. Extracted from [CloudAiKeyStore] so the
 * camera/AI view models can depend on this interface and stay unit-testable
 * without constructing [androidx.security.crypto.EncryptedSharedPreferences].
 */
interface CloudAiCredentials {
    val selectedProvider: CloudProviderType
    fun hasApiKey(type: CloudProviderType = selectedProvider): Boolean
}
