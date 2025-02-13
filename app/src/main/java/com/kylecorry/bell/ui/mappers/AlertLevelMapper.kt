package com.kylecorry.bell.ui.mappers

import com.kylecorry.bell.domain.AlertLevel
import com.kylecorry.bell.domain.Severity

object AlertLevelMapper {

    fun getColor(level: AlertLevel): Int {
        return when (level) {
            AlertLevel.High -> AppColor.Red.color
            AlertLevel.Medium -> AppColor.Orange.color
            AlertLevel.Low -> AppColor.Yellow.color
            else -> AppColor.Gray.color
        }
    }

    fun getColor(severity: Severity): Int {
        return when (severity) {
            Severity.Extreme -> AppColor.Red.color
            Severity.Severe -> AppColor.Orange.color
            Severity.Moderate -> AppColor.Yellow.color
            Severity.Minor -> AppColor.Blue.color
            else -> AppColor.Gray.color
        }
    }

}