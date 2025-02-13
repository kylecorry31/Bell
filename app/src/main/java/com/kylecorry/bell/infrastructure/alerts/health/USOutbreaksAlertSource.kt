//package com.kylecorry.bell.infrastructure.alerts.health
//
//import android.content.Context
//import com.kylecorry.bell.domain.Alert
//import com.kylecorry.bell.domain.AlertLevel
//import com.kylecorry.bell.domain.AlertType
//import com.kylecorry.bell.domain.SourceSystem
//import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
//import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
//import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
//import org.jsoup.Jsoup
//
//class USOutbreaksAlertSource(context: Context) : BaseAlertSource(context) {
//
//    override fun getSpecification(): AlertSpecification {
//        return rss(
//            SourceSystem.CDCUSOutbreaks,
//            "https://tools.cdc.gov/api/v2/resources/media/285676.rss",
//            AlertType.Health,
//            AlertLevel.High
//        )
//    }
//
//    override fun process(alerts: List<Alert>): List<Alert> {
//        return alerts.map {
//            val cleanedTitle = "Outbreak: " + it.title
//                .replace(Regex("<[^>]*>"), "")
//                .replace(Regex("\\sOutbreaks?"), "")
//
//            it.copy(
//                title = cleanedTitle,
//                isSummaryDownloadRequired = true
//            )
//        }
//    }
//
//    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
//        val parsed = Jsoup.parse(fullText)
//        // Remove .dfe-links-layout--featured from the HTML
//        parsed.select(".dfe-links-layout--featured").remove()
//
//        val top = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__top")
//        val center = HtmlTextFormatter.getText(parsed.html(), "#content .cdc-dfe-body__center")
//            .substringBefore("\nSee also").substringBefore("\nLearn more")
//
//        val level = when {
//            center.contains("Health Advisory") -> AlertLevel.Low
//            else -> AlertLevel.High
//        }
//
//        return alert.copy(summary = (top + "\n\n" + center).trim(), level = level)
//    }
//}