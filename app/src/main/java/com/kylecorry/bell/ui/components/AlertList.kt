package com.kylecorry.bell.ui.components

import android.text.util.Linkify
import androidx.core.text.buildSpannedString
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.list.ResourceListIcon
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.AndromedaList
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.ViewAttributes
import com.kylecorry.bell.R
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.domain.Category
import com.kylecorry.bell.infrastructure.alerts.AlertUpdater
import com.kylecorry.bell.infrastructure.persistence.AlertRepo
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.bell.infrastructure.utils.StateUtils
import com.kylecorry.bell.ui.FormatService
import com.kylecorry.bell.ui.mappers.SeverityMapper
import com.kylecorry.bell.ui.mappers.CategoryMapper
import com.kylecorry.luna.coroutines.onMain

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

    val repo = useMemo(context) {
        AlertRepo.getInstance(context)
    }

    val alertUpdater = useMemo(context) {
        AlertUpdater.getInstance(context)
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

    val showAlertDialog = useCallback<Alert, Unit>(context, formatter) { alert ->
        val content =
            buildSpannedString {
                if (alert.headline != null && alert.headline != alert.event) {
                    appendLine(alert.headline)
                    appendLine()
                }

                appendLine("Sent: ${formatter.formatDateTime(alert.sent)}")
                appendLine()

                if (alert.effective != null) {
                    appendLine("Effective: ${formatter.formatDateTime(alert.effective)}")
                    appendLine()
                }

                if (alert.onset != null) {
                    appendLine("Onset: ${formatter.formatDateTime(alert.onset)}")
                    appendLine()
                }

                if (alert.expires != null) {
                    appendLine("Expires: ${formatter.formatDateTime(alert.expires)}")
                    appendLine()
                }

                appendLine("Severity: ${alert.severity.name}")
                appendLine("Urgency: ${alert.urgency.name}")
                appendLine("Certainty: ${alert.certainty.name}")
                appendLine()

                if (alert.area != null) {
                    appendLine("Area: ${alert.area.areaDescription}")
                    appendLine()
                }

                if (alert.link != null) {
                    appendLine(alert.link.trimEnd('/'))
                    appendLine()
                }

                if (alert.llmSummary != null) {
                    appendLine("Summary:")
                    appendLine(formatter.formatMarkdown(alert.llmSummary.trim()))
                    appendLine()
                }

                if (alert.description != null) {
                    appendLine(formatter.formatMarkdown(alert.description.trim()))
                    appendLine()
                }

                if (alert.instruction != null) {
                    appendLine("Instructions:")
                    appendLine(formatter.formatMarkdown(alert.instruction.trim()))
                    appendLine()
                }

                if (alert.parameters != null) {
                    val sortedParameters =
                        alert.parameters.toList().sortedBy { it.first }
                    val parameterString = sortedParameters.joinToString("\n") {
                        "- **${it.first}**: ${it.second}"
                    }
                    appendLine(formatter.formatMarkdown(parameterString))
                }

            }.toSpannable()
        LinkifyCompat.addLinks(content, Linkify.WEB_URLS)

        Alerts.dialog(
            context,
            alert.event,
            content,
            allowLinks = true,
            okText = null,
            cancelText = "Reload Summary"
        ) { cancelled ->
            if (cancelled) {
                reloadAlertSummary(alert)
            }
        }
    }

    val reloadAlertSummary = useCallback<Alert, Unit>(context, alertUpdater, showAlertDialog) { alert ->
        inBackground {
            val updatedAlert = alertUpdater.reloadSummary(alert)
            onMain {
                Alerts.toast(context, "Summary reloaded")
                showAlertDialog(updatedAlert)
            }
        }
    }

    val listItems = useMemo(alertsToShow, attrs.onDelete, formatter, context, openTypes, showAlertDialog) {
        val secondaryTextColor = Resources.androidTextColorSecondary(context)
        alertsToShow.groupBy { it.category }
            .toList()
            .sortedBy { it.first.ordinal }
            .flatMap { (type, alerts) ->
                val items = mutableListOf(
                    ListItem(
                        type.ordinal.toLong(),
                        CategoryMapper.getName(context, type),
                        subtitle = "${alerts.size} alerts",
                        icon = ResourceListIcon(
                            CategoryMapper.getIcon(type),
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
                                SeverityMapper.getColor(it.severity)
                            ),
                            tags = listOf(
                                ListItemTag(
                                    it.severity.name,
                                    null,
                                    SeverityMapper.getColor(it.severity)
                                ),
                            ),
                            longClickAction = {
                                attrs.onDelete?.invoke(it)
                            }
                        ) {
                            showAlertDialog(it)
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