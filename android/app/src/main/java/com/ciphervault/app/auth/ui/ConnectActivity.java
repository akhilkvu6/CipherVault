package com.ciphervault.app.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ciphervault.app.R;
import com.ciphervault.app.auth.api.HealthApi;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.preferences.ConnectionPreferences;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectActivity extends AppCompatActivity {

    private ConnectionPreferences prefs;

    private LinearLayout llRecommended;
    private TextView tvRecTitle;
    private TextView tvRecSubtitle;
    private TextView tvRecUrl;
    private MaterialButton btnRecConnect;

    private TextView tvUsbState;
    private MaterialButton btnTestUsb;

    private TextView tvWifiState;
    private TextView tvWifiUrl;
    private MaterialButton btnTestWifi;

    private TextView tvEmuState;
    private MaterialButton btnTestEmu;

    private TextInputLayout tilManualUrl;
    private TextInputEditText etManualUrl;
    private MaterialButton btnTestManual;

    private MaterialButton btnRefresh;
    private CircularProgressIndicator pbConnect;

    private final String URL_USB = "http://127.0.0.1:8080/";
    private final String URL_EMU = "http://10.0.2.2:8080/";
    
    private String bestUrl = null;
    private String bestType = null;
    
    // States for recommendation priority
    private boolean usbReady = false;
    private boolean wifiReady = false;
    private boolean emuReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_connect);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.connectRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prefs = new ConnectionPreferences(this);

        initViews();
        setupListeners();
        
        String savedUrl = prefs.getServerUrl();
        if (savedUrl != null && !savedUrl.equals(URL_USB) && !savedUrl.equals(URL_EMU)) {
            tvWifiUrl.setText(savedUrl);
            etManualUrl.setText(savedUrl);
        }

        refreshAllConnections();
    }

    private void initViews() {
        llRecommended = findViewById(R.id.llRecommended);
        tvRecTitle = findViewById(R.id.tvRecTitle);
        tvRecSubtitle = findViewById(R.id.tvRecSubtitle);
        tvRecUrl = findViewById(R.id.tvRecUrl);
        btnRecConnect = findViewById(R.id.btnRecConnect);

        tvUsbState = findViewById(R.id.tvUsbState);
        btnTestUsb = findViewById(R.id.btnTestUsb);

        tvWifiState = findViewById(R.id.tvWifiState);
        tvWifiUrl = findViewById(R.id.tvWifiUrl);
        btnTestWifi = findViewById(R.id.btnTestWifi);

        tvEmuState = findViewById(R.id.tvEmuState);
        btnTestEmu = findViewById(R.id.btnTestEmu);

        tilManualUrl = findViewById(R.id.tilManualUrl);
        etManualUrl = findViewById(R.id.etManualUrl);
        btnTestManual = findViewById(R.id.btnTestManual);

        btnRefresh = findViewById(R.id.btnRefresh);
        pbConnect = findViewById(R.id.pbConnect);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        btnRefresh.setOnClickListener(v -> refreshAllConnections());

        btnTestUsb.setOnClickListener(v -> testConnection(URL_USB, "USB", tvUsbState, btnTestUsb));
        btnTestEmu.setOnClickListener(v -> testConnection(URL_EMU, "EMULATOR", tvEmuState, btnTestEmu));
        
        btnTestWifi.setOnClickListener(v -> {
            String url = tvWifiUrl.getText().toString();
            if (!url.equals("—")) {
                testConnection(url, "WIFI", tvWifiState, btnTestWifi);
            }
        });

        btnTestManual.setOnClickListener(v -> {
            tilManualUrl.setError(null);
            String url = String.valueOf(etManualUrl.getText()).trim();
            if (url.isEmpty()) {
                tilManualUrl.setError("Required");
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }
            if (!url.endsWith("/")) {
                url = url + "/";
            }
            etManualUrl.setText(url);
            
            // If it's not usb or emulator, save it as wifi/custom
            if (!url.equals(URL_USB) && !url.equals(URL_EMU)) {
                tvWifiUrl.setText(url);
            }
            testConnection(url, "MANUAL", null, btnTestManual);
        });
        
        btnRecConnect.setOnClickListener(v -> {
            if (bestUrl != null) {
                applyAndProceed(bestUrl);
            }
        });
    }

    private void refreshAllConnections() {
        setLoadingState(true);
        
        usbReady = false;
        wifiReady = false;
        emuReady = false;
        bestUrl = null;
        bestType = null;
        
        updateRecommendationUI();

        AtomicInteger pendingTests = new AtomicInteger(0);
        
        // 1. USB
        pendingTests.incrementAndGet();
        testConnectionSilent(URL_USB, "USB", tvUsbState, pendingTests);
        
        // 2. Emulator
        pendingTests.incrementAndGet();
        testConnectionSilent(URL_EMU, "EMULATOR", tvEmuState, pendingTests);
        
        // 3. Wi-Fi (if configured)
        String wifiUrl = tvWifiUrl.getText().toString();
        if (!wifiUrl.equals("—") && !wifiUrl.isEmpty()) {
            pendingTests.incrementAndGet();
            testConnectionSilent(wifiUrl, "WIFI", tvWifiState, pendingTests);
        } else {
            tvWifiState.setText("Unconfigured");
            tvWifiState.setTextColor(0xFF757575);
        }
    }

    private void testConnectionSilent(String url, String type, TextView stateLabel, AtomicInteger pendingCounter) {
        stateLabel.setText("Checking...");
        stateLabel.setTextColor(0xFF757575); // secondary
        
        HealthApi api = ApiClient.createTempClient(url).create(HealthApi.class);
        api.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                boolean up = response.isSuccessful() && response.body() != null && "UP".equals(response.body().getStatus());
                handleTestResult(type, url, up, stateLabel, pendingCounter);
            }
            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                handleTestResult(type, url, false, stateLabel, pendingCounter);
            }
        });
    }
    
    private void testConnection(String url, String type, TextView stateLabel, MaterialButton triggerBtn) {
        if (triggerBtn != null) triggerBtn.setEnabled(false);
        if (stateLabel != null) {
            stateLabel.setText("Checking...");
            stateLabel.setTextColor(0xFF757575);
        }
        
        HealthApi api = ApiClient.createTempClient(url).create(HealthApi.class);
        api.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                if (triggerBtn != null) triggerBtn.setEnabled(true);
                boolean up = response.isSuccessful() && response.body() != null && "UP".equals(response.body().getStatus());
                if (stateLabel != null) {
                    stateLabel.setText(up ? "● Ready" : "○ Unavailable");
                    stateLabel.setTextColor(up ? 0xFF2E7D32 : 0xFFD32F2F);
                }
                if (up && "MANUAL".equals(type)) {
                    applyAndProceed(url);
                }
                // Update specific flag and recalc if user manually tested
                if (type.equals("USB")) usbReady = up;
                if (type.equals("WIFI")) wifiReady = up;
                if (type.equals("EMULATOR")) emuReady = up;
                calculateBestRecommendation();
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                if (triggerBtn != null) triggerBtn.setEnabled(true);
                if (stateLabel != null) {
                    stateLabel.setText("○ Unavailable");
                    stateLabel.setTextColor(0xFFD32F2F);
                }
                if (type.equals("USB")) usbReady = false;
                if (type.equals("WIFI")) wifiReady = false;
                if (type.equals("EMULATOR")) emuReady = false;
                
                if ("MANUAL".equals(type)) {
                    tilManualUrl.setError("Unable to connect to CipherVault.");
                }
                calculateBestRecommendation();
            }
        });
    }

    private synchronized void handleTestResult(String type, String url, boolean success, TextView stateLabel, AtomicInteger pendingCounter) {
        stateLabel.setText(success ? "● Ready" : "○ Unavailable");
        stateLabel.setTextColor(success ? 0xFF2E7D32 : 0xFFD32F2F);
        
        if (type.equals("USB")) usbReady = success;
        if (type.equals("WIFI")) wifiReady = success;
        if (type.equals("EMULATOR")) emuReady = success;
        
        if (pendingCounter.decrementAndGet() == 0) {
            setLoadingState(false);
            calculateBestRecommendation();
        }
    }

    private void calculateBestRecommendation() {
        if (usbReady) {
            bestUrl = URL_USB;
            bestType = "USB / ADB";
        } else if (wifiReady) {
            bestUrl = tvWifiUrl.getText().toString();
            bestType = "Wi-Fi / LAN";
        } else if (emuReady) {
            bestUrl = URL_EMU;
            bestType = "Android Emulator";
        } else {
            bestUrl = null;
            bestType = null;
        }
        updateRecommendationUI();
    }

    private void updateRecommendationUI() {
        if (bestUrl != null) {
            llRecommended.setVisibility(View.VISIBLE);
            tvRecTitle.setText(bestType);
            tvRecSubtitle.setText("CipherVault server is reachable");
            tvRecUrl.setText(bestUrl);
            btnRecConnect.setText("Connect via " + bestType.split(" ")[0]);
            btnRecConnect.setEnabled(true);
        } else {
            llRecommended.setVisibility(View.VISIBLE);
            tvRecTitle.setText("No connection ready");
            tvRecSubtitle.setText("Configure a server address manually.");
            tvRecUrl.setText("");
            btnRecConnect.setText("No options");
            btnRecConnect.setEnabled(false);
        }
    }
    
    private void applyAndProceed(String url) {
        prefs.setServerUrl(url);
        ApiClient.invalidate();
        startActivity(new Intent(this, SignInActivity.class));
        finishAffinity();
    }

    private void setLoadingState(boolean isLoading) {
        btnRefresh.setEnabled(!isLoading);
        btnTestUsb.setEnabled(!isLoading);
        btnTestWifi.setEnabled(!isLoading);
        btnTestEmu.setEnabled(!isLoading);
        btnTestManual.setEnabled(!isLoading);
        pbConnect.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }
}
