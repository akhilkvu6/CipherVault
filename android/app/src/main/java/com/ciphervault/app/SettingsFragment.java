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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvSettingsUsername;
    private TextView tvSettingsInitials;
    private TextView tvSettingsUsedStorage;
    private LinearProgressIndicator progressSettingsQuota;

    private ChipGroup chipGroupMode;
    private Chip chipModeDark;
    private Chip chipModeLight;
    private Chip chipModeSystem;

    private ChipGroup chipGroupPalette;
    private Chip chipPaletteObsidian;
    private Chip chipPaletteRuby;
    private Chip chipPaletteCopper;
    private Chip chipPaletteAmethyst;
    private Chip chipPaletteRose;
    private Chip chipPaletteSapphire;
    private Chip chipPaletteMonochrome;

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
        tvSettingsInitials = view.findViewById(R.id.tvSettingsInitials);
        tvSettingsUsedStorage = view.findViewById(R.id.tvSettingsUsedStorage);
        progressSettingsQuota = view.findViewById(R.id.progressSettingsQuota);

        chipGroupMode = view.findViewById(R.id.chipGroupMode);
        chipModeDark = view.findViewById(R.id.chipModeDark);
        chipModeLight = view.findViewById(R.id.chipModeLight);
        chipModeSystem = view.findViewById(R.id.chipModeSystem);

        chipGroupPalette = view.findViewById(R.id.chipGroupPalette);
        chipPaletteObsidian = view.findViewById(R.id.chipPaletteObsidian);
        chipPaletteRuby = view.findViewById(R.id.chipPaletteRuby);
        chipPaletteCopper = view.findViewById(R.id.chipPaletteCopper);
        chipPaletteAmethyst = view.findViewById(R.id.chipPaletteAmethyst);
        chipPaletteRose = view.findViewById(R.id.chipPaletteRose);
        chipPaletteSapphire = view.findViewById(R.id.chipPaletteSapphire);
        chipPaletteMonochrome = view.findViewById(R.id.chipPaletteMonochrome);

        tvSettingsServerUrl = view.findViewById(R.id.tvSettingsServerUrl);
        tvSettingsHealthStatus = view.findViewById(R.id.tvSettingsHealthStatus);

        Button btnSettingsTestConnection = view.findViewById(R.id.btnSettingsTestConnection);
        Button btnSettingsSignOut = view.findViewById(R.id.btnSettingsSignOut);

        if (tvSettingsUsername != null) {
            String username = sessionManager.getUsername();
            String displayName = (username != null && !username.trim().isEmpty()) ? username : "Authenticated User";
            tvSettingsUsername.setText(displayName);

            if (tvSettingsInitials != null) {
                tvSettingsInitials.setText(calculateInitials(displayName));
            }
        }

        if (tvSettingsServerUrl != null) {
            String baseUrl = ApiClient.getBaseUrl(requireContext());
            tvSettingsServerUrl.setText("Host: " + baseUrl);
        }

        setupAppearanceControls();
        setupPaletteControls();

        if (btnSettingsTestConnection != null) {
            btnSettingsTestConnection.setOnClickListener(v -> testConnection());
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
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    long totalUsed = 0L;
                    for (StoredFile file : response.body()) {
                        totalUsed += file.getFileSize();
                    }

                    if (tvSettingsUsedStorage != null) {
                        String formattedUsed = Formatter.formatFileSize(requireContext(), totalUsed);
                        tvSettingsUsedStorage.setText(formattedUsed + " used of 1.0 GB");
                    }

                    double percentage = (totalUsed * 100.0) / SessionManager.DEFAULT_LIMIT;
                    if (progressSettingsQuota != null) {
                        progressSettingsQuota.setProgress((int) Math.min(100, percentage));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
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

    private void setupPaletteControls() {
        if (chipGroupPalette == null) return;

        CipherVaultPreferences.ThemeOption current = CipherVaultPreferences.getTheme(requireContext());
        switch (current) {
            case RUBY:
                if (chipPaletteRuby != null) chipPaletteRuby.setChecked(true);
                break;
            case COPPER:
                if (chipPaletteCopper != null) chipPaletteCopper.setChecked(true);
                break;
            case AMETHYST:
                if (chipPaletteAmethyst != null) chipPaletteAmethyst.setChecked(true);
                break;
            case ROSE:
                if (chipPaletteRose != null) chipPaletteRose.setChecked(true);
                break;
            case SAPPHIRE:
                if (chipPaletteSapphire != null) chipPaletteSapphire.setChecked(true);
                break;
            case MONOCHROME:
                if (chipPaletteMonochrome != null) chipPaletteMonochrome.setChecked(true);
                break;
            case OBSIDIAN:
            default:
                if (chipPaletteObsidian != null) chipPaletteObsidian.setChecked(true);
                break;
        }

        chipGroupPalette.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            CipherVaultPreferences.ThemeOption selectedTheme;

            if (checkedId == R.id.chipPaletteRuby) {
                selectedTheme = CipherVaultPreferences.ThemeOption.RUBY;
            } else if (checkedId == R.id.chipPaletteCopper) {
                selectedTheme = CipherVaultPreferences.ThemeOption.COPPER;
            } else if (checkedId == R.id.chipPaletteAmethyst) {
                selectedTheme = CipherVaultPreferences.ThemeOption.AMETHYST;
            } else if (checkedId == R.id.chipPaletteRose) {
                selectedTheme = CipherVaultPreferences.ThemeOption.ROSE;
            } else if (checkedId == R.id.chipPaletteSapphire) {
                selectedTheme = CipherVaultPreferences.ThemeOption.SAPPHIRE;
            } else if (checkedId == R.id.chipPaletteMonochrome) {
                selectedTheme = CipherVaultPreferences.ThemeOption.MONOCHROME;
            } else {
                selectedTheme = CipherVaultPreferences.ThemeOption.OBSIDIAN;
            }

            CipherVaultPreferences.saveTheme(requireContext(), selectedTheme);
            if (getActivity() != null) {
                ThemeManager.applyTheme(getActivity());
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

    private void signOut() {
        sessionManager.logout();
        Intent intent = new Intent(requireActivity(), ConnectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}