//package com.kylecorry.bell.infrastructure.alerts.water
//
//import android.content.Context
//import com.kylecorry.bell.domain.Alert
//import com.kylecorry.bell.domain.AlertLevel
//import com.kylecorry.bell.domain.AlertType
//import com.kylecorry.bell.domain.SourceSystem
//import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
//import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
//
//class USGSWaterAlertSource(context: Context) : BaseAlertSource(context) {
//
//    private val locationRegex = Regex("PROJECT ALERT NOTICE \\((.*)\\)")
//
//    override fun process(alerts: List<Alert>): List<Alert> {
//        return alerts.map {
//            val location = locationRegex.find(it.title)?.groupValues?.get(1) ?: ""
//            it.copy(
//                link = it.link.replace("http://", "https://"),
//                title = it.title.substringAfter(") ") + " ($location)",
//                expirationDate = it.publishedDate.plusDays(4)
//            )
//        }
//    }
//
//    override fun getSpecification(): AlertSpecification {
//        return rss(
//            SourceSystem.USGSWater,
//            "https://water.usgs.gov/alerts/project_alert.xml",
//            AlertType.Water,
//            AlertLevel.High
//        )
//    }
//}