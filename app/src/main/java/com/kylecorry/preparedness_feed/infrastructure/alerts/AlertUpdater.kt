package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.infrastructure.persistence.AlertRepo
import com.kylecorry.preparedness_feed.infrastructure.persistence.UserPreferences
import com.kylecorry.preparedness_feed.infrastructure.summarization.Gemini
import com.kylecorry.preparedness_feed.infrastructure.utils.ParallelCoroutineRunner
import java.time.ZonedDateTime

class AlertUpdater(private val context: Context) {

    private val repo = AlertRepo.getInstance(context)
    private val preferences = UserPreferences(context)
    private val gemini = Gemini(context, preferences.geminiApiKey)

    suspend fun update(
        setProgress: (Float) -> Unit = {},
        setLoadingMessage: (String) -> Unit = {},
        onAlertsUpdated: (List<Alert>) -> Unit = {}
    ) {
        repo.cleanup()

        val alerts = repo.getAll()

        val minTime = ZonedDateTime.now().minusDays(7)

        // TODO: Download alerts in parallel
        val sources = listOf(
            NationalWeatherServiceAlertSource(context, "RI"),
            ExecutiveOrderAlertSource(context),
            USGSEarthquakeAlertSource(context),
            USGSWaterAlertSource(context),
            SWPCAlertSource(context),
            CDCAlertSource(),
            USGSVolcanoAlertSource(),
        )

        var completedCount = 0
        val totalCount = sources.size
        val lock = Any()

        val runner = ParallelCoroutineRunner()
        val allAlerts = runner.map(sources) {
            tryOrDefault(emptyList()) {
                // TODO: If this fails, let the user know
                val sourceAlerts =
                    it.getAlerts(minTime).filter { alert -> alert.publishedDate.isAfter(minTime) }
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

        // If the source system is active only and an existing alert is not in the new alerts, remove it
        sources.filter { it.isActiveOnly() }.forEach { source ->
            val toDelete =
                alerts.filter { alert -> alert.sourceSystem == source.getSystemName() && allAlerts.none { alert.uniqueId == it.uniqueId && alert.sourceSystem == it.sourceSystem } }
            toDelete.forEach { repo.delete(it) }
        }

        setProgress(0f)
        setLoadingMessage("summaries")

        var completedSummaryCount = 0

        // Generate summaries and save new/updated alerts
        (newAlerts + updatedAlerts).forEach { alert ->
            setProgress(completedSummaryCount.toFloat() / (newAlerts.size + updatedAlerts.size))
            val summary = tryOrDefault(alert.summary) {
                if (!alert.shouldSummarize) {
                    alert.summary
                } else if (alert.useLinkForSummary) {
                    "## AI Summary\n\n${gemini.summarizeUrl(alert.link)}\n\n## Original Summary\n\n${alert.summary}"
                } else {
                    "## AI Summary\n\n${gemini.summarize(alert.summary)}\n\n## Original Summary\n\n${alert.summary}"
                }
            }
            val newAlert = alert.copy(summary = summary)
            repo.upsert(newAlert)
            onAlertsUpdated(repo.getAll())
            completedSummaryCount++
        }
    }

}