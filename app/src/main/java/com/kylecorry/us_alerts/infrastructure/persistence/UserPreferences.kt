package com.kylecorry.bell.infrastructure.persistence

import android.content.Context
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.andromeda.preferences.StringPreference

class UserPreferences(context: Context) {
    private val preferences = SharedPreferences(context)

    var geminiApiKey by StringPreference(preferences, "gemini_api_key", "", true)

}