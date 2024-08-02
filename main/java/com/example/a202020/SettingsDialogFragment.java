package com.example.a202020;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

public class SettingsDialogFragment extends DialogFragment {

    private SettingsManager settingsManager;

    public SettingsDialogFragment(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the dialog_settings.xml layout
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.custom_preference_layout, null);

        // Retrieve the SwitchCompat view
        SwitchCompat switchCompat = view.findViewById(R.id.vibrate_mute_switch);
        SwitchCompat switchCompat2 = view.findViewById(R.id.Test_mode_switch);

        // Update the switch's state based on the saved setting
        boolean savedSetting = settingsManager.getVibrateMuteSetting();
        switchCompat.setChecked(savedSetting);
        boolean savedSetting2 = settingsManager.getTestModeSetting();
        switchCompat2.setChecked(savedSetting2);

        // Create and configure your settings UI elements (e.g., checkboxes)

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setTitle("Settings");

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle saving settings here
                boolean isChecked = switchCompat.isChecked();
                settingsManager.saveVibrateMuteSetting(isChecked);
                boolean isChecked2 = switchCompat2.isChecked();
                settingsManager.saveTestModeSetting(isChecked2);
            }
        });

        builder.setNegativeButton("Cancel", null);

        return builder.create();
    }
}