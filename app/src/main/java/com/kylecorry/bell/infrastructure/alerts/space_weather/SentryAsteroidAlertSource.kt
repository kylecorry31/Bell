package com.kylecorry.bell.infrastructure.alerts.space_weather

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
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import com.kylecorry.bell.ui.FormatService
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.SolMath.roundPlaces
import java.net.URLEncoder
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SentryAsteroidAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)
    private val formatter = FormatService.getInstance(context)

    private val torinoScaleDescription = mapOf(
        3 to "A close encounter, meriting attention by astronomers. Current calculations give a 1% or greater chance of collision capable of localized destruction. Most likely, new telescopic observations will lead to re-assignment to Level 0.",
        4 to "A close encounter, meriting attention by astronomers. Current calculations give a 1% or greater chance of collision capable of regional devastation. Most likely, new telescopic observations will lead to re-assignment to Level 0.",
        5 to "A close encounter posing a serious, but still uncertain threat of regional devastation. Critical attention by astronomers is needed to determine conclusively whether or not a collision will occur. Governmental contingency planning may be warranted.",
        6 to "A close encounter by a large object posing a serious but still uncertain threat of a global catastrophe. Critical attention by astronomers is needed to determine conclusively whether or not a collision will occur. Governmental contingency planning may be warranted.",
        7 to "A very close encounter by a large object, which if occurring over the next century, poses an unprecedented but still uncertain threat of a global catastrophe. For such a threat, international contingency planning is warranted, especially to determine urgently and conclusively whether or not a collision will occur.",
        8 to "A collision is certain, capable of causing localized destruction for an impact over land or possibly a tsunami if close offshore.",
        9 to "A collision is certain, capable of causing unprecedented regional devastation for a land impact or the threat of a major tsunami for an ocean impact.",
        20 to "A collision is certain, capable of causing global climatic catastrophe that may threaten the future of civilization as we know it, whether impacting land or ocean."
    )

    private val ignoreLowLevels = true

    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.JSON,
            "https://ssd-api.jpl.nasa.gov/sentry.api?ps-min=-4&ip-min=1e-6",
            "$.data",
            mapOf(
                "id" to text("id"),
                "observed" to text("last_obs"),
                "name" to text("des"),
                "range" to text("range"),
                "level" to text("ts_max"),
                "velocity" to text("v_inf"),
                "diameter" to text("diameter"),
                "probability" to text("ip")
            ),
            mitigate304 = false
        )

        return rawAlerts.mapNotNull {
            val id = it["id"] ?: return@mapNotNull null
            val name = it["name"] ?: return@mapNotNull null
            val observed =
                DateTimeParser.parseInstant(
                    it["observed"] ?: return@mapNotNull null,
                    ZoneId.of("UTC")
                )
                    ?: return@mapNotNull null
            val range = it["range"]?.split("-") ?: return@mapNotNull null
            val level = it["level"]?.toIntOrNull() ?: return@mapNotNull null
            val velocity = it["velocity"]?.toFloatCompat()?.roundPlaces(2)
            val diameter = it["diameter"]?.toFloatCompat()?.roundPlaces(4)
            val probability = it["probability"]?.toFloatCompat()?.roundPlaces(4)?.times(100)

            // Ignore very low level threats
            if (ignoreLowLevels && level <= 2) {
                return@mapNotNull null
            }

            val effectiveYear = range.firstOrNull()?.toIntOrNull() ?: return@mapNotNull null
            val expirationYear = range.lastOrNull()?.toIntOrNull() ?: return@mapNotNull null

            val timeUntilImpact = effectiveYear - ZonedDateTime.now(ZoneId.of("UTC")).year

            // If over a decade away and under level 6, ignore
            if (ignoreLowLevels && timeUntilImpact > 10 && level < 6) {
                return@mapNotNull null
            }

            // If over 3 decades away and level 6, ignore
            if (ignoreLowLevels && timeUntilImpact > 30 && level == 6) {
                return@mapNotNull null
            }

            val severity = when (level) {
                in 3..4 -> Severity.Moderate
                in 5..8 -> Severity.Severe
                in 9..10 -> Severity.Extreme
                else -> Severity.Minor
            }

            val certainty = when (level) {
                in 5..7 -> Certainty.Possible
                in 8..10 -> Certainty.Likely
                else -> Certainty.Unlikely
            }

            Alert(
                id = 0,
                identifier = id,
                sender = "NASA",
                sent = observed,
                source = getUUID(),
                category = Category.Other,
                event = "Potential Asteroid Impact (between $effectiveYear and $expirationYear)",
                urgency = Urgency.Future,
                severity = severity,
                certainty = certainty,
                link = "https://ssd-api.jpl.nasa.gov/sentry.api?des=${
                    URLEncoder.encode(
                        name,
                        "UTF-8"
                    )
                }",
                description = torinoScaleDescription[level],
                effective = ZonedDateTime.of(effectiveYear, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
                    .toInstant(),
                expires = ZonedDateTime.of(
                    expirationYear,
                    12,
                    31,
                    23,
                    59,
                    59,
                    9999,
                    ZoneId.of("UTC")
                ).toInstant(),
                parameters = mapOf(
                    "Name" to name,
                    "Diameter" to diameter.toString() + " km",
                    "Velocity" to velocity.toString() + " km/s",
                    "Probability" to probability.toString() + "%",
                    "Torino scale" to level.toString(),
                    "Impact range" to "$effectiveYear to $expirationYear"
                ),
                isDownloadRequired = true,
                redownloadIntervalDays = if (timeUntilImpact < 1) 1 else 15
            )
        }
    }

    override fun getUUID(): String {
        return "12f7c57c-a703-4c2a-aad3-617ee953f2af"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val impacts = select<Any>(fullText, "data").mapNotNull {
            val date =
                select(it, text("date"))?.let { parseFractionalDate(it) } ?: return@mapNotNull null
            val probability = select(it, text("ip"))?.toFloatCompat()?.times(100)?.roundPlaces(4)
                ?: return@mapNotNull null
            date to probability
        }.filter { it.first.isAfter(Instant.now()) }.sortedBy { it.first }

        val pdate = select(fullText, text("summary.pdate"))?.let {
            DateTimeParser.parseInstant(
                it.replace(" ", "T") + "Z",
                ZoneId.of("UTC")
            )
        }

        return alert.copy(
            sent = pdate ?: alert.sent,
            link = "https://cneos.jpl.nasa.gov/sentry/details.html#?des=${
                URLEncoder.encode(
                    alert.parameters?.get(
                        "Name"
                    ) ?: "", "UTF-8"
                )
            }",
            parameters = alert.parameters?.plus(
                "Potential impacts" to "\n" + impacts.joinToString("\n") {
                    val pct = if (it.second < 1) {
                        "< 1%"
                    } else {
                        "${it.second}%"
                    }
                    "  - ${
                        formatter.formatDateTime(
                            it.first,
                            includeWeekDay = false,
                            abbreviateMonth = true
                        )
                    }: $pct"
                }
            ),
        )
    }

    private fun parseFractionalDate(date: String): Instant? {
        val parts = date.split(".")
        if (parts.size != 2) {
            return DateTimeParser.parseInstant(date, ZoneId.of("UTC"))
        }

        val parsed = DateTimeParser.parseInstant(parts[0], ZoneId.of("UTC")) ?: return null
        val percent = ("0." + parts[1]).toFloatCompat() ?: return null
        return parsed.plus(percentOfDayToDuration(percent))
    }

    private fun percentOfDayToDuration(percent: Float): Duration {
        val hours = (percent * 24).toInt()
        val minutes = ((percent * 24 - hours) * 60).toInt()
        val seconds = ((percent * 24 - hours - minutes / 60f) * 3600).toInt()
        return Duration.ofHours(hours.toLong()).plusMinutes(minutes.toLong())
            .plusSeconds(seconds.toLong())
    }
}