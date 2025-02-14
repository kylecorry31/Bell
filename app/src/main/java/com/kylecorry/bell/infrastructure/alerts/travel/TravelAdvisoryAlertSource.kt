package com.kylecorry.bell.infrastructure.alerts.travel

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

class TravelAdvisoryAlertSource(context: Context) : AlertSource {
    private val levelRegex = Regex("Level (\\d)")
    private val countryRegex = Regex("(.*)\\s+-\\s+Level")
    private val levelDescriptions = mapOf(
        2 to "Exercise Increased Caution in",
        3 to "Reconsider Travel to",
        4 to "Do Not Travel to"
    )
    private val levelSeverity = mapOf(
        2 to Severity.Minor,
        3 to Severity.Moderate,
        4 to Severity.Severe
    )

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://travel.state.gov/_res/rss/TAsTWs.xml",
            "item",
            mapOf(
                "title" to text("title"),
                "sent" to text("pubDate"),
                "description" to text("description"),
                "link" to text("link"),
                "identifier" to text("guid")
            )
        )

        return rawAlerts.mapNotNull {
            val sent = DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val title = it["title"] ?: ""
            val level =
                levelRegex.find(title)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            if (level == 1) {
                return@mapNotNull null
            }

            val headline = levelDescriptions[level] ?: ""
            val country = countryRegex.find(title)?.groupValues?.get(1) ?: ""

            // TODO: Parse the risk indicators:
            /*
            - C: Crime
            - T: Terrorism
            - U: Civil Unrest
            - H: Health
            - N: Natural Disaster
            - E: Time-limited Event
            - K: Kidnapping or Hostage Taking
            - D: Wrongful Detention
            - O: Other
             */

            Alert(
                id = 0,
                identifier = it["identifier"] ?: "",
                sender = "US Department of State",
                sent = sent,
                source = getUUID(),
                category = Category.Security,
                event = "$headline $country",
                urgency = Urgency.Immediate,
                severity = levelSeverity[level] ?: Severity.Unknown,
                certainty = Certainty.Observed,
                link = it["link"],
                description = it["description"]?.let { HtmlTextFormatter.getText(it) },
                headline = title
            )
        }
    }

    override fun getUUID(): String {
        return "6092e3f5-029e-4c0d-adb4-513a6ec7dabb"
    }
}