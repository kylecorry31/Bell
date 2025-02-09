package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import org.jsoup.Jsoup
import java.time.ZonedDateTime

class WhiteHousePresidentalActionsAlertSource(context: Context) : RssAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.whitehouse.gov/presidential-actions/feed/"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                type = AlertType.Government,
                level = AlertLevel.Announcement,
                sourceSystem = getSystemName(),
                title = "Executive Order: ${it.title}"
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

        val content = HtmlTextFormatter.getText(fullText, ".entry-content > p")
        return alert.copy(summary = content)
    }
}