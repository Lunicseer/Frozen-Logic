package com.example.a202020;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

public class SettingsManager {
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String VIBRATE_MUTE_KEY = "VibrateMuteSetting";
    private static final String TEST_MODE_KEY = "TestModeSetting";

    public void saveTestModeSetting(boolean isChecked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(TEST_MODE_KEY, isChecked);
        editor.apply();
    }

    public boolean getTestModeSetting() {return preferences.getBoolean(TEST_MODE_KEY, false);}

    public SettingsManager(View settingsContainer, Context context) {
        // ... other initialization
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveVibrateMuteSetting(boolean isChecked) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(VIBRATE_MUTE_KEY, isChecked);
        editor.apply();
    }

    public boolean getVibrateMuteSetting() {
        return preferences.getBoolean(VIBRATE_MUTE_KEY, false);
    }

}