package com.example.finalcountdown;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.Fragment;

public class SettingsDialogFragment extends AppCompatDialogFragment {
    
    public interface OnTimeSelectedListener {
        void onTimeSelected();
    }
    
    private OnTimeSelectedListener listener;

    public void setOnTimeSelectedListener(OnTimeSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.settings_title);

        View content = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_settings, null, false);
        builder.setView(content);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            if (listener != null) {
                listener.onTimeSelected();
            }
            dismiss();
        });

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Fragment existing = getChildFragmentManager().findFragmentById(R.id.settings);
        if (existing == null) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commitNow();
        }
    }
}


