package com.kylecorry.bell.infrastructure.persistence

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts")
    suspend fun getAll(): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): AlertEntity?

    @Upsert
    suspend fun upsert(alert: AlertEntity): Long

    @Delete
    suspend fun delete(alert: AlertEntity)

    @Query("DELETE FROM alerts WHERE sent < :time")
    suspend fun deleteOlderThan(time: Instant)

    @Query("DELETE FROM alerts WHERE expires IS NOT NULL AND expires < :time")
    suspend fun deleteExpired(time: Instant)
}