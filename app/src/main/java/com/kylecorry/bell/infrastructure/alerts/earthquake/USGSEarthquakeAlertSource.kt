package com.kylecorry.bell.infrastructure.alerts.earthquake

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
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.attr
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

class USGSEarthquakeAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    private val eventTimeRegex = Regex("<dt>Time</dt><dd>([^<]*)</dd>")

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.atom",
            "entry",
            mapOf(
                "title" to text("title"),
                "sent" to text("updated"),
                "description" to text("summary"),
                "link" to attr("link", "href"),
                "identifier" to text("id"),
                "origin" to text("georss:point"),
                "magnitude" to attr("category[label=Magnitude]", "term"),
            )
        )

        return rawAlerts.mapNotNull {
            val title = it["title"] ?: return@mapNotNull null
            val originalSent =
                DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val identifier = it["identifier"] ?: return@mapNotNull null
            val magnitude = it["magnitude"] ?: return@mapNotNull null
            val description = it["description"] ?: return@mapNotNull null
            val eventTime = eventTimeRegex.find(description)?.groupValues?.get(1) ?: ""
            val sent = DateTimeParser.parseInstant(eventTime) ?: originalSent
            val location = if (title.contains(" of ")) {
                title.substringAfter(" of")
            } else {
                null
            }
            val locationText = location?.let { "near $it" }

            val html = title + "\n\n" + HtmlTextFormatter.getText(
                description.replace("</dd>", "</dd><br /><br />")
                    .replace("</dt>", ":&nbsp;</dt>")
                    .replace("</p>", "</p><br/><br/>")
                    .replace("</a>", "</a><br /> <br />")
            )

            val magnitudeValue = magnitude.substringAfter(" ").toFloatOrNull() ?: 0f
            val severity = when {
                magnitudeValue >= 8 -> Severity.Extreme
                magnitudeValue >= 7 -> Severity.Severe
                magnitudeValue >= 6 -> Severity.Moderate
                else -> Severity.Minor
            }

            Alert(
                id = 0,
                identifier = identifier,
                sender = "USGS",
                sent = sent,
                source = getUUID(),
                category = Category.Geophysical,
                event = "$magnitude Earthquake $locationText".trim(),
                urgency = Urgency.Past,
                severity = severity,
                certainty = Certainty.Observed,
                headline = title,
                description = html,
                link = it["link"],
                area = it["origin"]?.let {
                    val parts = it.split(" ")
                    val lat = parts[0].toDoubleOrNull() ?: 0.0
                    val lon = parts[1].toDoubleOrNull() ?: 0.0
                    Area(
                        listOf(),
                        circles = listOf(
                            Geofence(Coordinate(lat, lon), Distance.kilometers(0f))
                        )
                    )
                }
            )
        }
    }

    override fun getUUID(): String {
        return "bafb54fe-b381-4e90-a88a-87773f5015d8"
    }
}