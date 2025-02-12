package com.kylecorry.bell.infrastructure.alerts.crime

import android.content.Context
import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.SourceSystem
import com.kylecorry.bell.infrastructure.alerts.AlertSpecification
import com.kylecorry.bell.infrastructure.alerts.BaseAlertSource
import com.kylecorry.bell.infrastructure.parsers.selectors.Selector

class IC3InternetCrimeAlertSource(context: Context) : BaseAlertSource(context) {
    override fun getSpecification(): AlertSpecification {
        return rss(
            SourceSystem.IC3InternetCrime,
            "https://www.ic3.gov/PSA/RSS",
            AlertType.Crime,
            AlertLevel.Advisory,
            summary = Selector.value("")
        )
    }
}