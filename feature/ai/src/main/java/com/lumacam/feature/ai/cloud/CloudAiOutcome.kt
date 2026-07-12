package com.lumacam.feature.ai.cloud

import com.lumacam.feature.ai.CompositionResult

/**
 * Result of a Cloud AI call: either a normalized [CompositionResult] or a typed
 * [CloudAiError]. Providers always return this and never throw.
 */
sealed interface CloudAiOutcome {
    data class Success(val result: CompositionResult) : CloudAiOutcome
    data class Failure(val error: CloudAiError) : CloudAiOutcome
}

/**
 * Lightweight outcome for the Settings "Test Connection" action — it only needs to
 * know whether the credentials/endpoint work, plus a message to display.
 */
sealed interface ConnectionTestResult {
    data object Success : ConnectionTestResult
    data class Failure(val error: CloudAiError) : ConnectionTestResult
}
