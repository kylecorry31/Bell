package com.kylecorry.bell.infrastructure.alerts.volcano

import android.content.Context
import com.kylecorry.andromeda.core.capitalizeWords
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector.Companion.text
import com.kylecorry.bell.infrastructure.utils.StateUtils
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

class USGSVolcanoAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.JSON,
            "https://volcanoes.usgs.gov/vsc/api/volcanoApi/elevated",
            "$",
            mapOf(
                "name" to text("vName"),
                "link" to text("noticeUrl"),
                "identifier" to text("vnum"),
                "sent" to text("sentUtc"),
                "description" to text("noticeSynopsis"),
                "alertLevel" to text("alertLevel"),
                "volcanoCode" to text("volcanoCd"),
                "latitude" to text("lat"),
                "longitude" to text("long"),
                "threat" to text("nvewsThreat")
            )
        )

        return rawAlerts.mapNotNull {
            val state =
                it["volcanoCode"]?.takeWhile { it.isLetter() }?.let { StateUtils.getStateCode(it) }

            val severity = when (it["threat"]) {
                "Very High Threat" -> Severity.Extreme
                "High Threat" -> Severity.Severe
                "Moderate Threat" -> Severity.Moderate
                else -> Severity.Minor
            }

            Alert(
                id = 0,
                identifier = it["identifier"] ?: "",
                sender = "USGS",
                sent = DateTimeParser.parseInstant(it["sent"]?.let { it.replace(" ", "T") + "Z" }
                    ?: "") ?: return@mapNotNull null,
                source = getUUID(),
                category = Category.Geophysical,
                event = "Volcano ${
                    it["alertLevel"]?.lowercase()?.capitalizeWords()
                } for ${it["name"]}",
                urgency = Urgency.Unknown, // TODO: Determine urgency from alert level
                severity = severity,
                certainty = Certainty.Unknown,
                link = it["link"],
                description = it["description"],
                area = Area(
                    listOf(state ?: ""), circles = listOf(
                        Geofence(
                            Coordinate(
                                it["latitude"]?.toDoubleOrNull() ?: 0.0,
                                it["longitude"]?.toDoubleOrNull() ?: 0.0
                            ),
                            Distance.kilometers(0f)
                        )
                    )
                ),
            )
        }
    }

    override fun getUUID(): String {
        return "c0617b66-6ed5-48d7-8b5c-bc51b6f81764"
    }
}