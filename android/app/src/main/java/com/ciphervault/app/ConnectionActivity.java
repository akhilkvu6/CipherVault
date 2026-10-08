package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Setup, recovery, and configuration activity for CipherVault Server connection.
 * Supports instant QR code scanning via camera, clean manual IPv4 fallback,
 * persistent 5-server LRU candidate selection, and verification against /api/health.
 */
public class ConnectionActivity extends BaseActivity {

    private MaterialButton btnScanQr;
    private TextInputEditText etServerIp;
    private TextInputEditText etServerPort;
    private MaterialButton btnTestConnection;
    private MaterialButton btnSaveConnection;
    private LinearProgressIndicator progressConnection;

    private Chip chipEmulator;

    private MaterialCardView cardStatusDetails;
    private TextView tvStatusTitle;
    private TextView tvStatusServer;
    private TextView tvStatusMessage;

    private MaterialCardView cardSavedServers;
    private TextView tvSavedServersCount;
    private LinearLayout containerSavedServers;

    private boolean isFormattingText = false;
    private String lastVerifiedUrl = null;

    private final ActivityResultLauncher<Intent> qrScanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scanned = result.getData().getStringExtra(QrScanActivity.EXTRA_SCAN_RESULT);
                    handleQrScanResult(scanned);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connection);

        // Cancel any lingering background probe sessions to prevent stale result overwrites
        ServerConnectionManager.getInstance(this).invalidateProbes();

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        loadExistingConnection();
        setupListeners();
        renderSavedServersList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderSavedServersList();
    }

    private void initViews() {
        btnScanQr = findViewById(R.id.btnScanQr);
        etServerIp = findViewById(R.id.etServerIp);
        etServerPort = findViewById(R.id.etServerPort);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSaveConnection = findViewById(R.id.btnSaveConnection);
        progressConnection = findViewById(R.id.progressConnection);

        chipEmulator = findViewById(R.id.chipEmulator);

        cardStatusDetails = findViewById(R.id.cardStatusDetails);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusServer = findViewById(R.id.tvStatusServer);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);

        cardSavedServers = findViewById(R.id.cardSavedServers);
        tvSavedServersCount = findViewById(R.id.tvSavedServersCount);
        containerSavedServers = findViewById(R.id.containerSavedServers);
    }

    private void loadExistingConnection() {
        String currentUrl = ApiClient.getBaseUrl(this);
        if (currentUrl == null || currentUrl.trim().isEmpty()) {
            currentUrl = NetworkPreferences.DEFAULT_BASE_URL;
        }

        try {
            URI uri = URI.create(currentUrl);
            String host = uri.getHost();
            int port = uri.getPort();

            if (host != null && !host.isEmpty()) {
                etServerIp.setText(host);
            } else {
                etServerIp.setText("10.0.2.2");
            }

            if (port > 0) {
                etServerPort.setText(String.valueOf(port));
            } else {
                etServerPort.setText("8080");
            }
        } catch (Exception e) {
            etServerIp.setText("10.0.2.2");
            etServerPort.setText("8080");
        }

        boolean autoOpened = getIntent().getBooleanExtra("EXTRA_AUTO_OPENED", false);
        if (autoOpened) {
            tvStatusTitle.setText("Saved Servers Unavailable");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_error));
            tvStatusServer.setText("Previous URL: " + currentUrl);
            tvStatusMessage.setText("Saved servers are unreachable. Ensure laptop and phone are on the same network and Spring Boot is running. Scan the QR code or enter address below.");
        } else {
            tvStatusServer.setText("Configured: " + currentUrl);
            tvStatusMessage.setText("Scan the QR code or enter laptop's IPv4 address and port to connect.");
        }
    }

    private void renderSavedServersList() {
        if (cardSavedServers == null || containerSavedServers == null) return;
        List<SavedServer> servers = ServerConnectionManager.getInstance(this).getSavedServers();
        if (servers.isEmpty()) {
            cardSavedServers.setVisibility(View.GONE);
            return;
        }

        cardSavedServers.setVisibility(View.VISIBLE);
        if (tvSavedServersCount != null) {
            tvSavedServersCount.setText(servers.size() + " / " + ServerConnectionManager.MAX_SAVED_SERVERS);
        }
        containerSavedServers.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < servers.size(); i++) {
            SavedServer server = servers.get(i);
            View itemView = inflater.inflate(R.layout.item_saved_server, containerSavedServers, false);

            TextView tvRank = itemView.findViewById(R.id.tvServerRank);
            TextView tvAddress = itemView.findViewById(R.id.tvServerAddress);
            TextView tvStatus = itemView.findViewById(R.id.tvServerLruStatus);
            MaterialButton btnConnect = itemView.findViewById(R.id.btnConnectSavedServer);
            ImageButton btnForget = itemView.findViewById(R.id.btnForgetSavedServer);

            tvRank.setText("#" + (i + 1));
            tvAddress.setText(server.getDisplayAddress());

            if (i == 0) {
                tvStatus.setText("Most recently used (#1)");
            } else if (i == servers.size() - 1 && servers.size() > 1) {
                tvStatus.setText("Least recently used (#" + (i + 1) + ")");
            } else {
                tvStatus.setText("Saved candidate (#" + (i + 1) + ")");
            }

            btnConnect.setOnClickListener(v -> {
                etServerIp.setText(server.getHost());
                etServerPort.setText(String.valueOf(server.getPort()));
                Toast.makeText(this, "Testing saved server: " + server.getDisplayAddress(), Toast.LENGTH_SHORT).show();
                testConnection(null);
            });

            btnForget.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Forget Server")
                        .setMessage("Remove " + server.getDisplayAddress() + " from saved connections?")
                        .setPositiveButton("Forget", (dialog, which) -> {
                            ServerConnectionManager.getInstance(this).forgetServer(server.getCanonicalUrl());
                            renderSavedServersList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            containerSavedServers.addView(itemView);
        }
    }

    private void setupListeners() {
        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrScanActivity.class);
            qrScanLauncher.launch(intent);
        });

        btnTestConnection.setOnClickListener(v -> testConnection(null));
        btnSaveConnection.setOnClickListener(v -> saveAndContinue());

        if (chipEmulator != null) {
            chipEmulator.setOnClickListener(v -> {
                etServerIp.setText("10.0.2.2");
                etServerPort.setText("8080");
                testConnection(null);
            });
        }

        // Auto-clean any pasted URLs with http:// or port numbers
        etServerIp.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingText || s == null) return;
                String text = s.toString();
                if (text.contains("http://") || text.contains("https://") || text.contains(":") || text.contains("/")) {
                    parseAndSetCleanInput(text);
                }
            }
        });
    }

    private void handleQrScanResult(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String host = null;
        String port = null;

        try {
            if (raw.startsWith("ciphervault://")) {
                android.net.Uri uri = android.net.Uri.parse(raw);
                host = uri.getQueryParameter("host");
                port = uri.getQueryParameter("port");
            } else if (raw.startsWith("http://") || raw.startsWith("https://")) {
                URI uri = URI.create(raw);
                host = uri.getHost();
                if (uri.getPort() > 0) {
                    port = String.valueOf(uri.getPort());
                }
            } else if (raw.contains(":")) {
                String[] parts = raw.split(":");
                host = parts[0].trim();
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    port = parts[1].trim();
                }
            } else {
                host = raw.trim();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not parse QR: " + raw, Toast.LENGTH_SHORT).show();
            return;
        }

        if (host != null && !host.isEmpty()) {
            isFormattingText = true;
            etServerIp.setText(host);
            if (etServerIp.getText() != null) {
                etServerIp.setSelection(etServerIp.getText().length());
            }
            isFormattingText = false;
        }

        if (port != null && !port.isEmpty()) {
            etServerPort.setText(port);
        } else {
            etServerPort.setText("8080");
        }

        Toast.makeText(this, "QR Code scanned! Testing connection...", Toast.LENGTH_SHORT).show();
        testConnection(null);
    }

    private void parseAndSetCleanInput(String raw) {
        if (raw == null) return;
        String clean = raw.trim()
                .replace("http://", "")
                .replace("https://", "");

        if (clean.contains("/")) {
            clean = clean.substring(0, clean.indexOf("/"));
        }

        String ip = clean;
        String port = null;

        if (clean.contains(":")) {
            String[] parts = clean.split(":");
            ip = parts[0];
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                port = parts[1].trim();
            }
        }

        isFormattingText = true;
        etServerIp.setText(ip);
        if (etServerIp.getText() != null) {
            etServerIp.setSelection(etServerIp.getText().length());
        }
        if (port != null && !port.isEmpty()) {
            etServerPort.setText(port);
        }
        isFormattingText = false;
    }

    /**
     * Builds and validates canonical server URL.
     * Enforces strict IPv4 validation and numeric port boundaries.
     */
    private String buildTargetUrl() {
        String rawIp = etServerIp.getText() != null ? etServerIp.getText().toString().trim() : "";
        String rawPort = etServerPort.getText() != null ? etServerPort.getText().toString().trim() : "";

        if (rawIp.isEmpty()) {
            etServerIp.setError("IPv4 address is required");
            etServerIp.requestFocus();
            return null;
        }

        // Clean any residual prefixes
        rawIp = rawIp.replace("http://", "").replace("https://", "");
        if (rawIp.contains("/")) {
            rawIp = rawIp.substring(0, rawIp.indexOf("/"));
        }
        if (rawIp.contains(":")) {
            String[] parts = rawIp.split(":");
            rawIp = parts[0];
            if (parts.length > 1 && !parts[1].isEmpty()) {
                rawPort = parts[1];
                etServerPort.setText(rawPort);
            }
        }

        // Strict IPv4 validation
        if (!NetworkPreferences.isValidIpv4(rawIp)) {
            etServerIp.setError("Enter a valid IPv4 address (e.g. 192.168.1.100)");
            etServerIp.requestFocus();
            return null;
        }

        if (rawPort.isEmpty()) {
            rawPort = "8080";
            etServerPort.setText("8080");
        }

        try {
            int portNum = Integer.parseInt(rawPort);
            if (!NetworkPreferences.isValidPort(portNum)) {
                etServerPort.setError("Invalid port (1–65535)");
                etServerPort.requestFocus();
                return null;
            }
        } catch (NumberFormatException e) {
            etServerPort.setError("Port must be numeric");
            etServerPort.requestFocus();
            return null;
        }

        return "http://" + rawIp + ":" + rawPort + "/";
    }

    private interface OnTestCallback {
        void onResult(boolean success, String url, long latencyMs);
    }

    private void testConnection(OnTestCallback callback) {
        String targetUrl = buildTargetUrl();
        if (targetUrl == null) {
            if (callback != null) callback.onResult(false, null, 0);
            return;
        }

        setLoading(true);
        tvStatusTitle.setText("Pinging Backend...");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
        tvStatusServer.setText("Testing: " + targetUrl);
        tvStatusMessage.setText("Sending GET /api/health to verify connectivity");

        long startTime = System.currentTimeMillis();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3500, TimeUnit.MILLISECONDS)
                .readTimeout(3500, TimeUnit.MILLISECONDS)
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
                setLoading(false);
                long latency = System.currentTimeMillis() - startTime;

                if (response.isSuccessful()) {
                    lastVerifiedUrl = targetUrl;
                    tvStatusTitle.setText("Backend Connected (" + latency + " ms)");
                    tvStatusTitle.setTextColor(ContextCompat.getColor(ConnectionActivity.this, R.color.cv_security_success));
                    tvStatusServer.setText("Target: " + targetUrl);
                    tvStatusMessage.setText("Health check returned HTTP 200 OK. Connection is stable.");
                    Toast.makeText(ConnectionActivity.this, "Connection successful (" + latency + " ms)", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onResult(true, targetUrl, latency);
                } else {
                    tvStatusTitle.setText("Connection Error (HTTP " + response.code() + ")");
                    tvStatusTitle.setTextColor(ContextCompat.getColor(ConnectionActivity.this, R.color.status_error));
                    tvStatusMessage.setText("Server reachable but returned error " + response.code() + ". Check backend logs.");
                    if (callback != null) callback.onResult(false, targetUrl, latency);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setLoading(false);
                long latency = System.currentTimeMillis() - startTime;

                tvStatusTitle.setText("Connection Failed");
                tvStatusTitle.setTextColor(ContextCompat.getColor(ConnectionActivity.this, R.color.status_error));
                tvStatusServer.setText("Target: " + targetUrl);
                tvStatusMessage.setText("Could not reach server: " + t.getMessage() + "\n\nTip: Ensure Laptop and Phone are on same network and Spring Boot is running.");
                if (callback != null) callback.onResult(false, targetUrl, latency);
            }
        });
    }

    /**
     * Saves configuration ONLY after verified connectivity.
     * If the current target URL has not been verified yet, auto-tests first.
     * If test fails, preserves the last known valid URL in SharedPreferences.
     */
    private void saveAndContinue() {
        String targetUrl = buildTargetUrl();
        if (targetUrl == null) {
            return;
        }

        if (targetUrl.equals(lastVerifiedUrl)) {
            persistAndProceed(targetUrl);
        } else {
            // Address not verified yet - test first
            Toast.makeText(this, "Testing connection before saving...", Toast.LENGTH_SHORT).show();
            testConnection((success, url, latency) -> {
                if (success && url != null) {
                    persistAndProceed(url);
                } else {
                    Toast.makeText(ConnectionActivity.this, "Cannot save unverified server. Previous working URL preserved.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void persistAndProceed(@NonNull String verifiedUrl) {
        ApiClient.setBaseUrl(this, verifiedUrl);
        // Record into 5-server LRU manager
        ServerConnectionManager.getInstance(this).recordServerUsed(verifiedUrl);

        Toast.makeText(this, "Connection saved: " + verifiedUrl, Toast.LENGTH_SHORT).show();
        AuditLogger.log(this, "Connect Server", "SUCCESS", "Connected to " + verifiedUrl);

        SessionManager sm = SessionManager.getInstance(this);
        Intent intent;
        if (sm.isLoggedIn()) {
            if (sm.isBiometricEnabled()) {
                intent = new Intent(this, AppLockActivity.class);
            } else {
                intent = new Intent(this, MainActivity.class);
            }
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressConnection.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnScanQr.setEnabled(!loading);
        btnTestConnection.setEnabled(!loading);
        btnSaveConnection.setEnabled(!loading);
    }
}