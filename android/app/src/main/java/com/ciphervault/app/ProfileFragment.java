package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvProfileUsername;
    private TextView tvProfileUsedStorage;
    private TextView tvProfileStoragePercent;
    private TextView tvProfileServerUrl;
    private TextView tvProfileHealthStatus;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        tvProfileUsername = view.findViewById(R.id.tvProfileUsername);
        tvProfileUsedStorage = view.findViewById(R.id.tvProfileUsedStorage);
        tvProfileStoragePercent = view.findViewById(R.id.tvProfileStoragePercent);
        tvProfileServerUrl = view.findViewById(R.id.tvProfileServerUrl);
        tvProfileHealthStatus = view.findViewById(R.id.tvProfileHealthStatus);
        Button btnTestConnection = view.findViewById(R.id.btnTestConnection);
        Button btnSignOut = view.findViewById(R.id.btnSignOut);

        if (tvProfileUsername != null) {
            String username = sessionManager.getUsername();
            tvProfileUsername.setText(username != null && !username.trim().isEmpty() ? username : "Authenticated User");
        }

        if (tvProfileServerUrl != null) {
            String baseUrl = ApiClient.getBaseUrl(requireContext());
            tvProfileServerUrl.setText("Host: " + baseUrl);
        }

        if (btnTestConnection != null) {
            btnTestConnection.setOnClickListener(v -> testConnection());
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> signOut());
        }

        loadStorageDetails();
        return view;
    }

    private void loadStorageDetails() {
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    long totalUsed = 0L;
                    for (StoredFile file : response.body()) {
                        totalUsed += file.getFileSize();
                    }

                    if (tvProfileUsedStorage != null) {
                        tvProfileUsedStorage.setText(Formatter.formatFileSize(requireContext(), totalUsed));
                    }

                    if (tvProfileStoragePercent != null) {
                        double percentage = (totalUsed * 100.0) / SessionManager.DEFAULT_LIMIT;
                        tvProfileStoragePercent.setText(String.format(Locale.US, "%.2f%% Used", percentage));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                // Ignore silent profile load failure
            }
        });
    }

    private void testConnection() {
        if (tvProfileHealthStatus != null) {
            tvProfileHealthStatus.setText("Health: Checking...");
        }

        apiService.checkHealth().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    if (tvProfileHealthStatus != null) {
                        tvProfileHealthStatus.setText("Health: Endpoint Active (HTTP 200)");
                    }
                    Toast.makeText(requireContext(), "Backend connection active!", Toast.LENGTH_SHORT).show();
                } else {
                    if (tvProfileHealthStatus != null) {
                        tvProfileHealthStatus.setText("Health: Error (HTTP " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (tvProfileHealthStatus != null) {
                    tvProfileHealthStatus.setText("Health: Offline (" + t.getLocalizedMessage() + ")");
                }
            }
        });
    }

    private void signOut() {
        sessionManager.logout();
        Intent intent = new Intent(requireActivity(), ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}