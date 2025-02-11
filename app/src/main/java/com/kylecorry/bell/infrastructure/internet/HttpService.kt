package com.kylecorry.bell.infrastructure.internet

import android.content.Context
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.kylecorry.luna.coroutines.onIO
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class HttpService(context: Context) {

    private val queue = Volley.newRequestQueue(context)

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String = onIO {
        suspendCoroutine {
            // Use Volley to make the request
            val request = object : StringRequest(Method.GET, url, { response ->
                it.resume(response)
            }, { error ->
                it.resumeWithException(error)
            }) {
                override fun getHeaders(): MutableMap<String, String> {
                    return headers.toMutableMap()
                }
            }
            queue.add(request)
        }
    }

    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String =
        onIO {
            suspendCoroutine {
                // Use Volley to make the request
//                val queue = Volley.newRequestQueue(context)
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