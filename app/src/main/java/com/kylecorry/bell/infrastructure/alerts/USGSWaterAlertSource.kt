package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType

class USGSWaterAlertSource(context: Context) : BaseAlertSource(context) {

    private val locationRegex = Regex("PROJECT ALERT NOTICE \\((.*)\\)")

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val location = locationRegex.find(it.title)?.groupValues?.get(1) ?: ""
            it.copy(
                link = it.link.replace("http://", "https://"),
                title = it.title.substringAfter(") ") + " ($location)",
                expirationDate = it.publishedDate.plusDays(4)
            )
        }
    }

    override fun getSpecification(): AlertSpecification {
        return rss(
            "USGS Water",
            "https://water.usgs.gov/alerts/project_alert.xml",
            AlertType.Water,
            AlertLevel.Warning
        )
    }
}