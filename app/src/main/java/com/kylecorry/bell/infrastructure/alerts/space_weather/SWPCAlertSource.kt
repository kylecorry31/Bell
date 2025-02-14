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

    // TODO: Capture the storm level
    private val stormDateRegex = Regex("([A-Z][a-z]{2}\\s\\d{2}):")
    private val messageCodeRegex = Regex("Space Weather Message Code: (\\w+)")
    private val validToRegex = Regex("Valid To: (.*)")
    private val validFromRegex = Regex("Valid From: (.*)")

    private val loader = AlertLoader(context)

    private val codeToSeverity = mapOf(
        "ALTK08" to Severity.Moderate,
        "ALTK09" to Severity.Moderate,
        "WATA99" to Severity.Moderate,
        // Everything else is minor
    )

    private val codeToStormLevel = mapOf(
        "ALTK05" to "1",
        "WARK05" to "1",
        "WATA20" to "1",
        "ALTK06" to "2",
        "WARK06" to "2",
        "WATA30" to "2",
        "ALTK07" to "3",
        "WARK07" to "3",
        "WATA50" to "3",
        "ALTK08" to "4",
        "WARK08" to "4",
        "ALTK09" to "5",
        "WARK09" to "5",
        "WATA99" to "4+"
    )

    private fun isWatch(code: String): Boolean {
        return code.startsWith("WAT")
    }

    private fun isWarning(code: String): Boolean {
        return code.startsWith("WAR")
    }

    private fun isAlert(code: String): Boolean {
        return code.startsWith("ALT")
    }

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

//            // TODO: Handle other types of alerts
//            if (!description.contains("WATCH: Geomagnetic Storm Category")) {
//                return@mapNotNull null
//            }

            val messageCode = messageCodeRegex.find(description)?.groupValues?.get(1)
                ?: return@mapNotNull null

            val stormLevel = codeToStormLevel[messageCode] ?: return@mapNotNull null

            val title = titleRegex.find(description)?.groupValues?.get(2)
                ?: return@mapNotNull null

            val dates = stormDateRegex.findAll(description).toList()


            val expirationDate = if (isWatch(messageCode)) {
                dates.flatMap {
                    listOf(
                        "${it.groupValues[1]} ${sent.atZone(ZoneId.of("UTC")).year}",
                        "${it.groupValues[1]} ${sent.atZone(ZoneId.of("UTC")).year + 1}"
                    )
                }.mapNotNull {
                    DateTimeParser.parse(it, ZoneId.of("UTC"))?.atEndOfDay()?.toInstant()
                }.filter { Duration.between(sent, it).abs() < Duration.ofDays(14) }
                    .maxOrNull()
            } else {
                validToRegex.find(description)?.groupValues?.get(1)?.let {
                    DateTimeParser.parseInstant(it)
                }
            }

            val effectiveDate = validFromRegex.find(description)?.groupValues?.get(1)?.let {
                DateTimeParser.parseInstant(it)
            }

            val label = if (isWatch(messageCode)) {
                "Watch"
            } else if (isWarning(messageCode)) {
                "Warning"
            } else {
                "Alert"
            }

            val identifier = if (isWatch(messageCode)) {
                "geomagnetic-storm-watch"
            } else {
                messageCode.replace("ALT", "").replace("WAR", "")
            }

            val certainty = if (isWatch(messageCode)) {
                Certainty.Possible
            } else if (isWarning(messageCode)) {
                Certainty.Likely
            } else {
                Certainty.Observed
            }


            // TODO: Handle cancellations
            Alert(
                id = 0,
                identifier = identifier,
                sender = "SWPC",
                sent = sent,
                source = getUUID(),
                category = Category.Infrastructure,
                event = "Geomagnetic Storm $label (G$stormLevel)",
                headline = title,
                urgency = Urgency.Unknown,
                severity = codeToSeverity[messageCode] ?: Severity.Minor,
                certainty = certainty,
                description = description,
                effective = effectiveDate,
                expires = expirationDate
                    ?: sent.plus(Duration.ofDays(1)),
            )
        }.sortedByDescending { it.sent }
    }

    override fun getUUID(): String {
        return "62facc3f-53d3-4600-94ec-c55208eff9f8"
    }
}