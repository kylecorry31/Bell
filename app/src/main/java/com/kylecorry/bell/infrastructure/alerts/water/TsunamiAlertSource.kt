package com.kylecorry.bell.infrastructure.alerts.water

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Area
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.domain.Certainty
import com.kylecorry.bell.domain.ResponseType
import com.kylecorry.bell.domain.Severity
import com.kylecorry.bell.domain.Urgency
import com.kylecorry.bell.domain.getByCode
import com.kylecorry.bell.infrastructure.alerts.AlertLoader
import com.kylecorry.bell.infrastructure.alerts.AlertSource
import com.kylecorry.bell.infrastructure.alerts.FileType
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

abstract class TsunamiAlertSource(context: Context, private val url: String) : AlertSource {

    private val loader = AlertLoader(context)

    private val stationToStateMap = mapOf(
        // Only using the non segmented alerts: https://tsunami.gov/?page=product_list
        "WEAK51" to listOf("AK", "WA", "OR", "CA"),
        "WEHW40" to listOf("HI"),
        "WEZS40" to listOf("AS"),
        "WEGM40" to listOf("GU", "MP"),
        "WEXX30" to listOf(
            "TX",
            "LA",
            "MS",
            "AL",
            "FL",
            "GA",
            "SC",
            "NC",
            "VA",
            "MD",
            "DE",
            "NJ",
            "NY",
            "CT",
            "RI",
            "MA",
            "NH",
            "ME",
        ),
        "WECA40" to listOf("PR", "VI"),
    )


    override suspend fun load(): List<Alert> {
        val rawAlerts = loader.load(
            FileType.XML,
            url,
            "entry",
            mapOf(
                "title" to Selector.text("title"),
                "sent" to Selector.text("updated"),
                "link" to Selector.attr("link[rel=related]", "href"),
                "summary" to Selector.text("summary"),
                "identifier" to Selector.text("id")
            )
        )

        return rawAlerts.mapNotNull {
            val states = stationToStateMap.entries.firstOrNull { entry ->
                it["link"]?.contains(entry.key) == true
            }?.value ?: return@mapNotNull null
            Alert(
                id = 0,
                identifier = it["identifier"] ?: "",
                sender = "NOAA Tsunami Warning Center",
                sent = DateTimeParser.parseInstant(it["sent"] ?: "") ?: return@mapNotNull null,
                source = getUUID(),
                category = Category.Geophysical,
                event = it["title"] ?: "",
                urgency = Urgency.Unknown,
                severity = Severity.Unknown,
                certainty = Certainty.Unknown,
                link = it["link"]?.replace("http://", "https://"),
                description = HtmlTextFormatter.getText(it["summary"] ?: ""),
                area = Area(states),
                isDownloadRequired = true
            )
        }
    }

    override fun getUUID(): String {
        return "35a5cfd8-f35b-405f-b9e9-58191112a2f8"
    }

    override fun updateFromFullText(alert: Alert, fullText: String): Alert {
        val xml = XMLConvert.parse(fullText)
        val info = select(xml, "info").firstOrNull() ?: return alert

        val event = select(info, Selector.text("event")) ?: return alert
        if (event == "Tsunami Cancellation" || event == "Tsunami Information") {
            return alert.copy(isTracked = false)
        }

        val urgency = select(info, Selector.text("urgency")) ?: return alert
        val severity = select(info, Selector.text("severity")) ?: return alert
        val certainty = select(info, Selector.text("certainty")) ?: return alert
        val onset = DateTimeParser.parseInstant(select(info, Selector.text("onset")) ?: "")
        val expires = DateTimeParser.parseInstant(select(info, Selector.text("expires")) ?: "")
        val headline = select(info, Selector.text("headline"))
        val description = select(info, Selector.text("description"))
        val instruction = select(info, Selector.text("instruction"))
        val link = select(info, Selector.text("web")) ?: alert.link
        val areaDesc = select(info, Selector.text("areaDesc"))
        val circle = select(info, Selector.text("circle"))
        val responseType = select(info, Selector.text("responseType"))

        val geofence = circle?.let {
            val split = it.split(" ")
            if (split.size != 3) {
                return@let null
            }
            val latitude = split[0].toDoubleCompat() ?: return@let null
            val longitude = split[1].toDoubleCompat() ?: return@let null
            val radius = split[2].toFloatCompat() ?: 0f

            Geofence(Coordinate(latitude, longitude), Distance.kilometers(radius))
        }

        // TODO: Parse more parameters


        return alert.copy(
            event = event,
            urgency = Urgency.entries.getByCode(urgency) ?: alert.urgency,
            severity = Severity.entries.getByCode(severity) ?: alert.severity,
            certainty = Certainty.entries.getByCode(certainty) ?: alert.certainty,
            onset = onset,
            expires = expires,
            headline = headline?.trim(),
            description = description?.trim(),
            instruction = instruction?.trim(),
            link = link,
            responseType = ResponseType.entries.getByCode(responseType ?: "") ?: alert.responseType,
            area = alert.area!!.copy(
                areaDescription = areaDesc ?: alert.area.areaDescription,
                circles = listOfNotNull(geofence)
            ),
        )
    }
}