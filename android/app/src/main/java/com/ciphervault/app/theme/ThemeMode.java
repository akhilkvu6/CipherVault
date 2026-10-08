package com.ciphervault.app.theme;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Conceptual theme modes supported by CipherVault:
 * - LIGHT: Force light theme
 * - DARK: Force dark theme
 * - SYSTEM: Follow system night mode setting
 */
public enum ThemeMode {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    private final int nightMode;

    ThemeMode(int nightMode) {
        this.nightMode = nightMode;
    }

    public int getNightMode() {
        return nightMode;
    }

    /**
     * Resolves a string value to a ThemeMode, returning SYSTEM if null or unrecognized.
     */
    public static ThemeMode fromString(String value) {
        if (value == null) {
            return SYSTEM;
        }
        try {
            return ThemeMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SYSTEM;
        }
    }
}
