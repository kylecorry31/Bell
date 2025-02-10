package com.kylecorry.bell.infrastructure.alerts

import android.content.Context

class PacificTsunamiAlertSource(context: Context) :
    TsunamiAlertSource(context, "https://www.tsunami.gov/events/xml/PHEBAtom.xml")