package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import java.time.ZonedDateTime

class USGSEarthquakeAlertSource(context: Context) :
    AtomAlertSource(context, "category[label=Magnitude][term]") {

        private val eventTimeRegex = Regex("<dt>Time</dt><dd>([^<]*)</dd>")

    override fun getUrl(since: ZonedDateTime): String {
        return "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.atom"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val eventTime = eventTimeRegex.find(it.summary)?.groupValues?.get(1) ?: ""
            val parsedTime = DateTimeParser.parse(eventTime) ?: it.publishedDate

            it.copy(
                type = AlertType.Earthquake,
                level = AlertLevel.Event,
                title = "${it.title} Earthquake",
                publishedDate = parsedTime
            )
        }
    }
}