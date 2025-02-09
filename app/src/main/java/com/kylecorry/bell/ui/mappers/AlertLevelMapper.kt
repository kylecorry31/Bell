package com.kylecorry.bell.ui.mappers

import com.kylecorry.bell.domain.AlertLevel

object AlertLevelMapper {

    fun getColor(level: AlertLevel): Int {
        return when (level) {
            AlertLevel.Watch -> AppColor.Orange.color
            AlertLevel.Warning -> AppColor.Red.color
            AlertLevel.Advisory -> AppColor.Yellow.color
            AlertLevel.Update -> AppColor.Gray.color
            AlertLevel.Event -> AppColor.Purple.color
            AlertLevel.Announcement -> AppColor.Gray.color
            AlertLevel.Other -> AppColor.Gray.color
            AlertLevel.Noise -> AppColor.Gray.color
        }
    }

}