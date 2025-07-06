package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.bell.infrastructure.internet.HttpService
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import com.kylecorry.luna.coroutines.onIO
import org.jsoup.Jsoup

enum class FileType {
    XML,
    JSON,
    HTML
}

class AlertLoader(context: Context) {
    private val http = HttpService(context)

    suspend fun load(
        type: FileType,
        url: String,
        items: String,
        attributes: Map<String, Selector>,
        additionalHeaders: Map<String, String?> = mapOf(),
        mitigate304: Boolean = true,
        responsePreprocessor: ((String) -> String)? = null
    ): List<Map<String, String?>> = onIO {
        val headers = mutableMapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
//            "If-Modified-Since" to ZonedDateTime.now().toString()
        )

        additionalHeaders.forEach { (key, value) ->
            if (value != null) {
                headers[key] = value
            } else {
                headers.remove(key)
            }
        }


        // Timestamp is for 304 mitigation
        val actualUrl = if (mitigate304) {
            "${url}${if (url.contains("?")) "&" else "?"}timestamp=${System.currentTimeMillis()}"
        } else {
            url
        }
        var response = http.get(actualUrl, headers = headers)
        if (responsePreprocessor != null) {
            response = responsePreprocessor(response)
        }

        val alertItems = select(
            when (type) {
                FileType.XML -> XMLConvert.parse(response)
                FileType.HTML -> Jsoup.parse(response)
                FileType.JSON -> response
            }, items
        )

        alertItems.map {
            attributes.mapValues { (_, selector) ->
                select(it, selector)
            }
        }
    }
}