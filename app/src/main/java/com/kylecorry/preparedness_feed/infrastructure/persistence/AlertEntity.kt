package com.kylecorry.preparedness_feed.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertType
import java.time.Instant
import java.time.ZoneId

@Entity(
    tableName = "alerts"
)
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    var id: Long = 0,
    @ColumnInfo(name = "title")
    var title: String,
    @ColumnInfo(name = "source_system")
    var sourceSystem: String,
    @ColumnInfo(name = "type")
    var type: String,
    @ColumnInfo(name = "level")
    var level: String,
    @ColumnInfo(name = "link")
    var link: String,
    @ColumnInfo(name = "unique_id")
    var uniqueId: String,
    @ColumnInfo(name = "published_date")
    var publishedDate: Instant,
    @ColumnInfo(name = "summary")
    var summary: String,
    @ColumnInfo(name = "expiration_date")
    var expirationDate: Instant? = null,
) {
    fun toAlert(): Alert {
        return Alert(
            id,
            title,
            sourceSystem,
            AlertType.entries.find { it.name == type } ?: AlertType.Other,
            AlertLevel.entries.find { it.name == level } ?: AlertLevel.Other,
            link,
            uniqueId,
            publishedDate.atZone(ZoneId.systemDefault()),
            summary,
            expirationDate = expirationDate?.atZone(ZoneId.systemDefault())
        )
    }

    companion object {
        fun fromAlert(alert: Alert): AlertEntity {
            return AlertEntity(
                alert.id,
                alert.title,
                alert.sourceSystem,
                alert.type.name,
                alert.level.name,
                alert.link,
                alert.uniqueId,
                alert.publishedDate.toInstant(),
                alert.summary,
                alert.expirationDate?.toInstant(),
            )
        }
    }
}