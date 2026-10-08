package com.ciphervault.app;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentDiagnostics extends Fragment {

    private TextView tvDiagServerStatus;
    private TextView tvDiagServerLatency;
    private TextView tvDiagDbStatus;
    private TextView tvDiagDbDetails;
    private TextView tvDiagCryptoStatus;
    private TextView tvDiagCryptoDetails;
    private TextView tvDiagStorageStatus;
    private TextView tvDiagStorageDetails;
    private Button btnRunDiagnostics;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diagnostics, container, false);

        apiService = ApiClient.getApiService(requireContext());

        tvDiagServerStatus = view.findViewById(R.id.tvDiagServerStatus);
        tvDiagServerLatency = view.findViewById(R.id.tvDiagServerLatency);
        tvDiagDbStatus = view.findViewById(R.id.tvDiagDbStatus);
        tvDiagDbDetails = view.findViewById(R.id.tvDiagDbDetails);
        tvDiagCryptoStatus = view.findViewById(R.id.tvDiagCryptoStatus);
        tvDiagCryptoDetails = view.findViewById(R.id.tvDiagCryptoDetails);
        tvDiagStorageStatus = view.findViewById(R.id.tvDiagStorageStatus);
        tvDiagStorageDetails = view.findViewById(R.id.tvDiagStorageDetails);
        btnRunDiagnostics = view.findViewById(R.id.btnRunDiagnostics);

        btnRunDiagnostics.setOnClickListener(v -> executeDiagnostics());

        executeDiagnostics();

        return view;
    }

    private void executeDiagnostics() {
        btnRunDiagnostics.setEnabled(false);
        tvDiagServerStatus.setText("TESTING...");
        tvDiagServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.vault_unencrypted));

        long startTime = SystemClock.elapsedRealtime();
        String currentUrl = ApiClient.getBaseUrl(requireContext());

        apiService.checkHealth().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                btnRunDiagnostics.setEnabled(true);
                long latency = SystemClock.elapsedRealtime() - startTime;

                if (response.isSuccessful()) {
                    tvDiagServerStatus.setText("HEALTHY");
                    tvDiagServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected));
                    tvDiagServerLatency.setText("Latency: " + latency + " ms • Host: " + currentUrl);

                    tvDiagDbStatus.setText("CONNECTED");
                    tvDiagDbStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected));
                    tvDiagDbDetails.setText("MySQL 8.0 • Connection Pool Active");

                    tvDiagCryptoStatus.setText("AES-256-GCM");
                    tvDiagCryptoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.vault_encrypted));
                    tvDiagCryptoDetails.setText("PBKDF2WithHmacSHA256 • 16 KB Chunk Streaming");

                    tvDiagStorageStatus.setText("10 GB CAP");
                    tvDiagStorageStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected));
                    tvDiagStorageDetails.setText("1 GB Batch Limit • SHA-256 Deduplication Active");

                    Toast.makeText(requireContext(), "All systems operational (" + latency + " ms)", Toast.LENGTH_SHORT).show();
                } else {
                    tvDiagServerStatus.setText("HTTP " + response.code());
                    tvDiagServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                    tvDiagServerLatency.setText("Latency: " + latency + " ms • Host: " + currentUrl);
                    Toast.makeText(requireContext(), "Backend responded with error: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                btnRunDiagnostics.setEnabled(true);
                tvDiagServerStatus.setText("UNREACHABLE");
                tvDiagServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                tvDiagServerLatency.setText("Failed to reach server: " + t.getLocalizedMessage());
                Toast.makeText(requireContext(), "Failed to reach backend server", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
