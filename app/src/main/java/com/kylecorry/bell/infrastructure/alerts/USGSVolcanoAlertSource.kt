package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import java.time.ZoneId

class USGSVolcanoAlertSource(context: Context) : BaseAlertSource(context) {
    override fun getSpecification(): AlertSpecification {
        return html(
            "USGS Volcanoes",
            "https://www.usgs.gov/programs/VHP/volcano-updates",
            items = ".usgs-vol-up-vonas",
            title = Selector.text("b"),
            link = Selector.value("https://www.usgs.gov/programs/VHP/volcano-updates"),
            uniqueId = Selector.text("b"),
            publishedDate = Selector.text(".hans-td:nth-child(2)") { it?.replace("Z", "") },
            summary = Selector.text(".hans-td span"),
            additionalAttributes = mapOf(
                "colorCode" to Selector.text(".hans-td:nth-child(2)", index = 2)
            ),
            defaultZoneId = ZoneId.of("UTC"),
            defaultAlertType = AlertType.Volcano
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {
            val colorCode = it.additionalAttributes["colorCode"] ?: return@mapNotNull null
            val level = when (colorCode.lowercase()) {
                "yellow" -> AlertLevel.Advisory
                "orange" -> AlertLevel.Watch
                "red" -> AlertLevel.Warning
                else -> AlertLevel.Other
            }

            if (level == AlertLevel.Other) {
                return@mapNotNull null
            }

            it.copy(
                title = "Volcano ${level.name} for ${it.title}",
                level = level,
                useLinkForSummary = false
            )
        }
    }
}