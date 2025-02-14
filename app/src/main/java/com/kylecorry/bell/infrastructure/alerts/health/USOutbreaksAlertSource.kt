package com.kylecorry.bell.infrastructure.alerts.health

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import org.jsoup.Jsoup

class USOutbreaksAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://tools.cdc.gov/api/v2/resources/media/285676.rss",
            "item",
            mapOf(
                "title" to text("title"),
                "sent" to text("pubDate"),
                "link" to text("link"),
                "identifier" to text("guid")
            )
        )

        return rawAlerts.mapNotNull {
            val sent = DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val title = it["title"] ?: return@mapNotNull null
            val link = it["link"] ?: return@mapNotNull null
            val identifier = it["identifier"] ?: return@mapNotNull null

            val event = "Outbreak: " + title
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\\sOutbreaks?"), "")

            Alert(
                id = 0,
                identifier = identifier,
                sender = "CDC",
                sent = sent,
                source = getUUID(),
                category = Category.Health,
                event = event,
                headline = HtmlTextFormatter.getText(title),
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Observed,
                link = link,
                isDownloadRequired = true
            )
        }
    }

    override fun getUUID(): String {
        return "4160c9d8-8ed7-43db-bda7-a7de5fc79e51"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val parsed = Jsoup.parse(fullText)
        // Remove .dfe-links-layout--featured from the HTML
        parsed.select(".dfe-links-layout--featured").remove()

        val top = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__top")
        val center = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__center")
            .substringBefore("\nSee also").substringBefore("\nLearn more")

        val severity = when {
            center.contains("Health Advisory") -> Severity.Minor
            else -> Severity.Severe
        }

        return alert.copy(description = (top + "\n\n" + center).trim(), severity = severity)
    }
}