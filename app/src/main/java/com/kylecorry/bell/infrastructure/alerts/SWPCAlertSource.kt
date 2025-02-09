package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.infrastructure.internet.HttpService
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.sol.time.Time.atEndOfDay
import java.time.Duration
import java.time.ZoneId

class SWPCAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    private val titleRegex = Regex("(WARNING|ALERT|SUMMARY|WATCH):\\s(.*)")
    private val stormDateRegex = Regex("([A-Z][a-z]{2}\\s\\d{2}):")

    class SWPCResponse(val issue_datetime: String, val message: String)

    override suspend fun getAlerts(): List<Alert> {
        val url = "https://services.swpc.noaa.gov/products/alerts.json"
        val response = http.get(url)
        val json = JsonConvert.fromJson<Array<SWPCResponse>>(response) ?: return emptyList()
        return json.mapNotNull {
            if (!it.message.contains("WATCH: Geomagnetic Storm Category")) {
                return@mapNotNull null
            }

            val titleMatch = titleRegex.find(it.message)
            val title = titleMatch?.groupValues?.get(2) ?: ""

            val dates = stormDateRegex.findAll(it.message).toList()

            val publishedDate = DateTimeParser.parse(it.issue_datetime.replace(" ", "T") + "Z")
                ?: return@mapNotNull null

            val expirationDate = dates.flatMap {
                listOf(
                    "${it.groupValues[1]} ${publishedDate.year}",
                    "${it.groupValues[1]} ${publishedDate.year + 1}"
                )
            }.mapNotNull {
                DateTimeParser.parse(it, ZoneId.of("UTC"))?.atEndOfDay()
            }.filter { Duration.between(publishedDate, it).abs() < Duration.ofDays(14) }
                .maxOrNull()

            // TODO: High KP watch / warnings

            Alert(
                0,
                title,
                getSystemName(),
                AlertType.SpaceWeather,
                AlertLevel.Watch,
                "https://www.swpc.noaa.gov/",
                "geomagnetic-storm", // Only one geomagnetic storm alert should be shown
                publishedDate,
                expirationDate
                    ?: publishedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS),
                it.message,
                useLinkForSummary = false
            )
        }
    }

    override fun getSystemName(): String {
        return "Space Weather Prediction Center"
    }
}