package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import org.jsoup.Jsoup
import java.time.ZonedDateTime

class ExecutiveOrderAlertSource(context: Context) : RssAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.whitehouse.gov/presidential-actions/feed/"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                type = AlertType.Government,
                level = AlertLevel.Announcement,
                sourceSystem = getSystemName()
            )
        }.distinctBy { it.title }
    }

    override fun getSystemName(): String {
        return "White House"
    }

    override fun isActiveOnly(): Boolean {
        return false
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val html = Jsoup.parse(fullText)
        val type = html.select(".wp-block-whitehouse-topper__meta--byline").text().trim()

        if (type.lowercase() != "executive order") {
            return alert.copy(summary = "", shouldSummarize = false, level = AlertLevel.Noise)
        }

        val content = html.select(".entry-content > p").text()
        return alert.copy(summary = content)
    }
}