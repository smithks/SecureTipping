package com.example.keegan.securetipping;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Settings fragment used to display preference summary below preference.
 * Created by Keegan on 11/12/2015.
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences sPref;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_tipping);
        sPref = getPreferenceScreen().getSharedPreferences();
        sPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i){
            Preference preference = getPreferenceScreen().getPreference(i);
            updatePreference(preference);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        updatePreference(pref);
    }

    /**
     * Sets the summary line to the current value of the preference.
     * Called when preference value changes.
     * @param preference preference changed
     */
    public void updatePreference(Preference preference){
        if (preference instanceof ListPreference)
            preference.setSummary(((ListPreference)preference).getEntry());
        else  if(preference instanceof NumberPickerPreference)
            preference.setSummary(((NumberPickerPreference)preference).getEntry());
    }
}
