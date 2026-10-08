package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;

import com.ciphervault.app.onboarding.OnboardingActivity;
import com.ciphervault.app.onboarding.OnboardingPreferences;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_MIN_DURATION_MS = 2000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isNavigated = false;
    private boolean timerElapsed = false;
    private Boolean isBackendOnline = null; // null = pending, true = healthy, false = failed

    private SessionManager sessionManager;
    private OnboardingPreferences onboardingPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);
        onboardingPrefs = new OnboardingPreferences(this);

        // Schedule minimum 2s splash display
        handler.postDelayed(() -> {
            timerElapsed = true;
            checkAndProceed();
        }, SPLASH_MIN_DURATION_MS);

        // If not first launch, run concurrent health probes across ALL saved servers (LRU Priority)
        if (onboardingPrefs.isOnboardingCompleted()) {
            ServerConnectionManager scm = ServerConnectionManager.getInstance(this);
            scm.probeAllSavedServers(this, (results, selectedServer) -> {
                runOnUiThread(() -> {
                    if (selectedServer != null) {
                        ApiClient.setBaseUrl(SplashActivity.this, selectedServer.getCanonicalUrl());
                        scm.recordServerUsed(selectedServer.getCanonicalUrl());
                        isBackendOnline = true;
                    } else {
                        isBackendOnline = false;
                    }
                    if (timerElapsed) {
                        checkAndProceed();
                    }
                });
            });

            // Safety timeout: if probe stalls beyond 4.5s, proceed with fallback to ConnectionActivity
            handler.postDelayed(() -> {
                if (isBackendOnline == null) {
                    isBackendOnline = false;
                    checkAndProceed();
                }
            }, 4500L);
        }
    }

    private synchronized void checkAndProceed() {
        if (isFinishing() || isDestroyed() || isNavigated) {
            return;
        }

        // Wait until at least 2s splash duration has passed
        if (!timerElapsed) {
            return;
        }

        // 1. First Run -> Route to Onboarding
        if (!onboardingPrefs.isOnboardingCompleted()) {
            isNavigated = true;
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Wait if health probe is still running within reasonable time
        if (isBackendOnline == null) {
            return;
        }

        isNavigated = true;
        Intent intent;

        if (Boolean.TRUE.equals(isBackendOnline)) {
            // Highest-priority reachable server connected
            if (sessionManager.isLoggedIn()) {
                if (sessionManager.isBiometricEnabled()) {
                    intent = new Intent(this, AppLockActivity.class);
                } else {
                    intent = new Intent(this, MainActivity.class);
                }
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
        } else {
            // No saved servers reachable (or none configured) -> Route to ConnectionActivity
            intent = new Intent(this, ConnectionActivity.class);
            intent.putExtra("EXTRA_AUTO_OPENED", true);
            intent.putExtra("EXTRA_FAILED_URL", ApiClient.getBaseUrl(this));
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
