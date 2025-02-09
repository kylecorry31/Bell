package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class FederalRegistryAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    private class Document(
        val executive_order_number: String,
        val raw_text_url: String,
        val signing_date: String,
        val title: String
    )

    private class DocumentsResponse(val results: List<Document>)

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> = onIO {
        val url = getUrl(
            listOf("type" to "PRESDOCU", "presidential_document_type" to "executive_order"),
            listOf("executive_order_number", "raw_text_url", "publication_date", "title"),
            listOf(
                "conditions%5Bpublication_date%5D%5Bgte%5D" to since.toLocalDate().minusDays(1)
                    .toString()
            )
        )
        val response = http.get(url)
        val json = JsonConvert.fromJson<DocumentsResponse>(response) ?: return@onIO emptyList()

        json.results.mapNotNull {

            val date = DateTimeParser.parse(it.signing_date, ZoneId.of("America/New_York"))
                ?: return@mapNotNull null

            Alert(
                id = 0,
                title = it.title,
                type = AlertType.Government,
                level = AlertLevel.Order,
                link = it.raw_text_url,
                uniqueId = it.executive_order_number,
                publishedDate = date,
                summary = "",
                sourceSystem = getSystemName()
            )
        }.filter { it.publishedDate.isAfter(since) }
    }

    override fun getSystemName(): String {
        return "Federal Register"
    }

    override fun isActiveOnly(): Boolean {
        return false
    }

    private fun getUrl(
        conditions: List<Pair<String, String>>,
        fields: List<String>,
        extraParameters: List<Pair<String, String>>
    ): String {
        val queryParameters = mutableListOf<Pair<String, String>>()
        queryParameters.addAll(conditions.map { "conditions%5B${it.first}%5D%5B%5D" to it.second })
        queryParameters.addAll(fields.map { "fields%5B%5D" to it })
        queryParameters.addAll(extraParameters)
        queryParameters.add("format" to "json")
        queryParameters.add("order" to "newest")
        queryParameters.add("per_page" to "20")

        return "https://www.federalregister.gov/api/v1/documents?${
            queryParameters.joinToString("&") {
                "${it.first}=${it.second}"
            }
        }"
    }


}