package com.ciphervault.app;

import android.content.Context;
import android.content.SharedPreferences;

public class CipherVaultPreferences {

    private static final String PREF_NAME = "ciphervault_prefs";
    private static final String KEY_APPEARANCE = "appearance";
    private static final String KEY_SORT_ORDER = "files_sort_order";

    public enum AppearanceMode {
        SYSTEM,
        LIGHT,
        DARK,
        AMOLED
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
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

    public static void saveFileSortOption(Context context, FileSortOption sortOption) {
        if (sortOption == null) return;
        getPrefs(context).edit().putString(KEY_SORT_ORDER, sortOption.name()).apply();
    }

    public static FileSortOption getFileSortOption(Context context) {
        String name = getPrefs(context).getString(KEY_SORT_ORDER, FileSortOption.NAME_ASC.name());
        try {
            return FileSortOption.valueOf(name);
        } catch (Exception e) {
            return FileSortOption.NAME_ASC;
        }
    }
}