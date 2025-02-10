package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class USGSEarthquakeAlertSource(context: Context) :
    BaseAlertSource(context) {

    private val eventTimeRegex = Regex("<dt>Time</dt><dd>([^<]*)</dd>")

    override fun getSpecification(): AlertSpecification {
        return atom(
            "USGS Earthquake",
            "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.atom",
            AlertType.Earthquake,
            AlertLevel.Event,
            title = Selector.attr("category[label=Magnitude]", "term"),
            additionalAttributes = mapOf(
                "originalTitle" to Selector.text("title")
            ),
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val eventTime = eventTimeRegex.find(it.summary)?.groupValues?.get(1) ?: ""
            val parsedTime = DateTimeParser.parse(eventTime) ?: it.publishedDate

            val originalTitle = it.additionalAttributes["originalTitle"] ?: ""

            val location = if (originalTitle.contains(" of ")) {
                "near ${originalTitle.substringAfter(" of ")}"
            } else {
                ""
            }

            val html = originalTitle + "\n\n" + HtmlTextFormatter.getText(
                it.summary.replace("</dd>", "</dd><br /><br />")
                    .replace("</dt>", ":&nbsp;</dt>")
                    .replace("</p>", "</p><br/><br/>")
                    .replace("</a>", "</a><br /> <br />")
            )

            it.copy(
                title = "${it.title} Earthquake $location".trim(),
                publishedDate = parsedTime,
                useLinkForSummary = false,
                summary = html
            )
        }
    }
}