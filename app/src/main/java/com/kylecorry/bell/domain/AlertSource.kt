package com.kylecorry.bell.domain

interface AlertSource {
    suspend fun getAlerts(): List<Alert>
    fun getSystemName(): SourceSystem
    fun updateFromFullText(alert: Alert, fullText: String): Alert {
        return alert
    }
}