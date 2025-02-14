package com.kylecorry.bell.infrastructure.alerts.health

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.attr
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import java.time.Duration

// https://tools.cdc.gov/api/v2/resources/media/126194/syndicate.json
class HealthAlertNetworkAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.HTML,
            "https://tools.cdc.gov/api/v2/resources/media/126194/content.html",
            ".bg-quaternary .card-body",
            mapOf(
                "headline" to text("a"),
                "link" to attr("a", "href"),
                "publishedDate" to text("p")
            )
        )

        return rawAlerts.mapNotNull {
            val headline = it["headline"] ?: return@mapNotNull null
            val link = it["link"]?.replace("emergency.", "www.") ?: return@mapNotNull null
            val sent =
                DateTimeParser.parseInstant(it["publishedDate"] ?: "") ?: return@mapNotNull null

            val event = if (headline.contains("Health Alert Network (HAN)")) {
                headline.substringAfter("– ")
            } else {
                headline
            }


            Alert(
                id = 0,
                identifier = link,
                sender = "CDC",
                sent = sent,
                source = getUUID(),
                category = Category.Health,
                event = event,
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Observed,
                link = link,
                headline = headline,
                expires = sent.plus(Duration.ofDays(Constants.DEFAULT_EXPIRATION_DAYS)),
                isDownloadRequired = true
            )
        }
    }

    override fun getUUID(): String {
        return "805b0e35-8e77-4425-8089-6c9814682139"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val severity = when {
            fullText.contains("HAN_badge_HEALTH_ADVISORY") -> Severity.Minor
            fullText.contains("HAN_badge_HEALTH_UPDATE") -> Severity.Minor
            else -> Severity.Severe
        }

        val eventPrefix = when {
            fullText.contains("HAN_badge_HEALTH_ADVISORY") -> "Health Advisory:"
            fullText.contains("HAN_badge_HEALTH_UPDATE") -> "Health Update:"
            else -> "Health Alert:"
        }

        val summary = HtmlTextFormatter.getText(
            fullText.substringAfter("<strong>Summary</strong>")
                .substringBefore("<strong>Background</strong>")
        )

        return alert.copy(
            event = "$eventPrefix ${alert.event}",
            severity = severity,
            description = summary,
            fullText = HtmlTextFormatter.getText(fullText)
        )
    }

}