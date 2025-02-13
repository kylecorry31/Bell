package com.kylecorry.bell.ui.mappers

import com.kylecorry.bell.domain.AlertLevel

object AlertLevelMapper {

    fun getColor(level: AlertLevel): Int {
        return when (level) {
            AlertLevel.High -> AppColor.Red.color
            AlertLevel.Medium -> AppColor.Orange.color
            AlertLevel.Low -> AppColor.Yellow.color
            else -> AppColor.Gray.color
        }
    }

}