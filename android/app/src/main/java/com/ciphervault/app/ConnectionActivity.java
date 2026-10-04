package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConnectionActivity extends BaseActivity {

    private EditText serverAddressInput;
    private Button testConnectionButton;
    private Button btnContinue;

    private LinearProgressIndicator progressConnection;
    private TextView tvStatusTitle;
    private TextView tvStatusServer;
    private TextView tvStatusMethod;
    private TextView tvStatusMessage;

    private interface HealthCheckCallback {
        void onResult(boolean success, String errorDetails);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        serverAddressInput = findViewById(R.id.etServerAddress);
        testConnectionButton = findViewById(R.id.btnTestConnection);
        btnContinue = findViewById(R.id.btnContinue);

        progressConnection = findViewById(R.id.progressConnection);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusServer = findViewById(R.id.tvStatusServer);
        tvStatusMethod = findViewById(R.id.tvStatusMethod);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);

        Chip chipAdbReverse = findViewById(R.id.chipAdbReverse);
        Chip chipEmulator = findViewById(R.id.chipEmulator);

        if (chipAdbReverse != null) {
            chipAdbReverse.setOnClickListener(v -> serverAddressInput.setText("127.0.0.1:8080"));
        }
        if (chipEmulator != null) {
            chipEmulator.setOnClickListener(v -> serverAddressInput.setText("10.0.2.2:8080"));
        }

        String currentUrl = ApiClient.getBaseUrl(this)
                .replace("http://", "")
                .replace("https://", "");
        if (currentUrl.endsWith("/")) {
            currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
        }
        serverAddressInput.setText(currentUrl);

        testConnectionButton.setOnClickListener(v -> performConnectionCheck());

        if (btnContinue != null) {
            btnContinue.setEnabled(false);
            btnContinue.setOnClickListener(v -> {
                applyAddress();
                startActivity(new Intent(ConnectionActivity.this, LoginActivity.class));
                finish();
            });
        }

        // Auto-check current configured address on activity start
        performConnectionCheck();
    }

    private void applyAddress() {
        String input = serverAddressInput.getText().toString().trim();
        if (!input.isEmpty()) {
            ApiClient.setBaseUrl(this, input);
        }
    }

    private String normalizeUrl(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "http://127.0.0.1:8080/";
        }
        String formatted = input.trim();
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            if (formatted.startsWith("127.0.0.1") || formatted.startsWith("localhost") || 
                formatted.startsWith("10.0.") || formatted.startsWith("192.168.")) {
                formatted = "http://" + formatted;
            } else {
                formatted = "https://" + formatted;
            }
        }
        if (!formatted.endsWith("/")) {
            formatted = formatted + "/";
        }
        return formatted;
    }

    private String determineConnectionMethod(String url) {
        if (url.startsWith("https://")) {
            return "Secure Internet Tunnel (HTTPS)";
        } else if (url.contains("127.0.0.1")) {
            return "USB / ADB Reverse";
        } else if (url.contains("10.0.2.2")) {
            return "Android Emulator";
        } else {
            return "Manual Gateway";
        }
    }

    private void performConnectionCheck() {
        String rawInput = serverAddressInput.getText().toString().trim();
        String primaryUrl = normalizeUrl(rawInput);

        showCheckingState();

        checkHealthAtUrl(primaryUrl, (success, errorDetails) -> {
            if (success) {
                showSuccessState(primaryUrl);
            } else {
                if (rawInput.isEmpty() || rawInput.equalsIgnoreCase("127.0.0.1:8080")) {
                    String secondaryUrl = "http://10.0.2.2:8080/";
                    checkHealthAtUrl(secondaryUrl, (secondarySuccess, secondaryError) -> {
                        if (secondarySuccess) {
                            showSuccessState(secondaryUrl);
                        } else {
                            showFailedState(primaryUrl, errorDetails);
                        }
                    });
                } else {
                    showFailedState(primaryUrl, errorDetails);
                }
            }
        });
    }

    private void checkHealthAtUrl(String targetUrl, HealthCheckCallback callback) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .writeTimeout(5, TimeUnit.SECONDS)
                    .build();

            Retrofit testRetrofit = new Retrofit.Builder()
                    .baseUrl(targetUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService testService = testRetrofit.create(ApiService.class);
            testService.checkHealth().enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onResult(true, null);
                    } else {
                        callback.onResult(false, "HTTP " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    callback.onResult(false, t.getLocalizedMessage());
                }
            });
        } catch (Exception e) {
            callback.onResult(false, e.getLocalizedMessage());
        }
    }

    private void showCheckingState() {
        if (progressConnection != null) progressConnection.setVisibility(View.VISIBLE);
        testConnectionButton.setEnabled(false);
        if (btnContinue != null) btnContinue.setEnabled(false);

        tvStatusTitle.setText("Checking connection...");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.vault_unencrypted));
        tvStatusServer.setVisibility(View.GONE);
        tvStatusMethod.setVisibility(View.GONE);
        tvStatusMessage.setText("Pinging backend instance health endpoint...");
    }

    private void showSuccessState(String successfulUrl) {
        ApiClient.setBaseUrl(this, successfulUrl);

        if (progressConnection != null) progressConnection.setVisibility(View.GONE);
        testConnectionButton.setEnabled(true);
        if (btnContinue != null) btnContinue.setEnabled(true);

        tvStatusTitle.setText("✓ Backend Connected");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_connected));

        tvStatusServer.setText("Server: " + successfulUrl);
        tvStatusServer.setVisibility(View.VISIBLE);

        tvStatusMethod.setText("Connection: " + determineConnectionMethod(successfulUrl));
        tvStatusMethod.setVisibility(View.VISIBLE);

        tvStatusMessage.setText("Connection verified successfully. Press Continue to proceed.");
    }

    private void showFailedState(String failedUrl, String errorDetails) {
        if (progressConnection != null) progressConnection.setVisibility(View.GONE);
        testConnectionButton.setEnabled(true);
        if (btnContinue != null) btnContinue.setEnabled(false);

        tvStatusTitle.setText("✕ Backend Not Connected");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_error));

        tvStatusServer.setText("Server: " + failedUrl);
        tvStatusServer.setVisibility(View.VISIBLE);

        tvStatusMethod.setVisibility(View.GONE);

        String msg = "Unable to reach the CipherVault backend.";
        if (errorDetails != null && !errorDetails.trim().isEmpty()) {
            msg += " (" + errorDetails + ")";
        }
        tvStatusMessage.setText(msg);
    }
}