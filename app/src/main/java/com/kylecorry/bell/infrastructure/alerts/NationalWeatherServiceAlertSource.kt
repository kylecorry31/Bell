package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector

class NationalWeatherServiceAlertSource(context: Context, private val area: String) :
    BaseAlertSource(context) {

    override fun getSpecification(): AlertSpecification {
        return atom(
            "National Weather Service",
            "https://api.weather.gov/alerts/active.atom?area=$area",
            AlertType.Weather,
            AlertLevel.Warning,
            title = Selector.text("cap:event"),
            link = Selector.value("https://alerts.weather.gov/search?area=$area")
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                type = if (it.title.lowercase().contains("red flag")) {
                    AlertType.Fire
                } else {
                    AlertType.Weather
                },
                level = AlertLevel.entries.firstOrNull { entry ->
                    it.title.replace("Statement", "Advisory")
                        .contains(entry.name, ignoreCase = true)
                } ?: AlertLevel.Other,
                useLinkForSummary = false,
                uniqueId = it.uniqueId.split("/").last().split(".")[6],
            )
        }
    }
}