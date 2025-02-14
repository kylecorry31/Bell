package com.kylecorry.bell.infrastructure.alerts.economy

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
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.sol.math.SolMath.roundPlaces
import java.time.Instant

class FuelPricesAlertSource(context: Context) : AlertSource {
    private val gasolinePriceRegex =
        Regex("Regular\\s+Gasoline\\s+Retail\\s+Price[^\\d]+(\\d+\\.\\d+)")
    private val dieselPriceRegex =
        Regex("On-Highway\\s+Diesel\\s+Fuel\\s+Retail\\s+Price[^\\d]+(\\d+\\.\\d+)")
    private val oilPriceRegex =
        Regex("Heating\\s+Oil\\s+Residential[^\\d]+(\\d+\\.\\d+)")
    private val propanePriceRegex =
        Regex("Propane\\s+Residential[^\\d]+(\\d+\\.\\d+)")

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val gasDiesel = loadGasolineDiesel()
        val heatingOilPropane = loadHeatingOilPropane()
        val sent = gasDiesel?.first ?: heatingOilPropane?.first ?: return emptyList()
        val fuelPrices =
            (gasDiesel?.second ?: emptyList()) + (heatingOilPropane?.second ?: emptyList())
        return listOf(
            Alert(
                id = 0,
                identifier = "fuel",
                sender = "EIA",
                sent = sent,
                source = getUUID(),
                category = Category.Infrastructure,
                event = "Fuel Prices (US Average)",
                urgency = Urgency.Unknown,
                severity = Severity.Minor,
                certainty = Certainty.Observed,
                parameters = fuelPrices.toMap()
            )
        )
    }

    private suspend fun loadHeatingOilPropane(): Pair<Instant, List<Pair<String, String>>>? {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://www.eia.gov/petroleum/heatingoilpropane/includes/hopu_rss.xml",
            "item",
            mapOf(
                "summary" to Selector.text("description"),
                "sent" to Selector.text("pubDate")
            ),
            mapOf(
                "Accept" to "application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "User-Agent" to null
            )
        ).firstOrNull() ?: return null

        val description = rawAlerts["summary"] ?: return null
        val sent = DateTimeParser.parseInstant(rawAlerts["sent"] ?: "") ?: return null
        val oilPrice = oilPriceRegex.find(description)?.groupValues?.get(1)?.toFloatOrNull()
        val propanePrice =
            propanePriceRegex.find(description)?.groupValues?.get(1)?.toFloatOrNull()

        return sent to listOfNotNull(
            oilPrice?.let { "Heating oil" to "\$${it.roundPlaces(2)}" },
            propanePrice?.let { "Propane" to "\$${it.roundPlaces(2)}" }
        )
    }

    private suspend fun loadGasolineDiesel(): Pair<Instant, List<Pair<String, String>>>? {
        val rawAlerts = loader.load(
            FileType.XML,
            "https://www.eia.gov/petroleum/gasdiesel/includes/gas_diesel_rss.xml",
            "item",
            mapOf(
                "summary" to Selector.text("description"),
                "sent" to Selector.text("pubDate")
            ),
            mapOf(
                "Accept" to "application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "User-Agent" to null
            )
        ).firstOrNull() ?: return null

        val sent = DateTimeParser.parseInstant(rawAlerts["sent"] ?: "") ?: return null
        val description = rawAlerts["summary"] ?: return null
        val gasPrice =
            gasolinePriceRegex.find(description)?.groupValues?.get(1)?.toFloatOrNull()
        val dieselPrice =
            dieselPriceRegex.find(description)?.groupValues?.get(1)?.toFloatOrNull()

        return sent to listOfNotNull(
            gasPrice?.let { "Gasoline" to "\$${it.roundPlaces(2)}" },
            dieselPrice?.let { "Diesel" to "\$${it.roundPlaces(2)}" }
        )
    }

    override fun getUUID(): String {
        return "8e9a2e85-a611-48b6-8035-ac516b035018"
    }
}