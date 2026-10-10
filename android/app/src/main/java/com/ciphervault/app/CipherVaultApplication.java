package com.ciphervault.app;

import android.app.Application;

import com.ciphervault.app.theme.ThemePreferences;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class CipherVaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply persisted appearance/theme mode before activities initialize
        CipherVaultPreferences.AppearanceMode appearance = CipherVaultPreferences.getAppearance(this);
        ThemeManager.applyAppearanceMode(appearance);

        ThemePreferences themePreferences = new ThemePreferences(this);
        themePreferences.applyStoredThemeMode();

        // Apply system-provided Material Dynamic Color on Android 12+ (API 31+) based on wallpaper
        if (DynamicColors.isDynamicColorAvailable()) {
            DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                    .setPrecondition((activity, theme) -> CipherVaultPreferences.isDynamicColorEnabled(activity))
                    .build();
            DynamicColors.applyToActivitiesIfAvailable(this, options);
        }
    }
}
