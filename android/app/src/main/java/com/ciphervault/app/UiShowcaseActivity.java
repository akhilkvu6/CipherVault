package com.ciphervault.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Developer Design System Showcase Activity (Easter Egg).
 * Demonstrates CipherVault's system sans-serif typography weights,
 * outlined Lucide icons, layered Samsung One UI-inspired cards,
 * button roles, input fields, interactive bottom sheets, and theme modes.
 */
public class UiShowcaseActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui_showcase);

        MaterialToolbar toolbar = findViewById(R.id.toolbarUiShowcase);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Interactive Theme Switcher
        ChipGroup chipGroupTheme = findViewById(R.id.chipGroupShowcaseTheme);
        if (chipGroupTheme != null) {
            CipherVaultPreferences.AppearanceMode current = CipherVaultPreferences.getAppearance(this);
            if (current == CipherVaultPreferences.AppearanceMode.LIGHT) {
                chipGroupTheme.check(R.id.chipShowcaseThemeLight);
            } else if (current == CipherVaultPreferences.AppearanceMode.DARK) {
                chipGroupTheme.check(R.id.chipShowcaseThemeDark);
            } else {
                chipGroupTheme.check(R.id.chipShowcaseThemeSystem);
            }

            chipGroupTheme.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                CipherVaultPreferences.AppearanceMode mode;
                if (id == R.id.chipShowcaseThemeLight) {
                    mode = CipherVaultPreferences.AppearanceMode.LIGHT;
                } else if (id == R.id.chipShowcaseThemeDark) {
                    mode = CipherVaultPreferences.AppearanceMode.DARK;
                } else {
                    mode = CipherVaultPreferences.AppearanceMode.SYSTEM;
                }
                CipherVaultPreferences.saveAppearance(this, mode);
                ThemeManager.applyAppearanceMode(mode);
                recreate();
            });
        }

        // Demo Dialog
        MaterialButton btnDemoDialog = findViewById(R.id.btnDemoDialog);
        if (btnDemoDialog != null) {
            btnDemoDialog.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Material Confirmation Dialog")
                        .setMessage("Demonstrates sans-serif typography, rounded 20dp dialog corners, and outlined Lucide action buttons.")
                        .setIcon(R.drawable.ic_lucide_shield_check)
                        .setPositiveButton("Confirm", (dialog, which) ->
                                Toast.makeText(this, "Dialog confirmed", Toast.LENGTH_SHORT).show())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // Demo Bottom Sheet
        MaterialButton btnDemoBottomSheet = findViewById(R.id.btnDemoBottomSheet);
        if (btnDemoBottomSheet != null) {
            btnDemoBottomSheet.setOnClickListener(v -> {
                BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
                android.widget.LinearLayout sheetView = new android.widget.LinearLayout(this);
                sheetView.setOrientation(android.widget.LinearLayout.VERTICAL);
                int pad = (int) (24 * getResources().getDisplayMetrics().density);
                sheetView.setPadding(pad, pad, pad, pad);

                android.widget.TextView tvTitle = new android.widget.TextView(this);
                tvTitle.setText("Interactive Bottom Sheet");
                tvTitle.setTextSize(18);
                tvTitle.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                sheetView.addView(tvTitle);

                android.widget.TextView tvDesc = new android.widget.TextView(this);
                tvDesc.setText("Demonstrating smooth modal surface elevation and sans-serif font family.");
                tvDesc.setTextSize(13);
                tvDesc.setPadding(0, (int) (8 * getResources().getDisplayMetrics().density), 0, (int) (16 * getResources().getDisplayMetrics().density));
                sheetView.addView(tvDesc);

                MaterialButton btnClose = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonTonalStyle);
                btnClose.setText("Close Sheet");
                btnClose.setOnClickListener(sv -> bottomSheet.dismiss());
                sheetView.addView(btnClose);

                bottomSheet.setContentView(sheetView);
                bottomSheet.show();
            });
        }

        MaterialButton btnPrimary = findViewById(R.id.btnShowcasePrimary);
        if (btnPrimary != null) {
            btnPrimary.setOnClickListener(v -> Toast.makeText(this, "Primary action clicked", Toast.LENGTH_SHORT).show());
        }

        MaterialButton btnTonal = findViewById(R.id.btnShowcaseTonal);
        if (btnTonal != null) {
            btnTonal.setOnClickListener(v -> Toast.makeText(this, "Tonal action clicked", Toast.LENGTH_SHORT).show());
        }

        MaterialButton btnOutlined = findViewById(R.id.btnShowcaseOutlined);
        if (btnOutlined != null) {
            btnOutlined.setOnClickListener(v -> Toast.makeText(this, "Outlined action clicked", Toast.LENGTH_SHORT).show());
        }

        MaterialButton btnDestructive = findViewById(R.id.btnShowcaseDestructive);
        if (btnDestructive != null) {
            btnDestructive.setOnClickListener(v -> Toast.makeText(this, "Destructive action clicked", Toast.LENGTH_SHORT).show());
        }
    }
}
