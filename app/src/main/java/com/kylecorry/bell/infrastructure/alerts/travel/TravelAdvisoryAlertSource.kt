package com.kylecorry.bell.infrastructure.alerts.travel

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class TravelAdvisoryAlertSource(context: Context) : BaseAlertSource(context) {
    private val levelRegex = Regex("Level (\\d)")
    private val countryRegex = Regex("(.*)\\s+-\\s+Level")
    private val levelDescriptions = mapOf(
        2 to "Exercise Increased Caution in",
        3 to "Reconsider Travel to",
        4 to "Do Not Travel to"
    )

    override fun getSpecification(): AlertSpecification {
        return rss(
            "State Department Travel Advisories",
            "https://travel.state.gov/_res/rss/TAsTWs.xml",
            AlertType.Travel
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {

            val level = levelRegex.find(it.title)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null

            if (level == 1) {
                return@mapNotNull null
            }

            val alertLevel = when (level) {
                2 -> AlertLevel.Advisory
                3 -> AlertLevel.Watch
                else -> AlertLevel.Warning
            }

            val description = levelDescriptions[level] ?: ""
            val country = countryRegex.find(it.title)?.groupValues?.get(1) ?: ""

            it.copy(
                level = alertLevel,
                title = "$description $country",
                summary = HtmlTextFormatter.getText(it.summary)
            )
        }
    }
}