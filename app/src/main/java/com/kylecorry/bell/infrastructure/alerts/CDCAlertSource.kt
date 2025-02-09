package com.kylecorry.bell.infrastructure.alerts

import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import com.kylecorry.luna.coroutines.onIO
import org.jsoup.Jsoup

class CDCAlertSource : AlertSource {

    override suspend fun getAlerts(): List<Alert> = onIO {
        val document = Jsoup.connect("https://www.cdc.gov/han/index.html").get()
        val alerts = document.select(".bg-quaternary .card")

        alerts.mapNotNull {
            val titleElement = it.select("a")
            if (titleElement.isEmpty()) {
                return@mapNotNull null
            }

            val dateElement = it.select("p")
            if (dateElement.isEmpty()) {
                return@mapNotNull null
            }

            val title = titleElement.text().let {
                if (it.contains("Health Alert Network (HAN)")) {
                    it.substringAfter("– ")
                } else {
                    it
                }
            }
            val date = dateElement.text()
            val link = titleElement.attr("href")

            val parsedDate = DateTimeParser.parse(date) ?: return@mapNotNull null

            Alert(
                0,
                title,
                getSystemName(),
                AlertType.Health,
                AlertLevel.Warning,
                "https://www.cdc.gov$link",
                link.split("/").last().replace(".html", ""),
                parsedDate,
                parsedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS),
                ""
            )
        }
    }

    override fun getSystemName(): String {
        return "CDC"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val level = when {
            fullText.contains("HAN_badge_HEALTH_ADVISORY") -> AlertLevel.Advisory
            fullText.contains("HAN_badge_HEALTH_UPDATE") -> AlertLevel.Update
            else -> AlertLevel.Warning
        }

        val summary = HtmlTextFormatter.getText(
            fullText.substringAfter("<strong>Summary</strong>")
                .substringBefore("<strong>Background</strong>")
        )

        return alert.copy(level = level, summary = summary)
    }

}