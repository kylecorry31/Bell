package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.infrastructure.alerts.earthquake.USGSEarthquakeAlertSource
import com.kylecorry.bell.infrastructure.alerts.economy.ConsumerPriceIndexAlertSource
import com.kylecorry.bell.infrastructure.alerts.fire.InciwebWildfireAlertSource
import com.kylecorry.bell.infrastructure.alerts.government.CongressionalBillsAlertSource
import com.kylecorry.bell.infrastructure.alerts.government.WhiteHousePresidentalActionsAlertSource
import com.kylecorry.bell.infrastructure.alerts.health.CDCAlertSource
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

        val runner = ParallelCoroutineRunner()
        val allAlerts = runner.map(sources) {
            tryOrDefault(emptyList()) {
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
            }
        }.flatten()

        val newAlerts = allAlerts.filter { alert ->
            alerts.none { it.uniqueId == alert.uniqueId && it.sourceSystem == alert.sourceSystem }
        }

        val updatedAlerts = allAlerts.mapNotNull { alert ->
            val existing =
                alerts.find { it.uniqueId == alert.uniqueId && it.sourceSystem == alert.sourceSystem && it.publishedDate.toInstant() != alert.publishedDate.toInstant() }
            if (existing != null) {
                alert.copy(id = existing.id)
            } else {
                null
            }
        }

        // Delete alerts that are no longer present in the feeds
        var anyDeleted = false
        sources.forEach { source ->
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
            val newAlert = reloadSummary(alert)
            val isOld = isOld(newAlert)
            // Don't update the list right away for old alerts
            if (!isOld) {
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
            CDCAlertSource(context),
            USGSVolcanoAlertSource(context),
            CongressionalBillsAlertSource(context),
            InciwebWildfireAlertSource(context),
            NationalTsunamiAlertSource(context),
            PacificTsunamiAlertSource(context),
            // Updated
            TravelAdvisoryAlertSource(context),
            ConsumerPriceIndexAlertSource(context),
        )
    }

    suspend fun reloadSummary(alert: Alert): Alert = onIO {
        val sources = getSources()

        // Old alerts are not summarized and don't immediately update the list
        val isOld = isOld(alert)
        if (isOld && alert.canSkipDownloadIfOld) {
            val id = repo.upsert(alert)
            return@onIO alert.copy(id = id)
        }

        // Load the full text if it should use the link for the summary
        val fullText = tryOrDefault(alert.summary) {
            if (alert.useLinkForSummary) {
                pageDownloader.download(alert.link)
            } else {
                alert.summary
            }
        }

        val source = sources.find { it.getSystemName() == alert.sourceSystem }
        val newAlert = source?.updateFromFullText(alert, fullText) ?: alert

        val summary = tryOrDefault(newAlert.summary) {
            if (isOld || !newAlert.shouldSummarize) {
                newAlert.summary
            } else {
                "### AI Summary\n\n${gemini.summarize(fullText)}\n\n### Original Summary\n\n${newAlert.summary}"
            }
        }
        val id = repo.upsert(newAlert.copy(summary = summary))
        newAlert.copy(summary = summary, id = id)
    }

    private fun isOld(alert: Alert): Boolean {
        return alert.publishedDate.isBefore(ZonedDateTime.now().minusDays(7))
    }

}