package com.kylecorry.bell.infrastructure.alerts

import com.kylecorry.bell.domain.Alert

interface AlertSource {
    suspend fun load(): List<Alert>
    fun getUUID(): String
    fun updateFromFullText(alert: Alert, fullText: String): Alert {
        return alert
    }
}