package com.kylecorry.bell.domain

interface AlertSource {
    suspend fun getAlerts(): List<Alert>
    fun getSystemName(): String
    fun updateFromFullText(alert: Alert, fullText: String): Alert {
        return alert
    }
}