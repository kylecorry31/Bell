package com.kylecorry.bell.infrastructure.summarization

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.bell.infrastructure.internet.HttpService
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
            "Write a formal twitter post summary of the following text for the general public, without any hashtags or links:\n\n$textContent"

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

        val summary =
            response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: text

        // Remove hashtags and links
        return summary.replace(Regex("#[a-zA-Z0-9]+"), "").replace(Regex("https?://[^\\s.]+"), "")
    }

}