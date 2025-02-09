package com.kylecorry.preparedness_feed.infrastructure.summarization

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import kotlinx.coroutines.delay
import org.jsoup.Jsoup

class Gemini(context: Context, private val apiKey: String) {

    class GeminiPart(var text: String)
    class GeminiContent(var parts: List<GeminiPart>)
    class GeminiCandidate(var content: GeminiContent)
    class GeminiResponse(var candidates: List<GeminiCandidate>)

    class GeminiInput(var contents: List<GeminiContent>)

    private val http = HttpService(context)

    private var requestTimes = mutableListOf<Long>()

    suspend fun summarize(text: String): String {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val prompt =
            "Write a concise summary of the text content of the following. It must be under 4 sentences:\n\n$text"

        val contents =
            JsonConvert.toJson(GeminiInput(listOf(GeminiContent(listOf(GeminiPart(prompt))))))

        val response = JsonConvert.fromJson<GeminiResponse>(
            http.post(
                url, contents, headers = mapOf(
                    "Content-Type" to "application/json",
                )
            )
        )

        // Delay for a bit to prevent rate limiting (15 requests per minute)
        requestTimes.add(System.currentTimeMillis())

        if (requestTimes.size > 15) {
            val lastRequestTime = requestTimes.first()
            val timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime
            if (timeSinceLastRequest < 60000) {
                delay(60000 - timeSinceLastRequest + 250)
            }
            requestTimes.removeAt(0)
        }

        return response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: text
    }

    suspend fun summarizeUrl(url: String): String {
        val text = http.get(
            url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
            )
        )

        // If the text is HTML, extract the text
        val textContent = if (text.contains("<html") || text.contains("<!DOCTYPE")) {
            Jsoup.parse(text).text()
        } else {
            text
        }

        return summarize(textContent)
    }

}