package com.ciphervault.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ciphervault.app.onboarding.OnboardingActivity;
import com.ciphervault.app.onboarding.OnboardingPreferences;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class MainActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 3000;
    private static final int FADE_DURATION_MS = 500;

    private LinearLayout splashContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Instantly dismiss native splash so our custom layout takes over
        splashScreen.setKeepOnScreenCondition(() -> false);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        splashContainer = findViewById(R.id.splashContainer);
        startBrandedSplashExperience();
    }

    private void startBrandedSplashExperience() {
        LinearProgressIndicator pbSplashProgress = findViewById(R.id.pbSplashProgress);

        // Initial state
        splashContainer.setAlpha(0f);
        splashContainer.setScaleX(0.94f);
        splashContainer.setScaleY(0.94f);

        // 1. Fade & Scale Logo
        splashContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(FADE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 2. Progress Bar Animation
        ValueAnimator progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(SPLASH_DURATION_MS);
        progressAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            pbSplashProgress.setProgress(progress);
        });
        progressAnimator.start();

        // 3. Routing after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(this::routeToNextDestination, SPLASH_DURATION_MS);
    }

    private void routeToNextDestination() {
        if (isFinishing()) return;

        OnboardingPreferences preferences = new OnboardingPreferences(this);
        boolean onboardingCompleted = preferences.isOnboardingCompleted();

        if (!onboardingCompleted) {
            // Transition to Onboarding
            Intent intent = new Intent(this, OnboardingActivity.class);
            startActivity(intent);
            applyTransition();
            finish();
        } else {
            com.ciphervault.app.core.preferences.ConnectionPreferences prefs = new com.ciphervault.app.core.preferences.ConnectionPreferences(this);
            if (prefs.getServerUrl() == null) {
                routeToActivity(com.ciphervault.app.auth.ui.ConnectActivity.class);
            } else {
                com.ciphervault.app.auth.data.AuthRepository repo = new com.ciphervault.app.auth.data.AuthRepository(this);
                repo.checkHealth(new com.ciphervault.app.auth.data.AuthRepository.RepoCallback<com.ciphervault.app.auth.model.HealthResponse>() {
                    @Override
                    public void onSuccess(com.ciphervault.app.auth.model.HealthResponse result) {
                        if ("UP".equals(result.getStatus())) {
                            routeBasedOnSession();
                        } else {
                            routeToActivity(com.ciphervault.app.auth.ui.ConnectActivity.class);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        routeToActivity(com.ciphervault.app.auth.ui.ConnectActivity.class);
                    }
                });
            }
        }
    }

    private void routeBasedOnSession() {
        com.ciphervault.app.core.session.AuthSessionManager sessionManager = new com.ciphervault.app.core.session.AuthSessionManager(this);
        if (sessionManager.hasValidSession()) {
            routeToActivity(com.ciphervault.app.main.ui.MainAppActivity.class);
        } else {
            routeToActivity(com.ciphervault.app.auth.ui.SignInActivity.class);
        }
    }

    private void routeToActivity(Class<?> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        applyTransition();
        finish();
    }

    private void applyTransition() {
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}