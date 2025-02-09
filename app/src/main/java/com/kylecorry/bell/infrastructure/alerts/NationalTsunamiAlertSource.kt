package com.kylecorry.bell.infrastructure.alerts

import android.content.Context
import java.time.ZonedDateTime

class NationalTsunamiAlertSource(context: Context) : TsunamiAlertSource(context) {
    override fun getUrl(): String {
        return "https://www.tsunami.gov/events/xml/PAAQAtom.xml"
    }
}