package com.kylecorry.bell.infrastructure.alerts.space_weather

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.sol.time.Time.atEndOfDay
import java.time.Duration
import java.time.ZoneId

class SWPCAlertSource(context: Context) : AlertSource {

    private val titleRegex = Regex("(WARNING|ALERT|SUMMARY|WATCH):\\s(.*)")
    private val stormDateRegex = Regex("([A-Z][a-z]{2}\\s\\d{2}):")
    private val messageCodeRegex = Regex("Space Weather Message Code: (\\w+)")

    private val loader = AlertLoader(context)

    private val codeToSeverity = mapOf(
        "ALTK07" to Severity.Minor,
        "WARK07" to Severity.Minor,
        "WATA50" to Severity.Minor,
        "ALTK08" to Severity.Moderate,
        "ALTK09" to Severity.Moderate,
        "WATA99" to Severity.Moderate,
    )


    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.JSON,
            "https://services.swpc.noaa.gov/products/alerts.json",
            "$",
            mapOf(
                "sent" to text("issue_datetime"),
                "description" to text("message")
            )
        )

        return rawAlerts.mapNotNull {
            val description = it["description"] ?: return@mapNotNull null
            val sent = DateTimeParser.parseInstant(it["sent"]?.replace(" ", "T") + "Z")
                ?: return@mapNotNull null

            // TODO: Handle other types of alerts
            if (!description.contains("WATCH: Geomagnetic Storm Category")!!) {
                return@mapNotNull null
            }

            val messageCode = messageCodeRegex.find(description)?.groupValues?.get(1)
                ?: return@mapNotNull null
            val title = titleRegex.find(description)?.groupValues?.get(2)
                ?: return@mapNotNull null

            val dates = stormDateRegex.findAll(description).toList()

            val expirationDate = dates.flatMap {
                listOf(
                    "${it.groupValues[1]} ${sent.atZone(ZoneId.of("UTC")).year}",
                    "${it.groupValues[1]} ${sent.atZone(ZoneId.of("UTC")).year + 1}"
                )
            }.mapNotNull {
                DateTimeParser.parse(it, ZoneId.of("UTC"))?.atEndOfDay()?.toInstant()
            }.filter { Duration.between(sent, it).abs() < Duration.ofDays(14) }
                .maxOrNull()


            // TODO: Handle cancelations
            Alert(
                id = 0,
                identifier = "geomagnetic-storm-warning",
                sender = "SWPC",
                sent = sent,
                source = getUUID(),
                category = Category.Infrastructure,
                event = title,
                urgency = Urgency.Unknown,
                severity = codeToSeverity[messageCode] ?: Severity.Unknown,
                certainty = Certainty.Unknown,
                description = description,
                expires = expirationDate
                    ?: sent.plus(Duration.ofDays(Constants.DEFAULT_EXPIRATION_DAYS)),
            )
        }
    }

    override fun getUUID(): String {
        return "62facc3f-53d3-4600-94ec-c55208eff9f8"
    }
}