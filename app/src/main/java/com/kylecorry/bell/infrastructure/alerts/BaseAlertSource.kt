package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import com.kylecorry.andromeda.xml.XMLConvert
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertSource
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.infrastructure.internet.HttpService
import com.kylecorry.bell.infrastructure.parsers.DateTimeParser
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector
import com.kylecorry.bell.infrastructure.parsers.selectors.select
import com.kylecorry.luna.coroutines.onIO
import org.jsoup.Jsoup
import java.time.ZoneId

enum class AlertSourceType {
    XML,
    JSON,
    HTML
}

data class AlertSpecification(
    val sourceSystem: String,
    val url: String,
    val type: AlertSourceType,
    val items: String,
    val title: Selector,
    val link: Selector,
    val uniqueId: Selector,
    val publishedDate: Selector,
    val summary: Selector,
    val additionalAttributes: Map<String, Selector> = mapOf(),
    val defaultAlertType: AlertType = AlertType.Other,
    val defaultAlertLevel: AlertLevel = AlertLevel.Other,
    val additionalHeaders: Map<String, String> = mapOf(),
    val defaultZoneId: ZoneId = ZoneId.systemDefault(),
    val limit: Int? = null
)

abstract class BaseAlertSource(
    context: Context
) : AlertSource {
    private val http = HttpService(context)

    abstract fun getSpecification(): AlertSpecification

    open fun process(alerts: List<Alert>): List<Alert> {
        return alerts
    }

    override suspend fun getAlerts(): List<Alert> = onIO {
        val specification = getSpecification()
        val response = http.get(
            specification.url, headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
            ) + specification.additionalHeaders
        )

        val alertItems = select(
            when (specification.type) {
                AlertSourceType.XML -> XMLConvert.parse(response)
                AlertSourceType.HTML -> Jsoup.parse(response)
                AlertSourceType.JSON -> response
            }, specification.items
        )

        val alerts = alertItems.mapNotNull {
            Alert(
                id = 0,
                title = select(it, specification.title) ?: return@mapNotNull null,
                sourceSystem = specification.sourceSystem,
                type = specification.defaultAlertType,
                level = specification.defaultAlertLevel,
                link = select(it, specification.link) ?: return@mapNotNull null,
                uniqueId = select(it, specification.uniqueId) ?: return@mapNotNull null,
                publishedDate = DateTimeParser.parse(
                    select(it, specification.publishedDate) ?: return@mapNotNull null,
                    specification.defaultZoneId
                ) ?: return@mapNotNull null,
                summary = select(it, specification.summary) ?: return@mapNotNull null,
                additionalAttributes = specification.additionalAttributes.mapValues { (_, selector) ->
                    select(it, selector) ?: ""
                }
            )
        }

        val processed = process(alerts).sortedByDescending { it.publishedDate }
        if (specification.limit != null) {
            processed.take(specification.limit)
        } else {
            processed
        }
    }

    override fun getSystemName(): String {
        return getSpecification().sourceSystem
    }

    protected fun rss(
        sourceSystem: String,
        url: String,
        defaultAlertType: AlertType = AlertType.Other,
        defaultAlertLevel: AlertLevel = AlertLevel.Other,
        items: String = "item",
        title: Selector = Selector.text("title"),
        link: Selector = Selector.text("link"),
        uniqueId: Selector = Selector.text("guid"),
        publishedDate: Selector = Selector.text("pubDate"),
        summary: Selector = Selector.text("description"),
        additionalAttributes: Map<String, Selector> = mapOf(),
        additionalHeaders: Map<String, String> = mapOf(),
        defaultZoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int? = null
    ): AlertSpecification {
        return AlertSpecification(
            sourceSystem = sourceSystem,
            url = url,
            type = AlertSourceType.XML,
            items = items,
            title = title,
            link = link,
            uniqueId = uniqueId,
            publishedDate = publishedDate,
            summary = summary,
            additionalAttributes = additionalAttributes,
            defaultAlertType = defaultAlertType,
            defaultAlertLevel = defaultAlertLevel,
            defaultZoneId = defaultZoneId,
            additionalHeaders = additionalHeaders,
            limit = limit
        )
    }

    protected fun atom(
        sourceSystem: String,
        url: String,
        defaultAlertType: AlertType = AlertType.Other,
        defaultAlertLevel: AlertLevel = AlertLevel.Other,
        items: String = "entry",
        title: Selector = Selector.text("title"),
        link: Selector = Selector.attr("link", "href"),
        uniqueId: Selector = Selector.text("id"),
        publishedDate: Selector = Selector.text("published"),
        summary: Selector = Selector.text("summary"),
        additionalAttributes: Map<String, Selector> = mapOf(),
        additionalHeaders: Map<String, String> = mapOf(),
        defaultZoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int? = null
    ): AlertSpecification {
        return AlertSpecification(
            sourceSystem = sourceSystem,
            url = url,
            type = AlertSourceType.XML,
            items = items,
            title = title,
            link = link,
            uniqueId = uniqueId,
            publishedDate = publishedDate,
            summary = summary,
            additionalAttributes = additionalAttributes,
            defaultAlertType = defaultAlertType,
            defaultAlertLevel = defaultAlertLevel,
            defaultZoneId = defaultZoneId,
            additionalHeaders = additionalHeaders,
            limit = limit
        )
    }

    protected fun html(
        sourceSystem: String,
        url: String,
        items: String,
        title: Selector,
        link: Selector,
        uniqueId: Selector,
        publishedDate: Selector,
        summary: Selector,
        additionalAttributes: Map<String, Selector> = mapOf(),
        additionalHeaders: Map<String, String> = mapOf(),
        defaultAlertType: AlertType = AlertType.Other,
        defaultAlertLevel: AlertLevel = AlertLevel.Other,
        defaultZoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int? = null,
    ): AlertSpecification {
        return AlertSpecification(
            sourceSystem = sourceSystem,
            url = url,
            type = AlertSourceType.HTML,
            items = items,
            title = title,
            link = link,
            uniqueId = uniqueId,
            publishedDate = publishedDate,
            summary = summary,
            additionalAttributes = additionalAttributes,
            defaultAlertType = defaultAlertType,
            defaultAlertLevel = defaultAlertLevel,
            defaultZoneId = defaultZoneId,
            additionalHeaders = additionalHeaders,
            limit = limit
        )
    }

    protected fun json(
        sourceSystem: String,
        url: String,
        items: String,
        title: Selector,
        link: Selector,
        uniqueId: Selector,
        publishedDate: Selector,
        summary: Selector,
        additionalAttributes: Map<String, Selector> = mapOf(),
        additionalHeaders: Map<String, String> = mapOf(),
        defaultAlertType: AlertType = AlertType.Other,
        defaultAlertLevel: AlertLevel = AlertLevel.Other,
        defaultZoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int? = null,
    ): AlertSpecification {
        return AlertSpecification(
            sourceSystem = sourceSystem,
            url = url,
            type = AlertSourceType.JSON,
            items = items,
            title = title,
            link = link,
            uniqueId = uniqueId,
            publishedDate = publishedDate,
            summary = summary,
            additionalAttributes = additionalAttributes,
            additionalHeaders = additionalHeaders,
            defaultAlertType = defaultAlertType,
            defaultAlertLevel = defaultAlertLevel,
            defaultZoneId = defaultZoneId,
            limit = limit
        )
    }
}