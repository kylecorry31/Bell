package com.kylecorry.bell.infrastructure.internet

import java.time.Duration

class HttpService {

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        shouldRetryNotFoundWithoutExtension: Boolean = true
    ): String {
        val client = HttpClient()
        val response = client.send(
            url,
            headers = headers,
            followRedirects = false,
            connectTimeout = CONNECT_TIMEOUT,
            readTimeout = READ_TIMEOUT
        )
        if (response.code == 301 || response.code == 302) {
            val location = response.headers.getOrDefault("Location", emptyList()).firstOrNull()
            if (location != null) {
                return get(
                    location,
                    headers,
                    shouldRetryNotFoundWithoutExtension
                )
            }
        } else if (response.code == 404 && shouldRetryNotFoundWithoutExtension && url.endsWith(".html")) {
            return get(url.removeSuffix(".html"), headers, false)
        }

        if (!response.isSuccessful()) {
            throw Exception("HTTP error ${response.code} ($url)")
        }

        return response.contentAsString() ?: ""
    }

    suspend fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String {
        val client = HttpClient()
        val response =
            client.send(
                url,
                method = HttpMethod.POST,
                body = body.toByteArray(),
                headers = headers,
                connectTimeout = CONNECT_TIMEOUT,
                readTimeout = READ_TIMEOUT
            )
        if (!response.isSuccessful()) {
            throw Exception("HTTP error ${response.code} ($url)")
        }
        return response.contentAsString() ?: ""
    }

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(10)
        private val READ_TIMEOUT = Duration.ofSeconds(10)
    }
}