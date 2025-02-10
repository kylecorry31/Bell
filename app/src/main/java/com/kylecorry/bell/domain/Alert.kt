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
    val publishedDate: ZonedDateTime,
    val expirationDate: ZonedDateTime? = null,
    val summary: String,
    val useLinkForSummary: Boolean = true,
    val shouldSummarize: Boolean = true,
    val canSkipDownloadIfOld: Boolean = true,
    val additionalAttributes: Map<String, String> = mapOf()
) {
    fun isExpired(time: ZonedDateTime = ZonedDateTime.now()): Boolean {
        return expirationDate?.isBefore(time) ?: false
    }
}