package com.ciphervault.app.core.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class ConnectionPreferences {
    private static final String PREFS_NAME = "ciphervault_connection_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    private final SharedPreferences prefs;

    public ConnectionPreferences(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setServerUrl(String url) {
        if (url != null && !url.endsWith("/")) {
            url = url + "/";
        }
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, null);
    }
}
