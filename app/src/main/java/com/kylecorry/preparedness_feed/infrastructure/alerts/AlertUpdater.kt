package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.infrastructure.persistence.AlertRepo
import com.kylecorry.preparedness_feed.infrastructure.persistence.UserPreferences
import com.kylecorry.preparedness_feed.infrastructure.summarization.Gemini
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
            CDCAlertSource()
        )

        val allAlerts = mutableListOf<Alert>()

        for (index in sources.indices) {
            setProgress(index.toFloat() / sources.size)
            val source = sources[index]
            allAlerts.addAll(source.getAlerts(minTime))
        }

        // Remove all old alerts
        allAlerts.removeIf { it.publishedDate.isBefore(minTime) }

        val newAlerts = allAlerts.filter { alert ->
            alerts.none { it.uniqueId == alert.uniqueId && it.type == alert.type }
        }

        val updatedAlerts = allAlerts.mapNotNull { alert ->
            val existing =
                alerts.find { it.uniqueId == alert.uniqueId && it.type == alert.type && it.publishedDate.toInstant() != alert.publishedDate.toInstant() }
            if (existing != null) {
                alert.copy(id = existing.id)
            } else {
                null
            }
        }

        setProgress(0f)
        setLoadingMessage("summaries")

        var completedSummaryCount = 0

        // Generate summaries and save new/updated alerts
        (newAlerts + updatedAlerts).forEach { alert ->
            setProgress(completedSummaryCount.toFloat() / (newAlerts.size + updatedAlerts.size))
            val summary = if (!alert.shouldSummarize) {
                alert.summary
            } else if (alert.useLinkForSummary) {
                gemini.summarizeUrl(alert.link)
            } else {
                gemini.summarize(
                    alert.summary
                )
            }
            val newAlert = alert.copy(summary = summary)
            repo.upsert(newAlert)
            onAlertsUpdated(repo.getAll())
            completedSummaryCount++
        }
    }

}