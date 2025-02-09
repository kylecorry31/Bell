package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import java.time.ZonedDateTime

class NationalWeatherServiceAlertSource(context: Context, private val area: String) :
    AtomAlertSource(context, "cap:event") {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://api.weather.gov/alerts/active.atom?area=$area"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                source = "National Weather Service",
                type = "NWS",
                useLinkForSummary = false,
                uniqueId = it.uniqueId.split("/").last().split(".")[6]
            )
        }.sortedByDescending { it.publishedDate }.distinctBy { it.uniqueId }
    }
}