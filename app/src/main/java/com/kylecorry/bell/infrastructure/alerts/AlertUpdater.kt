package com.kylecorry.bell.infrastructure.alerts

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.infrastructure.alerts.crime.IC3InternetCrimeAlertSource
import com.kylecorry.bell.infrastructure.alerts.crime.NationalTerrorismAdvisoryAlertSource
import com.kylecorry.bell.infrastructure.alerts.earthquake.USGSEarthquakeAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.BLSSummaryAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.FuelPricesAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.USPSAlertSource
import com.kylecorry.bell.infrastructure.alerts.fire.InciwebWildfireAlertSource
import com.kylecorry.bell.infrastructure.alerts.health.HealthAlertNetworkAlertSource
import com.kylecorry.bell.infrastructure.alerts.health.USOutbreaksAlertSource
import com.kylecorry.bell.infrastructure.alerts.space_weather.SWPCAlertSource
import com.kylecorry.bell.infrastructure.alerts.space_weather.SentryAsteroidAlertSource
import com.kylecorry.bell.infrastructure.alerts.travel.TravelAdvisoryAlertSource
import com.kylecorry.bell.infrastructure.alerts.travel.WorldwideCautionAlertSource
import com.kylecorry.bell.infrastructure.alerts.volcano.USGSVolcanoAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.NationalTsunamiAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.PacificTsunamiAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.USGSWaterAlertSource
import com.kylecorry.bell.infrastructure.alerts.weather.NationalWeatherServiceAlertSource
import com.kylecorry.bell.infrastructure.internet.WebPageDownloader
import com.kylecorry.bell.infrastructure.persistence.AlertRepo
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.bell.infrastructure.utils.ParallelCoroutineRunner
import com.kylecorry.bell.infrastructure.utils.StateUtils
import com.kylecorry.luna.coroutines.CoroutineQueueRunner
import com.kylecorry.luna.coroutines.onIO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.Duration

class AlertUpdater private constructor(private val context: Context) {

    private val repo = AlertRepo.getInstance(context)
    private val preferences = UserPreferences(context)
    private val pageDownloader = WebPageDownloader(context)

    private val lock = Mutex()

    suspend fun update(
        setProgress: (Float) -> Unit = {},
        onAlertsUpdated: (List<Alert>) -> Unit = {},
        onlyUpdateVitalAlerts: Boolean = false
    ): List<Alert> {
        lock.withLock {
            repo.cleanup()

            val state = preferences.state

            val alerts = repo.getAll()

            val sources = getSources(onlyUpdateVitalAlerts)

            var completedCount = 0
            val totalCount = sources.size
            val lock = Any()
            val allNewAlerts = mutableListOf<Alert>()

            val updateQueue = CoroutineQueueRunner()

            val runner = ParallelCoroutineRunner(16)
            runner.run(sources) { source ->
                try {
                    Log.d("AlertUpdater", "Loading ${source.getUUID()}")
                    val oldAlerts = alerts.filter { it.source == source.getUUID() }
                    val currentAlerts = withTimeout(TIMEOUT.toMillis()) {
                        source.load()
                            .filter {
                                it.isValid() && StateUtils.shouldShowAlert(
                                    state,
                                    it.area,
                                    it.impactsBorderingStates
                                )
                            }
                            .sortedByDescending { it.sent }
                            .distinctBy { it.identifier }
                    }

                    val newAlerts = currentAlerts.filterNot { alert ->
                        oldAlerts.any { it.identifier == alert.identifier && it.source == alert.source }
                    }

                    val updatedAlerts = currentAlerts.mapNotNull { alert ->
                        val existing =
                            oldAlerts.find { it.identifier == alert.identifier && it.source == alert.source }
                        if (existing != null && existing.sent != alert.sent && existing.isTracked) {
                            alert.copy(id = existing.id)
                        } else {
                            null
                        }
                    }

                    val toDelete = oldAlerts.filterNot {
                        currentAlerts.any { alert -> alert.identifier == it.identifier && alert.source == it.source }
                    }

                    toDelete.forEach { repo.delete(it) }

                    val updateRunner = ParallelCoroutineRunner(16)
                    updateRunner.run(newAlerts + updatedAlerts) { alert ->
                        if (alert.shouldDownload()) {
                            reloadSummary(alert)
                        } else {
                            repo.upsert(alert)
                        }
                    }

                    synchronized(lock) {
                        completedCount++
                        setProgress(completedCount.toFloat() / totalCount)
                        allNewAlerts.addAll(newAlerts)
                    }
                    updateQueue.enqueue {
                        onAlertsUpdated(repo.getAll())
                    }
                    Log.d("AlertUpdater", "Loaded ${source.getUUID()}")
                } catch (e: Exception) {
                    Log.e("AlertUpdater", "Failed to get alerts from ${source.getUUID()}", e)
                    synchronized(lock) {
                        completedCount++
                        setProgress(completedCount.toFloat() / totalCount)
                    }
                }
            }

            return allNewAlerts
        }
    }

    private fun getSources(vitalOnly: Boolean = false): List<AlertSource> {
        return listOfNotNull(
            NationalWeatherServiceAlertSource(context, preferences.state),
            if (!vitalOnly) USGSEarthquakeAlertSource(context) else null,
            if (!vitalOnly) USGSWaterAlertSource(context) else null,
            if (!vitalOnly) SWPCAlertSource(context) else null,
            if (!vitalOnly) HealthAlertNetworkAlertSource(context) else null,
            USGSVolcanoAlertSource(context),
            InciwebWildfireAlertSource(context),
            NationalTsunamiAlertSource(context),
            PacificTsunamiAlertSource(context),
            if (!vitalOnly) TravelAdvisoryAlertSource(context) else null,
            if (!vitalOnly) BLSSummaryAlertSource(context) else null,
            if (!vitalOnly) FuelPricesAlertSource(context) else null,
            if (!vitalOnly) USOutbreaksAlertSource(context) else null,
            if (!vitalOnly) IC3InternetCrimeAlertSource(context) else null,
            if (!vitalOnly) NationalTerrorismAdvisoryAlertSource(context) else null,
            if (!vitalOnly) SentryAsteroidAlertSource(context) else null,
            if (!vitalOnly) USPSAlertSource(context, preferences.state) else null,
            if (!vitalOnly) WorldwideCautionAlertSource(context) else null
        )
    }

    suspend fun reloadSummary(alert: Alert): Alert = onIO {
        val sources = getSources()

        // Load the full text if it should use the link for the summary
        val fullText = if (alert.link != null) {
            pageDownloader.download(alert.link, TIMEOUT)
                ?: pageDownloader.downloadAsBrowser(alert.link, TIMEOUT)
                ?: alert.description
        } else {
            alert.description
        } ?: ""

        val source = sources.find { it.getUUID() == alert.source }
        val newAlert =
            (source?.updateFromFullText(alert, fullText) ?: alert).copy(isDownloadRequired = false)
        val id = repo.upsert(newAlert)
        newAlert.copy(id = id)
    }

    companion object {

        private val TIMEOUT = Duration.ofSeconds(15)

        @SuppressLint("StaticFieldLeak")
        private var instance: AlertUpdater? = null

        @Synchronized
        fun getInstance(context: Context): AlertUpdater {
            if (instance == null) {
                instance = AlertUpdater(context.applicationContext)
            }
            return instance!!
        }
    }
}