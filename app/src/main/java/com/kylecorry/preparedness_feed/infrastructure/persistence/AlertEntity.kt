package com.kylecorry.preparedness_feed.infrastructure.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kylecorry.preparedness_feed.domain.Alert
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
    @ColumnInfo(name = "source")
    var source: String,
    @ColumnInfo(name = "type")
    var type: String,
    @ColumnInfo(name = "link")
    var link: String,
    @ColumnInfo(name = "unique_id")
    var uniqueId: String,
    @ColumnInfo(name = "published_date")
    var publishedDate: Instant,
    @ColumnInfo(name = "summary")
    var summary: String,
) {
    fun toAlert(): Alert {
        return Alert(
            id,
            title,
            source,
            type,
            link,
            uniqueId,
            publishedDate.atZone(ZoneId.systemDefault()),
            summary
        )
    }

    companion object {
        fun fromAlert(alert: Alert): AlertEntity {
            return AlertEntity(
                alert.id,
                alert.title,
                alert.source,
                alert.type,
                alert.link,
                alert.uniqueId,
                alert.publishedDate.toInstant(),
                alert.summary
            )
        }
    }
}