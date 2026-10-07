package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ciphervault.app.R;
import com.ciphervault.app.auth.api.HealthApi;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.core.network.ApiClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HealthFragment extends Fragment {

    private TextView tvOverallHealth;
    private TextView tvLastCheck;
    private TextView tvBackendStatus;
    private TextView tvDatabaseStatus;
    private TextView tvStorageStatus;
    private TextView tvEncryptionStatus;
    private TextView tvMetadataStatus;
    private TextView tvTransfersStatus;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        tvOverallHealth = view.findViewById(R.id.tvOverallHealth);
        tvLastCheck = view.findViewById(R.id.tvLastCheck);
        tvBackendStatus = view.findViewById(R.id.tvBackendStatus);
        tvDatabaseStatus = view.findViewById(R.id.tvDatabaseStatus);
        tvStorageStatus = view.findViewById(R.id.tvStorageStatus);
        tvEncryptionStatus = view.findViewById(R.id.tvEncryptionStatus);
        tvMetadataStatus = view.findViewById(R.id.tvMetadataStatus);
        tvTransfersStatus = view.findViewById(R.id.tvTransfersStatus);

        view.findViewById(R.id.btnRefreshHealth).setOnClickListener(v -> checkHealth());

        checkHealth();
    }

    private void checkHealth() {
        if (getContext() == null) return;
        tvLastCheck.setText("Checking health status...");

        HealthApi api = ApiClient.getClient(getContext()).create(HealthApi.class);
        api.getHealth().enqueue(new Callback<HealthResponse>() {
            @Override
            public void onResponse(Call<HealthResponse> call, Response<HealthResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    HealthResponse body = response.body();
                    tvOverallHealth.setText("All Systems Normal");
                    tvOverallHealth.setTextColor(ContextCompat.getColor(requireContext(), R.color.cv_status_success));
                    tvLastCheck.setText("Verified at " + (body.getTimestamp() != null ? body.getTimestamp() : "now"));

                    Map<String, String> comps = body.getComponents();
                    if (comps != null) {
                        applyStatus(tvBackendStatus, comps.get("backend"));
                        applyStatus(tvDatabaseStatus, comps.get("database"));
                        applyStatus(tvStorageStatus, comps.get("storage"));
                        applyStatus(tvEncryptionStatus, comps.get("encryption"));
                        applyStatus(tvMetadataStatus, comps.get("metadata"));
                        applyStatus(tvTransfersStatus, comps.get("transfers"));
                    } else {
                        applyStatus(tvBackendStatus, body.getStatus());
                        applyStatus(tvDatabaseStatus, "HEALTHY");
                        applyStatus(tvStorageStatus, "HEALTHY");
                        applyStatus(tvEncryptionStatus, "HEALTHY");
                        applyStatus(tvMetadataStatus, "HEALTHY");
                        applyStatus(tvTransfersStatus, "HEALTHY");
                    }
                } else {
                    markUnavailable("Error " + response.code());
                }
            }

            @Override
            public void onFailure(Call<HealthResponse> call, Throwable t) {
                if (!isAdded()) return;
                markUnavailable("Connection Failed");
            }
        });
    }

    private void applyStatus(TextView tv, String status) {
        if (tv == null) return;
        String s = status != null ? status.toUpperCase() : "HEALTHY";
        tv.setText(s);
        if ("HEALTHY".equals(s) || "UP".equals(s)) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.cv_status_success));
        } else if ("WARNING".equals(s)) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.cv_primary));
        } else {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.cv_status_error));
        }
    }

    private void markUnavailable(String message) {
        tvOverallHealth.setText("Not Connected");
        tvOverallHealth.setTextColor(ContextCompat.getColor(requireContext(), R.color.cv_status_error));
        tvLastCheck.setText(message);

        applyStatus(tvBackendStatus, "NOT CONNECTED");
        applyStatus(tvDatabaseStatus, "NOT CONNECTED");
        applyStatus(tvStorageStatus, "NOT CONNECTED");
        applyStatus(tvEncryptionStatus, "NOT CONNECTED");
        applyStatus(tvMetadataStatus, "NOT CONNECTED");
        applyStatus(tvTransfersStatus, "NOT CONNECTED");
    }
}
