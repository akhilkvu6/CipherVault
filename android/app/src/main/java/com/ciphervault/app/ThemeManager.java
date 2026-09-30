package com.ciphervault.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class ThemeManager {

    public static void applyTheme(Activity activity) {
        if (activity == null) return;

        CipherVaultPreferences.AppearanceMode appearance = CipherVaultPreferences.getAppearance(activity);
        applyAppearanceMode(appearance);
        if (appearance == CipherVaultPreferences.AppearanceMode.AMOLED) {
            activity.setTheme(R.style.Theme_CipherVault_Amoled);
        } else {
            activity.setTheme(R.style.Theme_CipherVault);
        }
    }

    public static void applyAppearanceMode(CipherVaultPreferences.AppearanceMode mode) {
        if (mode == null) return;
        switch (mode) {
            case LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case DARK:
            case AMOLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    public static boolean isAmoled(Context context) {
        if (context == null) return false;
        return CipherVaultPreferences.getAppearance(context) == CipherVaultPreferences.AppearanceMode.AMOLED;
    }

    public static int getEncryptedColor(Context context) {
        if (context == null) return Color.parseColor("#B79A6A");
        if (isAmoled(context)) {
            return Color.parseColor("#FFFFFF");
        }
        return ContextCompat.getColor(context, R.color.vault_encrypted);
    }
}