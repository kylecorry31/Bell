package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
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
                source = "Weather",
                type = AlertType.entries.firstOrNull { entry ->
                    it.title.contains(entry.name, ignoreCase = true)
                }?.name ?: AlertType.Other.name,
                useLinkForSummary = false,
                link = "https://alerts.weather.gov/search?area=$area",
                uniqueId = it.uniqueId.split("/").last().split(".")[6]
            )
        }.sortedByDescending { it.publishedDate }.distinctBy { it.uniqueId }
    }
}