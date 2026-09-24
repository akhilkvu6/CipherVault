package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

public class CipherVaultPreferences {

    private static final String PREF_NAME = "ciphervault_prefs";
    private static final String KEY_THEME = "theme";
    private static final String KEY_APPEARANCE = "appearance";

    public enum ThemeOption {
        OBSIDIAN,
        RUBY,
        COPPER,
        AMETHYST,
        ROSE,
        SAPPHIRE,
        MONOCHROME
    }

    public enum AppearanceMode {
        LIGHT,
        DARK,
        SYSTEM
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveTheme(Context context, ThemeOption theme) {
        if (theme == null) return;
        getPrefs(context).edit().putString(KEY_THEME, theme.name()).apply();
    }

    public static ThemeOption getTheme(Context context) {
        String name = getPrefs(context).getString(KEY_THEME, ThemeOption.OBSIDIAN.name());
        try {
            return ThemeOption.valueOf(name);
        } catch (Exception e) {
            return ThemeOption.OBSIDIAN;
        }
    }

    public static void saveAppearance(Context context, AppearanceMode mode) {
        if (mode == null) return;
        getPrefs(context).edit().putString(KEY_APPEARANCE, mode.name()).apply();
    }

    public static AppearanceMode getAppearance(Context context) {
        String name = getPrefs(context).getString(KEY_APPEARANCE, AppearanceMode.DARK.name());
        try {
            return AppearanceMode.valueOf(name);
        } catch (Exception e) {
            return AppearanceMode.DARK;
        }
    }
}