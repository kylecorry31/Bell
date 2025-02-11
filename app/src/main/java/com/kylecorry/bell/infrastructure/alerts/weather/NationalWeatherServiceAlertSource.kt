package com.kylecorry.bell.infrastructure.alerts.weather

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector

class NationalWeatherServiceAlertSource(context: Context, private val area: String) :
    BaseAlertSource(context) {

    override fun getSpecification(): AlertSpecification {
        return atom(
            SourceSystem.NWSWeather,
            "https://api.weather.gov/alerts/active.atom?area=$area",
            AlertType.Weather,
            AlertLevel.Warning,
            title = Selector.text("cap:event"),
            link = Selector.value("https://alerts.weather.gov/search?area=$area"),
            mitigate304 = false,
            additionalHeaders = mapOf(
                "If-Modified-Since" to "Thu, 01 Jan 2030 00:00:00 GMT"
            )
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