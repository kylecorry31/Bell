package com.kylecorry.bell.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
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
    @ColumnInfo(name = "update_date")
    var updateDate: Instant = Instant.now()
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
            updateDate.atZone(ZoneId.systemDefault()),
            publishedDate.atZone(ZoneId.systemDefault()),
            expirationDate?.atZone(ZoneId.systemDefault()),
            summary,
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
                alert.updateDate.toInstant()
            )
        }
    }
}