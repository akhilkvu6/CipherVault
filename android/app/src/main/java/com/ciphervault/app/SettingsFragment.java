package com.ciphervault.app;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

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
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_settings,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        initializeViews(view);
        loadAccountDetails();
        loadServerDetails();
        setupAppearanceControls();
        setupClickListeners();
        loadStorageDetails();
    }

    private void initializeViews(@NonNull View view) {

        tvSettingsUsername =
                view.findViewById(R.id.tvSettingsUsername);

        tvSettingsEmail =
                view.findViewById(R.id.tvSettingsEmail);

        tvSettingsInitials =
                view.findViewById(R.id.tvSettingsInitials);

        tvSettingsUsedStorage =
                view.findViewById(R.id.tvSettingsUsedStorage);

        progressSettingsQuota =
                view.findViewById(R.id.progressSettingsQuota);

        chipGroupMode =
                view.findViewById(R.id.chipGroupMode);

        chipModeDark =
                view.findViewById(R.id.chipModeDark);

        chipModeLight =
                view.findViewById(R.id.chipModeLight);

        chipModeSystem =
                view.findViewById(R.id.chipModeSystem);

        tvSettingsServerUrl =
                view.findViewById(R.id.tvSettingsServerUrl);

        tvSettingsHealthStatus =
                view.findViewById(R.id.tvSettingsHealthStatus);
    }

    private void loadAccountDetails() {

        if (!isAdded() || getView() == null) {
            return;
        }

        String username = sessionManager.getUsername();

        String displayName;

        if (username != null && !username.trim().isEmpty()) {
            displayName = username.trim();
        } else {
            displayName = "Authenticated User";
        }

        if (tvSettingsUsername != null) {
            tvSettingsUsername.setText(displayName);
        }

        if (tvSettingsInitials != null) {
            tvSettingsInitials.setText(
                    calculateInitials(displayName)
            );
        }

        String email = sessionManager.getEmail();

        if (tvSettingsEmail != null) {

            if (email != null && !email.trim().isEmpty()) {
                tvSettingsEmail.setText(email.trim());
            } else {
                tvSettingsEmail.setText(
                        "AES-256-GCM Encrypted Storage"
                );
            }
        }
    }

    private String calculateInitials(String name) {

        if (name == null || name.trim().isEmpty()) {
            return "CV";
        }

        String[] parts = name.trim().split("\\s+");

        if (parts.length >= 2) {

            String first = parts[0];
            String second = parts[1];

            if (!first.isEmpty() && !second.isEmpty()) {
                return (
                        first.substring(0, 1)
                                + second.substring(0, 1)
                ).toUpperCase(Locale.US);
            }
        }

        String firstPart = parts[0];

        if (firstPart.length() >= 2) {
            return firstPart
                    .substring(0, 2)
                    .toUpperCase(Locale.US);
        }

        return firstPart.toUpperCase(Locale.US);
    }

    private void loadServerDetails() {

        if (!isAdded()) {
            return;
        }

        String baseUrl = ApiClient.getBaseUrl(requireContext());

        if (tvSettingsServerUrl != null) {
            tvSettingsServerUrl.setText(
                    "Host: " + baseUrl
            );
        }
    }

    private void setupClickListeners() {

        if (!isAdded() || getView() == null) {
            return;
        }

        View view = getView();

        if (view == null) {
            return;
        }

        View cardAccountDetails =
                view.findViewById(R.id.cardAccountDetails);

        Button btnSettingsTestConnection =
                view.findViewById(R.id.btnSettingsTestConnection);

        Button btnSettingsChangeServer =
                view.findViewById(R.id.btnSettingsChangeServer);

        Button btnSettingsSignOut =
                view.findViewById(R.id.btnSettingsSignOut);

        if (cardAccountDetails != null) {

            cardAccountDetails.setOnClickListener(
                    v -> {

                        if (!isAdded()) {
                            return;
                        }

                        AccountBottomSheet.show(
                                requireContext()
                        );
                    }
            );
        }

        if (btnSettingsTestConnection != null) {

            btnSettingsTestConnection.setOnClickListener(
                    v -> testConnection()
            );
        }

        if (btnSettingsChangeServer != null) {

            btnSettingsChangeServer.setOnClickListener(
                    v -> showChangeServerAddressDialog()
            );
        }

        if (btnSettingsSignOut != null) {

            btnSettingsSignOut.setOnClickListener(
                    v -> promptSignOutConfirmation()
            );
        }
    }

    private void loadStorageDetails() {

        if (!isAdded() || apiService == null) {
            return;
        }

        apiService.getUserProfile().enqueue(
                new Callback<UserProfileResponse>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<UserProfileResponse> call,
                            @NonNull Response<UserProfileResponse> response
                    ) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {
                            return;
                        }

                        UserProfileResponse profile =
                                response.body();

                        long totalUsed =
                                Math.max(
                                        0L,
                                        profile.getUsedStorage()
                                );

                        long limit =
                                profile.getStorageLimit() > 0
                                        ? profile.getStorageLimit()
                                        : SessionManager.DEFAULT_LIMIT;

                        if (tvSettingsUsedStorage != null) {

                            String formattedUsed =
                                    FileUtils.formatStorageSize(
                                            requireContext(),
                                            totalUsed
                                    );

                            String formattedLimit =
                                    FileUtils.formatStorageSize(
                                            requireContext(),
                                            limit
                                    );

                            tvSettingsUsedStorage.setText(
                                    formattedUsed
                                            + " used of "
                                            + formattedLimit
                            );
                        }

                        if (progressSettingsQuota != null) {

                            double percentage =
                                    limit > 0
                                            ? (totalUsed * 100.0) / limit
                                            : 0.0;

                            int progress =
                                    (int) Math.max(
                                            0,
                                            Math.min(
                                                    100,
                                                    percentage
                                            )
                                    );

                            progressSettingsQuota.setProgress(
                                    progress
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<UserProfileResponse> call,
                            @NonNull Throwable t
                    ) {
                        // Keep storage section silent if profile loading fails.
                    }
                }
        );
    }

    private void setupAppearanceControls() {

        if (!isAdded() || chipGroupMode == null) {
            return;
        }

        CipherVaultPreferences.AppearanceMode current =
                CipherVaultPreferences.getAppearance(
                        requireContext()
                );

        if (current ==
                CipherVaultPreferences.AppearanceMode.LIGHT) {

            if (chipModeLight != null) {
                chipModeLight.setChecked(true);
            }

        } else if (
                current ==
                        CipherVaultPreferences.AppearanceMode.DARK
        ) {

            if (chipModeDark != null) {
                chipModeDark.setChecked(true);
            }

        } else {

            if (chipModeSystem != null) {
                chipModeSystem.setChecked(true);
            }
        }

        chipGroupMode.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {

                    if (checkedIds == null
                            || checkedIds.isEmpty()
                            || !isAdded()) {
                        return;
                    }

                    int checkedId =
                            checkedIds.get(0);

                    CipherVaultPreferences.AppearanceMode selectedMode;

                    if (checkedId ==
                            R.id.chipModeLight) {

                        selectedMode =
                                CipherVaultPreferences.AppearanceMode.LIGHT;

                    } else if (
                            checkedId ==
                                    R.id.chipModeDark
                    ) {

                        selectedMode =
                                CipherVaultPreferences.AppearanceMode.DARK;

                    } else {

                        selectedMode =
                                CipherVaultPreferences.AppearanceMode.SYSTEM;
                    }

                    CipherVaultPreferences.saveAppearance(
                            requireContext(),
                            selectedMode
                    );

                    ThemeManager.applyAppearanceMode(
                            selectedMode
                    );

                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
        );
    }

    private void testConnection() {

        if (!isAdded() || apiService == null) {
            return;
        }

        if (tvSettingsHealthStatus != null) {
            tvSettingsHealthStatus.setText(
                    "Health: Checking..."
            );
        }

        long startTime =
                System.currentTimeMillis();

        apiService.checkHealth().enqueue(
                new Callback<Map<String, Object>>() {

                    @Override
                    public void onResponse(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Response<Map<String, Object>> response
                    ) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        long latency =
                                System.currentTimeMillis()
                                        - startTime;

                        if (response.isSuccessful()) {

                            if (tvSettingsHealthStatus != null) {

                                tvSettingsHealthStatus.setText(
                                        String.format(
                                                Locale.US,
                                                "Health: Active • %d ms",
                                                latency
                                        )
                                );
                            }

                            Toast.makeText(
                                    requireContext(),
                                    "Backend connection active ("
                                            + latency
                                            + " ms)",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            if (tvSettingsHealthStatus != null) {

                                tvSettingsHealthStatus.setText(
                                        "Health: Error (HTTP "
                                                + response.code()
                                                + ")"
                                );
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<Map<String, Object>> call,
                            @NonNull Throwable t
                    ) {

                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        String message =
                                t.getLocalizedMessage();

                        if (message == null
                                || message.trim().isEmpty()) {
                            message = "Connection failed";
                        }

                        if (tvSettingsHealthStatus != null) {

                            tvSettingsHealthStatus.setText(
                                    "Health: Offline ("
                                            + message
                                            + ")"
                            );
                        }
                    }
                }
        );
    }

    private void showChangeServerAddressDialog() {

        if (!isAdded()) {
            return;
        }

        int paddingHorizontal =
                (int) (
                        24
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        int paddingTop =
                (int) (
                        12
                                * getResources()
                                .getDisplayMetrics()
                                .density
                );

        android.widget.FrameLayout container =
                new android.widget.FrameLayout(
                        requireContext()
                );

        container.setPadding(
                paddingHorizontal,
                paddingTop,
                paddingHorizontal,
                0
        );

        TextInputLayout inputLayout =
                new TextInputLayout(
                        requireContext()
                );

        inputLayout.setHint(
                "Server Base URL"
        );

        inputLayout.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE
        );

        TextInputEditText etUrl =
                new TextInputEditText(
                        inputLayout.getContext()
                );

        String currentUrl =
                ApiClient.getBaseUrl(
                        requireContext()
                );

        etUrl.setText(currentUrl);
        etUrl.setSingleLine(true);

        inputLayout.addView(etUrl);
        container.addView(inputLayout);

        new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle("Server Address")
                .setView(container)
                .setPositiveButton(
                        "Save & Reconnect",
                        (dialog, which) -> {

                            if (!isAdded()) {
                                return;
                            }

                            String newUrl =
                                    etUrl.getText() != null
                                            ? etUrl
                                            .getText()
                                            .toString()
                                            .trim()
                                            : "";

                            if (newUrl.isEmpty()) {

                                etUrl.setError(
                                        "Enter a server address"
                                );

                                return;
                            }

                            try {

                                String normalizedUrl =
                                        ApiClient.sanitizeAndValidateUrl(
                                                newUrl
                                        );

                                ApiClient.setBaseUrl(
                                        requireContext(),
                                        normalizedUrl
                                );

                                if (tvSettingsServerUrl != null) {

                                    tvSettingsServerUrl.setText(
                                            "Host: "
                                                    + ApiClient.getBaseUrl(
                                                    requireContext()
                                            )
                                    );
                                }

                                apiService =
                                        ApiClient.getApiService(
                                                requireContext()
                                        );

                                testConnection();

                            } catch (Exception e) {

                                etUrl.setError(
                                        "Invalid server address"
                                );
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void promptSignOutConfirmation() {

        if (!isAdded()) {
            return;
        }

        new MaterialAlertDialogBuilder(
                requireContext()
        )
                .setTitle(
                        R.string.sign_out_dialog_title
                )
                .setMessage(
                        R.string.sign_out_dialog_msg
                )
                .setPositiveButton(
                        "Sign Out",
                        (dialog, which) -> signOut()
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void signOut() {

        if (!isAdded()) {
            return;
        }

        sessionManager.logout();

        Intent intent =
                new Intent(
                        requireActivity(),
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        tvSettingsUsername = null;
        tvSettingsEmail = null;
        tvSettingsInitials = null;
        tvSettingsUsedStorage = null;
        progressSettingsQuota = null;

        chipGroupMode = null;
        chipModeDark = null;
        chipModeLight = null;
        chipModeSystem = null;

        tvSettingsServerUrl = null;
        tvSettingsHealthStatus = null;
    }
}