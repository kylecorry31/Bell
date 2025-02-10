package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import java.time.ZoneId

class InciwebWildfireAlertSource(context: Context) : BaseAlertSource(context) {

    private val lastUpdatedDateRegex = Regex("Last updated: (\\d{4}-\\d{2}-\\d{2})")
    private val stateRegex = Regex("State: (\\w+)")
    private val fireNameRegex = Regex("[A-Z0-9]+\\s(.+)\\sFire")

    private val containedText = "This page will no longer be updated"

    override fun getSpecification(): AlertSpecification {
        return rss(
            "Inciweb Wildfire",
            "https://inciweb.wildfire.gov/incidents/rss.xml",
            AlertType.Fire,
            AlertLevel.Warning
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {
            if (!it.summary.contains("type of incident is Wildfire", true)) {
                return@mapNotNull null
            }

            if (it.summary.contains(containedText, true)) {
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
                useLinkForSummary = false,
                publishedDate = lastUpdated,
                expirationDate = null
            )
        }
    }
}