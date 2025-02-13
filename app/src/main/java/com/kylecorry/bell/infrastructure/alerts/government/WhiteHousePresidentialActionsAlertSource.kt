package com.kylecorry.bell.infrastructure.alerts.government

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import org.jsoup.Jsoup

class WhiteHousePresidentialActionsAlertSource(context: Context) : BaseAlertSource(context) {
    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(
                title = "Executive Order: ${it.title}",
                expirationDate = it.publishedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS),
                isSummaryDownloadRequired = true
            )
        }.distinctBy { it.title }
    }

    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.WhiteHousePresidentialActions,
            "https://www.whitehouse.gov/presidential-actions/feed/",
            AlertType.Government,
            AlertLevel.Information,
            limit = 10
        )
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val html = Jsoup.parse(fullText)
        val type = html.select(".wp-block-whitehouse-topper__meta--byline").text().trim()

        if (type.lowercase() != "executive order") {
            return alert.copy(summary = "", level = AlertLevel.Ignored)
        }

        val content = HtmlTextFormatter.getText(fullText, ".entry-content > p")
        return alert.copy(summary = content)
    }
}