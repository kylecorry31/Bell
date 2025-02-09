package com.kylecorry.preparedness_feed.ui.components

import android.text.util.Linkify
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.AndromedaList
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.ViewAttributes
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.domain.AlertLevel
import com.kylecorry.preparedness_feed.ui.FormatService
import com.kylecorry.preparedness_feed.ui.mappers.AlertLevelMapper
import com.kylecorry.preparedness_feed.ui.mappers.AlertTypeMapper

class AlertListAttributes : ViewAttributes() {
    var alerts: List<Alert> = emptyList()
    var onDelete: ((Alert) -> Unit)? = null
}

fun AlertList(config: AlertListAttributes.() -> Unit) = Component(config) { attrs ->
    val context = useAndroidContext()
    val formatter = useMemo(context) {
        FormatService.getInstance(context)
    }

    val listItems = useMemo(attrs.alerts, attrs.onDelete, formatter, context) {
        val secondaryTextColor = Resources.androidTextColorSecondary(context)
        attrs.alerts.filter { it.level != AlertLevel.Noise }.map {
            ListItem(
                it.id,
                it.title,
                formatter.formatDateTime(it.publishedDate),
                icon = ResourceListIcon(
                    AlertTypeMapper.getIcon(it.type),
                    secondaryTextColor
                ),
                tags = listOf(
                    ListItemTag(it.level.name, null, AlertLevelMapper.getColor(it.level)),
                ),
                longClickAction = {
                    attrs.onDelete?.invoke(it)
                }
            ) {
                val content =
                    buildSpannedString {
                        appendLine(formatter.formatDateTime(it.publishedDate))
                        appendLine()
                        appendLine(it.link.trimEnd('/'))
                        appendLine()
                        appendLine(formatter.formatMarkdown(it.summary))
                    }.toSpannable()
                LinkifyCompat.addLinks(content, Linkify.WEB_URLS)

                Alerts.dialog(
                    context,
                    it.title,
                    content,
                    allowLinks = true,
                    cancelText = null
                )
            }
        }
    }

    AndromedaList {
        items = listItems
    }
}