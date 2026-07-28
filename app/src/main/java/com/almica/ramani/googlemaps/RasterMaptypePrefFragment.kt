package com.almica.ramani.googlemaps

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.almica.ramani.R
import timber.log.Timber

/**
 * not working with TileOverlayActivity ComponentActivity
 *
 * The solution was to use Theme.AppCompat as the base theme.
 * Apparently, android:Theme.Material and other SDK themes do not work with Androidx Preferences.
 */
class RasterMaptypePrefFragment : PreferenceFragmentCompat() {
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            Timber.i("")
            super.onViewCreated(view, savedInstanceState)
            view.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            //preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Timber.i("rootKey: $rootKey")
        addPreferencesFromResource(R.xml.raster_maptype_settings)

        val listPreferenceMapType = findPreference<ListPreference>(getString(R.string.pref_tilemaker_url))
        if (listPreferenceMapType.isNotNull()) {
            if (listPreferenceMapType is ListPreference) {
                listPreferenceMapType.summary = "${listPreferenceMapType.value}"
                listPreferenceMapType.title =
                    "${context?.getString(R.string.maptype)}: ${listPreferenceMapType.entry}"
            }
        } else
            Timber.e("pref not found: ${getString(R.string.pref_tilemaker_url)}")

    }

    /*
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        Timber.i("key: $key")
        if (isAdded) {
            if (key == getString(R.string.pref_tilemaker_url)) {
                val listPreference =
                    findPreference<ListPreference>(getString(R.string.pref_tilemaker_url))
                listPreference?.summary = listPreference.value
                listPreference?.title = "${context?.getString(R.string.maptype)}: ${listPreference.entry}"
                val liveSharedPreferences = LiveSharedPreferences(sharedPreferences)
                liveSharedPreferences.preferences.edit {
                    if (listPreference != null) {
                        putString(getString(R.string.pref_tilemaker_maptype), listPreference.entry.toString())
                        Timber.i("${getString(R.string.pref_tilemaker_maptype)} ${listPreference.entry.toString()}")
                    }
                }
            }
        }
    }

     */
}
