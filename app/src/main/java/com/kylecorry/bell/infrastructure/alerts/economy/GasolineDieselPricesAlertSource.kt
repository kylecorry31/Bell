package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class GasolineDieselPricesAlertSource(context: Context) : BaseAlertSource(context) {
    private val gasolinePriceRegex =
        Regex("Regular Gasoline Retail Price.*<br/>.*(\\d+\\.\\d+).*U\\.S\\.")
    private val dieselPriceRegex =
        Regex("On-Highway Diesel Fuel Retail Price.*<br/>.*(\\d+\\.\\d+).*U\\.S\\.")

    override fun getSpecification(): AlertSpecification {
        return rss(
            "EIA",
            "https://www.eia.gov/petroleum/gasdiesel/includes/gas_diesel_rss.xml",
            AlertType.Economy,
            AlertLevel.Announcement,
            uniqueId = Selector.value("gas_diesel"),
            additionalHeaders = mapOf(
                "Accept" to "application/xhtml+xml,application/xml",
            ),
            limit = 1
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {
            val gasPrice = gasolinePriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                ?: return@mapNotNull null
            val dieselPrice =
                dieselPriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                    ?: return@mapNotNull null

            it.copy(
                title = "Gasoline: $gasPrice, Diesel: $dieselPrice (US Average)",
                shouldSummarize = false,
                summary = HtmlTextFormatter.getText(it.summary)
            )
        }
    }
}