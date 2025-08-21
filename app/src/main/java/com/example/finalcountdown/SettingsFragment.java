package com.example.finalcountdown;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String PREF_NAMED_TIMES = "named_times";
    private static final String PREF_SELECTED_TIME = "pref_selected_time";
    private List<NamedTime> namedTimes;
    private ListPreference selectedTimePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        
        loadNamedTimes();
        setupPreferences();
    }

    private void loadNamedTimes() {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        String json = prefs.getString(PREF_NAMED_TIMES, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<NamedTime>>(){}.getType();
        namedTimes = gson.fromJson(json, type);
    }

    private void saveNamedTimes() {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(namedTimes);
        editor.putString(PREF_NAMED_TIMES, json);
        editor.apply();
        updateSelectedTimePreference();
    }

    private void setupPreferences() {
        selectedTimePreference = findPreference(PREF_SELECTED_TIME);
        updateSelectedTimePreference();

        Preference addTimePreference = findPreference("pref_add_time");
        if (addTimePreference != null) {
            addTimePreference.setOnPreferenceClickListener(preference -> {
                showAddTimeDialog();
                return true;
            });
        }

        Preference removeTimePreference = findPreference("pref_remove_time");
        if (removeTimePreference != null) {
            removeTimePreference.setOnPreferenceClickListener(preference -> {
                showRemoveTimeDialog();
                return true;
            });
        }

        if (selectedTimePreference != null) {
            selectedTimePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Handle time selection
                return true;
            });
        }
    }

    private void updateSelectedTimePreference() {
        if (selectedTimePreference == null) return;

        String[] entries = new String[namedTimes.size()];
        String[] entryValues = new String[namedTimes.size()];
        
        for (int i = 0; i < namedTimes.size(); i++) {
            NamedTime namedTime = namedTimes.get(i);
            entries[i] = namedTime.toString();
            entryValues[i] = namedTime.getId();
        }

        selectedTimePreference.setEntries(entries);
        selectedTimePreference.setEntryValues(entryValues);
        
        // Set default value if none selected
        if (TextUtils.isEmpty(selectedTimePreference.getValue()) && entryValues.length > 0) {
            selectedTimePreference.setValue(entryValues[0]);
        }
    }

    private void showAddTimeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.add_time_dialog_title);

        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, null);
        EditText nameEdit = new EditText(requireContext());
        nameEdit.setHint(R.string.time_name_hint);
        EditText timeEdit = new EditText(requireContext());
        timeEdit.setHint(R.string.time_value_hint);
        timeEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        View container = new View(requireContext());
        container.setLayoutParams(new android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        
        // Create a simple layout for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(nameEdit);
        layout.addView(timeEdit);
        
        builder.setView(layout);

        builder.setPositiveButton(R.string.add, (dialog, which) -> {
            String name = nameEdit.getText().toString().trim();
            String time = timeEdit.getText().toString().trim();
            
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(time)) {
                Toast.makeText(requireContext(), "Name and time are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate and format time
            String formattedTime = parseAndFormatTime(time);
            if (formattedTime == null) {
                Toast.makeText(requireContext(), "Invalid time format. Use HH.MM.SS or HHMMSS", Toast.LENGTH_SHORT).show();
                return;
            }

            NamedTime newTime = new NamedTime(name, formattedTime);
            namedTimes.add(newTime);
            saveNamedTimes();
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showRemoveTimeDialog() {
        if (namedTimes.size() <= 1) {
            Toast.makeText(requireContext(), R.string.remove_time_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] timeNames = new String[namedTimes.size()];
        for (int i = 0; i < namedTimes.size(); i++) {
            timeNames[i] = namedTimes.get(i).toString();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.remove_time_dialog_title);
        builder.setMessage(R.string.remove_time_dialog_message);
        builder.setItems(timeNames, (dialog, which) -> {
            NamedTime timeToRemove = namedTimes.get(which);
            
            // Check if this is the currently selected time
            String selectedId = selectedTimePreference.getValue();
            if (timeToRemove.getId().equals(selectedId)) {
                // If removing the selected time, select the first available time
                String newSelectedId = namedTimes.get(0).getId();
                if (which == 0 && namedTimes.size() > 1) {
                    newSelectedId = namedTimes.get(1).getId();
                }
                selectedTimePreference.setValue(newSelectedId);
            }
            
            namedTimes.remove(which);
            saveNamedTimes();
            Toast.makeText(requireContext(), R.string.remove_time_success, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private String parseAndFormatTime(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        // Remove any non-digit characters except dots
        String cleaned = input.replaceAll("[^0-9.]", "");
        
        int hour, minute, second;
        
        if (cleaned.contains(".")) {
            // Format: HH.MM.SS
            String[] parts = cleaned.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            try {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
                second = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            // Format: HHMMSS
            if (cleaned.length() != 6) {
                return null;
            }
            try {
                hour = Integer.parseInt(cleaned.substring(0, 2));
                minute = Integer.parseInt(cleaned.substring(2, 4));
                second = Integer.parseInt(cleaned.substring(4, 6));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Validate time ranges
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 || second < 0 || second > 59) {
            return null;
        }

        // Format as HH:MM:SS
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    public static String getSelectedTimeValue(SharedPreferences prefs) {
        String selectedId = prefs.getString(PREF_SELECTED_TIME, "");
        if (TextUtils.isEmpty(selectedId)) {
            return null;
        }

        String json = prefs.getString(PREF_NAMED_TIMES, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<NamedTime>>(){}.getType();
        List<NamedTime> times = gson.fromJson(json, type);
        
        for (NamedTime time : times) {
            if (time.getId().equals(selectedId)) {
                return time.getTime();
            }
        }
        
        return null;
    }
}
