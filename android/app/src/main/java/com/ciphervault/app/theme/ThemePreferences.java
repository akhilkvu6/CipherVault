package com.ciphervault.app.theme;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Persistence abstraction for storing non-sensitive user theme preference.
 * Defaults to ThemeMode.SYSTEM.
 */
public class ThemePreferences {

    public static final String PREFS_NAME = "ciphervault_theme_preferences";
    public static final String KEY_THEME_MODE = "key_theme_mode";
    public static final ThemeMode DEFAULT_THEME_MODE = ThemeMode.SYSTEM;

    private final SharedPreferences preferences;

    public ThemePreferences(@NonNull Context context) {
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    public ThemePreferences(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Retrieves the stored theme mode, falling back to DEFAULT_THEME_MODE (SYSTEM).
     */
    @NonNull
    public ThemeMode getThemeMode() {
        String savedMode = preferences.getString(KEY_THEME_MODE, null);
        if (savedMode == null) {
            return DEFAULT_THEME_MODE;
        }
        return ThemeMode.fromString(savedMode);
    }

    /**
     * Persists the given theme mode and immediately applies it to the app.
     */
    public void setThemeMode(@NonNull ThemeMode themeMode) {
        if (themeMode == null) {
            themeMode = DEFAULT_THEME_MODE;
        }
        preferences.edit()
                .putString(KEY_THEME_MODE, themeMode.name())
                .apply();
        applyThemeMode(themeMode);
    }

    /**
     * Applies the given ThemeMode using AppCompatDelegate.
     */
    public void applyThemeMode(@NonNull ThemeMode themeMode) {
        if (themeMode != null) {
            try {
                AppCompatDelegate.setDefaultNightMode(themeMode.getNightMode());
            } catch (Throwable ignored) {
                // Defensive safeguard against environments where AppCompatDelegate is not initialized
            }
        }
    }

    /**
     * Applies the currently persisted theme mode.
     */
    public void applyStoredThemeMode() {
        applyThemeMode(getThemeMode());
    }
}
