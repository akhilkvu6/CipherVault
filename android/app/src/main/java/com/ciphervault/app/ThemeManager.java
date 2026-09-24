package com.ciphervault.app;

import android.app.Activity;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    public static void applyTheme(Activity activity) {
        if (activity == null) return;

        CipherVaultPreferences.AppearanceMode appearance = CipherVaultPreferences.getAppearance(activity);
        applyAppearanceMode(appearance);

        CipherVaultPreferences.ThemeOption theme = CipherVaultPreferences.getTheme(activity);
        int styleRes = getStyleForTheme(theme);
        activity.setTheme(styleRes);
    }

    public static void applyAppearanceMode(CipherVaultPreferences.AppearanceMode mode) {
        if (mode == null) return;
        switch (mode) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static int getStyleForTheme(CipherVaultPreferences.ThemeOption theme) {
        if (theme == null) return R.style.Theme_CipherVault_Obsidian;
        switch (theme) {
            case RUBY:
                return R.style.Theme_CipherVault_Ruby;
            case COPPER:
                return R.style.Theme_CipherVault_Copper;
            case AMETHYST:
                return R.style.Theme_CipherVault_Amethyst;
            case ROSE:
                return R.style.Theme_CipherVault_Rose;
            case SAPPHIRE:
                return R.style.Theme_CipherVault_Sapphire;
            case MONOCHROME:
                return R.style.Theme_CipherVault_Monochrome;
            case OBSIDIAN:
            default:
                return R.style.Theme_CipherVault_Obsidian;
        }
    }
}