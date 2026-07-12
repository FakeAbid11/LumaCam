package com.lumacam.feature.ai.local

/**
 * Every way a Local AI (on-device model) analysis can fail, each carrying a calm,
 * specific, user-facing [message]. Mirrors the Cloud AI error model: the provider
 * surfaces these instead of throwing, so the app never crashes — even when the
 * device runs out of memory mid-inference.
 */
sealed class LocalAiError(val message: String) {

    /** No local model has been selected as active yet. */
    data object NoModelSelected : LocalAiError(
        "No on-device model selected. Download and select one in Settings to use Local AI."
    )

    /** A model is selected, but its file isn't present on disk. */
    data class ModelNotDownloaded(val modelName: String) : LocalAiError(
        "\"$modelName\" isn't downloaded yet. Download it in Settings, then try again."
    )

    /** Not enough free storage to download the model. */
    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) :
        LocalAiError(
            "Not enough free space to download this model. Free up some storage and try again."
        )

    /** The device ran out of memory while loading or running the model. */
    data object OutOfMemory : LocalAiError(
        "This device ran low on memory for that model. Try a smaller model in Settings."
    )

    /** The on-device inference runtime isn't available in this build. */
    data object RuntimeUnavailable : LocalAiError(
        "On-device inference isn't available in this build yet. Use Cloud AI or Luma Vision."
    )

    /** Inference ran but produced an unusable or unparseable result. */
    data class InferenceFailed(val detail: String) : LocalAiError(
        "The on-device model couldn't complete that analysis. Try again or pick another model."
    )

    /** A genuinely unknown failure; [detail] aids debugging without alarming users. */
    data class Unknown(val detail: String) : LocalAiError(
        "Something interrupted the on-device analysis. Please try again."
    )
}
