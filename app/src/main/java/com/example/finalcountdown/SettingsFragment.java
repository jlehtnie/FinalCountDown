package com.example.finalcountdown;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
        try {
            namedTimes = gson.fromJson(json, type);
            if (namedTimes == null) {
                namedTimes = new ArrayList<>();
            }
            // Filter out any null entries that might have been created due to deserialization issues
            namedTimes.removeIf(time -> time == null || time.getName() == null || time.getTime() == null);
        } catch (Exception e) {
            // If deserialization fails, start with an empty list
            namedTimes = new ArrayList<>();
        }
    }

    private void saveNamedTimes() {
        if (namedTimes == null) {
            namedTimes = new ArrayList<>();
        }
        // Ensure all times have valid IDs (fix any that might have been deserialized without IDs)
        for (NamedTime time : namedTimes) {
            if (time != null && (time.getId() == null || time.getId().isEmpty())) {
                // Generate ID if missing (can happen if deserialized without proper ID)
                String id = System.currentTimeMillis() + "_" + (time.getName() != null ? time.getName().hashCode() : 0);
                time.setId(id);
            }
        }
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
        if (selectedTimePreference == null || namedTimes == null) return;

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

        EditText nameEdit = new EditText(requireContext());
        nameEdit.setHint(R.string.time_name_hint);
        EditText timeEdit = new EditText(requireContext());
        timeEdit.setHint(R.string.time_value_hint);
        timeEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        
        // Create a simple layout for the dialog
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.addView(nameEdit);
        layout.addView(timeEdit);
        
        builder.setView(layout);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.add, null); // Set to null initially to prevent auto-dismiss

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            // Override the positive button to validate before dismissing
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = nameEdit.getText().toString().trim();
                String time = timeEdit.getText().toString().trim();
                
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(time)) {
                    Toast.makeText(requireContext(), "Name and time are required", Toast.LENGTH_SHORT).show();
                    return; // Don't dismiss dialog on validation error
                }

                // Validate and format time
                String formattedTime = parseAndFormatTime(time);
                if (formattedTime == null) {
                    Toast.makeText(requireContext(), "Invalid time format. Use: HHMMSS", Toast.LENGTH_SHORT).show();
                    return; // Don't dismiss dialog on validation error
                }

                // Validation passed - add the time and dismiss
                // Ensure we have a valid list
                if (namedTimes == null) {
                    namedTimes = new ArrayList<>();
                }
                NamedTime newTime = new NamedTime(name, formattedTime);
                namedTimes.add(newTime);
                saveNamedTimes();
                Toast.makeText(requireContext(), "Time added successfully", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showRemoveTimeDialog() {
        final android.content.Context context = getActivity() != null ? getActivity() : requireContext();

        loadNamedTimes();

        if (namedTimes == null || namedTimes.isEmpty()) {
            Toast.makeText(context, "No times available to remove. Please add a time first.", Toast.LENGTH_LONG).show();
            return;
        }

        final List<NamedTime> validTimes = new ArrayList<>();
        for (NamedTime time : namedTimes) {
            if (time != null && time.getName() != null && !time.getName().trim().isEmpty()
                    && time.getTime() != null && !time.getTime().trim().isEmpty()) {
                validTimes.add(time);
            }
        }

        if (validTimes.isEmpty()) {
            Toast.makeText(context, "No valid times found to remove.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (validTimes.size() <= 1) {
            Toast.makeText(context, R.string.remove_time_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] displayNames = new String[validTimes.size()];
        for (int i = 0; i < validTimes.size(); i++) {
            displayNames[i] = validTimes.get(i).getName() + " (" + validTimes.get(i).getTime() + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.remove_time_dialog_title);
        builder.setItems(displayNames, (dialog, which) -> {
            NamedTime timeToRemove = validTimes.get(which);
            String timeToRemoveId = timeToRemove.getId();

            loadNamedTimes();

            NamedTime toRemove = null;
            for (NamedTime time : namedTimes) {
                if (time != null && time.getId() != null && time.getId().equals(timeToRemoveId)) {
                    toRemove = time;
                    break;
                }
            }

            if (toRemove == null) {
                Toast.makeText(context, "Time not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedId = selectedTimePreference != null ? selectedTimePreference.getValue() : null;
            boolean wasSelected = selectedId != null && timeToRemoveId.equals(selectedId);

            namedTimes.remove(toRemove);

            if (wasSelected && selectedTimePreference != null) {
                if (!namedTimes.isEmpty()) {
                    String newSelectedId = namedTimes.get(0).getId();
                    if (newSelectedId != null) {
                        selectedTimePreference.setValue(newSelectedId);
                    }
                } else {
                    selectedTimePreference.setValue("");
                }
            }

            saveNamedTimes();
            Toast.makeText(context, R.string.remove_time_success, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private String parseAndFormatTime(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }

        // Remove any non-digit characters
        String cleaned = input.replaceAll("[^0-9]", "");
        
        int hour, minute, second;
        
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
        
        if (times == null) {
            return null;
        }
        
        for (NamedTime time : times) {
            if (time != null && time.getId() != null && time.getId().equals(selectedId)) {
                return time.getTime();
            }
        }
        
        return null;
    }
}


