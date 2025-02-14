package com.kylecorry.bell.infrastructure.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.ResponseType
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import java.time.Instant

@Entity(
    tableName = "alerts"
)
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    // Alert
    val identifier: String,
    val sender: String,
    val sent: Instant,
    val source: String,
    // Info
    val category: Category,
    val event: String,
    val urgency: Urgency,
    val severity: Severity,
    val certainty: Certainty,
    val responseType: ResponseType? = null,
    val effective: Instant? = null,
    val onset: Instant? = null,
    val expires: Instant? = null,
    val headline: String? = null,
    val description: String? = null,
    val instruction: String? = null,
    val link: String? = null,
    val area: Area? = null,
    val parameters: Map<String, String>? = null,
    // Additional info created by Bell
    val fullText: String? = null,
    val llmSummary: String? = null,
    val created: Instant = Instant.now(),
    val updated: Instant = Instant.now(),
    /**
     * Used to hide alerts that are not actively tracked
     */
    val isTracked: Boolean = true,
    val isDownloadRequired: Boolean = false,
    val redownloadIntervalDays: Long? = null,
    val impactsBorderingStates: Boolean = false,
) {
    fun toAlert(): Alert {
        return Alert(
            id,
            identifier,
            sender,
            sent,
            source,
            category,
            event,
            urgency,
            severity,
            certainty,
            responseType,
            effective,
            onset,
            expires,
            headline,
            description,
            instruction,
            link,
            area,
            parameters,
            fullText,
            llmSummary,
            created,
            updated,
            isTracked,
            isDownloadRequired,
            redownloadIntervalDays
        )
    }

    companion object {
        fun fromAlert(alert: Alert): AlertEntity {
            return AlertEntity(
                alert.id,
                alert.identifier,
                alert.sender,
                alert.sent,
                alert.source,
                alert.category,
                alert.event,
                alert.urgency,
                alert.severity,
                alert.certainty,
                alert.responseType,
                alert.effective,
                alert.onset,
                alert.expires,
                alert.headline,
                alert.description,
                alert.instruction,
                alert.link,
                alert.area,
                alert.parameters,
                alert.fullText,
                alert.llmSummary,
                alert.created,
                alert.updated,
                alert.isTracked,
                alert.isDownloadRequired,
                alert.redownloadIntervalDays
            )
        }
    }
}