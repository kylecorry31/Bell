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
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import java.time.Instant

class TravelAdvisoryAlertSource(context: Context) : AlertSource {
    private val levelRegex = Regex("Level (\\d)")
    private val countryRegex = Regex("(.*)\\s+-\\s+Level")
    private val levelDescriptions = mapOf(
        2 to "Exercise Increased Caution in:",
        3 to "Reconsider Travel to:",
        4 to "Do Not Travel to:"
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
                "description" to text("description"),
                "link" to text("link"),
                "identifier" to text("guid")
            )
        )

        return rawAlerts
            .groupBy {
                val title = it["title"] ?: ""
                val level = levelRegex.find(title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                level
            }
            .filter { it.key > 1 }
            .toSortedMap()
            .mapNotNull { (level, alerts) ->
                Alert(
                    id = 0,
                    identifier = "travel-advisory-$level",
                    sender = "US Department of State",
                    sent = Instant.now(), // Ensures this will remain up to date
                    source = getUUID(),
                    category = Category.Security,
                    event = "Level $level Travel Advisories",
                    urgency = Urgency.Immediate,
                    severity = levelSeverity[level] ?: Severity.Unknown,
                    certainty = Certainty.Observed,
                    description = levelDescriptions[level] + "\n\n" + alerts.map {
                        val country =
                            countryRegex.find(it["title"] ?: "")?.groupValues?.get(1) ?: ""
                        "- [$country](${it["link"]})"
                    }.sorted().joinToString("\n"),
                )
            }
    }

    override fun getUUID(): String {
        return "6092e3f5-029e-4c0d-adb4-513a6ec7dabb"
    }
}