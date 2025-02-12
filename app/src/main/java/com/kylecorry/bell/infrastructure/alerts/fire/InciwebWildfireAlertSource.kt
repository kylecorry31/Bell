package com.kylecorry.bell.infrastructure.alerts.fire

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.utils.StateUtils
import org.jsoup.Jsoup
import java.time.ZoneId

class InciwebWildfireAlertSource(context: Context) : BaseAlertSource(context) {

    private val lastUpdatedDateRegex = Regex("Last updated: (\\d{4}-\\d{2}-\\d{2})")
    private val stateRegex = Regex("State: (\\w+)")
    private val fireNameRegex = Regex("[A-Z0-9]+\\s(.+)\\sFire")

    private val containedText = listOf(
        "This page will no longer be updated",
        "100% contained",
    )

    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.InciwebWildfires,
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

            if (containedText.any { text -> it.summary.contains(text, true) }) {
                return@mapNotNull null
            }

            val lastUpdated = lastUpdatedDateRegex.find(it.summary)?.groupValues?.get(1)
                ?.let { DateTimeParser.parse(it, ZoneId.of("America/New_York")) }
                ?: it.publishedDate

            val state = stateRegex.find(it.summary)?.groupValues?.get(1) ?: ""

            if (!StateUtils.isSelectedState(this.state, state, true)) {
                return@mapNotNull null
            }

            val fireName = fireNameRegex.find(it.title)?.groupValues?.get(1)?.let { "($it)" } ?: ""

            val title = "Wildfire in $state $fireName".trim()

            it.copy(
                title = title,
                publishedDate = lastUpdated,
                expirationDate = null,
                link = it.link.replace("http://", "https://"),
                summaryUpdateIntervalDays = Constants.DEFAULT_EXPIRATION_DAYS,
                isSummaryDownloadRequired = true
            )
        }
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {

        // The sibling td item of the th that contains Percent of Perimeter Contained
        val containedPercent =
            Jsoup.parse(fullText).select("th:contains(Percent of Perimeter Contained)")
                .firstOrNull()?.nextElementSibling()?.text()

        if (containedPercent == "100%") {
            return alert.copy(
                level = AlertLevel.Ignored,
                useLinkForSummary = false,
            )
        }

        return alert.copy(useLinkForSummary = false)
    }
}