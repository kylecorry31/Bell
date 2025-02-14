package com.kylecorry.bell.ui.mappers

import com.kylecorry.bell.domain.Severity

object SeverityMapper {

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