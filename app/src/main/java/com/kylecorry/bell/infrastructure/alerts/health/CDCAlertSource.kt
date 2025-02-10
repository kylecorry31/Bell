package com.kylecorry.bell.infrastructure.alerts.health

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class CDCAlertSource(context: Context) : BaseAlertSource(context) {
    override fun getSpecification(): AlertSpecification {
        return html(
            "CDC",
            "https://www.cdc.gov/han/index.html",
            items = ".bg-quaternary .card",
            title = Selector.text("a"),
            link = Selector.attr("a", "href"),
            uniqueId = Selector.attr("a", "href") { it?.split("/")?.last()?.replace(".html", "") },
            publishedDate = Selector.text("p"),
            summary = Selector.value(""),
            defaultAlertType = AlertType.Health
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val title = if (it.title.contains("Health Alert Network (HAN)")) {
                it.title.substringAfter("– ")
            } else {
                it.title
            }

            it.copy(
                title = title,
                link = "https://www.cdc.gov${it.link}",
                expirationDate = it.publishedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS)
            )
        }
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