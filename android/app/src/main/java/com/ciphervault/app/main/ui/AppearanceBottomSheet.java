package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ciphervault.app.R;
import com.ciphervault.app.core.preferences.ThemeManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AppearanceBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_appearance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RadioGroup rgTheme = view.findViewById(R.id.rgTheme);
        String currentMode = ThemeManager.getThemeMode(requireContext());

        if (ThemeManager.MODE_LIGHT.equals(currentMode)) {
            ((RadioButton) view.findViewById(R.id.rbThemeLight)).setChecked(true);
        } else if (ThemeManager.MODE_DARK.equals(currentMode)) {
            ((RadioButton) view.findViewById(R.id.rbThemeDark)).setChecked(true);
        } else {
            ((RadioButton) view.findViewById(R.id.rbThemeSystem)).setChecked(true);
        }

        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedMode;
            if (checkedId == R.id.rbThemeLight) {
                selectedMode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.rbThemeDark) {
                selectedMode = ThemeManager.MODE_DARK;
            } else {
                selectedMode = ThemeManager.MODE_SYSTEM;
            }
            ThemeManager.setThemeMode(requireContext(), selectedMode);
            dismiss();
        });
    }
}
