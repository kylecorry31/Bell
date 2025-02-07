package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.json.JsonConvert
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import java.time.ZonedDateTime

class SWPCAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    private val serialNumberRegex = Regex("Serial Number:\\s([0-9]+)")
    private val titleRegex = Regex("(WARNING|ALERT|SUMMARY|WATCH):\\s(.*)")

    class SWPCResponse(val product_id: String, val issue_datetime: String, val message: String)

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> {
        val url = "https://services.swpc.noaa.gov/products/alerts.json"
        val response = http.get(url)
        val json = JsonConvert.fromJson<Array<SWPCResponse>>(response) ?: return emptyList()
        return json.mapNotNull {
            if (!it.message.contains("WATCH: Geomagnetic Storm Category")) {
                return@mapNotNull null
            }

            val titleMatch = titleRegex.find(it.message)
            val title = titleMatch?.groupValues?.get(2) ?: ""

            val serialNumberMatch = serialNumberRegex.find(it.message)
            val serialNumber = serialNumberMatch?.groupValues?.get(1) ?: ""

            Alert(
                0,
                title,
                "SWPC",
                it.product_id,
                "https://www.swpc.noaa.gov/",
                serialNumber,
                ZonedDateTime.parse(it.issue_datetime.replace(" ", "T") + "Z"),
                it.message,
                useLinkForSummary = false
            )
        }.filter { it.publishedDate.isAfter(since) }
    }
}