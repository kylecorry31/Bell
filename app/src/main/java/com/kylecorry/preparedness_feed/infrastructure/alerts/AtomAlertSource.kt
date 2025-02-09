package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import com.kylecorry.preparedness_feed.infrastructure.utils.XmlUtils
import java.time.ZonedDateTime


abstract class AtomAlertSource(
    context: Context,
    private val titleSelector: String = "title",
    private val summarySelector: String = "summary",
    private val linkSelector: String = "link"
) :
    AlertSource {

    private val http = HttpService(context)

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> {
        val url = getUrl(since)
        val response = http.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        )
        val xml = XMLConvert.parse(response)
        val items = XmlUtils.findAll(xml, "entry").mapNotNull {
            val title = XmlUtils.getTextBySelector(it, titleSelector) ?: ""
            val link = XmlUtils.getTextBySelector(it, linkSelector)
                ?: if (!linkSelector.contains("href")) {
                    XmlUtils.getTextBySelector(it, "$linkSelector[href]") ?: ""
                } else {
                    ""
                }
            val guid = XmlUtils.find(it, "id")?.text ?: ""
            val pubDate = XmlUtils.find(it, "updated")?.text ?: ""
            val summary = XmlUtils.getTextBySelector(it, summarySelector) ?: ""
            val pubDateTimestamp = DateTimeParser.parse(pubDate) ?: return@mapNotNull null
            Alert(
                id = 0,
                title = title,
                sourceSystem = "",
                type = AlertType.Other,
                level = AlertLevel.Other,
                link = link,
                uniqueId = guid,
                publishedDate = pubDateTimestamp,
                summary = summary
            )
        }.filter { it.publishedDate.isAfter(since) }
        return postProcessAlerts(items)
    }

    abstract fun getUrl(since: ZonedDateTime): String

    open fun postProcessAlerts(alerts: List<Alert>): List<Alert> {
        return alerts
    }


}