package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.andromeda.xml.XMLNode
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

abstract class RssAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    override suspend fun getAlerts(since: ZonedDateTime): List<Alert> {
        val url = getUrl(since)
        val response = http.get(
            url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        )
        val xml = XMLConvert.parse(response)
        val items = findAll(xml, "item").map {
            val title = find(it, "title")?.text ?: ""
            val link = find(it, "link")?.text ?: ""
            val guid = find(it, "guid")?.text ?: ""
            val pubDate = find(it, "pubDate")?.text ?: ""
            val summary = find(it, "description")?.text ?: ""
            val pubDateTimestamp = parseDate(pubDate)
            Alert(
                id = 0,
                title = title,
                source = url,
                type = "RSS",
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

    private fun parseDate(date: String): ZonedDateTime {
        return try {
            //             pub_date = int(datetime.strptime(pub_date, "%a, %d %b %Y %H:%M:%S %z").timestamp())
            val parsed = DateTimeFormatter.RFC_1123_DATE_TIME.parse(date)
            ZonedDateTime.from(parsed)
        } catch (e: DateTimeParseException) {
            val tzAbbr = date.split(" ").last()
            val zone = ZoneId.of(tzAbbr.ifEmpty { "UTC" })
            val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss")
            val parsedUtc = LocalDateTime.parse(date.replace(tzAbbr, "").trim(), formatter)
            ZonedDateTime.of(parsedUtc, zone)
        }
    }

    private fun findAll(xml: XMLNode, tag: String): List<XMLNode> {
        val matches = mutableListOf<XMLNode>()
        if (xml.tag.lowercase() == tag.lowercase()) {
            matches.add(xml)
        }
        for (child in xml.children) {
            matches.addAll(findAll(child, tag))
        }
        return matches
    }

    private fun find(xml: XMLNode, tag: String): XMLNode? {
        if (xml.tag.lowercase() == tag.lowercase()) {
            return xml
        }
        for (child in xml.children) {
            val match = find(child, tag)
            if (match != null) {
                return match
            }
        }
        return null
    }
}