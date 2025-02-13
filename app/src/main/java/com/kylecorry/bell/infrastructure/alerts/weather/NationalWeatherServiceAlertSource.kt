package com.kylecorry.bell.infrastructure.alerts.weather

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.domain.getByCode
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.attr
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text

class NationalWeatherServiceAlertSource(context: Context, private val state: String) :
    AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts =
            loader.load(
                FileType.XML,
                "https://api.weather.gov/alerts/active.atom?area=$state",
                "entry",
                mapOf(
                    "identifier" to text("id"),
                    "sender" to text("author name"),
                    "sent" to text("cap:sent"),
                    "published" to text("published"),
                    "updated" to text("updated"),
                    "category" to text("cap:category"),
                    "event" to text("cap:event"),
                    "urgency" to text("cap:urgency"),
                    "severity" to text("cap:severity"),
                    "certainty" to text("cap:certainty"),
                    "effective" to text("cap:effective"),
                    "onset" to text("cap:onset"),
                    "expires" to text("cap:expires"),
                    "headline" to text("title"),
                    "description" to text("summary"),
                    "link" to attr("link", "href"),
                    "areaDescription" to text("cap:areaDesc")
                ),
                mapOf(
                    "If-Modified-Since" to "Thu, 01 Jan 2030 00:00:00 GMT"
                ),
                mitigate304 = false
            )

        return rawAlerts.mapNotNull {
            Alert(
                id = 0,
                identifier = it["identifier"] ?: "",
                sender = it["sender"] ?: "NWS",
                sent = DateTimeParser.parseInstant(
                    it["sent"] ?: it["updated"] ?: it["published"] ?: ""
                )
                    ?: return@mapNotNull null,
                source = getUUID(),
                category = Category.entries.getByCode(it["category"] ?: "")
                    ?: Category.Meteorological,
                event = it["event"] ?: "",
                urgency = Urgency.entries.getByCode(it["urgency"] ?: "") ?: Urgency.Unknown,
                severity = Severity.entries.getByCode(it["severity"] ?: "") ?: Severity.Unknown,
                certainty = Certainty.entries.getByCode(it["certainty"] ?: "") ?: Certainty.Unknown,
                effective = DateTimeParser.parseInstant(it["effective"] ?: ""),
                onset = DateTimeParser.parseInstant(it["onset"] ?: ""),
                expires = DateTimeParser.parseInstant(it["expires"] ?: ""),
                headline = it["headline"],
                description = it["description"],
                link = it["link"],
                area = it["areaDescription"]?.let { Area(it, listOf(state)) },
            )
        }
    }

    override fun getUUID(): String {
        return "b0781ea9-aa68-4096-8516-dd56c24c7b2f"
    }
}