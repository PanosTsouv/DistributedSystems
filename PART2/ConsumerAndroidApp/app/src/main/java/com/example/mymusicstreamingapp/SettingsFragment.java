package com.example.mymusicstreamingapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import java.util.concurrent.ExecutionException;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_screen);

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        int numberOfPreferences = prefScreen.getPreferenceCount();

        for (int i = 0; i < numberOfPreferences; i++)
        {
            Preference currentPref = prefScreen.getPreference(i);
            if(currentPref instanceof EditTextPreference) {
                if(currentPref.getKey().equals(getString(R.string.pref_client_pass_key)))
                {
                    ((EditTextPreference) currentPref).setOnBindEditTextListener(
                            new EditTextPreference.OnBindEditTextListener() {
                                @Override
                                public void onBindEditText(@NonNull EditText editText) {
                                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                }
                            });
                }
                String value = sharedPreferences.getString(currentPref.getKey(), "");
                setPreferenceSummary(currentPref, value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference, String value)
    {
        if(preference instanceof EditTextPreference)
        {
            if(preference.getKey().equals(getString(R.string.pref_client_pass_key)))
            {
                preference.setSummary("Hidden");
                return;
            }
            preference.setSummary(value);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = findPreference(key);
        if(preference != null)
        {
            if(preference instanceof EditTextPreference)
            {
                if (preference.getKey().equals(getString(R.string.pref_random_ip_key)) || preference.getKey().equals(getString(R.string.pref_random_port_key)))
                {
                    UnregisterTask myTask = new UnregisterTask();
                    try {
                        myTask.execute(true).get();
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String value = sharedPreferences.getString(preference.getKey(), "");
                preference.setSummary(value);
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
