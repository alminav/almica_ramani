package com.almica.ramani

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import timber.log.Timber

class PrefFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use theme attribute for background to support light/dark modes
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            view.setBackgroundColor(typedValue.data)
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)

        findPreference<Preference>(getString(R.string.pref_gps_altitude_correction_reset_key))?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                findPreference<SeekBarPreference>(getString(R.string.pref_gps_altitude_correction_key))?.value = Const.ALTITUDE_CORRECTION
                false
            }
        }

        findPreference<EditTextPreference>(getString(R.string.pref_OfflineMapboxTileCountLimit))?.apply {
            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

        findPreference<ListPreference>(getString(R.string.pref_render_mode))?.apply {
            // Automatic summary provider handles displaying the current selection
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (isAdded) {
            Timber.i("key: $key")
            // Note: SimpleSummaryProvider handles pref_render_mode updates automatically
        }
    }
}
