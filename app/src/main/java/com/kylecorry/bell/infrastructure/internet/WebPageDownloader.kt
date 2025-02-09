package com.kylecorry.bell.infrastructure.internet

import android.content.Context

class WebPageDownloader(context: Context) {

    private val http = HttpService(context)

    suspend fun download(url: String): String {
        return http.get(
            url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
            )
        )
    }

}