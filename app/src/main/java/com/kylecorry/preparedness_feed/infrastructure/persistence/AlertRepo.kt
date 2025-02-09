package com.kylecorry.preparedness_feed.infrastructure.persistence

import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.preparedness_feed.domain.Alert
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
        dao.deleteExpired(Instant.now())
        dao.deleteOlderThan(Instant.now().minus(Duration.ofDays(DAYS_TO_KEEP_ALERTS)))
    }

    companion object {
        const val DAYS_TO_KEEP_ALERTS = 14L

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