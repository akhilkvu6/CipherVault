package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Map;
import java.util.concurrent.Executor;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * AppLockActivity secures the vault when app opens or returns from lock.
 *
 * Implements strict four-state model:
 * LOCKED -> UNLOCK_REQUESTED -> AUTHENTICATING -> UNLOCKED
 *
 * Authentication NEVER automatically triggers on launch, restart, recreate, or resume.
 * User must explicitly press "Unlock Vault".
 * Failed authentication remains LOCKED without clearing session, JWT, or saved servers.
 */
public class AppLockActivity extends BaseActivity {

    public enum LockState {
        LOCKED,
        UNLOCK_REQUESTED,
        AUTHENTICATING,
        UNLOCKED
    }

    private SessionManager sessionManager;
    private ApiService apiService;

    private ImageView ivLockIcon;
    private TextView tvLockTitle;
    private TextView tvLockSubtitle;
    private MaterialButton btnUnlockVault;
    private Button btnSignOut;

    private LockState currentState = LockState.LOCKED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_lock);

        sessionManager = SessionManager.getInstance(this);
        if (!sessionManager.isBiometricEnabled()) {
            unlockAndProceed();
            return;
        }
        apiService = ApiClient.getApiService(this);

        ivLockIcon = findViewById(R.id.ivLockIcon);
        tvLockTitle = findViewById(R.id.tvLockTitle);
        tvLockSubtitle = findViewById(R.id.tvLockSubtitle);
        TextView tvUserDisplayName = findViewById(R.id.tvUserDisplayName);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        btnUnlockVault = findViewById(R.id.btnUnlockVault);
        btnSignOut = findViewById(R.id.btnSignOut);

        if (tvUserDisplayName != null) {
            String name = sessionManager.getName();
            if (name == null || name.trim().isEmpty()) {
                name = sessionManager.getUsername();
            }
            tvUserDisplayName.setText(name != null ? name : "Vault User");
        }
        if (tvUserEmail != null) {
            String email = sessionManager.getEmail();
            tvUserEmail.setText(email != null ? email : "user@ciphervault.local");
        }

        if (btnUnlockVault != null) {
            btnUnlockVault.setOnClickListener(v -> onUnlockVaultClicked());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> handleSignOut());
        }

        // Intercept back button so back press does not bypass the lock
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });

        // Initialize UI in LOCKED state.
        // DO NOT automatically show biometric prompt! User MUST explicitly tap Unlock Vault.
        updateUiForState(LockState.LOCKED);
    }

    private void onUnlockVaultClicked() {
        if (currentState == LockState.AUTHENTICATING) return;

        updateUiForState(LockState.UNLOCK_REQUESTED);
        updateUiForState(LockState.AUTHENTICATING);
        requestBiometricAuthentication();
    }

    private void updateUiForState(LockState state) {
        this.currentState = state;

        switch (state) {
            case LOCKED:
                if (ivLockIcon != null) {
                    ivLockIcon.setImageResource(R.drawable.ic_lucide_lock);
                }
                if (tvLockTitle != null) {
                    tvLockTitle.setText("CipherVault Locked");
                }
                if (tvLockSubtitle != null) {
                    tvLockSubtitle.setText("Your vault is encrypted with AES-256-GCM.\nTap \"Unlock Vault\" to verify your identity.");
                }
                if (btnUnlockVault != null) {
                    btnUnlockVault.setEnabled(true);
                    btnUnlockVault.setText("Unlock Vault");
                    btnUnlockVault.setIconResource(R.drawable.ic_lucide_fingerprint);
                }
                break;

            case UNLOCK_REQUESTED:
                if (tvLockTitle != null) {
                    tvLockTitle.setText("Requesting Unlock...");
                }
                if (btnUnlockVault != null) {
                    btnUnlockVault.setEnabled(false);
                    btnUnlockVault.setText("Requesting...");
                }
                break;

            case AUTHENTICATING:
                if (tvLockTitle != null) {
                    tvLockTitle.setText("Authenticating...");
                }
                if (tvLockSubtitle != null) {
                    tvLockSubtitle.setText("Touch fingerprint sensor or confirm device credentials.");
                }
                if (btnUnlockVault != null) {
                    btnUnlockVault.setEnabled(false);
                    btnUnlockVault.setText("Authenticating...");
                }
                break;

            case UNLOCKED:
                if (ivLockIcon != null) {
                    ivLockIcon.setImageResource(R.drawable.ic_lucide_unlock);
                }
                if (tvLockTitle != null) {
                    tvLockTitle.setText("Vault Unlocked");
                }
                if (tvLockSubtitle != null) {
                    tvLockSubtitle.setText("Decrypting session keys and loading vault...");
                }
                if (btnUnlockVault != null) {
                    btnUnlockVault.setEnabled(false);
                    btnUnlockVault.setText("Unlocked");
                }
                break;
        }
    }

    private void requestBiometricAuthentication() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // If device credential or biometric is unavailable/unsupported, allow unlocking directly
            AuditLogger.log(this, "Unlock Vault", "SUCCESS", "Unlocked (Device credentials bypassed/unsupported)");
            updateUiForState(LockState.UNLOCKED);
            unlockAndProceed();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                AuditLogger.log(AppLockActivity.this, "Unlock Vault", "SUCCESS", "Biometric/device credential verified");
                updateUiForState(LockState.UNLOCKED);
                unlockAndProceed();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                AuditLogger.log(AppLockActivity.this, "Failed Unlock", "ERROR", "Authentication error: " + errString);
                updateUiForState(LockState.LOCKED);
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(AppLockActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                AuditLogger.log(AppLockActivity.this, "Failed Unlock", "FAILED", "Fingerprint or credential mismatch");
                updateUiForState(LockState.LOCKED);
                Toast.makeText(AppLockActivity.this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock CipherVault")
                .setSubtitle("Confirm your identity to access your encrypted vault")
                .setAllowedAuthenticators(authenticators)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void unlockAndProceed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSignOut() {
        AuditLogger.log(this, "Logout", "SUCCESS", "Explicit user sign out from lock screen");
        try {
            apiService.logout().enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {}

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
            });
        } catch (Exception ignored) {}

        sessionManager.logout();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
