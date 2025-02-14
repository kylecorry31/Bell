package com.kylecorry.bell.ui.components

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
import com.kylecorry.bell.R
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.bell.infrastructure.utils.StateUtils
import com.kylecorry.bell.ui.FormatService
import com.kylecorry.bell.ui.mappers.AlertLevelMapper
import com.kylecorry.bell.ui.mappers.AlertTypeMapper

class AlertListAttributes : ViewAttributes() {
    var alerts: List<Alert> = emptyList()
    var onDelete: ((Alert) -> Unit)? = null
}

fun AlertList(config: AlertListAttributes.() -> Unit) = Component(config) { attrs ->

    val (openTypes, setOpenTypes) = useState(emptySet<Category>())

    val context = useAndroidContext()
    val formatter = useMemo(context) {
        FormatService.getInstance(context)
    }

    val preferences = useMemo(context) {
        UserPreferences(context)
    }

    val state = preferences.state

    val alertsToShow = useMemo(attrs.alerts, state) {
        attrs.alerts.filter {
            it.isValid() && StateUtils.shouldShowAlert(
                state,
                it.area,
                true
            )
        }
    }

    val listItems = useMemo(alertsToShow, attrs.onDelete, formatter, context, openTypes) {
        val secondaryTextColor = Resources.androidTextColorSecondary(context)
        alertsToShow.groupBy { it.category }
            .toList()
            .sortedBy { it.first.ordinal }
            .flatMap { (type, alerts) ->
                val items = mutableListOf(
                    ListItem(
                        type.ordinal.toLong(),
                        AlertTypeMapper.getName(context, type),
                        subtitle = "${alerts.size} alerts",
                        icon = ResourceListIcon(
                            AlertTypeMapper.getIcon(type),
                            secondaryTextColor
                        ),
                        trailingIcon = ResourceListIcon(
                            if (openTypes.contains(type)) R.drawable.menu_up else R.drawable.menu_down,
                            secondaryTextColor
                        ),
                    ) {
                        if (openTypes.contains(type)) {
                            setOpenTypes(openTypes - type)
                        } else {
                            setOpenTypes(openTypes + type)
                        }
                    }
                )
                if (openTypes.contains(type)) {
                    items.addAll(alerts.map {
                        ListItem(
                            it.id,
                            it.event,
                            formatter.formatDateTime(it.sent),
                            icon = ResourceListIcon(
                                R.drawable.alert_circle,
                                AlertLevelMapper.getColor(it.severity)
                            ),
                            tags = listOf(
                                ListItemTag(
                                    it.severity.name,
                                    null,
                                    AlertLevelMapper.getColor(it.severity)
                                ),
                            ),
                            longClickAction = {
                                attrs.onDelete?.invoke(it)
                            }
                        ) {
                            val content =
                                buildSpannedString {
                                    if (it.headline != null) {
                                        appendLine(it.headline)
                                        appendLine()
                                    }

                                    appendLine(formatter.formatDateTime(it.sent))
                                    appendLine()
                                    if (it.link != null) {
                                        appendLine(it.link.trimEnd('/'))
                                        appendLine()
                                    }
                                    appendLine(
                                        formatter.formatMarkdown(
                                            it.description?.trim() ?: ""
                                        )
                                    )
                                }.toSpannable()
                            LinkifyCompat.addLinks(content, Linkify.WEB_URLS)

                            Alerts.dialog(
                                context,
                                it.event,
                                content,
                                allowLinks = true,
                                cancelText = null
                            )
                        }
                    }
                    )
                }
                items
            }
    }

    AndromedaList {
        items = listItems
    }
}