package com.kylecorry.bell.domain

import java.time.ZonedDateTime

data class Alert(
    val id: Long,
    val title: String,
    val sourceSystem: String,
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
    // TODO: Intermediates - move to a separate model
    val useLinkForSummary: Boolean = true,
    val shouldSummarize: Boolean = true,
    val canSkipDownloadIfOld: Boolean = true,
    val summaryUpdateIntervalDays: Long? = null,
    val additionalAttributes: Map<String, String> = mapOf()
) {
    fun isExpired(time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return expirationDate?.isBefore(time) ?: false
    }

    fun requiresSummaryUpdate(time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        // Never load a summary for an ignored alert
        if (level == AlertLevel.Ignored) {
            return false
        }

        // Don't load a summary for an expired alert
        if (isExpired(time)) {
            return false
        }

        // If the alert is new and has a summary update interval, update the summary
        if (id == 0L && summaryUpdateIntervalDays != null) {
            return true
        }

        // If an update interval is set and the summary is old, update the summary
        if (summaryUpdateIntervalDays != null && updateDate.plusDays(summaryUpdateIntervalDays)
                .isBefore(time)
        ) {
            return true
        }

        // The alert is not new, and the above conditions are not met, so don't update the summary
        if (id != 0L) {
            return false
        }

        // The download can't be skipped
        if (!canSkipDownloadIfOld) {
            return true
        }

        // Otherwise, only load summaries for newer alerts
        return publishedDate.isAfter(ZonedDateTime.now().minusDays(7))
    }
}