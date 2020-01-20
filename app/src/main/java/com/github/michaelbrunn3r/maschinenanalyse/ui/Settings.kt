package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.michaelbrunn3r.maschinenanalyse.R

class SettingsFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}