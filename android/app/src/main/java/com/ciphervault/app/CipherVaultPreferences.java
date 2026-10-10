package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

public class CipherVaultPreferences {

    private static final String PREF_NAME = "ciphervault_prefs";
    private static final String KEY_APPEARANCE = "appearance";
    private static final String KEY_SORT_ORDER = "files_sort_order";
    private static final String KEY_DYNAMIC_COLOR = "dynamic_color_enabled";

    public enum AppearanceMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDynamicColorEnabled(Context context) {
        if (context == null) return true;
        return getPrefs(context).getBoolean(KEY_DYNAMIC_COLOR, true);
    }

    public static void setDynamicColorEnabled(Context context, boolean enabled) {
        if (context == null) return;
        getPrefs(context).edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply();
    }

    public static void saveAppearance(Context context, AppearanceMode mode) {
        if (mode == null) return;
        getPrefs(context).edit().putString(KEY_APPEARANCE, mode.name()).apply();
    }

    public static AppearanceMode getAppearance(Context context) {
        if (context == null) return AppearanceMode.SYSTEM;
        try {
            SharedPreferences prefs = getPrefs(context);
            Object raw = prefs.getAll().get(KEY_APPEARANCE);
            if (raw instanceof Integer) {
                int modeInt = (Integer) raw;
                if (modeInt == 1) return AppearanceMode.LIGHT;
                if (modeInt == 2) return AppearanceMode.DARK;
                return AppearanceMode.SYSTEM;
            }
            String name = prefs.getString(KEY_APPEARANCE, AppearanceMode.SYSTEM.name());
            return AppearanceMode.valueOf(name);
        } catch (ClassCastException | IllegalArgumentException e) {
            return AppearanceMode.SYSTEM;
        }
    }

    public static void saveFileSortOption(Context context, FileSortOption sortOption) {
        if (sortOption == null) return;
        getPrefs(context).edit().putString(KEY_SORT_ORDER, sortOption.name()).apply();
    }

    public static FileSortOption getFileSortOption(Context context) {
        if (context == null) return FileSortOption.NAME_ASC;
        try {
            SharedPreferences prefs = getPrefs(context);
            Object raw = prefs.getAll().get(KEY_SORT_ORDER);
            if (raw instanceof Integer) {
                int idx = (Integer) raw;
                FileSortOption[] options = FileSortOption.values();
                if (idx >= 0 && idx < options.length) {
                    return options[idx];
                }
                return FileSortOption.NAME_ASC;
            }
            String name = prefs.getString(KEY_SORT_ORDER, FileSortOption.NAME_ASC.name());
            return FileSortOption.valueOf(name);
        } catch (ClassCastException | IllegalArgumentException e) {
            return FileSortOption.NAME_ASC;
        }
    }
}