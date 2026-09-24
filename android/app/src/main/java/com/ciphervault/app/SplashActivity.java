package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DURATION = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View loadingLine = findViewById(R.id.loadingLine);
        if (loadingLine != null) {
            loadingLine.setScaleX(0f);
            loadingLine.animate()
                    .scaleX(1f)
                    .setDuration(1100L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            loadingLine.postDelayed(this::routeNextScreen, SPLASH_DURATION);
        } else {
            routeNextScreen();
        }
    }

    private void routeNextScreen() {
        SessionManager sessionManager = new SessionManager(this);

        Intent intent;
        if (sessionManager.isLoggedIn()) {
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else if (sessionManager.isOnboardingCompleted()) {
            intent = new Intent(SplashActivity.this, ConnectionActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}