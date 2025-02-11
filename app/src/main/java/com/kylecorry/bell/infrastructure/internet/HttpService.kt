package com.kylecorry.bell.infrastructure.internet

import android.content.Context
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.kylecorry.luna.coroutines.onIO
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class HttpService(context: Context) {
    private val hurl = object : HurlStack() {
        override fun createConnection(url: java.net.URL?): java.net.HttpURLConnection {
            val connection = super.createConnection(url)
            connection.instanceFollowRedirects = false
            return connection
        }
    }

    private val queue = Volley.newRequestQueue(context, hurl)

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        shouldRetryNotFoundWithoutExtension: Boolean = true
    ): String = onIO {
        val result = suspendCoroutine {
            // Use Volley to make the request
            val request = object : StringRequest(Method.GET, url, { response ->
                it.resume(response)
            }, { error ->
                if (error.networkResponse?.statusCode == 301 || error.networkResponse?.statusCode == 302) {
                    it.resume("REDIRECT TO: ${error.networkResponse.headers?.get("Location")}")
                } else if (shouldRetryNotFoundWithoutExtension && error.networkResponse?.statusCode == 404 && url.endsWith(
                        ".html"
                    )
                ) {
                    it.resume("NOT FOUND: ${url.removeSuffix(".html")}")
                } else {
                    it.resumeWithException(error)
                }
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return headers.toMutableMap()
                }
            }

            // Handle redirects
            request.setShouldCache(true)

            queue.add(request)
        }

        if (result.startsWith("REDIRECT TO: ")) {
            return@onIO get(
                result.removePrefix("REDIRECT TO: "),
                headers,
                shouldRetryNotFoundWithoutExtension
            )
        } else if (result.startsWith("NOT FOUND: ")) {
            return@onIO get(result.removePrefix("NOT FOUND: "), headers, false)
        }

        result
    }

    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String =
        onIO {
            suspendCoroutine {
                // Use Volley to make the request
                val request = object : StringRequest(Method.POST, url, { response ->
                    it.resume(response)
                }, { error ->
                    error.printStackTrace()
                    it.resumeWithException(error)
                }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        return headers.toMutableMap()
                    }

                    override fun getBody(): ByteArray {
                        return body.toByteArray()
                    }
                }
                queue.add(request)
            }
        }
}