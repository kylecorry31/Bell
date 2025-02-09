package com.kylecorry.bell.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.bell.domain.Alert
import java.time.Duration
import java.time.Instant

class AlertRepo private constructor(context: Context) {

    private val dao = AppDatabase.getInstance(context).alertDao()

    suspend fun getAll(): List<Alert> = onIO {
        dao.getAll().map { it.toAlert() }.sortedByDescending { it.publishedDate }
    }

    suspend fun upsert(alert: Alert) = onIO {
        dao.upsert(AlertEntity.fromAlert(alert))
    }

    suspend fun delete(alert: Alert) = onIO {
        dao.delete(AlertEntity.fromAlert(alert))
    }

    suspend fun cleanup() = onIO {
        // Don't delete expired alerts right away in case they still appear in the feed
        dao.deleteExpired(Instant.now().minus(Duration.ofDays(30)))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: AlertRepo? = null

        @Synchronized
        fun getInstance(context: Context): AlertRepo {
            if (instance == null) {
                instance = AlertRepo(context.applicationContext)
            }
            return instance!!
        }
    }
}