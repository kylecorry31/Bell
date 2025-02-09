package com.kylecorry.preparedness_feed.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts")
    suspend fun getAll(): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE _id = :id LIMIT 1")
    suspend fun get(id: Long): AlertEntity?

    @Upsert
    suspend fun upsert(alert: AlertEntity): Long

    @Delete
    suspend fun delete(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE published_date < :time")
    suspend fun deleteOlderThan(time: Instant)
}