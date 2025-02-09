package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import java.time.ZonedDateTime

class SWPCAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    private val titleRegex = Regex("(WARNING|ALERT|SUMMARY|WATCH):\\s(.*)")

    class SWPCResponse(val issue_datetime: String, val message: String)

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> {
        val url = "https://services.swpc.noaa.gov/products/alerts.json"
        val response = http.get(url)
        val json = JsonConvert.fromJson<Array<SWPCResponse>>(response) ?: return emptyList()
        return json.mapNotNull {
            if (!it.message.contains("WATCH: Geomagnetic Storm Category")) {
                return@mapNotNull null
            }

            val titleMatch = titleRegex.find(it.message)
            val title = titleMatch?.groupValues?.get(2) ?: ""

            // TODO: High KP watch / warnings

            // TODO: Find the last mentioned date in the message and set that + 1 day as the expiration date for the geomagnetic storm watches
            Alert(
                0,
                title,
                AlertType.SpaceWeather,
                AlertLevel.Watch,
                "https://www.swpc.noaa.gov/",
                "geomagnetic-storm", // Only one geomagnetic storm alert should be shown
                DateTimeParser.parse(it.issue_datetime.replace(" ", "T") + "Z") ?: return@mapNotNull null,
                it.message,
                useLinkForSummary = false
            )
        }.filter { it.publishedDate.isAfter(since) }
    }
}