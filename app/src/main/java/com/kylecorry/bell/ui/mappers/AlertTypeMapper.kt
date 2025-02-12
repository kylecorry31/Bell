package com.kylecorry.bell.ui.mappers

import android.content.Context
import com.kylecorry.bell.R
import com.kylecorry.bell.domain.AlertType

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
            AlertType.Travel -> R.drawable.travel
            AlertType.Economy -> R.drawable.economy
            AlertType.Crime -> R.drawable.crime
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
            AlertType.Travel -> context.getString(R.string.travel)
            AlertType.Economy -> context.getString(R.string.economy)
            AlertType.Crime -> context.getString(R.string.crime)
        }
    }

}