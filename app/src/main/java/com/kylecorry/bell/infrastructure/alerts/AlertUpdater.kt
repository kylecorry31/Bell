package com.kylecorry.bell.infrastructure.alerts

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
import com.kylecorry.bell.infrastructure.alerts.volcano.USGSVolcanoAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.NationalTsunamiAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.PacificTsunamiAlertSource
import com.kylecorry.bell.infrastructure.alerts.water.USGSWaterAlertSource
import com.kylecorry.bell.infrastructure.alerts.weather.NationalWeatherServiceAlertSource
import com.kylecorry.bell.infrastructure.internet.WebPageDownloader
import com.kylecorry.bell.infrastructure.persistence.AlertRepo
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.bell.infrastructure.summarization.Gemini
import com.kylecorry.bell.infrastructure.utils.ParallelCoroutineRunner
import com.kylecorry.bell.infrastructure.utils.StateUtils
import com.kylecorry.luna.coroutines.onIO

class AlertUpdater(private val context: Context) {

    private val repo = AlertRepo.getInstance(context)
    private val preferences = UserPreferences(context)
    private val gemini = Gemini(context, preferences.geminiApiKey)
    private val pageDownloader = WebPageDownloader(context)

    suspend fun update(
        setProgress: (Float) -> Unit = {},
        setLoadingMessage: (String) -> Unit = {},
        onAlertsUpdated: (List<Alert>) -> Unit = {},
        onlyUpdateVitalAlerts: Boolean = false
    ): List<Alert> {
        repo.cleanup()

        val state = preferences.state

        val alerts = repo.getAll()

        val sources = getSources(onlyUpdateVitalAlerts)

        var completedCount = 0
        val totalCount = sources.size
        val lock = Any()
        val failedSources = mutableSetOf<String>()

        val runner = ParallelCoroutineRunner()
        val allAlerts = runner.map(sources) {
            try {
                // TODO: If this fails, let the user know
                val sourceAlerts = it.load()
                    .filter {
                        it.isValid() && StateUtils.shouldShowAlert(
                            state,
                            it.area,
                            it.impactsBorderingStates
                        )
                    }
                    .sortedByDescending { it.sent }
                    .distinctBy { it.identifier }
                synchronized(lock) {
                    completedCount++
                    setProgress(completedCount.toFloat() / totalCount)
                }
                sourceAlerts
            } catch (e: Exception) {
                Log.e("AlertUpdater", "Failed to get alerts from ${it.getUUID()}", e)
                synchronized(lock) {
                    failedSources.add(it.getUUID())
                }
                emptyList()
            }
        }.flatten()

        val newAlerts = allAlerts.filterNot { alert ->
            alerts.any { it.identifier == alert.identifier && it.source == alert.source }
        }

        val updatedAlerts = allAlerts.mapNotNull { alert ->
            val existing =
                alerts.find { it.identifier == alert.identifier && it.source == alert.source }
            if (existing != null && existing.sent != alert.sent && existing.isTracked) {
                alert.copy(id = existing.id)
            } else {
                null
            }
        }

        // Delete any alerts that are no longer present
        val sourceIds = sources.map { it.getUUID() }
        val toDelete = alerts
            .filter { sourceIds.contains(it.source) }
            .filterNot {
                !failedSources.contains(it.source) && allAlerts.any { alert -> alert.identifier == it.identifier && alert.source == it.source }
            }
        toDelete.forEach { repo.delete(it) }
        onAlertsUpdated(repo.getAll())

        setProgress(0f)
        setLoadingMessage("summaries")

        var completedSummaryCount = 0

        // Generate summaries and save new/updated alerts
        (newAlerts + updatedAlerts).forEach { alert ->
            setProgress(completedSummaryCount.toFloat() / (newAlerts.size + updatedAlerts.size))

            if (alert.shouldDownload()) {
                reloadSummary(alert)
            } else {
                repo.upsert(alert)
            }
            // Don't update the list right away for old alerts
            if (completedSummaryCount % 10 == 0) {
                onAlertsUpdated(repo.getAll())
            }
            completedSummaryCount++
        }

        onAlertsUpdated(repo.getAll())
        Log.d(
            "AlertUpdater",
            "New: ${newAlerts.size}, Updated: ${updatedAlerts.size}, Deleted: ${toDelete.size}, Failed: ${failedSources.size}"
        )
        return newAlerts
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
            if (!vitalOnly) USPSAlertSource(context, preferences.state) else null
        )
    }

    suspend fun reloadSummary(alert: Alert): Alert = onIO {
        val sources = getSources()

        // Load the full text if it should use the link for the summary
        val fullText = if (alert.link != null) {
            pageDownloader.download(alert.link) ?: pageDownloader.downloadAsBrowser(alert.link)
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

    suspend fun generateAISummary(alert: Alert): Alert {
        val text = alert.fullText ?: alert.description ?: return alert
        val summary = gemini.summarize(text)
        val newAlert = alert.copy(llmSummary = summary)
        val id = repo.upsert(newAlert)
        return newAlert.copy(id = id)
    }
}