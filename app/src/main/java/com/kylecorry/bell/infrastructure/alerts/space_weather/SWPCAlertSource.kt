package com.kylecorry.bell.infrastructure.alerts.space_weather

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.sol.time.Time.atEndOfDay
import java.time.Duration
import java.time.ZoneId

class SWPCAlertSource(context: Context) : BaseAlertSource(context) {

    private val titleRegex = Regex("(WARNING|ALERT|SUMMARY|WATCH):\\s(.*)")
    private val stormDateRegex = Regex("([A-Z][a-z]{2}\\s\\d{2}):")

    override fun getSpecification(): AlertSpecification {
        return json(
            "Space Weather Prediction Center",
            "https://services.swpc.noaa.gov/products/alerts.json",
            items = "$",
            title = Selector.value(""),
            link = Selector.value("https://www.swpc.noaa.gov/"),
            uniqueId = Selector.value(""),
            publishedDate = Selector.text("issue_datetime") { it?.replace(" ", "T") + "Z" },
            summary = Selector.text("message"),
            defaultAlertType = AlertType.SpaceWeather,
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull { alert ->
            if (!alert.summary.contains("WATCH: Geomagnetic Storm Category")) {
                return@mapNotNull null
            }

            val titleMatch = titleRegex.find(alert.summary)
            val title = titleMatch?.groupValues?.get(2) ?: ""

            val dates = stormDateRegex.findAll(alert.summary).toList()

            val expirationDate = dates.flatMap {
                listOf(
                    "${it.groupValues[1]} ${alert.publishedDate.year}",
                    "${it.groupValues[1]} ${alert.publishedDate.year + 1}"
                )
            }.mapNotNull {
                DateTimeParser.parse(it, ZoneId.of("UTC"))?.atEndOfDay()
            }.filter { Duration.between(alert.publishedDate, it).abs() < Duration.ofDays(14) }
                .maxOrNull()

            // TODO: High KP watch / warnings

            alert.copy(
                title = title,
                expirationDate = expirationDate
                    ?: alert.publishedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS),
                uniqueId = "geomagnetic-storm",
                level = AlertLevel.Watch,
                useLinkForSummary = false
            )
        }
    }
}