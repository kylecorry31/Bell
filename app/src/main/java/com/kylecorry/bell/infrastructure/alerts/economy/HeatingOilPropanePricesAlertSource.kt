package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter
import com.kylecorry.sol.math.SolMath.roundPlaces

class HeatingOilPropanePricesAlertSource(context: Context) : BaseAlertSource(context) {
    private val oilPriceRegex =
        Regex("Heating\\s+Oil\\s+Residential[^\\d]+(\\d+\\.\\d+)")
    private val propanePriceRegex =
        Regex("Propane\\s+Residential[^\\d]+(\\d+\\.\\d+)")

    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.EIAHeatingOilPrices,
            "https://www.eia.gov/petroleum/heatingoilpropane/includes/hopu_rss.xml",
            AlertType.Economy,
            AlertLevel.Announcement,
            uniqueId = Selector.value("oil_propane"),
            additionalHeaders = mapOf(
                "Accept" to "application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "User-Agent" to null
            ),
            limit = 2
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.flatMap {
            val oilPrice = oilPriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
            val propanePrice =
                propanePriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()

            val summary = HtmlTextFormatter.getText(it.summary)

            listOfNotNull(
                oilPrice?.let { price ->
                    it.copy(
                        title = "Oil: ${price.roundPlaces(2)} (US Average)",
                        useLinkForSummary = false,
                        summary = summary,
                        uniqueId = "oil"
                    )
                },
                propanePrice?.let { price ->
                    it.copy(
                        title = "Propane: ${price.roundPlaces(2)} (US Average)",
                        useLinkForSummary = false,
                        summary = summary,
                        uniqueId = "propane"
                    )
                }
            )
        }
    }
}