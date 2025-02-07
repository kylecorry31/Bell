package com.kylecorry.preparedness_feed.infrastructure.summarization

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import kotlinx.coroutines.delay

class Gemini(context: Context, private val apiKey: String) {

    class GeminiPart(var text: String)
    class GeminiContent(var parts: List<GeminiPart>)
    class GeminiCandidate(var content: GeminiContent)
    class GeminiResponse(var candidates: List<GeminiCandidate>)

    class GeminiInput(var contents: List<GeminiContent>)

    private val http = HttpService(context)

    suspend fun summarize(text: String): String {
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"

        val prompt = "Write a concise summary of the text content of the following:\n\n$text"

        val contents = JsonConvert.toJson(GeminiInput(listOf(GeminiContent(listOf(GeminiPart(prompt))))))

        val response = JsonConvert.fromJson<GeminiResponse>(
            http.post(
                url, contents, headers = mapOf(
                    "Content-Type" to "application/json",
                )
            )
        )

        // Delay for a bit to prevent rate limiting
        delay(250)

        return response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: text
    }

    suspend fun summarizeUrl(url: String): String {
        val text = http.get(
            url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3"
            )
        )
        return summarize(text)
    }

}