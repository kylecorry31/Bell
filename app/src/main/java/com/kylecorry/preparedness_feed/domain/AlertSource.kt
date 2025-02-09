package com.kylecorry.preparedness_feed.domain

import java.time.ZonedDateTime

interface AlertSource {
    suspend fun getAlerts(since: ZonedDateTime): List<Alert>
    fun getSystemName(): String
    fun isActiveOnly(): Boolean
    fun updateFromFullText(alert: Alert, fullText: String): Alert {
        return alert
    }
}