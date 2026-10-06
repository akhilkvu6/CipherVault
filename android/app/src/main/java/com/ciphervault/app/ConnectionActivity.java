package com.ciphervault.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ConnectionActivity extends BaseActivity {

    private static final String METHOD_ADB =
            "USB — ADB Reverse";

    private static final String METHOD_EMULATOR =
            "Android Emulator";

    private static final String METHOD_HOTSPOT =
            "Phone Hotspot";

    private static final String METHOD_LAN =
            "Local Wi-Fi / LAN";

    private static final String METHOD_CUSTOM =
            "Custom Server";

    private EditText serverAddressInput;
    private Button testConnectionButton;

    private MaterialAutoCompleteTextView connectionMethodDropdown;

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

        WindowCompat.setDecorFitsSystemWindows(
                getWindow(),
                false
        );

        setContentView(R.layout.activity_connection);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        serverAddressInput =
                findViewById(R.id.etServerAddress);

        testConnectionButton =
                findViewById(R.id.btnTestConnection);

        connectionMethodDropdown =
                findViewById(R.id.connectionMethodDropdown);

        progressConnection =
                findViewById(R.id.progressConnection);

        tvStatusTitle =
                findViewById(R.id.tvStatusTitle);

        tvStatusServer =
                findViewById(R.id.tvStatusServer);

        tvStatusMethod =
                findViewById(R.id.tvStatusMethod);

        tvStatusMessage =
                findViewById(R.id.tvStatusMessage);

        setupConnectionMethodDropdown();
        loadCurrentServer();

        testConnectionButton.setOnClickListener(
                v -> performConnectionCheck()
        );

        // Automatically check the currently configured server.
        performConnectionCheck();
    }

    private void setupConnectionMethodDropdown() {

        if (connectionMethodDropdown == null) {
            return;
        }

        String[] methods = {
                METHOD_ADB,
                METHOD_EMULATOR,
                METHOD_HOTSPOT,
                METHOD_LAN,
                METHOD_CUSTOM
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        methods
                );

        connectionMethodDropdown.setAdapter(adapter);

        connectionMethodDropdown.setOnItemClickListener(
                (parent, view, position, id) -> {

                    String selectedMethod =
                            parent.getItemAtPosition(position)
                                    .toString();

                    applyConnectionMethod(
                            selectedMethod
                    );
                }
        );
    }

    private void loadCurrentServer() {

        String baseUrl =
                ApiClient.getBaseUrl(this);

        String currentUrl =
                baseUrl
                        .replace("http://", "")
                        .replace("https://", "");

        while (currentUrl.endsWith("/")) {
            currentUrl =
                    currentUrl.substring(
                            0,
                            currentUrl.length() - 1
                    );
        }

        if (currentUrl.isEmpty()) {
            currentUrl = "127.0.0.1:8080";
        }

        serverAddressInput.setText(currentUrl);

        String detectedMethod =
                detectConnectionMethod(currentUrl);

        if (connectionMethodDropdown != null) {

            connectionMethodDropdown.setText(
                    detectedMethod,
                    false
            );
        }

        applyConnectionMethodState(
                detectedMethod
        );
    }

    private void applyConnectionMethod(
            String selectedMethod) {

        if (selectedMethod == null) {
            return;
        }

        if (METHOD_ADB.equals(selectedMethod)) {

            serverAddressInput.setText(
                    "127.0.0.1:8080"
            );

            ApiClient.setBaseUrl(
                    this,
                    "http://127.0.0.1:8080/"
            );

        } else if (
                METHOD_EMULATOR.equals(selectedMethod)) {

            serverAddressInput.setText(
                    "10.0.2.2:8080"
            );

            ApiClient.setBaseUrl(
                    this,
                    "http://10.0.2.2:8080/"
            );
        }

        applyConnectionMethodState(
                selectedMethod
        );
    }

    private void applyConnectionMethodState(
            String selectedMethod) {

        if (serverAddressInput == null) {
            return;
        }

        boolean fixedAddress =
                METHOD_ADB.equals(selectedMethod)
                        || METHOD_EMULATOR.equals(selectedMethod);

        /*
         * ADB and Emulator have known addresses.
         * Hotspot, LAN and Custom Server require
         * the user to provide the actual address.
         */
        serverAddressInput.setEnabled(
                !fixedAddress
        );
    }

    private String detectConnectionMethod(
            String url) {

        if (url == null) {
            return METHOD_CUSTOM;
        }

        String normalized =
                url.toLowerCase();

        if (normalized.contains("127.0.0.1")) {
            return METHOD_ADB;
        }

        if (normalized.contains("10.0.2.2")) {
            return METHOD_EMULATOR;
        }

        if (normalized.contains("192.168.43.")) {
            return METHOD_HOTSPOT;
        }

        if (normalized.contains("192.168.")
                || normalized.contains("10.")
                || normalized.contains("172.")) {

            return METHOD_LAN;
        }

        return METHOD_CUSTOM;
    }

    private String normalizeUrl(String input) {

        return ApiClient.sanitizeAndValidateUrl(
                input
        );
    }

    private void performConnectionCheck() {

        String rawInput =
                serverAddressInput
                        .getText()
                        .toString()
                        .trim();

        if (rawInput.isEmpty()) {

            serverAddressInput.setError(
                    "Enter a server address"
            );

            return;
        }

        String primaryUrl;

        try {

            primaryUrl =
                    normalizeUrl(rawInput);

        } catch (Exception e) {

            serverAddressInput.setError(
                    "Invalid server address"
            );

            return;
        }

        if (primaryUrl == null
                || primaryUrl.trim().isEmpty()) {

            serverAddressInput.setError(
                    "Invalid server address"
            );

            return;
        }

        serverAddressInput.setError(null);

        showCheckingState();

        checkHealthAtUrl(
                primaryUrl,
                (success, errorDetails) -> {

                    runOnUiThread(() -> {

                        if (isFinishing()
                                || isDestroyed()) {
                            return;
                        }

                        if (success) {

                            showSuccessState(
                                    primaryUrl
                            );

                        } else {

                            showFailedState(
                                    primaryUrl,
                                    errorDetails
                            );
                        }
                    });
                }
        );
    }

    private void checkHealthAtUrl(
            String targetUrl,
            HealthCheckCallback callback) {

        try {

            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .connectTimeout(
                                    5,
                                    TimeUnit.SECONDS
                            )
                            .readTimeout(
                                    5,
                                    TimeUnit.SECONDS
                            )
                            .writeTimeout(
                                    5,
                                    TimeUnit.SECONDS
                            )
                            .build();

            Retrofit testRetrofit =
                    new Retrofit.Builder()
                            .baseUrl(targetUrl)
                            .client(client)
                            .addConverterFactory(
                                    GsonConverterFactory.create()
                            )
                            .build();

            ApiService testService =
                    testRetrofit.create(
                            ApiService.class
                    );

            testService.checkHealth().enqueue(
                    new Callback<Map<String, Object>>() {

                        @Override
                        public void onResponse(
                                @NonNull Call<Map<String, Object>> call,
                                @NonNull Response<Map<String, Object>> response) {

                            if (response.isSuccessful()
                                    && response.body() != null) {

                                callback.onResult(
                                        true,
                                        null
                                );

                            } else {

                                callback.onResult(
                                        false,
                                        "HTTP "
                                                + response.code()
                                );
                            }
                        }

                        @Override
                        public void onFailure(
                                @NonNull Call<Map<String, Object>> call,
                                @NonNull Throwable t) {

                            String message =
                                    t.getLocalizedMessage();

                            if (message == null
                                    || message.trim().isEmpty()) {

                                message =
                                        "Connection failed";
                            }

                            callback.onResult(
                                    false,
                                    message
                            );
                        }
                    }
            );

        } catch (Exception e) {

            String message =
                    e.getLocalizedMessage();

            if (message == null
                    || message.trim().isEmpty()) {

                message = "Invalid server address";
            }

            callback.onResult(
                    false,
                    message
            );
        }
    }

    private void showCheckingState() {

        if (progressConnection != null) {
            progressConnection.setVisibility(
                    View.VISIBLE
            );
        }

        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(
                    false
            );
        }

        if (tvStatusTitle != null) {

            tvStatusTitle.setText(
                    "Checking connection..."
            );

            tvStatusTitle.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.vault_unencrypted
                    )
            );
        }

        if (tvStatusServer != null) {
            tvStatusServer.setVisibility(
                    View.GONE
            );
        }

        if (tvStatusMethod != null) {
            tvStatusMethod.setVisibility(
                    View.GONE
            );
        }

        if (tvStatusMessage != null) {

            tvStatusMessage.setText(
                    "Pinging backend instance health endpoint..."
            );
        }
    }

    private void showSuccessState(
            String successfulUrl) {

        ApiClient.setBaseUrl(
                this,
                successfulUrl
        );

        if (progressConnection != null) {
            progressConnection.setVisibility(
                    View.GONE
            );
        }

        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(
                    true
            );
        }

        if (tvStatusTitle != null) {

            tvStatusTitle.setText(
                    R.string.status_connected
            );

            tvStatusTitle.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.status_connected
                    )
            );
        }

        if (tvStatusServer != null) {

            tvStatusServer.setText(
                    "Server: "
                            + successfulUrl
            );

            tvStatusServer.setVisibility(
                    View.VISIBLE
            );
        }

        if (tvStatusMethod != null) {

            tvStatusMethod.setText(
                    "Connection: "
                            + determineConnectionMethod(
                            successfulUrl
                    )
            );

            tvStatusMethod.setVisibility(
                    View.VISIBLE
            );
        }

        if (tvStatusMessage != null) {

            tvStatusMessage.setText(
                    "Connection verified successfully."
            );
        }
    }

    private void showFailedState(
            String failedUrl,
            String errorDetails) {

        if (progressConnection != null) {
            progressConnection.setVisibility(
                    View.GONE
            );
        }

        if (testConnectionButton != null) {
            testConnectionButton.setEnabled(
                    true
            );
        }

        if (tvStatusTitle != null) {

            tvStatusTitle.setText(
                    "Backend Not Connected"
            );

            tvStatusTitle.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.status_error
                    )
            );
        }

        if (tvStatusServer != null) {

            tvStatusServer.setText(
                    "Server: "
                            + failedUrl
            );

            tvStatusServer.setVisibility(
                    View.VISIBLE
            );
        }

        if (tvStatusMethod != null) {
            tvStatusMethod.setVisibility(
                    View.GONE
            );
        }

        String message =
                "Unable to reach the CipherVault backend.";

        if (errorDetails != null
                && !errorDetails.trim().isEmpty()) {

            message +=
                    " ("
                            + errorDetails
                            + ")";
        }

        if (tvStatusMessage != null) {
            tvStatusMessage.setText(
                    message
            );
        }
    }

    private String determineConnectionMethod(
            String url) {

        if (url == null) {
            return "Custom Server";
        }

        String normalized =
                url.toLowerCase();

        if (normalized.contains("127.0.0.1")) {
            return "USB Cable (ADB Reverse)";
        }

        if (normalized.contains("10.0.2.2")) {
            return "Android Emulator";
        }

        if (normalized.contains("192.168.43.")) {
            return "Phone Hotspot";
        }

        if (normalized.contains("192.168.")
                || normalized.contains("10.")
                || normalized.contains("172.")) {

            return "Local Wi-Fi / LAN";
        }

        return "Custom Server";
    }
}