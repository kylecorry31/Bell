//package com.kylecorry.bell.infrastructure.alerts.government
//
//import android.content.Context
//import com.kylecorry.bell.domain.Alert
//import com.kylecorry.bell.domain.AlertLevel
//import com.kylecorry.bell.domain.AlertType
//import com.kylecorry.bell.domain.Constants
//import com.kylecorry.bell.domain.SourceSystem
//import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
//import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
//import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
//
//class CongressionalBillsAlertSource(context: Context) : BaseAlertSource(context) {
//    override fun getSpecification(): AlertSpecification {
//        return rss(
//            SourceSystem.CongressBills,
//            "https://www.congress.gov/rss/presented-to-president.xml",
//            AlertType.Government,
//            AlertLevel.Information
//        )
//    }
//
//    override fun process(alerts: List<Alert>): List<Alert> {
//        return alerts.mapNotNull {
//            if (it.link.contains("concurrent-resolution")) {
//                return@mapNotNull null
//            }
//            it.copy(
//                title = "Bill: ${it.title}",
//                expirationDate = it.publishedDate.plusDays(Constants.DEFAULT_EXPIRATION_DAYS)
//            )
//        }
//    }
//
//    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
//        val summary = HtmlTextFormatter.getText(fullText, "#bill-summary")
//        return alert.copy(summary = summary)
//    }
//}