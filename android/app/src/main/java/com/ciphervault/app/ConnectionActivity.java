package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
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
 * Supports instant QR code scanning, single combined server address manual input,
 * dynamic test button states, and single most-recently-used server record.
 */
public class ConnectionActivity extends BaseActivity {

    private View btnScanQr;
    private MaterialCardView cardQrScan;
    private TextInputEditText etServerIp;
    private MaterialButton btnTestConnection;
    private MaterialButton btnSaveConnection;
    private LinearProgressIndicator progressConnection;

    private MaterialCardView cardStatusDetails;
    private TextView tvStatusTitle;
    private TextView tvStatusServer;
    private TextView tvStatusMessage;

    private MaterialCardView cardSavedServers;
    private LinearLayout containerSavedServers;

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
        cardQrScan = findViewById(R.id.cardQrScan);
        etServerIp = findViewById(R.id.etServerIp);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnSaveConnection = findViewById(R.id.btnSaveConnection);
        progressConnection = findViewById(R.id.progressConnection);

        cardStatusDetails = findViewById(R.id.cardStatusDetails);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusServer = findViewById(R.id.tvStatusServer);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);

        cardSavedServers = findViewById(R.id.cardSavedServers);
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
                if (port > 0) {
                    etServerIp.setText(host + ":" + port);
                } else {
                    etServerIp.setText(host + ":8080");
                }
            } else {
                etServerIp.setText("10.0.2.2:8080");
            }
        } catch (Exception e) {
            etServerIp.setText("10.0.2.2:8080");
        }

        boolean autoOpened = getIntent().getBooleanExtra("EXTRA_AUTO_OPENED", false);
        if (autoOpened) {
            tvStatusTitle.setText("Saved Servers Unavailable");
            tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_error));
            tvStatusServer.setText("Previous URL: " + currentUrl);
            tvStatusMessage.setText("Saved servers are unreachable. Ensure laptop and phone are on the same network and Spring Boot is running. Scan QR code or enter address below.");
        } else {
            tvStatusServer.setText("Configured: " + currentUrl);
            tvStatusMessage.setText("Scan QR code or enter server address to connect.");
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
        containerSavedServers.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        // Requirement 2.4: Show a single most-recently-used server record, not multiple history items.
        // Do not show a delete button for this item.
        SavedServer server = servers.get(0);
        View itemView = inflater.inflate(R.layout.item_saved_server, containerSavedServers, false);

        TextView tvAddress = itemView.findViewById(R.id.tvServerAddress);
        TextView tvStatus = itemView.findViewById(R.id.tvServerLruStatus);
        MaterialButton btnConnect = itemView.findViewById(R.id.btnConnectSavedServer);

        if (tvAddress != null) tvAddress.setText(server.getDisplayAddress());
        if (tvStatus != null) tvStatus.setText("Recent Server");

        if (btnConnect != null) {
            btnConnect.setText("Select");
            btnConnect.setOnClickListener(v -> {
                etServerIp.setText(server.getDisplayAddress());
                testConnection(null);
            });
        }

        containerSavedServers.addView(itemView);
    }

    private void setupListeners() {
        if (btnScanQr != null) {
            btnScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(this, QrScanActivity.class);
                qrScanLauncher.launch(intent);
            });
        }
        if (cardQrScan != null) {
            cardQrScan.setOnClickListener(v -> {
                Intent intent = new Intent(this, QrScanActivity.class);
                qrScanLauncher.launch(intent);
            });
        }

        btnTestConnection.setOnClickListener(v -> testConnection(null));
        btnSaveConnection.setOnClickListener(v -> saveAndContinue());
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

        final String scannedAddress;
        if (host != null && !host.isEmpty()) {
            if (port != null && !port.isEmpty()) {
                scannedAddress = host + ":" + port;
            } else {
                scannedAddress = host + ":8080";
            }
        } else {
            scannedAddress = raw.trim();
        }

        // Requirement 2.3:
        // 3. The scanned server address is displayed in a popup.
        // 4. The popup contains a Connect action.
        // 5. When the user clicks Connect, the address is inserted into the server-address text field.
        new MaterialAlertDialogBuilder(this)
                .setTitle("Server QR Scanned")
                .setMessage("Scanned Server Address:\n" + scannedAddress)
                .setPositiveButton("Connect", (dialog, which) -> {
                    etServerIp.setText(scannedAddress);
                    if (etServerIp.getText() != null) {
                        etServerIp.setSelection(etServerIp.getText().length());
                    }
                    Toast.makeText(this, "Address inserted. Tap 'Test Server Connection' to verify.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Builds and validates canonical server URL from single server address input.
     */
    private String buildTargetUrl() {
        String raw = etServerIp.getText() != null ? etServerIp.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            etServerIp.setError("Server address is required");
            etServerIp.requestFocus();
            return null;
        }

        // Clean any residual prefixes
        raw = raw.replace("http://", "").replace("https://", "");
        if (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }

        String host = raw;
        int portNum = 8080;

        if (raw.contains(":")) {
            String[] parts = raw.split(":");
            host = parts[0].trim();
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                try {
                    portNum = Integer.parseInt(parts[1].trim());
                    if (!NetworkPreferences.isValidPort(portNum)) {
                        etServerIp.setError("Invalid port (1–65535)");
                        etServerIp.requestFocus();
                        return null;
                    }
                } catch (NumberFormatException e) {
                    etServerIp.setError("Port must be numeric");
                    etServerIp.requestFocus();
                    return null;
                }
            }
        }

        if (host.isEmpty()) {
            etServerIp.setError("Host is required");
            etServerIp.requestFocus();
            return null;
        }

        return "http://" + host + ":" + portNum + "/";
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
        // Requirement 2.3: Test button changes label and icon
        btnTestConnection.setText("Testing Connection...");
        btnTestConnection.setIconResource(R.drawable.ic_lucide_activity);

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
                    btnTestConnection.setText("Connected");
                    btnTestConnection.setIconResource(R.drawable.ic_lucide_check_circle);

                    tvStatusTitle.setText("Backend Connected (" + latency + " ms)");
                    tvStatusTitle.setTextColor(ContextCompat.getColor(ConnectionActivity.this, R.color.cv_security_success));
                    tvStatusServer.setText("Target: " + targetUrl);
                    tvStatusMessage.setText("Connection is stable.");
                    Toast.makeText(ConnectionActivity.this, "Connection successful (" + latency + " ms)", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onResult(true, targetUrl, latency);
                } else {
                    btnTestConnection.setText("Test Failed (Retry)");
                    btnTestConnection.setIconResource(R.drawable.ic_lucide_alert_triangle);

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

                btnTestConnection.setText("Test Failed (Retry)");
                btnTestConnection.setIconResource(R.drawable.ic_lucide_alert_triangle);

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
     */
    private void saveAndContinue() {
        String targetUrl = buildTargetUrl();
        if (targetUrl == null) {
            return;
        }

        if (targetUrl.equals(lastVerifiedUrl)) {
            persistAndProceed(targetUrl);
        } else {
            Toast.makeText(this, "Testing connection before saving...", Toast.LENGTH_SHORT).show();
            testConnection((success, url, latency) -> {
                if (success && url != null) {
                    persistAndProceed(url);
                } else {
                    Toast.makeText(ConnectionActivity.this, "Cannot connect to unverified server. Previous working URL preserved.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void persistAndProceed(@NonNull String verifiedUrl) {
        ApiClient.setBaseUrl(this, verifiedUrl);
        ServerConnectionManager.getInstance(this).recordServerUsed(verifiedUrl);

        Toast.makeText(this, "Connected: " + verifiedUrl, Toast.LENGTH_SHORT).show();
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
        if (progressConnection != null) {
            progressConnection.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnScanQr.setEnabled(!loading);
        btnTestConnection.setEnabled(!loading);
        btnSaveConnection.setEnabled(!loading);
    }
}