package com.kylecorry.bell.infrastructure.alerts.health

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import org.jsoup.Jsoup

class USOutbreaksAlertSource(context: Context) : BaseAlertSource(context) {

//    private val linkReplacements = mapOf(
//        "https://tools.cdc.gov/api/embed/downloader/download.asp?m=285676&c=754996" to "https://www.cdc.gov/listeria/outbreaks/meat-and-poultry-products-11-24/"
//    )

    override fun getSpecification(): AlertSpecification {
        return rss(
            "CDC Outbreaks",
            "https://tools.cdc.gov/api/v2/resources/media/285676.rss",
            AlertType.Health,
            AlertLevel.Warning
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            val cleanedTitle = "Outbreak: " + it.title
                .replace(Regex("<[^>]*>"), "")
                .replace(Regex("\\sOutbreaks?"), "")

            it.copy(
                title = cleanedTitle,
                canSkipDownloadIfOld = false
            )
        }
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val parsed = Jsoup.parse(fullText)
        // Remove .dfe-links-layout--featured from the HTML
        parsed.select(".dfe-links-layout--featured").remove()

        val top = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__top")
        val center = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__center")
            .substringBefore("\nSee also").substringBefore("\nLearn more")

        val level = when {
            center.contains("Health Advisory") -> AlertLevel.Advisory
            else -> AlertLevel.Warning
        }

        return alert.copy(summary = (top + "\n\n" + center).trim(), level = level)
    }
}