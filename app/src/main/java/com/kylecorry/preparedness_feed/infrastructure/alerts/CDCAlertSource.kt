package com.kylecorry.preparedness_feed.infrastructure.alerts

import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import org.jsoup.Jsoup
import java.time.ZonedDateTime

class CDCAlertSource : AlertSource {

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> = onIO {
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
                AlertType.Health,
                AlertLevel.Warning,
                "https://www.cdc.gov$link",
                link.split("/").last().replace(".html", ""),
                parsedDate,
                ""
            )
        }.filter { it.publishedDate.isAfter(since) }

    }

}