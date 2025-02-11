package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import android.util.Log
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.infrastructure.alerts.earthquake.USGSEarthquakeAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.BLSSummaryAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.GasolineDieselPricesAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.HeatingOilPropanePricesAlertSource
import com.kylecorry.bell.infrastructure.alerts.fire.InciwebWildfireAlertSource
import com.kylecorry.bell.infrastructure.alerts.government.CongressionalBillsAlertSource
import com.kylecorry.bell.infrastructure.alerts.government.WhiteHousePresidentalActionsAlertSource
import com.kylecorry.bell.infrastructure.alerts.health.HealthAlertNetworkAlertSource
import com.kylecorry.bell.infrastructure.alerts.health.USOutbreaksAlertSource
import com.kylecorry.bell.infrastructure.alerts.space_weather.SWPCAlertSource
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
import com.kylecorry.luna.coroutines.onIO
import java.time.ZonedDateTime

class AlertUpdater(private val context: Context) {

    private val repo = AlertRepo.getInstance(context)
    private val preferences = UserPreferences(context)
    private val gemini = Gemini(context, preferences.geminiApiKey)
    private val pageDownloader = WebPageDownloader(context)

    suspend fun update(
        setProgress: (Float) -> Unit = {},
        setLoadingMessage: (String) -> Unit = {},
        onAlertsUpdated: (List<Alert>) -> Unit = {}
    ) {
        repo.cleanup()

        val alerts = repo.getAll()

        val sources = getSources()

        var completedCount = 0
        val totalCount = sources.size
        val lock = Any()
        val failedSources = mutableSetOf<AlertSource>()

        val runner = ParallelCoroutineRunner()
        val allAlerts = runner.map(sources) {
            try {
                // TODO: If this fails, let the user know
                val sourceAlerts = it.getAlerts()
                    .filterNot { it.isExpired() }
                    .sortedByDescending { it.publishedDate }
                    .distinctBy { it.uniqueId }
                synchronized(lock) {
                    completedCount++
                    setProgress(completedCount.toFloat() / totalCount)
                }
                sourceAlerts
            } catch (e: Exception) {
                Log.e("AlertUpdater", "Failed to get alerts from ${it.getSystemName()}", e)
                synchronized(lock) {
                    failedSources.add(it)
                }
                emptyList()
            }
        }.flatten()

        val newAlerts = allAlerts.filter { alert ->
            alerts.none { it.uniqueId == alert.uniqueId && it.sourceSystem == alert.sourceSystem }
        }

        val updatedAlerts = allAlerts.mapNotNull { alert ->
            val existing =
                alerts.find { it.uniqueId == alert.uniqueId && it.sourceSystem == alert.sourceSystem && it.publishedDate.toInstant() != alert.publishedDate.toInstant() }
            if (existing != null && existing.level != AlertLevel.Ignored) {
                alert.copy(id = existing.id, updateDate = existing.updateDate)
            } else {
                null
            }
        }

        // Delete alerts that are no longer present in the feeds
        var anyDeleted = false
        sources.filter { !failedSources.contains(it) }.forEach { source ->
            val toDelete =
                alerts.filter { alert -> alert.sourceSystem == source.getSystemName() && allAlerts.none { alert.uniqueId == it.uniqueId && alert.sourceSystem == it.sourceSystem } }
            anyDeleted = anyDeleted || toDelete.isNotEmpty()
            toDelete.forEach { repo.delete(it) }
        }
        if (anyDeleted) {
            onAlertsUpdated(repo.getAll())
        }

        setProgress(0f)
        setLoadingMessage("summaries")

        var completedSummaryCount = 0

        // Generate summaries and save new/updated alerts
        (newAlerts + updatedAlerts).forEach { alert ->
            setProgress(completedSummaryCount.toFloat() / (newAlerts.size + updatedAlerts.size))

            val newAlert = if (alert.requiresSummaryUpdate()) {
                reloadSummary(alert)
            } else {
                val newId = repo.upsert(alert)
                alert.copy(id = newId)
            }
            // Don't update the list right away for old alerts
            if (!isOld(newAlert)) {
                onAlertsUpdated(repo.getAll())
            }
            completedSummaryCount++
        }

        onAlertsUpdated(repo.getAll())
    }

    private fun getSources(): List<AlertSource> {
        return listOf(
            NationalWeatherServiceAlertSource(context, preferences.state),
            WhiteHousePresidentalActionsAlertSource(context),
            USGSEarthquakeAlertSource(context),
            USGSWaterAlertSource(context),
            SWPCAlertSource(context),
            HealthAlertNetworkAlertSource(context),
            USGSVolcanoAlertSource(context),
            CongressionalBillsAlertSource(context),
            InciwebWildfireAlertSource(context),
            NationalTsunamiAlertSource(context),
            PacificTsunamiAlertSource(context),
            TravelAdvisoryAlertSource(context),
            BLSSummaryAlertSource(context),
            GasolineDieselPricesAlertSource(context),
            HeatingOilPropanePricesAlertSource(context),
            USOutbreaksAlertSource(context)
        )
    }

    suspend fun reloadSummary(alert: Alert): Alert = onIO {
        val sources = getSources()

        // Load the full text if it should use the link for the summary
        val fullText = tryOrDefault(alert.summary) {
            if (alert.useLinkForSummary) {
                pageDownloader.download(alert.link)
            } else {
                alert.summary
            }
        }

        // TODO: This isn't correct
        val source = sources.find { it.getSystemName() == alert.sourceSystem }
        val newAlert = source?.updateFromFullText(alert, fullText) ?: alert
        val id = repo.upsert(newAlert)
        newAlert.copy(id = id)
    }

    suspend fun generateAISummary(alert: Alert): Alert {
        val summary = gemini.summarize(alert.fullText ?: alert.summary)
        val newAlert = alert.copy(llmSummary = summary)
        val id = repo.upsert(newAlert)
        return newAlert.copy(id = id)
    }

    private fun isOld(alert: Alert): Boolean {
        return alert.publishedDate.isBefore(ZonedDateTime.now().minusDays(7))
    }

}