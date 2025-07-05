package com.kylecorry.bell.infrastructure.alerts.crime

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.allText
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.attr
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import java.time.ZoneId

class NationalTerrorismAdvisoryAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.HTML,
            "https://www.dhs.gov/ntas/1.1/feed.xml",
            "alert",
            mapOf(
                "start" to attr("alert", "start"),
                "end" to attr("alert", "end"),
                "link" to attr("alert", "href"),
                "type" to attr("alert", "type"),
                "summary" to text("summary", raw = true),
                "details" to text("details", raw = true),
                "locations" to allText("location")
            )
        )

        return rawAlerts.mapNotNull {
            val start = DateTimeParser.parseInstant(it["start"] ?: "", ZoneId.of("UTC"))
                ?: return@mapNotNull null
            val end = DateTimeParser.parseInstant(it["end"] ?: "", ZoneId.of("UTC"))
            val link = it["link"] ?: return@mapNotNull null
            val type = it["type"] ?: return@mapNotNull null
            val summary = it["summary"]
            val details = it["details"]
            val locations = it["locations"]

            val severity = when (type) {
                "Imminent Threat" -> Severity.Severe
                "Elevated Threat" -> Severity.Moderate
                else -> Severity.Unknown
            }

            Alert(
                id = 0,
                identifier = link,
                sender = "DHS",
                sent = start,
                effective = start,
                expires = end,
                source = getUUID(),
                category = Category.Security,
                event = type,
                urgency = Urgency.Unknown,
                severity = severity,
                certainty = Certainty.Unknown,
                link = link,
                description = HtmlTextFormatter.getText("$summary\n\n$details".trim()),
                area = locations?.let { Area(emptyList(), it) }
            )
        }
    }

    override fun getUUID(): String {
        return "d40a50ba-caa9-437f-992b-df9f8106dbc9"
    }
}