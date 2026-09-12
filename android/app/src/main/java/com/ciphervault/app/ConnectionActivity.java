package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectionActivity extends AppCompatActivity {

    private EditText serverAddressInput;
    private Button testConnectionButton;
    private Button signInButton;
    private Button signUpButton;
    private TextView statusDot;
    private TextView statusText;

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
        signInButton = findViewById(R.id.btnSignIn);
        signUpButton = findViewById(R.id.btnSignUp);
        statusDot = findViewById(R.id.statusDot);
        statusText = findViewById(R.id.tvStatus);

        String currentUrl = ApiClient.getBaseUrl(this)
                .replace("http://", "")
                .replace("https://", "");
        if (currentUrl.endsWith("/")) {
            currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
        }
        serverAddressInput.setText(currentUrl);

        testConnectionButton.setOnClickListener(v -> {
            applyAddress();
            checkConnection();
        });

        signInButton.setOnClickListener(v -> {
            applyAddress();
            startActivity(new Intent(ConnectionActivity.this, LoginActivity.class));
        });

        signUpButton.setOnClickListener(v -> {
            applyAddress();
            startActivity(new Intent(ConnectionActivity.this, SignUpActivity.class));
        });
    }

    private void applyAddress() {
        String input = serverAddressInput.getText().toString().trim();
        if (!input.isEmpty()) {
            ApiClient.setBaseUrl(this, input);
        }
    }

    private void checkConnection() {
        setConnectionState(false, "Checking connection...");
        testConnectionButton.setEnabled(false);

        ApiClient.getApiService(this).checkHealth().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                testConnectionButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    setConnectionState(true, "Backend Connected");
                } else {
                    setConnectionState(false, "Connection failed (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                testConnectionButton.setEnabled(true);
                setConnectionState(false, "Unable to connect: " + t.getLocalizedMessage());
            }
        });
    }

    private void setConnectionState(boolean connected, String message) {
        statusText.setText(message);

        if (connected) {
            statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            signInButton.setEnabled(true);
            signUpButton.setEnabled(true);
        } else {
            statusDot.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            signInButton.setEnabled(false);
            signUpButton.setEnabled(false);
        }
    }
}