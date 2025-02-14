package com.kylecorry.bell.infrastructure.alerts.water

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Constants
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.allText
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import java.time.Duration

class USGSWaterAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://water.usgs.gov/alerts/project_alert.xml",
            "item",
            mapOf(
                "headline" to text("title"),
                "description" to text("description"),
                "link" to text("link"),
                "sent" to text("pubDate"),
                "identifier" to text("guid"),
                "categories" to allText("category")
            )
        )

        return rawAlerts.mapNotNull {
            val sent = DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null
            val categories = it["categories"]?.split(", ") ?: emptyList()
            val states = categories.filter { it.length == 2 }
            Alert(
                id = 0,
                identifier = it["identifier"] ?: "",
                sender = "USGS",
                sent = sent,
                source = getUUID(),
                category = Category.Meteorological,
                event = categories.filter { it.length > 2 }.maxByOrNull { it.length }
                    ?: it["headline"] ?: "",
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Unknown,
                expires = sent.plus(Duration.ofDays(Constants.DEFAULT_EXPIRATION_DAYS)),
                area = Area(states),
                link = it["link"],
                description = it["description"],
            )
        }
    }

    override fun getUUID(): String {
        return "eadbd059-d1fb-4f8d-a4aa-20dbc91da89b"
    }
}