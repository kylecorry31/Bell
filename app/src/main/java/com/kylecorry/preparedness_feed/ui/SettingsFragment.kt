package com.kylecorry.preparedness_feed.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.preparedness_feed.R

class SettingsFragment : AndromedaPreferenceFragment() {

    private val navigationMap = mapOf<Int, Int>(
        // Pref key to action id
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        setIconColor(preferenceScreen, Resources.androidTextColorSecondary(requireContext()))
    }

}