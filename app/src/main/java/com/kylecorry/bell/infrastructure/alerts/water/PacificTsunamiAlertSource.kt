package com.kylecorry.bell.infrastructure.alerts.water

import android.content.Context

class PacificTsunamiAlertSource(context: Context) :
    TsunamiAlertSource(context, "https://www.tsunami.gov/events/xml/PHEBAtom.xml")