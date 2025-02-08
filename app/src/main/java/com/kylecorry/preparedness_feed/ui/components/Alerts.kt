package com.kylecorry.preparedness_feed.ui.components

import android.view.Gravity
import android.view.ViewGroup
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useSizeDp
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Column
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Text
import com.kylecorry.preparedness_feed.domain.Alert
import com.kylecorry.preparedness_feed.infrastructure.alerts.AlertUpdater
import com.kylecorry.preparedness_feed.infrastructure.persistence.AlertRepo


fun Alerts() = Component {
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

    val openAlert = useCallback<Alert, Unit>(context) { alert ->
        if (alert.link.isEmpty()) {
            return@useCallback
        }
        val intent = Intents.url(alert.link)
        context.startActivity(intent)
    }

    val deleteAlert = useCallback<Alert, Unit>(context) { alert ->
        inBackground {
            repo.delete(alert)
            setAlerts(repo.getAll())
        }
    }

    val dp16 = useSizeDp(16f).toInt()

    Column(if (loading) {
        Text {
            text = "Loading $loadingMessage ${(progress * 100).toInt()}%"
            marginTop = dp16
            marginStart = dp16
            marginEnd = dp16
            marginBottom = dp16
            layoutGravity = Gravity.CENTER_HORIZONTAL
            width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    } else {
        UpdateButton {
            onUpdate = updateAlerts
            marginTop = dp16
            marginStart = dp16
            marginEnd = dp16
            marginBottom = dp16
            layoutGravity = Gravity.CENTER_HORIZONTAL
            width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    },
        AlertList {
            this.alerts = alerts
            onDelete = deleteAlert
            onOpen = openAlert
        }
    )
}