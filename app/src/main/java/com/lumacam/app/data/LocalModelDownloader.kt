package com.lumacam.app.data

import com.lumacam.feature.ai.local.DownloadState
import com.lumacam.feature.ai.local.LocalModelSpec
import com.lumacam.feature.ai.local.StorageChecker
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Downloads a model file to app-specific storage (PRD §4 Tier 3), emitting a
 * [DownloadState] stream the UI can render. Behaviour:
 *  - checks free storage (with headroom) before starting;
 *  - streams to a `.part` file and renames on success (never leaves a half file
 *    looking complete);
 *  - resumes from existing partial bytes via an HTTP `Range` request when the
 *    server supports it;
 *  - cancels cleanly when the collecting coroutine is cancelled.
 *
 * Uses [HttpURLConnection] to avoid pulling extra HTTP deps into `:app`.
 */
class LocalModelDownloader(private val storage: LocalModelStorage) {

    fun download(spec: LocalModelSpec): Flow<DownloadState> = flow {
        emit(DownloadState.CheckingSpace)

        val partial = storage.partialFileFor(spec)
        val existingBytes = if (partial.exists()) partial.length() else 0L

        val space = StorageChecker.check(
            modelSizeBytes = (spec.sizeBytes - existingBytes).coerceAtLeast(0L),
            availableBytes = storage.availableBytes()
        )
        if (!space.hasEnoughSpace) {
            emit(DownloadState.Failed("Not enough free storage. Free up space and try again."))
            return@flow
        }

        try {
            downloadInto(this, spec, partial, existingBytes)
        } catch (io: IOException) {
            emit(DownloadState.Failed(io.message ?: "Network error during download."))
            return@flow
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadInto(
        collector: FlowCollector<DownloadState>,
        spec: LocalModelSpec,
        partial: File,
        existingBytes: Long
    ) {
        val connection = (URL(spec.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            requestMethod = "GET"
            if (existingBytes > 0) setRequestProperty("Range", "bytes=$existingBytes-")
        }

        try {
            connection.connect()
            val code = connection.responseCode
            val resuming = code == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0
            if (code != HttpURLConnection.HTTP_OK && !resuming) {
                collector.emit(DownloadState.Failed("Server returned HTTP $code."))
                return
            }

            val startBytes = if (resuming) existingBytes else 0L
            // If the server ignored our Range, restart from scratch.
            val appendMode = resuming
            val reported = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
            val totalBytes = if (reported > 0) startBytes + reported else spec.sizeBytes

            var downloaded = startBytes
            collector.emit(DownloadState.Downloading(downloaded, totalBytes))

            connection.inputStream.use { input ->
                java.io.FileOutputStream(partial, appendMode).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var lastEmit = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (downloaded - lastEmit >= EMIT_INTERVAL_BYTES) {
                            lastEmit = downloaded
                            collector.emit(DownloadState.Downloading(downloaded, totalBytes))
                        }
                    }
                    output.flush()
                }
            }

            val finalFile = storage.fileFor(spec)
            if (finalFile.exists()) finalFile.delete()
            if (!partial.renameTo(finalFile)) {
                collector.emit(DownloadState.Failed("Couldn't finalize the downloaded file."))
                return
            }
            collector.emit(DownloadState.Completed)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 30_000
        const val BUFFER_SIZE = 64 * 1024
        const val EMIT_INTERVAL_BYTES = 512L * 1024
    }
}
