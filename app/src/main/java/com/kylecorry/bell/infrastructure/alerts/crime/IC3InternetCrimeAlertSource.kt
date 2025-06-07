package com.kylecorry.bell.infrastructure.alerts.crime

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class IC3InternetCrimeAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://www.ic3.gov/PSA/RSS",
            "item",
            mapOf(
                "title" to text("title"),
                "link" to text("link"),
                "sent" to text("pubDate"),
                "identifier" to text("guid")
            )
        )

        return rawAlerts.mapNotNull {
            val title = it["title"] ?: return@mapNotNull null
            val sent = DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val identifier = it["identifier"] ?: return@mapNotNull null

            Alert(
                id = 0,
                identifier = identifier,
                sender = "IC3",
                sent = sent,
                source = getUUID(),
                category = Category.Security,
                event = title,
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Unknown,
                link = it["link"],
                isDownloadRequired = true
            )
        }

    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        return alert.copy(description = HtmlTextFormatter.getText(fullText, "#main"))
    }

    override fun getUUID(): String {
        return "fec0a816-2f51-43c6-9d49-1e346f0dedb0"
    }
}