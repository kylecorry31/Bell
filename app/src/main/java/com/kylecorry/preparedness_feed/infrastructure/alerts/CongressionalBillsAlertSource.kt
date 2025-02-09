package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.utils.HtmlTextFormatter
import java.time.ZonedDateTime

class CongressionalBillsAlertSource(context: Context) : RssAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.congress.gov/rss/presented-to-president.xml"
    }

    override fun getSystemName(): String {
        return "Congress"
    }

    override fun isActiveOnly(): Boolean {
        return false
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {
            if (it.link.contains("concurrent-resolution")) {
                return@mapNotNull null
            }
            it.copy(
                type = AlertType.Government,
                level = AlertLevel.Announcement,
                title = "Bill: ${it.title}",
                sourceSystem = getSystemName(),
            )
        }
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val summary = HtmlTextFormatter.getText(fullText, "#bill-summary")
        return alert.copy(summary = summary)
    }
}