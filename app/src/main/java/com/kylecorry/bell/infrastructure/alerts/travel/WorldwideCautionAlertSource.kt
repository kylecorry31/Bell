package com.kylecorry.bell.infrastructure.alerts.travel

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
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.allText
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import java.time.Instant
import java.time.ZoneId

class WorldwideCautionAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)
    private val delimiter = "::::"

    override suspend fun load(): List<Alert> {
        val link =
            "https://travel.state.gov/en/international-travel/travel-advisories/global-events/worldwide-caution.html"

        val rawAlerts =
            loader.load(
                FileType.HTML,
                link,
                "#tsg_right_section_main_container",
                mapOf(
                    "date" to allText(".summary-regular b", delimiter = delimiter),
                    "summary" to allText(".summary-regular", delimiter = delimiter),
                    "content" to allText(".pageContent", delimiter = delimiter, raw = true)
                )
            )

        return rawAlerts.flatMap {
            val dates = it["date"]?.split(delimiter) ?: emptyList()
            val summaries = it["summary"]?.split(delimiter) ?: emptyList()
            // Summaries are included in the contents, so filter them out
            val contents =
                it["content"]?.split(delimiter)?.filterIndexed { index, _ -> index % 2 == 1 }
                    ?: emptyList()

            return summaries.mapIndexed { index, summary ->
                val dateString = dates.getOrNull(index) ?: ""
                val date = DateTimeParser.parseInstant(
                    dateString,
                    ZoneId.of("America/New_York")
                )
                Alert(
                    id = 0,
                    identifier = "worldwide-caution-${date?.toEpochMilli()}",
                    sender = "US Department of State",
                    sent = date ?: Instant.now(),
                    source = getUUID(),
                    category = Category.Security,
                    event = summary.replace("$dateString - ", "").trim(),
                    urgency = Urgency.Unknown,
                    severity = Severity.Unknown,
                    certainty = Certainty.Unknown,
                    description = contents.getOrNull(index)
                        ?.let {
                            HtmlTextFormatter.getText(
                                it.replace("&nbsp;", " ")
                                    .replace("</p>", "</p><br/><br/>")
                            )
                        },
                    link = link

                )
            }
        }
    }

    override fun getUUID(): String {
        return "edc63fb3-2510-43f5-bc8a-a548be1ea236"
    }
}