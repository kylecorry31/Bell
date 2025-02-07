package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import java.time.ZonedDateTime

class USGSWaterAlertSource(context: Context) : AtomAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://water.usgs.gov/alerts/project_alert.xml"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(source = "USGS", type = "Water", link = it.link.replace("http://", "https://"))
        }
    }
}