package com.kylecorry.bell.ui

import android.os.Bundle
import androidx.preference.Preference
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.bell.R
import com.kylecorry.bell.infrastructure.background.BackgroundWorker

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

        // Update background worker when sync interval changes
        val syncIntervalPref = findPreference<Preference>("sync_interval")
        syncIntervalPref?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                // Re-enable background worker with new interval
                BackgroundWorker.enable(requireContext(), true)
                true
            }
    }

}