package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private TextView tvServerInfo;
    private Button btnLogout;
    private Button btnUploadFile;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Apply Edge-to-Edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        // Safety check: If for any reason token is absent, boot back to connection
        if (!sessionManager.isLoggedIn()) {
            performLogout();
            return;
        }

        tvGreeting = findViewById(R.id.tvGreeting);
        tvServerInfo = findViewById(R.id.tvServerInfo);
        btnLogout = findViewById(R.id.btnLogout);
        btnUploadFile = findViewById(R.id.btnUploadFile);

        // Populate dynamic user info
        String username = sessionManager.getUsername();
        if (username != null && !username.isEmpty()) {
            tvGreeting.setText("Logged in as @" + username);
        }

        String currentBaseUrl = ApiClient.getBaseUrl(this);
        tvServerInfo.setText("Host: " + currentBaseUrl);

        btnLogout.setOnClickListener(v -> performLogout());

        btnUploadFile.setOnClickListener(v -> {
            Toast.makeText(this, "Encrypted file picker coming next", Toast.LENGTH_SHORT).show();
        });
    }

    private void performLogout() {
        // 1. Invalidate session storage
        sessionManager.logout();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // 2. Reset backstack and route to Connection screen
        Intent intent = new Intent(MainActivity.this, ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}