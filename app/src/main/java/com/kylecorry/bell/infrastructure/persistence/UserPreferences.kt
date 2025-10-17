package com.kylecorry.bell.infrastructure.persistence

import android.content.Context
import com.kylecorry.andromeda.preferences.BooleanPreference
import com.kylecorry.andromeda.preferences.IntPreference
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.andromeda.preferences.StringPreference
import com.kylecorry.andromeda.preferences.StringSetPreference
import com.kylecorry.bell.domain.Category

class UserPreferences(context: Context) {
    private val preferences = SharedPreferences(context)

    var geminiApiKey by StringPreference(preferences, "gemini_api_key", "", true)

    var useGemini by BooleanPreference(preferences, "use_gemini", false)

    var state by StringPreference(preferences, "state", "RI")

    var syncIntervalMinutes by IntPreference(preferences, "sync_interval", 60)

    private var notificationCategoryStrings by StringSetPreference(
        preferences,
        "notification_categories",
        setOf(
            "Geophysical",
            "Meteorological",
            "Safety",
            "Security",
            "Rescue",
            "Fire",
            "Health",
            "Environmental",
            "Transport",
            "Infrastructure",
            "CBRNE",
            "Other"
        )
    )

    var notificationCategories: Set<Category>
        get() = notificationCategoryStrings.mapNotNull { categoryString ->
            Category.entries.find { it.name == categoryString }
        }.toSet()
        set(value) {
            notificationCategoryStrings = value.map { it.name }.toSet()
        }

}