package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseActivity {

    private static final String TAG = "CipherVaultLogin";

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        View mainView = findViewById(android.R.id.content);

        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, insets) -> {

                Insets systemBars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());

                view.setPadding(
                        systemBars.left,
                        systemBars.top,
                        systemBars.right,
                        systemBars.bottom
                );

                return insets;
            });
        }

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);

        TextView registerLink = findViewById(R.id.registerLink);

        sessionManager = new SessionManager(this);

        // Sign In
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> login());
        }

        // Server Settings
        View btnServerConfig = findViewById(R.id.btnServerConfig);

        if (btnServerConfig != null) {
            btnServerConfig.setOnClickListener(v -> {

                Intent intent = new Intent(
                        LoginActivity.this,
                        ConnectionActivity.class
                );

                startActivity(intent);
            });
        }

        // Sign Up
        if (registerLink != null) {
            registerLink.setOnClickListener(v -> {

                Intent intent = new Intent(
                        LoginActivity.this,
                        SignupActivity.class
                );

                startActivity(intent);
                finish();
            });
        }

        // Back → Connection Activity
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        Intent intent = new Intent(
                                LoginActivity.this,
                                ConnectionActivity.class
                        );

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                        );

                        startActivity(intent);
                        finish();
                    }
                }
        );
    }

    private void login() {

        if (emailInput == null || passwordInput == null) {
            return;
        }

        String identifier = emailInput
                .getText()
                .toString()
                .trim();

        String password = passwordInput
                .getText()
                .toString();

        // Identifier validation
        if (TextUtils.isEmpty(identifier)) {

            emailInput.setError("Email or username is required");
            emailInput.requestFocus();

            return;
        }

        if (identifier.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {

            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();

            return;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {

            passwordInput.setError("Password is required");
            passwordInput.requestFocus();

            return;
        }

        // Disable button during request
        if (loginButton != null) {

            loginButton.setEnabled(false);
            loginButton.setText("Signing In...");
        }

        LoginRequest request =
                new LoginRequest(identifier, password);

        try {

            ApiClient
                    .getApiService(this)
                    .login(request)
                    .enqueue(new Callback<LoginResponse>() {

                        @Override
                        public void onResponse(
                                Call<LoginResponse> call,
                                Response<LoginResponse> response
                        ) {

                            if (loginButton != null) {

                                loginButton.setEnabled(true);
                                loginButton.setText("Sign In");
                            }

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                LoginResponse loginResponse =
                                        response.body();

                                String token =
                                        loginResponse.getToken();

                                String username =
                                        loginResponse.getUsername();

                                // Validate token
                                if (TextUtils.isEmpty(token)) {

                                    Toast.makeText(
                                            LoginActivity.this,
                                            "Authentication token missing from response",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                String resolvedEmail = loginResponse.getEmail() != null && !loginResponse.getEmail().trim().isEmpty()
                                        ? loginResponse.getEmail().trim()
                                        : (identifier.contains("@") ? identifier : null);

                                // Save session
                                if (sessionManager != null) {
                                    String displayName = loginResponse.getName() != null && !loginResponse.getName().trim().isEmpty()
                                            ? loginResponse.getName().trim() : username;
                                    sessionManager.saveLogin(
                                            token,
                                            username,
                                            resolvedEmail,
                                            displayName
                                    );
                                }

                                if (resolvedEmail != null) {
                                    ProfilePhotoHelper.uploadPendingPhotoIfPresent(LoginActivity.this, resolvedEmail, ApiClient.getApiService(LoginActivity.this));
                                }

                                String auditUser = resolvedEmail != null ? resolvedEmail : (username != null ? username : identifier);
                                AuditLogger.AuditEntry loginEntry = new AuditLogger.AuditEntry(AuditLogger.ACTION_LOGIN_SUCCESS, "SUCCESS", "User authenticated: " + auditUser, null);
                                loginEntry.primaryEntity = auditUser;
                                loginEntry.authMethod = "PASSWORD";
                                AuditLogger.logEvent(LoginActivity.this, loginEntry);

                                String welcomeName = loginResponse.getName() != null && !loginResponse.getName().trim().isEmpty()
                                        ? loginResponse.getName().trim() : username;
                                Toast.makeText(
                                        LoginActivity.this,
                                        "Welcome back, " + welcomeName,
                                        Toast.LENGTH_SHORT
                                ).show();

                                // Open Home/Main screen
                                Intent intent =
                                        new Intent(
                                                LoginActivity.this,
                                                MainActivity.class
                                        );

                                intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                                finish();

                            } else {

                                String message;

                                if (response.code() == 401
                                        || response.code() == 403) {

                                    message =
                                            "Incorrect email or password";

                                } else if (response.code() == 404) {

                                    message =
                                            "Account not found";

                                } else if (response.code() == 429) {

                                    String serverMsg = null;

                                    try {

                                        if (response.errorBody() != null) {

                                            String errStr =
                                                    response
                                                            .errorBody()
                                                            .string();

                                            org.json.JSONObject obj =
                                                    new org.json.JSONObject(
                                                            errStr
                                                    );

                                            if (obj.has("message")) {

                                                serverMsg =
                                                        obj.getString("message");
                                            }
                                        }

                                    } catch (Exception ignored) {
                                    }

                                    if (serverMsg == null
                                            || serverMsg.isEmpty()) {

                                        String retryAfter =
                                                response
                                                        .headers()
                                                        .get("Retry-After");

                                        if (retryAfter != null) {

                                            try {

                                                long seconds =
                                                        Long.parseLong(
                                                                retryAfter
                                                        );

                                                long minutes =
                                                        Math.max(
                                                                1,
                                                                (seconds + 59) / 60
                                                        );

                                                serverMsg =
                                                        "Too many failed login attempts. "
                                                                + "Please wait "
                                                                + minutes
                                                                + " minute(s) "
                                                                + "before trying again.";

                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }

                                    message =
                                            serverMsg != null
                                                    ? serverMsg
                                                    : "Too many attempts. "
                                                      + "Account temporarily locked "
                                                      + "for 15 minutes.";

                                } else if (response.code() >= 500) {

                                    message =
                                            "Server error. Please try again later";

                                } else {

                                    message =
                                            "Unable to sign in ("
                                                    + response.code()
                                                    + ")";
                                }

                                AuditLogger.AuditEntry failEntry = new AuditLogger.AuditEntry(AuditLogger.ACTION_LOGIN_FAILED, "FAILED", message + " (" + identifier + ")", null);
                                failEntry.primaryEntity = identifier;
                                failEntry.authMethod = "PASSWORD";
                                failEntry.errorMessage = message;
                                AuditLogger.logEvent(LoginActivity.this, failEntry);

                                Toast.makeText(
                                        LoginActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                        @Override
                        public void onFailure(
                                Call<LoginResponse> call,
                                Throwable t
                        ) {

                            if (loginButton != null) {

                                loginButton.setEnabled(true);
                                loginButton.setText("Sign In");
                            }

                            Log.e(
                                    TAG,
                                    "Login failure: ",
                                    t
                            );

                            AuditLogger.AuditEntry failEntry = new AuditLogger.AuditEntry(AuditLogger.ACTION_LOGIN_FAILED, "FAILED", "Server unreachable: " + t.getLocalizedMessage(), null);
                            failEntry.primaryEntity = identifier;
                            failEntry.authMethod = "PASSWORD";
                            failEntry.errorMessage = t.getLocalizedMessage();
                            AuditLogger.logEvent(LoginActivity.this, failEntry);

                            com.google.android.material.snackbar.Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Server unreachable: " + t.getLocalizedMessage(),
                                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                            ).setAction("Server IP", v -> {
                                Intent intent = new Intent(LoginActivity.this, ConnectionActivity.class);
                                startActivity(intent);
                            }).show();
                        }
                    });

        } catch (Exception e) {

            if (loginButton != null) {

                loginButton.setEnabled(true);
                loginButton.setText("Sign In");
            }

            com.google.android.material.snackbar.Snackbar.make(
                    findViewById(android.R.id.content),
                    "Network error: " + e.getMessage(),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).setAction("Server IP", v -> {
                Intent intent = new Intent(LoginActivity.this, ConnectionActivity.class);
                startActivity(intent);
            }).show();
        }
    }
}