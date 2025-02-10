package com.kylecorry.bell.infrastructure.alerts

import android.content.Context

class NationalTsunamiAlertSource(context: Context) : TsunamiAlertSource(context, "https://www.tsunami.gov/events/xml/PAAQAtom.xml")