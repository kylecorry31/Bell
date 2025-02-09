package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import java.time.ZonedDateTime

class NationalWeatherServiceAlertSource(context: Context, private val area: String) :
    AtomAlertSource(context, "cap:event") {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://api.weather.gov/alerts/active.atom?area=$area"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                type = AlertType.Weather,
                level = AlertLevel.entries.firstOrNull { entry ->
                    it.title.contains(entry.name, ignoreCase = true)
                } ?: AlertLevel.Other,
                useLinkForSummary = false,
                link = "https://alerts.weather.gov/search?area=$area",
                uniqueId = it.uniqueId.split("/").last().split(".")[6],
                sourceSystem = getSystemName()
            )
        }.sortedByDescending { it.publishedDate }.distinctBy { it.uniqueId }
    }

    override fun getSystemName(): String {
        return "National Weather Service"
    }

    override fun isActiveOnly(): Boolean {
        return true
    }
}