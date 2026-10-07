package com.ciphervault.app;

import android.app.Application;

import com.ciphervault.app.core.preferences.ThemeManager;

public class CipherVaultApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Authoritative application-wide theme initialization
        ThemeManager.applyTheme(this);
    }
}
