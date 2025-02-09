package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import java.time.ZonedDateTime

class PacificTsunamiAlertSource(context: Context) : TsunamiAlertSource(context) {
    override fun getUrl(since: ZonedDateTime): String {
        return "https://www.tsunami.gov/events/xml/PHEBAtom.xml"
    }
}