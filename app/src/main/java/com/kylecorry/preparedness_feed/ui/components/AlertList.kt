package com.kylecorry.preparedness_feed.ui.components

import android.graphics.Color
import android.text.util.Linkify
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.AndromedaList
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.ViewAttributes
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.ui.FormatService

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
        val primaryColor = Resources.getAndroidColorAttr(context, android.R.attr.colorPrimary)
        attrs.alerts.map {
            ListItem(
                it.id,
                it.title,
                formatter.formatDateTime(it.publishedDate),
                tags = listOf(
                    ListItemTag(it.source, null, primaryColor),
                    ListItemTag(it.type, null, Color.LTGRAY),
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
                        appendLine(it.summary)
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