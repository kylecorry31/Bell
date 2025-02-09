package com.kylecorry.preparedness_feed.ui.mappers

import com.kylecorry.preparedness_feed.domain.AlertLevel

object AlertLevelMapper {

    fun getColor(level: AlertLevel): Int {
        return when (level) {
            AlertLevel.Watch -> AppColor.Orange.color
            AlertLevel.Warning -> AppColor.Red.color
            AlertLevel.Advisory -> AppColor.Yellow.color
            AlertLevel.Update -> AppColor.Gray.color
            AlertLevel.Event -> AppColor.Purple.color
            AlertLevel.Announcement -> AppColor.Gray.color
            AlertLevel.Order -> AppColor.Gray.color
            AlertLevel.Law -> AppColor.Gray.color
            AlertLevel.Other -> AppColor.Gray.color
        }
    }

}