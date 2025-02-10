package com.kylecorry.bell.infrastructure.alerts.water

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector

abstract class TsunamiAlertSource(context: Context, private val url: String) :
    BaseAlertSource(context) {

    override fun getSpecification(): AlertSpecification {
        return atom(
            "NOAA Tsunami",
            url,
            AlertType.Water,
            AlertLevel.Warning,
            link = Selector.attr("link[title=Bulletin]", "href"),
        )
    }

    private val header = "TSUNAMI WARNING CENTER"

    private val advisory = "TSUNAMI ADVISORY"
    private val watch = "TSUNAMI WATCH"
    private val warning = "TSUNAMI WARNING"
    private val threat = "TSUNAMI THREAT MESSAGE"

    private val locationMap = mapOf(
        // Only using the non segmented alerts: https://tsunami.gov/?page=product_list
        "WEAK51" to "Alaska, British Colombia, U.S. West Coast",
        "WEHW40" to "Hawaii",
        "WEZS40" to "American Samoa",
        "WEGM40" to "Guam, CNMI",
        "WEXX30" to "U.S. Atlantic, Gulf of Mexico, Canada",
        "WECA40" to "Puerto Rico, Virgin Islands",
    )

    private val cancellationMessages = listOf(
        "IS CANCELLED",
        "IS NOW CANCELLED",
        "TSUNAMI THREAT HAS NOW LARGELY PASSED",
        "NO LONGER A TSUNAMI THREAT",
        "FINAL TSUNAMI THREAT MESSAGE"
    )

    override fun getSystemName(): String {
        return "NOAA Tsunami"
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(title = "Tsunami ${it.title}")
        }.filter { alert ->
            locationMap.any { alert.link.contains(it.key) }
        }
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val headerIndex = fullText.uppercase().indexOf(header) + header.length
        val body = fullText.substring(headerIndex).trim()

        val level = when {
            cancellationMessages.any { body.contains(it) } -> null
            body.contains(threat) -> AlertLevel.Warning
            body.contains(warning) -> AlertLevel.Warning
            body.contains(watch) -> AlertLevel.Watch
            body.contains(advisory) -> AlertLevel.Advisory
            else -> null
        }

        val location = locationMap.entries.find { alert.link.contains(it.key) }?.value ?: ""

        if (level == null) {
            return alert.copy(expirationDate = alert.publishedDate)
        }

        return alert.copy(level = level, title = "Tsunami $level for $location")
    }
}