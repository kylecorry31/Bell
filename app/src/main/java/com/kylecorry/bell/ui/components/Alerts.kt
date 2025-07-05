package com.kylecorry.bell.ui.components

import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useSizeDp
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Column
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Component
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.EditText
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Row
import com.kylecorry.andromeda.views.reactivity.AndromedaViews.Text
import com.kylecorry.bell.domain.Alert
import com.kylecorry.bell.infrastructure.alerts.AlertUpdater
import com.kylecorry.bell.infrastructure.persistence.AlertRepo
import com.kylecorry.bell.infrastructure.persistence.UserPreferences
import com.kylecorry.luna.coroutines.onMain


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

    val preferences = useMemo(context) {
        UserPreferences(context)
    }

    // Load the initial alerts
    useBackgroundEffect(repo) {
        setLoading(true)
        repo.cleanup()
        setAlerts(repo.getAll())
        setLoading(false)
    }

    val updateAlerts = useCallback<Unit>(context, alerts) {
        setLoading(true)
        setLoadingMessage("sources")
        setProgress(0f)
        inBackground {
            val newAlerts = AlertUpdater(context).update(
                setProgress = setProgress,
                setLoadingMessage = setLoadingMessage,
                onAlertsUpdated = setAlerts
            )
            // TODO: Highlight new alerts instead
            onMain {
                Alerts.toast(context, "${newAlerts.size} new alerts")
            }
            setLoading(false)
        }
    }

    val deleteAlert = useCallback<Alert, Unit>(context) { alert ->
        inBackground {
            repo.delete(alert)
            setAlerts(repo.getAll())
        }
    }

    val dp16 = useSizeDp(16f).toInt()
    val dp48 = useSizeDp(48f).toInt()

    val onStateTextChanged = useMemo {
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                preferences.state = s.toString()
            }

            override fun afterTextChanged(s: android.text.Editable?) {
            }
        }
    }

    Column(
        Row(
            {
            marginTop = dp16
            marginStart = dp16
            marginEnd = dp16
            marginBottom = dp16
            layoutGravity = Gravity.CENTER_HORIZONTAL
            width = ViewGroup.LayoutParams.WRAP_CONTENT
        },
            EditText {
                text = preferences.state
                updateText = true
                hint = "State Code"
                onTextChanged = onStateTextChanged
                marginEnd = dp16
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                layoutGravity = Gravity.CENTER
                height = dp48
            },
            if (loading) {
                Text {
                    text = "Loading $loadingMessage ${(progress * 100).toInt()}%"
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    layoutGravity = Gravity.CENTER
                }
            } else {
                UpdateButton {
                    onUpdate = updateAlerts
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    layoutGravity = Gravity.CENTER
                }
            }
        ),
        AlertList {
            this.alerts = alerts
            onDelete = deleteAlert
        }
    )
}