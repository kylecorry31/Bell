package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.utils.HtmlTextFormatter

class HeatingOilPropanePricesAlertSource(context: Context) : BaseAlertSource(context) {
    private val oilPriceRegex =
        Regex("Heating Oil Residential.*<br/>.*(\\d+\\.\\d+).*U\\.S\\.")
    private val propanePriceRegex =
        Regex("Propane Residential.*<br/>.*(\\d+\\.\\d+).*U\\.S\\.")

    override fun getSpecification(): AlertSpecification {
        return rss(
            "EIA",
            "https://www.eia.gov/petroleum/heatingoilpropane/includes/hopu_rss.xml",
            AlertType.Economy,
            AlertLevel.Announcement,
            uniqueId = Selector.value("oil_propane"),
            additionalHeaders = mapOf(
                "Accept" to "application/xhtml+xml,application/xml",
            ),
            limit = 1
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.mapNotNull {
            val oilPrice = oilPriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                ?: return@mapNotNull null
            val propanePrice =
                propanePriceRegex.find(it.summary)?.groupValues?.get(1)?.toFloatOrNull()
                    ?: return@mapNotNull null

            it.copy(
                title = "Oil: $oilPrice, Propane: $propanePrice (US Average)",
                shouldSummarize = false,
                summary = HtmlTextFormatter.getText(it.summary)
            )
        }
    }
}