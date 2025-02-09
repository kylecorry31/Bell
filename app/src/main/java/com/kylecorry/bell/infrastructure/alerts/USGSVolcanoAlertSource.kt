package com.kylecorry.bell.infrastructure.alerts

import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import org.jsoup.Jsoup
import java.time.ZoneId
import java.time.ZonedDateTime

class USGSVolcanoAlertSource : AlertSource {

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> = onIO {
        val document = Jsoup.connect("https://www.usgs.gov/programs/VHP/volcano-updates").get()
        val alerts = document.select(".usgs-vol-up-vonas")

        alerts.mapNotNull {
            val volcanoNameElement = it.select("b").firstOrNull() ?: return@mapNotNull null

            val dateElement =
                it.select(".hans-td:nth-child(2)").firstOrNull() ?: return@mapNotNull null

            val colorCodeElement = it.select(".hans-td:nth-child(2)")[2] ?: return@mapNotNull null

            // TODO: Parse location and filter based on area

            val summary = it.select(".hans-td span").text()

            val colorCode = colorCodeElement.text().trim()
            val level = when (colorCode.lowercase()) {
                "yellow" -> AlertLevel.Advisory
                "orange" -> AlertLevel.Watch
                "red" -> AlertLevel.Warning
                else -> AlertLevel.Other
            }

            if (level == AlertLevel.Other) {
                return@mapNotNull null
            }

            val volcano = volcanoNameElement.text()
            val title = "Volcano ${level.name} for $volcano"

            val parsedDate =
                DateTimeParser.parse(dateElement.text().replace("Z", ""), ZoneId.of("UTC"))
                    ?: return@mapNotNull null

            Alert(
                0,
                title,
                getSystemName(),
                AlertType.Volcano,
                level,
                "https://www.usgs.gov/programs/VHP/volcano-updates",
                volcano,
                parsedDate,
                summary,
                useLinkForSummary = false
            )
        }.sortedByDescending { it.publishedDate }.distinctBy { it.uniqueId }
            .filter { it.publishedDate.isAfter(since) }

    }

    override fun getSystemName(): String {
        return "USGS Volcanoes"
    }

    override fun isActiveOnly(): Boolean {
        return true
    }
}