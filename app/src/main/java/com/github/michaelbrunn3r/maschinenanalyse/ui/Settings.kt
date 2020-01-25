package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.util.formatTime
import java.lang.NumberFormatException
import java.util.concurrent.TimeUnit

class Settings {
    companion object {
        const val MIN_RECORDING_DURATION: Long = 500
        const val MIN_COMPARE_TIME_WINDOW: Long = 500

        // Defaults
        const val DEFAULT_NUM_AUDIO_SAMPLES = 4096
        const val DEFAULT_AUDIO_SAMPLE_RATE = 44100
        const val DEFAULT_NUM_ACCEL_SAMPLES = 512
        const val DEFAULT_RECORDING_DURATION: Long = 10000
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        /** Recording Duration Preference **/
        val recordingDurationPreference: EditTextPreference? = findPreference("recordingDuration")
        recordingDurationPreference?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            return@SummaryProvider formatTime(it.text.toLong(), TimeUnit.MILLISECONDS)
        }
        recordingDurationPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.addTextChangedListener(StrikeThroughInvalid(Settings.MIN_COMPARE_TIME_WINDOW, it))
        }
        recordingDurationPreference?.onPreferenceChangeListener = MinValuePreferenceChangeListener(Settings.MIN_RECORDING_DURATION)


        /** Comparison Time Window Preference **/
        val compTimeWindowPreference: EditTextPreference? = findPreference("compTimeWindow")
        compTimeWindowPreference?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            return@SummaryProvider formatTime(preference.text.toLong(), TimeUnit.MILLISECONDS)
        }
        compTimeWindowPreference?.setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
            it.addTextChangedListener(StrikeThroughInvalid(Settings.MIN_COMPARE_TIME_WINDOW, it))
        }
        compTimeWindowPreference?.onPreferenceChangeListener = MinValuePreferenceChangeListener(Settings.MIN_COMPARE_TIME_WINDOW)
    }
}

class MinValuePreferenceChangeListener(val min: Long): Preference.OnPreferenceChangeListener {
    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        try {
            val value = (newValue as String).toInt()
            if (value < min) return false
        } catch (e:NumberFormatException) {}
        return true
    }
}

class StrikeThroughInvalid(val min:Long, val watched: EditText): TextWatcher {
    override fun afterTextChanged(s: Editable?) {
        try {
            val value = s.toString().toInt()
            if(value < Settings.MIN_RECORDING_DURATION) {
                watched.paintFlags = watched.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                watched.paintFlags = watched.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        } catch (e: NumberFormatException) {}
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}