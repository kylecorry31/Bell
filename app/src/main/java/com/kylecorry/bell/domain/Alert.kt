package com.kylecorry.bell.domain

import java.time.ZonedDateTime

data class Alert(
    val id: Long,
    val title: String,
    val sourceSystem: SourceSystem,
    val type: AlertType,
    val level: AlertLevel,
    val link: String,
    val uniqueId: String,
    val updateDate: ZonedDateTime,
    val publishedDate: ZonedDateTime,
    val expirationDate: ZonedDateTime? = null,
    val summary: String,
    val fullText: String? = null,
    val llmSummary: String? = null,
    val wasSummaryDownloaded: Boolean = false,
    // TODO: Intermediates - move to a separate model
    val useLinkForSummary: Boolean = true,
    val isSummaryDownloadRequired: Boolean = false,
    val summaryUpdateIntervalDays: Long? = null,
    val additionalAttributes: Map<String, String> = mapOf()
) {
    fun isExpired(time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return expirationDate?.isBefore(time) ?: false
    }

    fun requiresSummaryUpdate(time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        // Summary download isn't required
        if (!isSummaryDownloadRequired) {
            return false
        }

        // Never load a summary for an ignored alert
        if (level == AlertLevel.Ignored) {
            return false
        }

        // Don't load a summary for an expired alert
        if (isExpired(time)) {
            return false
        }

        // If an update interval is set and the summary is old, update the summary
        if (summaryUpdateIntervalDays != null && updateDate.plusDays(summaryUpdateIntervalDays)
                .isBefore(time)
        ) {
            return true
        }

        // Only load for new alerts
        return id == 0L
    }
}