package com.kylecorry.bell.app

import android.content.Context
import com.kylecorry.andromeda.notify.Notify

object NotificationChannels {

    fun createChannels(context: Context) {
        // Create channels here
        Notify.createChannel(
            context,
            CHANNEL_ID_ALERTS,
            "Alerts",
            "Alerts",
            Notify.CHANNEL_IMPORTANCE_HIGH
        )
    }

    const val CHANNEL_ID_ALERTS = "alerts"

}