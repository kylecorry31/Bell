package com.kylecorry.preparedness_feed.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.view.children
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useSizeDp
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.views.list.AndromedaListView
import com.kylecorry.andromeda.views.list.ListItem
import com.kylecorry.andromeda.views.list.ListItemTag
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.AndromedaListAttributes
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Button
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Column
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Text
import com.kylecorry.andromeda.views.reactivity.VDOMNode
import com.kylecorry.preparedness_feed.databinding.FragmentMainBinding
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.infrastructure.alerts.CDCAlertSource
import com.kylecorry.preparedness_feed.infrastructure.alerts.ExecutiveOrderAlertSource
import com.kylecorry.preparedness_feed.infrastructure.alerts.NationalWeatherServiceAlertSource
import com.kylecorry.preparedness_feed.infrastructure.alerts.SWPCAlertSource
import com.kylecorry.preparedness_feed.infrastructure.alerts.USGSEarthquakeAlertSource
import com.kylecorry.preparedness_feed.infrastructure.alerts.USGSWaterAlertSource
import com.kylecorry.preparedness_feed.infrastructure.persistence.AlertRepo
import com.kylecorry.preparedness_feed.infrastructure.persistence.UserPreferences
import com.kylecorry.preparedness_feed.infrastructure.summarization.Gemini
import java.time.ZonedDateTime

class MainFragment : BoundFragment<FragmentMainBinding>() {

    private val formatter by lazy { FormatService(requireContext()) }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMainBinding {
        return FragmentMainBinding.inflate(layoutInflater, container, false)
    }

    override fun onUpdate() {
        super.onUpdate()

        val (alerts, setAlerts) = useState(emptyList<Alert>())
        val (loading, setLoading) = useState(false)
        val (progress, setProgress) = useState(0f)
        val (loadingMessage, setLoadingMessage) = useState("")
        val context = useAndroidContext()

        val preferences = useMemo(context) {
            UserPreferences(context)
        }

        // TODO: Prompt for Gemini API key if not set
        // TODO: Prompt for area if not set
        val gemini = useMemo(context, preferences) {
            Gemini(context, preferences.geminiApiKey)
        }

        val repo = useMemo(context) {
            AlertRepo.getInstance(context)
        }

        useBackgroundEffect(repo) {
            setLoading(true)
            setAlerts(repo.getAll())
            setLoading(false)
        }

        val updateAlerts = useCallback<Unit>(context, alerts) {
            setLoading(true)
            setLoadingMessage("Sources")
            setProgress(0f)
            inBackground {
                repo.cleanup()

                val minTime = ZonedDateTime.now().minusDays(7)

                // TODO: Download alerts in parallel
                val sources = listOf(
                    NationalWeatherServiceAlertSource(context, "RI"),
                    ExecutiveOrderAlertSource(context),
                    USGSEarthquakeAlertSource(context),
                    USGSWaterAlertSource(context),
                    SWPCAlertSource(context),
                    CDCAlertSource()
                )

                val allAlerts = mutableListOf<Alert>()

                for (index in sources.indices) {
                    setProgress(index.toFloat() / sources.size)
                    val source = sources[index]
                    allAlerts.addAll(source.getAlerts(minTime))
                }

                // Remove all old alerts
                allAlerts.removeIf { it.publishedDate.isBefore(minTime) }

                val newAlerts = allAlerts.filter { alert ->
                    alerts.none { it.uniqueId == alert.uniqueId && it.source == alert.source }
                }

                val updatedAlerts = allAlerts.mapNotNull { alert ->
                    val existing =
                        alerts.find { it.uniqueId == alert.uniqueId && it.source == alert.source && it.publishedDate.toInstant() != alert.publishedDate.toInstant() }
                    if (existing != null) {
                        alert.copy(id = existing.id)
                    } else {
                        null
                    }
                }

                setProgress(0f)
                setLoadingMessage("Summarizing")

                var completedSummaryCount = 0

                // Generate summaries and save new/updated alerts
                (newAlerts + updatedAlerts).forEach { alert ->
                    setProgress(completedSummaryCount.toFloat() / (newAlerts.size + updatedAlerts.size))
                    val summary = if (alert.useLinkForSummary) {
                        gemini.summarizeUrl(alert.link)
                    } else {
                        gemini.summarize(
                            alert.summary
                        )
                    }
                    val newAlert = alert.copy(summary = summary)
                    repo.upsert(newAlert)
                    setAlerts(repo.getAll())
                    completedSummaryCount++
                }
                setLoading(false)
            }
        }

        val deleteAlert = useCallback<Unit, Alert>(repo) { alert ->
            // TODO: Prompt for confirmation
            inBackground {
                repo.delete(alert)
                setAlerts(repo.getAll())
            }
        }

        val openAlert = useCallback<Unit, Alert>(context) { alert ->
            if (alert.link.isEmpty()) {
                return@useCallback
            }
            val intent = Intents.url(alert.link)
            context.startActivity(intent)
        }

        val listItems = useMemo(alerts, deleteAlert) {
            alerts.map {
                ListItem(
                    it.id,
                    it.title,
                    formatter.formatDate(it.publishedDate) + "\n\n" + it.summary,
                    tags = listOf(
                        ListItemTag(it.source, null, Color.GREEN),
                        ListItemTag(it.type, null, Color.BLUE),
                    ),
                    longClickAction = {
                        deleteAlert(it)
                    }
                ) {
                    openAlert(it)
                }
            }
        }

        val onUpdateClicked = useMemo(updateAlerts) {
            OnClickListener {
                updateAlerts()
            }
        }

        val dp16 = useSizeDp(16f).toInt()

        render(
            Column(
                if (loading) {
                    Text {
                        text = "Loading $loadingMessage ${progress?.times(100)?.toInt()}%"
                        marginTop = dp16
                        marginStart = dp16
                        marginEnd = dp16
                        marginBottom = dp16
                    }
                } else {
                    Button {
                        text = "Update"
                        onClick = onUpdateClicked
                        width = ViewGroup.LayoutParams.WRAP_CONTENT
                        marginTop = dp16
                        marginStart = dp16
                        marginEnd = dp16
                        marginBottom = dp16
                    }
                },
                AndromedaList {
                    items = listItems
                }
            )
        )
    }

    private fun render(node: VDOMNode<*, *>) {
        VDOM2.render(binding.root, node, binding.root.children.firstOrNull())
    }

    fun AndromedaList(
        config: AndromedaListAttributes.() -> Unit,
    ): VDOMNode<AndromedaListView, AndromedaListAttributes> {
        val attributes = AndromedaListAttributes().apply(config)
        return VDOMNode(
            AndromedaListView::class.java,
            attributes,
            managesOwnChildren = true,
            create = { context ->
                AndromedaListView(context, null)
            },
            update = { view, attrs ->
                if (view.items != attrs.items) {
                    view.setItems(attrs.items)
                }
            }
        )
    }
}