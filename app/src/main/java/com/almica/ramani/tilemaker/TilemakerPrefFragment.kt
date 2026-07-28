package com.almica.ramani.tilemaker

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.almica.ramani.R
import timber.log.Timber

class TilemakerPrefFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    companion object {
        private const val MIN_ZOOM = 4
        private const val MAX_ZOOM = 15
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Background color should be handled by the theme
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tilemaker_settings, rootKey)

        setupListPreference()
        setupZoomPreference(R.string.pref_tilemaker_minzoom, MIN_ZOOM, MAX_ZOOM)
        setupZoomPreference(R.string.pref_tilemaker_maxzoom, MIN_ZOOM, MAX_ZOOM)
        
        findPreference<EditTextPreference>(getString(R.string.pref_OfflineMapboxTileCountLimit))?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
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

    private fun setupListPreference() {
        findPreference<ListPreference>(getString(R.string.pref_tilemaker_url))?.apply {
            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            updateListPreferenceTitle(this)
        }
    }

    private fun updateListPreferenceTitle(preference: ListPreference) {
        preference.title = "${context?.getString(R.string.maptype)}: ${preference.entry}"
    }

    private fun setupZoomPreference(resId: Int, min: Int, max: Int) {
        findPreference<EditTextPreference>(getString(resId))?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

            setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.addTextChangedListener(createZoomTextWatcher(editText, min, max))
            }

            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                val zoom = newValue.toString().toIntOrNull()
                zoom != null && zoom in min..max
            }
        }
    }

    private fun createZoomTextWatcher(editText: EditText, min: Int, max: Int) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(editable: Editable?) {
            val value = editable.toString().toIntOrNull()
            if (value == null || value !in min..max) {
                editText.error = context?.getString(R.string.empty_string).let { "Value must be between $min and $max" }
            } else {
                editText.error = null
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (!isAdded) return

        when (key) {
            getString(R.string.pref_tilemaker_url) -> {
                val listPref = findPreference<ListPreference>(key)
                listPref?.let {
                    updateListPreferenceTitle(it)
                    sharedPreferences.edit {
                        putString(getString(R.string.pref_tilemaker_maptype), it.entry.toString())
                    }
                    Timber.i("${getString(R.string.pref_tilemaker_maptype)} ${it.entry}")
                }
            }
        }
    }
}
