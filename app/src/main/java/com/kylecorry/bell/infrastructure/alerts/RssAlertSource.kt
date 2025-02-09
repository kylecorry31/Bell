package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.andromeda.xml.XMLNode
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.internet.HttpService
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import java.time.ZonedDateTime

abstract class RssAlertSource(context: Context) : AlertSource {

    private val http = HttpService(context)

    override suspend fun getAlerts(): List<Alert> {
        val url = getUrl()
        val response = http.get(
            url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
        )
        val xml = XMLConvert.parse(response)
        val items = findAll(xml, "item").mapNotNull {
            val title = find(it, "title")?.text ?: ""
            val link = find(it, "link")?.text ?: ""
            val guid = find(it, "guid")?.text ?: ""
            val pubDate = find(it, "pubDate")?.text ?: ""
            val summary = find(it, "description")?.text ?: ""
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
        }
        return postProcessAlerts(items)
    }

    abstract fun getUrl(): String

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
}