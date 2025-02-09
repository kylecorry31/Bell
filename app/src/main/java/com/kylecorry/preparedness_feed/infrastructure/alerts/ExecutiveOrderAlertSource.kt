package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import java.time.ZonedDateTime

class ExecutiveOrderAlertSource(context: Context) : RssAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.whitehouse.gov/presidential-actions/feed/"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(type = AlertType.Government, level = AlertLevel.Order)
        }.distinctBy { it.title }
    }
}