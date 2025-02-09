package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
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
                type = AlertType.Water,
                level = AlertLevel.Warning,
                link = it.link.replace("http://", "https://"),
                title = it.title.substringAfter(") ") + " ($location)"
            )
        }
    }

    override fun getSystemName(): String {
        return "USGS Water"
    }

    override fun isActiveOnly(): Boolean {
        return false
    }
}