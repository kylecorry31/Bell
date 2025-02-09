package com.kylecorry.preparedness_feed.infrastructure.alerts

import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertSource
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

            val parsedDate = parseDate(date) ?: return@mapNotNull null

            Alert(
                0,
                title,
                "CDC",
                "Alert",
                "https://www.cdc.gov$link",
                link.split("/").last().replace(".html", ""),
                parsedDate,
                ""
            )
        }.filter { it.publishedDate.isAfter(since) }

    }

    private fun parseDate(date: String): ZonedDateTime? {
        // "%m/%d/%Y %I:%M %p" OR "%m/%d/%Y, %I:%M %p"
        val formats = listOf("MM/dd/yyyy h:mm a", "MM/dd/yyyy, h:mm a")
        for (format in formats) {
            var dateString = date
            while (dateString.length > format.length) {
                try {
                    return LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern(format))
                        .atZone(ZoneId.systemDefault())
                } catch (e: Exception) {
                    dateString = dateString.drop(1)
                }
            }
        }
        return null
    }
}