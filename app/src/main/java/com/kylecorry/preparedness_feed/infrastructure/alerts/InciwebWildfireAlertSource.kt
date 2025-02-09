package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import java.time.ZoneId
import java.time.ZonedDateTime

class InciwebWildfireAlertSource(context: Context) : RssAlertSource(context) {

    private val lastUpdatedDateRegex = Regex("Last updated: (\\d{4}-\\d{2}-\\d{2})")
    private val stateRegex = Regex("State: (\\w+)")
    private val fireNameRegex = Regex("[A-Z0-9]+\\s(.+)\\sFire")

    override fun getUrl(since: ZonedDateTime): String {
        return "https://inciweb.wildfire.gov/incidents/rss.xml"
    }

    override fun getSystemName(): String {
        return "Inciweb Wildfire"
    }

    override fun isActiveOnly(): Boolean {
        return true
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {

            if (!it.summary.contains("type of incident is Wildfire")) {
                return@mapNotNull null
            }

            val lastUpdated = lastUpdatedDateRegex.find(it.summary)?.groupValues?.get(1)
                ?.let { DateTimeParser.parse(it, ZoneId.of("America/New_York")) }
                ?: it.publishedDate

            val state = stateRegex.find(it.summary)?.groupValues?.get(1) ?: ""
            val fireName = fireNameRegex.find(it.title)?.groupValues?.get(1)?.let { "($it)" } ?: ""

            val title = "Wildfire in $state $fireName".trim()

            it.copy(
                title = title,
                type = AlertType.Fire,
                level = AlertLevel.Warning,
                sourceSystem = getSystemName(),
                useLinkForSummary = false,
                publishedDate = lastUpdated
            )
        }
    }
}