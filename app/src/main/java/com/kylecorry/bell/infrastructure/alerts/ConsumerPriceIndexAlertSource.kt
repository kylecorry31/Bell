package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector

class ConsumerPriceIndexAlertSource(context: Context) : BaseAlertSource(context) {
    override fun getSpecification(): AlertSpecification {
        return atom(
            "BLS Consumer Price Index",
            "https://www.bls.gov/feed/cpi.rss",
            AlertType.Economy,
            AlertLevel.Announcement,
            summary = Selector.text("content")
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(uniqueId = "cpi")
        }.sortedByDescending { it.publishedDate }.take(1)
    }
}