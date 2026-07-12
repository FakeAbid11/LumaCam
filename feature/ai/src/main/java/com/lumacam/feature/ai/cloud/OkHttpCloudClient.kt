package com.lumacam.feature.ai.cloud

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume

/**
 * OkHttp-backed [CloudHttpClient]. Applies a per-call timeout so cloud requests
 * never hang, and translates transport exceptions into [CloudHttpException] kinds
 * the provider can map to calm user messages.
 */
class OkHttpCloudClient(
    private val baseClient: OkHttpClient = OkHttpClient()
) : CloudHttpClient {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMillis: Long
    ): CloudHttpResponse {
        val client = baseClient.newBuilder()
            .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .build()

        val requestBuilder = Request.Builder()
            .url(url)
            .post(body.toRequestBody(jsonMedia))
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val call = client.newCall(requestBuilder.build())

        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { runCatching { call.cancel() } }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val text = runCatching { it.body?.string() }.getOrNull().orEmpty()
                        cont.resume(CloudHttpResponse(it.code, text))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    val kind = when (e) {
                        is SocketTimeoutException -> CloudHttpException.Kind.TIMEOUT
                        is UnknownHostException -> CloudHttpException.Kind.NO_NETWORK
                        else -> CloudHttpException.Kind.UNREACHABLE
                    }
                    cont.resumeWithException(CloudHttpException(kind, e))
                }
            })
        }
    }
}
