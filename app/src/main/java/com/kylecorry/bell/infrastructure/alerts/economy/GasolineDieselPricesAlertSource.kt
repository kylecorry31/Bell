package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
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
            "EIA",
            "https://www.eia.gov/petroleum/gasdiesel/includes/gas_diesel_rss.xml",
            AlertType.Economy,
            AlertLevel.Announcement,
            uniqueId = Selector.value("gas_diesel"),
            additionalHeaders = mapOf(
                "Accept" to "application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                "User-Agent" to null
            ),
            limit = 1
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        // TODO: Split gas and diesel
        return alerts.mapNotNull {
            val gasPrice = gasolinePriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                ?: return@mapNotNull null
            val dieselPrice =
                dieselPriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                    ?: return@mapNotNull null

            it.copy(
                title = "Gasoline: ${gasPrice.roundPlaces(2)}, Diesel: ${dieselPrice.roundPlaces(2)} (US Average)",
                shouldSummarize = false,
                summary = HtmlTextFormatter.getText(it.summary)
            )
        }
    }
}