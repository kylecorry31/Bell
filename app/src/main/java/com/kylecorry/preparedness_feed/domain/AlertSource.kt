package com.kylecorry.preparedness_feed.domain

import java.time.ZonedDateTime

interface AlertSource {
    suspend fun getAlerts(since: ZonedDateTime): List<Alert>
}