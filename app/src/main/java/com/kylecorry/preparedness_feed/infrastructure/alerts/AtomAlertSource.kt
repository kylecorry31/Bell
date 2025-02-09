package com.kylecorry.preparedness_feed.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.andromeda.xml.XMLNode
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.domain.AlertSource
import com.kylecorry.preparedness_feed.domain.AlertType
import com.kylecorry.preparedness_feed.infrastructure.internet.HttpService
import com.kylecorry.preparedness_feed.infrastructure.parsers.DateTimeParser
import java.time.ZonedDateTime


abstract class AtomAlertSource(
    context: Context,
    private val titleSelector: String = "title",
    private val summarySelector: String = "summary"
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
        val items = findAll(xml, "entry").mapNotNull {
            val title = getTextBySelector(it, titleSelector) ?: ""
            val linkElement = find(it, "link")
            val link = linkElement?.attributes?.get("href") ?: linkElement?.text ?: ""
            val guid = find(it, "id")?.text ?: ""
            val pubDate = find(it, "updated")?.text ?: ""
            val summary = getTextBySelector(it, summarySelector) ?: ""
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

    private fun getTextBySelector(xml: XMLNode, selector: String): String? {
        if (selector.contains(" + ")) {
            val parts = selector.split(" + ").mapNotNull { getTextBySelector(xml, it) }
            return parts.joinToString("\n\n")
        }

        val tag = selector.substringBefore("[")
        var matches = findAll(xml, tag)

        var remainingSelector = if (selector == tag) {
            ""
        } else {
            selector
        }
        while (matches.isNotEmpty() && remainingSelector.isNotEmpty()) {
            val attribute = remainingSelector.substringAfter("[").substringBefore("]")
            if (attribute.contains("=")) {
                val parts = attribute.split("=")
                val key = parts[0]
                val value = parts[1]
                matches = matches.filter { it.attributes[key] == value }
                remainingSelector = remainingSelector.substringAfter("]")
            } else {
                // This is a stop condition
                return matches.firstOrNull()?.attributes?.get(attribute)
            }
        }

        return matches.firstOrNull()?.text

    }
}