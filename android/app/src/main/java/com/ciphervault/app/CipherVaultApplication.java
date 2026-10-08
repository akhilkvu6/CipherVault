package com.ciphervault.app;

import android.app.Application;

import com.ciphervault.app.theme.ThemePreferences;
import com.google.android.material.color.DynamicColors;

public class CipherVaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply persisted theme mode before activities initialize
        ThemePreferences themePreferences = new ThemePreferences(this);
        themePreferences.applyStoredThemeMode();

        // Apply system-provided Material Dynamic Color on Android 12+ (API 31+)
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
