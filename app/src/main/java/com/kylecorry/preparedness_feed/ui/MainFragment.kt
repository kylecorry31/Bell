package com.kylecorry.preparedness_feed.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useSizeDp
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Column
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Text
import com.kylecorry.andromeda.views.reactivity.VDOM
import com.kylecorry.andromeda.views.reactivity.VDOMNode
import com.kylecorry.preparedness_feed.databinding.FragmentMainBinding
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.infrastructure.alerts.AlertUpdater
import com.kylecorry.preparedness_feed.infrastructure.persistence.AlertRepo
import com.kylecorry.preparedness_feed.ui.components.AlertList
import com.kylecorry.preparedness_feed.ui.components.UpdateButton

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

        // TODO: Prompt for Gemini API key if not set
        // TODO: Prompt for area if not set

        val repo = useMemo(context) {
            AlertRepo.getInstance(context)
        }

        // Load the initial alerts
        useBackgroundEffect(repo) {
            setLoading(true)
            setAlerts(repo.getAll())
            setLoading(false)
        }

        val updateAlerts = useCallback<Unit>(context, alerts) {
            setLoading(true)
            setLoadingMessage("sources")
            setProgress(0f)
            inBackground {
                AlertUpdater(context).update(
                    setProgress = setProgress,
                    setLoadingMessage = setLoadingMessage,
                    onAlertsUpdated = setAlerts
                )
                setLoading(false)
            }
        }

        val openAlert = useCallback<Unit, Alert>(context) { alert ->
            if (alert.link.isEmpty()) {
                return@useCallback
            }
            val intent = Intents.url(alert.link)
            context.startActivity(intent)
        }

        val deleteAlert = useCallback<Unit, Alert>(context) { alert ->
            inBackground {
                repo.delete(alert)
                setAlerts(repo.getAll())
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
                    UpdateButton {
                        onUpdate = updateAlerts
                        marginTop = dp16
                        marginStart = dp16
                        marginEnd = dp16
                        marginBottom = dp16
                    }
                },
                AlertList {
                    this.alerts = alerts
                    onDelete = deleteAlert
                    onOpen = openAlert
                }
            )
        )
    }

    private fun render(node: VDOMNode<*, *>) {
        VDOM.render(binding.root, node, binding.root.children.firstOrNull())
    }
}