package com.kylecorry.bell.ui.mappers

import android.content.Context
import com.kylecorry.bell.R
import com.kylecorry.bell.domain.AlertType
import com.kylecorry.bell.domain.Category

object AlertTypeMapper {

    fun getIcon(category: Category): Int {
        return when (category) {
            Category.Geophysical -> R.drawable.category_geo
            Category.Meteorological -> R.drawable.category_met
            Category.Safety -> R.drawable.category_safety
            Category.Security -> R.drawable.category_security
            Category.Rescue -> R.drawable.category_rescue
            Category.Fire -> R.drawable.category_fire
            Category.Health -> R.drawable.category_health
            Category.Environmental -> R.drawable.category_env
            Category.Transport -> R.drawable.category_transport
            Category.Infrastructure -> R.drawable.category_infra
            Category.CBRNE -> R.drawable.category_cbrne
            Category.Other -> R.drawable.ic_info
        }
    }

    fun getName(context: Context, category: Category): String {
        return when(category){
            Category.Geophysical -> "Geophysical"
            Category.Meteorological -> "Meteorological"
            Category.Safety -> "Safety"
            Category.Security -> "Security"
            Category.Rescue -> "Rescue"
            Category.Fire -> "Fire"
            Category.Health -> "Health"
            Category.Environmental -> "Environmental"
            Category.Transport -> "Transport"
            Category.Infrastructure -> "Infrastructure"
            Category.CBRNE -> "CBRNE"
            Category.Other -> "Other"
        }
    }

    fun getIcon(type: AlertType): Int {
        return when (type) {
            AlertType.Weather -> R.drawable.category_met
            AlertType.Government -> R.drawable.government
            AlertType.Earthquake -> R.drawable.earthquake
            AlertType.Water -> R.drawable.water
            AlertType.SpaceWeather -> R.drawable.space_weather
            AlertType.Health -> R.drawable.category_health
            AlertType.Volcano -> R.drawable.volcano
            AlertType.Other -> R.drawable.ic_info
            AlertType.Fire -> R.drawable.category_fire
            AlertType.Travel -> R.drawable.travel
            AlertType.Economy -> R.drawable.economy
            AlertType.Crime -> R.drawable.category_security
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