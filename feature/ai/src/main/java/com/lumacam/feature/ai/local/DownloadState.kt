package com.lumacam.feature.ai.local

/**
 * The lifecycle of a single model download, modeled as an explicit state machine so
 * the UI can render progress/pause/failure precisely and the logic is JVM-testable
 * without any real network I/O.
 */
sealed interface DownloadState {

    /** Nothing downloading; the model isn't present. */
    data object Idle : DownloadState

    /** Verifying there's enough free storage before starting. */
    data object CheckingSpace : DownloadState

    /** Actively downloading. [totalBytes] is 0 when the server didn't report a size. */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadState {
        /** Progress in [0,1], or null when the total size is unknown. */
        val fraction: Float?
            get() = if (totalBytes > 0) {
                (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
            } else {
                null
            }

        /** Progress as a whole percent [0,100], or null when unknown. */
        val percent: Int? get() = fraction?.let { (it * 100).toInt() }
    }

    /**
     * The download failed; [reason] is a short user-facing explanation. Any bytes
     * already written stay on disk so a retry can resume via an HTTP Range request.
     */
    data class Failed(val reason: String) : DownloadState

    /** The model finished downloading and is ready to use. */
    data object Completed : DownloadState
}

/** Events that drive the [DownloadState] machine. */
sealed interface DownloadEvent {
    data object Start : DownloadEvent
    data object SpaceConfirmed : DownloadEvent
    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadEvent
    data class Fail(val reason: String) : DownloadEvent
    data object Resume : DownloadEvent
    data object Finish : DownloadEvent
    data object Cancel : DownloadEvent
}

/**
 * Pure reducer for the download state machine. Given the [current] state and an
 * [event], returns the next state. Invalid transitions are no-ops (return
 * [current]) so callers can drive it defensively.
 */
object DownloadStateMachine {

    fun reduce(current: DownloadState, event: DownloadEvent): DownloadState = when (event) {
        DownloadEvent.Cancel -> DownloadState.Idle

        DownloadEvent.Start -> when (current) {
            DownloadState.Idle, is DownloadState.Failed -> DownloadState.CheckingSpace
            else -> current
        }

        DownloadEvent.SpaceConfirmed -> when (current) {
            DownloadState.CheckingSpace -> DownloadState.Downloading(0L, 0L)
            else -> current
        }

        DownloadEvent.Resume -> when (current) {
            is DownloadState.Failed -> DownloadState.CheckingSpace
            else -> current
        }

        is DownloadEvent.Progress -> when (current) {
            is DownloadState.Downloading, DownloadState.CheckingSpace ->
                DownloadState.Downloading(event.bytesDownloaded, event.totalBytes)
            else -> current
        }

        is DownloadEvent.Fail -> when (current) {
            is DownloadState.Downloading, DownloadState.CheckingSpace ->
                DownloadState.Failed(event.reason)
            else -> current
        }

        DownloadEvent.Finish -> when (current) {
            is DownloadState.Downloading -> DownloadState.Completed
            else -> current
        }
    }
}
