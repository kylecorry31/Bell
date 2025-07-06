package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.allText
import com.kylecorry.bell.infrastructure.utils.StateUtils
import java.time.Instant

class USPSAlertSource(context: Context, private val state: String) :
    AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val stateName = StateUtils.getStateName(state) ?: return emptyList()
        val link = "https://about.usps.com/newsroom/service-alerts/residential/${
            stateName.replace(
                " ",
                "-"
            ).lowercase()
        }.htm"

        val rawAlerts =
            loader.load(
                FileType.HTML,
                link,
                "#svc-content",
                mapOf(
                    "description" to allText("p", delimiter = "\n\n")
                )
            )

        // There should be only 1
        return rawAlerts.mapNotNull {
            if (it["description"]?.contains("No disruptions are currently reported") == true) {
                return@mapNotNull null
            }
            Alert(
                id = 0,
                identifier = "postal-disruption-${state}",
                sender = "USPS",
                sent = Instant.now(),
                source = getUUID(),
                category = Category.Infrastructure,
                event = "Postal service disruptions",
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Observed,
                description = it["description"],
                link = link,
                area = Area(listOf(state), state),
            )
        }
    }

    override fun getUUID(): String {
        return "72d56f8d-c196-4767-ba3f-e4dc8be05cab"
    }
}