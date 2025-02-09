package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import java.time.ZonedDateTime

class ExecutiveOrderAlertSource(context: Context) : RssAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.whitehouse.gov/presidential-actions/feed/"
    }

    override fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts.map {
            it.copy(type = "Executive Order", source = "White House")
        }.distinctBy { it.title }
    }
}