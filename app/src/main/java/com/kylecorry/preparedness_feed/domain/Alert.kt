package com.kylecorry.preparedness_feed.domain

import java.time.ZonedDateTime

data class Alert(
    val id: Long,
    val title: String,
    val source: String,
    val type: String,
    val link: String,
    val uniqueId: String,
    val publishedDate: ZonedDateTime,
    val summary: String,
    val useLinkForSummary: Boolean = true,
    val shouldSummarize: Boolean = true
)