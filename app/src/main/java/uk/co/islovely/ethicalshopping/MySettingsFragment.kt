package uk.co.islovely.ethicalshopping;

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat;

class MySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}
