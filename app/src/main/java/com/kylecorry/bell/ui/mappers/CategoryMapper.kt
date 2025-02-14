package com.kylecorry.bell.ui.mappers

import android.content.Context
import com.kylecorry.bell.R
import com.kylecorry.bell.domain.Category

object CategoryMapper {

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
        return when (category) {
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
}