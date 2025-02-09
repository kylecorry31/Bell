package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import java.time.ZonedDateTime

class USGSEarthquakeAlertSource(context: Context) :
    AtomAlertSource(context, "category[label=Magnitude][term]", "title") {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/significant_day.atom"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                source = "USGS",
                type = "Significant Earthquake",
                shouldSummarize = false,
                title = "${it.title} Earthquake"
            )
        }
    }
}