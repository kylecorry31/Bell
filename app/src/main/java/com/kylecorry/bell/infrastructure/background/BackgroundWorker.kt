package com.kylecorry.bell.infrastructure.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kylecorry.andromeda.background.IPeriodicTaskScheduler
import com.kylecorry.andromeda.background.PeriodicTaskSchedulerFactory
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.bell.R
import com.kylecorry.bell.app.NavigationUtils
import com.kylecorry.bell.app.NotificationChannels
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.infrastructure.alerts.AlertUpdater
import com.kylecorry.bell.ui.mappers.CategoryMapper
import java.time.Duration

class BackgroundWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("BackgroundWorker", "Updating alerts")
        val newAlerts = AlertUpdater(applicationContext).update()
        val importantAlerts =
            newAlerts.filter { it.severity != Severity.Minor && it.severity != Severity.Unknown }

        if (importantAlerts.any()) {
            val grouped = importantAlerts.groupBy { it.category }
            for (group in grouped) {
                val message =
                    group.value.joinToString("\n") { "[${it.severity.name.uppercase()}] ${it.event}" }
                val notification = Notify.alert(
                    applicationContext,
                    NotificationChannels.CHANNEL_ID_ALERTS,
                    CategoryMapper.getName(applicationContext, group.key),
                    message,
                    R.drawable.alert_circle,
                    group = "alerts",
                    intent = NavigationUtils.pendingIntent(applicationContext, R.id.action_main)
                )
                Notify.send(applicationContext, group.key.ordinal, notification)
            }
        } else {
            Log.d("BackgroundWorker", "No important alerts found")
        }

        Log.d("BackgroundWorker", "Found ${newAlerts.size} new alerts")
        return Result.success()
    }

    companion object {
        private const val UNIQUE_ID = 17023481

        fun enable(context: Context, enabled: Boolean) {
            if (enabled) {
                scheduler(context).interval(Duration.ofHours(1))
            } else {
                scheduler(context).cancel()
            }
        }

        private fun scheduler(context: Context): IPeriodicTaskScheduler {
            return PeriodicTaskSchedulerFactory(context).deferrable(
                BackgroundWorker::class.java,
                UNIQUE_ID
            )
        }
    }
}