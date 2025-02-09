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
        // If the text is HTML, extract the text
        val textContent = Jsoup.parse(text).wholeText()

        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val prompt =
            "Write a concise, high level, one sentence (< 280 characters) summary of following text:\n\n$textContent"

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

}