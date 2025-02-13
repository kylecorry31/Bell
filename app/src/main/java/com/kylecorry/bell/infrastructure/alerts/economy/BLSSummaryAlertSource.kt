package com.kylecorry.bell.infrastructure.alerts.economy

import android.content.Context
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import org.jsoup.Jsoup

class BLSSummaryAlertSource(context: Context) : BaseAlertSource(context) {
    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.BLSSummary,
            "https://www.bls.gov/feed/bls_latest.rss",
            AlertType.Economy,
            AlertLevel.Information,
            uniqueId = Selector.value(""),
        )
    }

    override fun process(alerts: List<Alert>): List<Alert> {
        return alerts.flatMap {
            val summary = Jsoup.parse(it.summary)

            // TODO: Set level based on the data (ex. major increase in CPI)

            // CPI
            val cpi = select(summary, Selector.text("p:contains(Consumer Price Index) .data"))
            val cpiLink = select(summary, Selector.attr("p:contains(Consumer Price Index) a", "href"))

            // Unemployment rate
            val unemploymentRate = select(summary, Selector.text("p:contains(Unemployment Rate) .data"))
            val unemploymentRateLink = select(summary, Selector.attr("p:contains(Unemployment Rate) a", "href"))

            // Payroll employment
            val payrollEmployment = select(summary, Selector.text("p:contains(Payroll Employment) .data"))
            val payrollEmploymentLink = select(summary, Selector.attr("p:contains(Payroll Employment) a", "href"))

            // Average hourly earnings
            val averageHourlyEarnings = select(summary, Selector.text("p:contains(Average Hourly Earnings) .data"))
            val averageHourlyEarningsLink = select(summary, Selector.attr("p:contains(Average Hourly Earnings) a", "href"))

            // Producer price index
            val producerPriceIndex = select(summary, Selector.text("p:contains(Producer Price Index) .data"))
            val producerPriceIndexLink = select(summary, Selector.attr("p:contains(Producer Price Index) a", "href"))

            // Employment cost index
            val employmentCostIndex = select(summary, Selector.text("p:contains(Employment Cost Index) .data"))
            val employmentCostIndexLink = select(summary, Selector.attr("p:contains(Employment Cost Index) a", "href"))

            // Productivity
            val productivity = select(summary, Selector.text("p:contains(Productivity) .data"))
            val productivityLink = select(summary, Selector.attr("p:contains(Productivity) a", "href"))

            // Import prices
            val importPrices = select(summary, Selector.text("p:contains(Import Price Index) .data"))
            val importPricesLink = select(summary, Selector.attr("p:contains(Import Price Index) a", "href"))

            // Export prices
            val exportPrices = select(summary, Selector.text("p:contains(Export Price Index) .data"))
            val exportPricesLink = select(summary, Selector.attr("p:contains(Export Price Index) a", "href"))

            listOfNotNull(
                cpi?.let { value ->
                    it.copy(
                        title = "Consumer Price Index: $value",
                        summary = "",
                        link = cpiLink ?: it.link,
                        uniqueId = "cpi",
                        useLinkForSummary = false
                    )
                },
                unemploymentRate?.let { value ->
                    it.copy(
                        title = "Unemployment Rate: $value",
                        summary = "",
                        link = unemploymentRateLink ?: it.link,
                        uniqueId = "unemployment_rate",
                        useLinkForSummary = false
                    )
                },
                payrollEmployment?.let { value ->
                    it.copy(
                        title = "Payroll Employment: $value",
                        summary = "",
                        link = payrollEmploymentLink ?: it.link,
                        uniqueId = "payroll_employment",
                        useLinkForSummary = false
                    )
                },
                averageHourlyEarnings?.let { value ->
                    it.copy(
                        title = "Average Hourly Earnings: $value",
                        summary = "",
                        link = averageHourlyEarningsLink ?: it.link,
                        uniqueId = "average_hourly_earnings",
                        useLinkForSummary = false
                    )
                },
                producerPriceIndex?.let { value ->
                    it.copy(
                        title = "Producer Price Index: $value",
                        summary = "",
                        link = producerPriceIndexLink ?: it.link,
                        uniqueId = "producer_price_index",
                        useLinkForSummary = false
                    )
                },
                employmentCostIndex?.let { value ->
                    it.copy(
                        title = "Employment Cost Index: $value",
                        summary = "",
                        link = employmentCostIndexLink ?: it.link,
                        uniqueId = "employment_cost_index",
                        useLinkForSummary = false
                    )
                },
                productivity?.let { value ->
                    it.copy(
                        title = "Productivity: $value",
                        summary = "",
                        link = productivityLink ?: it.link,
                        uniqueId = "productivity",
                        useLinkForSummary = false
                    )
                },
                importPrices?.let { value ->
                    it.copy(
                        title = "Import Prices: $value",
                        summary = "",
                        link = importPricesLink ?: it.link,
                        uniqueId = "import_prices",
                        useLinkForSummary = false
                    )
                },
                exportPrices?.let { value ->
                    it.copy(
                        title = "Export Prices: $value",
                        summary = "",
                        link = exportPricesLink ?: it.link,
                        uniqueId = "export_prices",
                        useLinkForSummary = false
                    )
                }
            )
        }
    }
}