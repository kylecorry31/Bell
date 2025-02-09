package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.infrastructure.internet.WebPageDownloader
import com.kylecorry.bell.infrastructure.persistence.AlertRepo
import com.kylecorry.bell.infrastructure.persistence.AlertRepo.Companion.DAYS_TO_KEEP_ALERTS
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.bell.infrastructure.summarization.Gemini
import com.kylecorry.bell.infrastructure.utils.ParallelCoroutineRunner
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

        val minTime = ZonedDateTime.now().minusDays(DAYS_TO_KEEP_ALERTS)

        val sources = listOf(
            NationalWeatherServiceAlertSource(context, "RI"),
            WhiteHousePresidentalActionsAlertSource(context),
            USGSEarthquakeAlertSource(context),
            USGSWaterAlertSource(context),
            SWPCAlertSource(context),
            CDCAlertSource(),
            USGSVolcanoAlertSource(),
            CongressionalBillsAlertSource(context),
            InciwebWildfireAlertSource(context),
            NationalTsunamiAlertSource(context),
            PacificTsunamiAlertSource(context),
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
        var anyDeleted = false
        sources.filter { it.isActiveOnly() }.forEach { source ->
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
                if (!newAlert.shouldSummarize) {
                    newAlert.summary
                } else {
                    "### AI Summary\n\n${gemini.summarize(fullText)}\n\n### Original Summary\n\n${newAlert.summary}"
                }
            }
            repo.upsert(newAlert.copy(summary = summary))
            onAlertsUpdated(repo.getAll())
            completedSummaryCount++
        }
    }

}