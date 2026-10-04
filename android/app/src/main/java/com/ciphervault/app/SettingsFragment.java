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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvSettingsUsername;
    private TextView tvSettingsEmail;
    private TextView tvSettingsInitials;
    private TextView tvSettingsUsedStorage;
    private LinearProgressIndicator progressSettingsQuota;

    private ChipGroup chipGroupMode;
    private Chip chipModeDark;
    private Chip chipModeLight;
    private Chip chipModeSystem;

    private TextView tvSettingsServerUrl;
    private TextView tvSettingsHealthStatus;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        tvSettingsUsername = view.findViewById(R.id.tvSettingsUsername);
        tvSettingsEmail = view.findViewById(R.id.tvSettingsEmail);
        tvSettingsInitials = view.findViewById(R.id.tvSettingsInitials);
        tvSettingsUsedStorage = view.findViewById(R.id.tvSettingsUsedStorage);
        progressSettingsQuota = view.findViewById(R.id.progressSettingsQuota);

        chipGroupMode = view.findViewById(R.id.chipGroupMode);
        chipModeDark = view.findViewById(R.id.chipModeDark);
        chipModeLight = view.findViewById(R.id.chipModeLight);
        chipModeSystem = view.findViewById(R.id.chipModeSystem);

        tvSettingsServerUrl = view.findViewById(R.id.tvSettingsServerUrl);
        tvSettingsHealthStatus = view.findViewById(R.id.tvSettingsHealthStatus);

        Button btnSettingsTestConnection = view.findViewById(R.id.btnSettingsTestConnection);
        Button btnSettingsChangeServer = view.findViewById(R.id.btnSettingsChangeServer);
        Button btnSettingsSignOut = view.findViewById(R.id.btnSettingsSignOut);

        if (tvSettingsUsername != null) {
            String username = sessionManager.getUsername();
            String displayName = (username != null && !username.trim().isEmpty()) ? username : "Authenticated User";
            tvSettingsUsername.setText(displayName);

            if (tvSettingsInitials != null) {
                tvSettingsInitials.setText(calculateInitials(displayName));
            }
        }

        if (tvSettingsEmail != null) {
            String email = sessionManager.getEmail();
            tvSettingsEmail.setText(email != null && !email.trim().isEmpty() ? email : "AES-256-GCM Encrypted Storage");
        }

        View cardAccountDetails = view.findViewById(R.id.cardAccountDetails);
        if (cardAccountDetails != null) {
            cardAccountDetails.setOnClickListener(v -> AccountBottomSheet.show(requireContext()));
        }

        if (tvSettingsServerUrl != null) {
            String baseUrl = ApiClient.getBaseUrl(requireContext());
            tvSettingsServerUrl.setText("Host: " + baseUrl);
        }

        setupAppearanceControls();

        if (btnSettingsTestConnection != null) {
            btnSettingsTestConnection.setOnClickListener(v -> testConnection());
        }

        if (btnSettingsChangeServer != null) {
            btnSettingsChangeServer.setOnClickListener(v -> showChangeServerAddressDialog());
        }

        if (btnSettingsSignOut != null) {
            btnSettingsSignOut.setOnClickListener(v -> promptSignOutConfirmation());
        }

        loadStorageDetails();
        return view;
    }

    private String calculateInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "CV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else {
            return parts[0].toUpperCase();
        }
    }

    private void loadStorageDetails() {
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    long totalUsed = profile.getUsedStorage();
                    long limit = profile.getStorageLimit() > 0 ? profile.getStorageLimit() : SessionManager.DEFAULT_LIMIT;

                    if (tvSettingsUsedStorage != null) {
                        String formattedUsed = FileUtils.formatStorageSize(requireContext(), totalUsed);
                        String formattedLimit = FileUtils.formatStorageSize(requireContext(), limit);
                        tvSettingsUsedStorage.setText(formattedUsed + " used of " + formattedLimit);
                    }

                    double percentage = (totalUsed * 100.0) / limit;
                    if (progressSettingsQuota != null) {
                        progressSettingsQuota.setProgress((int) Math.min(100, percentage));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                // Ignore silent profile load failure
            }
        });
    }

    private void setupAppearanceControls() {
        if (chipGroupMode == null) return;

        CipherVaultPreferences.AppearanceMode current = CipherVaultPreferences.getAppearance(requireContext());
        if (current == CipherVaultPreferences.AppearanceMode.LIGHT && chipModeLight != null) {
            chipModeLight.setChecked(true);
        } else if (current == CipherVaultPreferences.AppearanceMode.DARK && chipModeDark != null) {
            chipModeDark.setChecked(true);
        } else if (chipModeSystem != null) {
            chipModeSystem.setChecked(true);
        }

        chipGroupMode.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            CipherVaultPreferences.AppearanceMode selectedMode;

            if (checkedId == R.id.chipModeLight) {
                selectedMode = CipherVaultPreferences.AppearanceMode.LIGHT;
            } else if (checkedId == R.id.chipModeDark) {
                selectedMode = CipherVaultPreferences.AppearanceMode.DARK;
            } else {
                selectedMode = CipherVaultPreferences.AppearanceMode.SYSTEM;
            }

            CipherVaultPreferences.saveAppearance(requireContext(), selectedMode);
            ThemeManager.applyAppearanceMode(selectedMode);
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }

    private void testConnection() {
        if (tvSettingsHealthStatus != null) {
            tvSettingsHealthStatus.setText("Health: Checking...");
        }

        long startTime = System.currentTimeMillis();

        apiService.checkHealth().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;

                long latency = System.currentTimeMillis() - startTime;

                if (response.isSuccessful()) {
                    if (tvSettingsHealthStatus != null) {
                        tvSettingsHealthStatus.setText(String.format(Locale.US, "Health: ● Active • %d ms", latency));
                    }
                    Toast.makeText(requireContext(), "Backend connection active (" + latency + " ms)", Toast.LENGTH_SHORT).show();
                } else {
                    if (tvSettingsHealthStatus != null) {
                        tvSettingsHealthStatus.setText("Health: Error (HTTP " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (tvSettingsHealthStatus != null) {
                    tvSettingsHealthStatus.setText("Health: Offline (" + t.getLocalizedMessage() + ")");
                }
            }
        });
    }

    private void promptSignOutConfirmation() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_out_dialog_title)
                .setMessage(R.string.sign_out_dialog_msg)
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangeServerAddressDialog() {
        if (!isAdded()) return;
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        int paddingHorizontal = (int) (24 * getResources().getDisplayMetrics().density);
        int paddingTop = (int) (12 * getResources().getDisplayMetrics().density);
        container.setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0);

        com.google.android.material.textfield.TextInputLayout inputLayout =
                new com.google.android.material.textfield.TextInputLayout(requireContext());
        inputLayout.setHint("Server Base URL");
        inputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);

        com.google.android.material.textfield.TextInputEditText etUrl =
                new com.google.android.material.textfield.TextInputEditText(inputLayout.getContext());
        etUrl.setText(ApiClient.getBaseUrl(requireContext()));
        etUrl.setSingleLine(true);
        inputLayout.addView(etUrl);
        container.addView(inputLayout);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Server Address")
                .setView(container)
                .setPositiveButton("Save & Reconnect", (dialog, which) -> {
                    String newUrl = etUrl.getText() != null ? etUrl.getText().toString().trim() : "";
                    if (!newUrl.isEmpty()) {
                        ApiClient.setBaseUrl(requireContext(), newUrl);
                        if (tvSettingsServerUrl != null) {
                            tvSettingsServerUrl.setText("Host: " + ApiClient.getBaseUrl(requireContext()));
                        }
                        apiService = ApiClient.getApiService(requireContext());
                        testConnection();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        sessionManager.logout();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}