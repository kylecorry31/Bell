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

class GasolineDieselPricesAlertSource(context: Context) : BaseAlertSource(context) {
    private val gasolinePriceRegex =
        Regex("Regular\\s+Gasoline\\s+Retail\\s+Price[^\\d]+(\\d+\\.\\d+)")
    private val dieselPriceRegex =
        Regex("On-Highway\\s+Diesel\\s+Fuel\\s+Retail\\s+Price[^\\d]+(\\d+\\.\\d+)")

    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.EIAGasolineDieselPrices,
            "https://www.eia.gov/petroleum/gasdiesel/includes/gas_diesel_rss.xml",
            AlertType.Economy,
            AlertLevel.Information,
            uniqueId = Selector.value("gas_diesel"),
            additionalHeaders = mapOf(
                "Accept" to "application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "User-Agent" to null
            ),
            limit = 2
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.flatMap {
            val gasPrice = gasolinePriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
            val dieselPrice =
                dieselPriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()

            val summary = HtmlTextFormatter.getText(it.summary)

            listOfNotNull(
                gasPrice?.let { price ->
                    it.copy(
                        title = "Gasoline: ${price.roundPlaces(2)} (US Average)",
                        useLinkForSummary = false,
                        summary = summary,
                        uniqueId = "gasoline"
                    )
                },
                dieselPrice?.let { price ->
                    it.copy(
                        title = "Diesel: ${price.roundPlaces(2)} (US Average)",
                        useLinkForSummary = false,
                        summary = summary,
                        uniqueId = "diesel"
                    )
                }
            )
        }
    }
}