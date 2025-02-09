package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import java.time.ZonedDateTime

class USGSWaterAlertSource(context: Context) : RssAlertSource(context) {

    private val locationRegex = Regex("PROJECT ALERT NOTICE \\((.*)\\)")

    override fun getUrl(since: ZonedDateTime): String {
        return "https://water.usgs.gov/alerts/project_alert.xml"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val location = locationRegex.find(it.title)?.groupValues?.get(1) ?: ""
            it.copy(
                source = "USGS",
                type = "Water",
                link = it.link.replace("http://", "https://"),
                title = it.title.substringAfter(") ") + " ($location)"
            )
        }
    }
}