package com.kylecorry.preparedness_feed.ui.mappers

import android.content.Context
import com.kylecorry.preparedness_feed.R
import com.kylecorry.preparedness_feed.domain.AlertType

object AlertTypeMapper {

    fun getIcon(type: AlertType): Int {
        return when (type) {
            AlertType.Weather -> R.drawable.weather
            AlertType.Government -> R.drawable.government
            AlertType.Earthquake -> R.drawable.earthquake
            AlertType.Water -> R.drawable.water
            AlertType.SpaceWeather -> R.drawable.space_weather
            AlertType.Health -> R.drawable.health
            AlertType.Volcano -> R.drawable.volcano
            AlertType.Other -> R.drawable.ic_info
            AlertType.Fire -> R.drawable.fire
        }
    }

    fun getName(context: Context, type: AlertType): String {
        return when (type) {
            AlertType.Weather -> context.getString(R.string.weather)
            AlertType.Government -> context.getString(R.string.government)
            AlertType.Earthquake -> context.getString(R.string.earthquake)
            AlertType.Water -> context.getString(R.string.water)
            AlertType.SpaceWeather -> context.getString(R.string.space_weather)
            AlertType.Health -> context.getString(R.string.health)
            AlertType.Volcano -> context.getString(R.string.volcano)
            AlertType.Other -> context.getString(R.string.other)
            AlertType.Fire -> context.getString(R.string.fire)
        }
    }

}