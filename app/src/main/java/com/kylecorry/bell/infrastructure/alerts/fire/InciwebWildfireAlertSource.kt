package com.kylecorry.bell.infrastructure.alerts.fire

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import com.kylecorry.bell.infrastructure.utils.SimpleWordTokenizer
import com.kylecorry.bell.infrastructure.utils.StateUtils
import org.jsoup.Jsoup
import java.time.ZoneId

class InciwebWildfireAlertSource(context: Context) : AlertSource {

    private val lastUpdatedDateRegex = Regex("Last updated: (\\d{4}-\\d{2}-\\d{2})")
    private val stateRegex = Regex("State: (\\w+)")
    private val fireNameRegex = Regex("[A-Z0-9]+\\s(.+)\\sFire")

    private val loader = AlertLoader(context)

    private val containedText = listOf(
        "This page will no longer be updated",
        "100% contained",
    )

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://inciweb.wildfire.gov/incidents/rss.xml",
            "item",
            mapOf(
                "title" to text("title"),
                "link" to text("link"),
                "description" to text("description"),
                "sent" to text("pubDate"),
                "identifier" to text("guid")
            )
        ){
            // This message sometimes gets appended after the closing tag of the rss feed
            it.replace("The website encountered an unexpected error. Try again later.<br />", "")
        }

        return rawAlerts.mapNotNull {
            val title = it["title"] ?: return@mapNotNull null
            val link = it["link"]?.replace("http://", "https://") ?: return@mapNotNull null
            val description = it["description"] ?: return@mapNotNull null
            val originalSent =
                DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val identifier = it["identifier"] ?: return@mapNotNull null

            if (!description.contains("type of incident is Wildfire", true)) {
                return@mapNotNull null
            }

            if (containedText.any { text -> description.contains(text, true) }) {
                return@mapNotNull null
            }

            val sent = lastUpdatedDateRegex.find(description)?.groupValues?.get(1)
                ?.let { DateTimeParser.parseInstant(it, ZoneId.of("America/New_York")) }
                ?: originalSent

            var state = stateRegex.find(description)?.groupValues?.get(1)
            if (state.isNullOrBlank()) {
                val words = SimpleWordTokenizer().tokenize(description).toSet()
                state = words.firstOrNull { word -> StateUtils.isState(word, false) }
                    ?: words.firstOrNull { word -> StateUtils.isState(word, true) }
            }

            val fireName = fireNameRegex.find(title)?.groupValues?.get(1)?.let { "($it)" } ?: ""

            val event = "Wildfire in $state $fireName".trim()

            // TODO: Get coordinates

            Alert(
                id = 0,
                identifier = identifier,
                sender = "Inciweb",
                sent = sent,
                source = getUUID(),
                category = Category.Fire,
                event = event,
                urgency = Urgency.Immediate,
                severity = Severity.Severe,
                certainty = Certainty.Observed,
                headline = title,
                description = description,
                link = link,
                area = state?.let { Area(listOf(it)) },
                isDownloadRequired = true,
                redownloadIntervalDays = Constants.DEFAULT_EXPIRATION_DAYS,
                impactsBorderingStates = true
            )
        }
    }

    override fun getUUID(): String {
        return "9be67ef1-cbb4-4a64-b3e0-53b5ba92f89c"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {

        val containedPercent =
            Jsoup.parse(fullText).select("th:contains(Percent of Perimeter Contained)")
                .firstOrNull()?.nextElementSibling()?.text()

        if (containedPercent == "100%") {
            return alert.copy(isTracked = false)
        }

        // TODO: Update severity based on percent contained and acres burned

        return alert.copy(fullText = HtmlTextFormatter.getText(fullText))
    }
}