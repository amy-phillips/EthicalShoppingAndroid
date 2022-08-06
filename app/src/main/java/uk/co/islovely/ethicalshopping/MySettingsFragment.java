package uk.co.islovely.ethicalshopping;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment : PreferenceFragmentCompat() {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
