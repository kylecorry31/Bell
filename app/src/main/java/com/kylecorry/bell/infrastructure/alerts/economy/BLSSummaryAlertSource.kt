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
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import org.jsoup.Jsoup

class BLSSummaryAlertSource(context: Context) : AlertSource {

    private val loader = AlertLoader(context)

    override suspend fun load(): List<Alert> {
        val rawAlert = loader.load(
            FileType.XML,
            "https://www.bls.gov/feed/bls_latest.rss",
            "item",
            mapOf(
                "title" to Selector.text("title"),
                "link" to Selector.text("link"),
                "sent" to Selector.text("pubDate"),
                "summary" to Selector.text("description")
            )
        ).firstOrNull() ?: return emptyList()

        val sent = DateTimeParser.parseInstant(rawAlert["sent"] ?: "") ?: return emptyList()
        val summary = Jsoup.parse(rawAlert["summary"] ?: "")


        val cpi = select(summary, Selector.text("p:contains(Consumer Price Index) .data"))
            ?: return emptyList()
        val unemploymentRate = select(summary, Selector.text("p:contains(Unemployment Rate) .data"))
            ?: return emptyList()
        val payrollEmployment =
            select(summary, Selector.text("p:contains(Payroll Employment) .data"))
                ?: return emptyList()
        val averageHourlyEarnings =
            select(summary, Selector.text("p:contains(Average Hourly Earnings) .data"))
                ?: return emptyList()
        val producerPriceIndex =
            select(summary, Selector.text("p:contains(Producer Price Index) .data"))
                ?: return emptyList()
        val employmentCostIndex =
            select(summary, Selector.text("p:contains(Employment Cost Index) .data"))
                ?: return emptyList()
        val productivity =
            select(summary, Selector.text("p:contains(Productivity) .data")) ?: return emptyList()
        val importPrices = select(summary, Selector.text("p:contains(Import Price Index) .data"))
            ?: return emptyList()
        val exportPrices = select(summary, Selector.text("p:contains(Export Price Index) .data"))
            ?: return emptyList()

        return listOf(
            Alert(
                id = 0,
                identifier = "economic-indicators",
                sender = "BLS",
                sent = sent,
                source = getUUID(),
                category = Category.Infrastructure,
                event = "US Economic Indicators",
                urgency = Urgency.Unknown,
                severity = Severity.Minor,
                certainty = Certainty.Observed,
                link = rawAlert["link"],
                parameters = mapOf(
                    "Consumer Price Index" to cpi,
                    "Unemployment Rate" to unemploymentRate,
                    "Payroll Employment" to payrollEmployment,
                    "Average Hourly Earnings" to averageHourlyEarnings,
                    "Producer Price Index" to producerPriceIndex,
                    "Employee Cost Index" to employmentCostIndex,
                    "Productivity" to productivity,
                    "Import Prices" to importPrices,
                    "Export Prices" to exportPrices
                )
            )
        )

    }

    override fun getUUID(): String {
        return "93a9787d-000b-4a8b-b92a-bb2c325ffc1c"
    }
}