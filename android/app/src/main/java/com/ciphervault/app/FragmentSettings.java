package com.ciphervault.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSettings extends Fragment {

    private SessionManager sessionManager;
    private ApiService apiService;

    // Profile View Mode
    private View layoutProfileViewMode;
    private ShapeableImageView ivSettingsAvatar;
    private TextView tvSettingsName;
    private TextView tvSettingsUsername;
    private TextView tvSettingsEmail;
    private MaterialButton btnEditProfile;
    private MaterialButton btnChangePassword;

    // Profile Edit Mode
    private View layoutProfileEditMode;
    private ShapeableImageView ivEditAvatar;
    private MaterialButton btnChangePhoto;
    private MaterialButton btnRemovePhoto;
    private TextInputLayout tilEditName;
    private TextInputEditText etEditName;
    private TextInputLayout tilEditUsername;
    private TextInputEditText etEditUsername;
    private TextInputLayout tilEditEmail;
    private TextInputEditText etEditEmail;
    private LinearProgressIndicator progressEditProfile;
    private MaterialButton btnCancelEditProfile;
    private MaterialButton btnSaveProfile;

    // Other settings
    private TextView tvSettingsUsedStorage;
    private LinearProgressIndicator progressSettingsQuota;
    private TextView tvSettingsHealthStatus;
    private TextView tvSettingsServerUrl;
    private MaterialSwitch switchBiometricLock;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadProfilePhotoFromUri(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        // Profile View Mode bindings
        layoutProfileViewMode = view.findViewById(R.id.layoutProfileViewMode);
        ivSettingsAvatar = view.findViewById(R.id.ivSettingsAvatar);
        tvSettingsName = view.findViewById(R.id.tvSettingsName);
        tvSettingsUsername = view.findViewById(R.id.tvSettingsUsername);
        tvSettingsEmail = view.findViewById(R.id.tvSettingsEmail);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        // Profile Edit Mode bindings
        layoutProfileEditMode = view.findViewById(R.id.layoutProfileEditMode);
        ivEditAvatar = view.findViewById(R.id.ivEditAvatar);
        btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        btnRemovePhoto = view.findViewById(R.id.btnRemovePhoto);
        tilEditName = view.findViewById(R.id.tilEditName);
        etEditName = view.findViewById(R.id.etEditName);
        tilEditUsername = view.findViewById(R.id.tilEditUsername);
        etEditUsername = view.findViewById(R.id.etEditUsername);
        tilEditEmail = view.findViewById(R.id.tilEditEmail);
        etEditEmail = view.findViewById(R.id.etEditEmail);
        progressEditProfile = view.findViewById(R.id.progressEditProfile);
        btnCancelEditProfile = view.findViewById(R.id.btnCancelEditProfile);
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile);

        // Other settings views
        tvSettingsUsedStorage = view.findViewById(R.id.tvSettingsUsedStorage);
        progressSettingsQuota = view.findViewById(R.id.progressSettingsQuota);
        tvSettingsHealthStatus = view.findViewById(R.id.tvSettingsHealthStatus);
        tvSettingsServerUrl = view.findViewById(R.id.tvSettingsServerUrl);
        switchBiometricLock = view.findViewById(R.id.switchBiometricLock);

        // Profile View/Edit actions
        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> enterProfileEditMode());
        if (btnCancelEditProfile != null) btnCancelEditProfile.setOnClickListener(v -> exitProfileEditMode());
        if (btnSaveProfile != null) btnSaveProfile.setOnClickListener(v -> handleSaveProfile());

        View layoutAvatarPicker = view.findViewById(R.id.layoutAvatarPicker);
        if (layoutAvatarPicker != null) layoutAvatarPicker.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        if (btnChangePhoto != null) btnChangePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        if (btnRemovePhoto != null) btnRemovePhoto.setOnClickListener(v -> handleRemovePhoto());

        if (btnChangePassword != null) btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // TextWatchers to clear errors
        if (etEditName != null) {
            etEditName.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tilEditName != null) tilEditName.setError(null);
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
        if (etEditUsername != null) {
            etEditUsername.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tilEditUsername != null) tilEditUsername.setError(null);
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        view.findViewById(R.id.btnSettingsChangeServer).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ConnectionActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btnSettingsDiagnostics).setOnClickListener(v -> showDiagnosticsDialog());

        View btnDisconnect = view.findViewById(R.id.btnSettingsDisconnectServer);
        if (btnDisconnect != null) {
            btnDisconnect.setOnClickListener(v -> showDisconnectServerDialog());
        }

        view.findViewById(R.id.btnPurgeVault).setOnClickListener(v -> showPurgeVaultDialog());
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());

        View tvBuildNumber = view.findViewById(R.id.tvSettingsBuildNumber);
        if (tvBuildNumber != null) {
            tvBuildNumber.setOnClickListener(v -> handleBuildNumberTap());
        }

        View btnLearnMore = view.findViewById(R.id.btnLearnMoreAbout);
        if (btnLearnMore != null) {
            btnLearnMore.setOnClickListener(v -> startActivity(new Intent(requireContext(), AboutActivity.class)));
        }

        View btnOpenGitHub = view.findViewById(R.id.btnOpenGitHubSettings);
        if (btnOpenGitHub != null) {
            btnOpenGitHub.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/akhilkvu6/CipherVault")));
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Unable to open repository link", Toast.LENGTH_SHORT).show();
                }
            });
        }

        MaterialSwitch switchTransfers = view.findViewById(R.id.switchTransferNotifications);
        if (switchTransfers != null) {
            switchTransfers.setOnCheckedChangeListener((btn, isChecked) -> {
                Toast.makeText(requireContext(), isChecked ? "Transfer notifications enabled" : "Transfer notifications silenced", Toast.LENGTH_SHORT).show();
            });
        }

        MaterialSwitch switchSecurity = view.findViewById(R.id.switchSecurityAlerts);
        if (switchSecurity != null) {
            switchSecurity.setOnCheckedChangeListener((btn, isChecked) -> {
                Toast.makeText(requireContext(), isChecked ? "Security alerts enabled" : "Security alerts silenced", Toast.LENGTH_SHORT).show();
            });
        }

        view.findViewById(R.id.btnSettingsSignOut).setOnClickListener(v -> handleSignOut());

        setupBiometricSwitch();
        setupThemeSelection(view);
        loadProfileData();

        return view;
    }

    private void setupThemeSelection(View view) {
        ChipGroup chipGroupTheme = view.findViewById(R.id.chipGroupTheme);
        if (chipGroupTheme == null) return;

        CipherVaultPreferences.AppearanceMode current = CipherVaultPreferences.getAppearance(requireContext());
        switch (current) {
            case LIGHT:
                chipGroupTheme.check(R.id.chipThemeLight);
                break;
            case DARK:
                chipGroupTheme.check(R.id.chipThemeDark);
                break;
            case SYSTEM:
            default:
                chipGroupTheme.check(R.id.chipThemeSystem);
                break;
        }

        chipGroupTheme.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            CipherVaultPreferences.AppearanceMode mode;
            if (id == R.id.chipThemeLight) {
                mode = CipherVaultPreferences.AppearanceMode.LIGHT;
            } else if (id == R.id.chipThemeDark) {
                mode = CipherVaultPreferences.AppearanceMode.DARK;
            } else {
                mode = CipherVaultPreferences.AppearanceMode.SYSTEM;
            }
            CipherVaultPreferences.saveAppearance(requireContext(), mode);
            ThemeManager.applyAppearanceMode(mode);
        });

        MaterialSwitch switchDynamicColor = view.findViewById(R.id.switchDynamicColor);
        TextView tvDynamicColorSubtitle = view.findViewById(R.id.tvDynamicColorSubtitle);
        if (switchDynamicColor != null) {
            boolean isSupported = com.google.android.material.color.DynamicColors.isDynamicColorAvailable();
            switchDynamicColor.setEnabled(isSupported);
            if (isSupported) {
                switchDynamicColor.setChecked(CipherVaultPreferences.isDynamicColorEnabled(requireContext()));
                if (tvDynamicColorSubtitle != null) {
                    tvDynamicColorSubtitle.setText("Adapt app colors to system wallpaper (Material You)");
                }
            } else {
                switchDynamicColor.setChecked(false);
                if (tvDynamicColorSubtitle != null) {
                    tvDynamicColorSubtitle.setText("Dynamic colors require Android 12+ (API 31+)");
                }
            }
            switchDynamicColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
                CipherVaultPreferences.setDynamicColorEnabled(requireContext(), isChecked);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadProfileData();
        }
    }

    private void setupBiometricSwitch() {
        if (switchBiometricLock != null) {
            switchBiometricLock.setChecked(sessionManager.isBiometricEnabled());
            switchBiometricLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sessionManager.setBiometricEnabled(isChecked);
                Toast.makeText(requireContext(), isChecked ? "Biometric App Lock enabled" : "Biometric App Lock disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadProfileData() {
        if (tvSettingsName != null) tvSettingsName.setText(sessionManager.getName());
        if (tvSettingsUsername != null) tvSettingsUsername.setText("@" + sessionManager.getUsername());
        if (tvSettingsEmail != null) tvSettingsEmail.setText(sessionManager.getEmail());

        String serverUrl = ApiClient.getBaseUrl(requireContext());
        if (tvSettingsServerUrl != null) tvSettingsServerUrl.setText(serverUrl);

        loadProfilePhoto();

        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserProfileResponse profile = response.body();
                    if (profile.getName() != null && !profile.getName().isEmpty()) {
                        sessionManager.saveName(profile.getName());
                        if (tvSettingsName != null) tvSettingsName.setText(profile.getName());
                    }
                    if (profile.getUsername() != null && !profile.getUsername().isEmpty()) {
                        sessionManager.saveUsername(profile.getUsername());
                        if (tvSettingsUsername != null) tvSettingsUsername.setText("@" + profile.getUsername());
                    }
                    if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                        sessionManager.saveEmail(profile.getEmail());
                        if (tvSettingsEmail != null) tvSettingsEmail.setText(profile.getEmail());
                    }

                    long used = profile.getUsedStorage();
                    long limit = SessionManager.DEFAULT_LIMIT;
                    if (tvSettingsUsedStorage != null) {
                        tvSettingsUsedStorage.setText(FileUtils.formatStorageSize(requireContext(), used) + " used of " + FileUtils.formatStorageSize(requireContext(), limit));
                    }
                    if (progressSettingsQuota != null) {
                        int pct = (int) Math.min(100, (used * 100) / limit);
                        progressSettingsQuota.setProgress(pct);
                    }

                    if (profile.isPhotoAvailable()) {
                        loadProfilePhoto();
                    } else {
                        resetAvatarToDefault();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {}
        });
    }

    private void enterProfileEditMode() {
        if (layoutProfileViewMode != null) layoutProfileViewMode.setVisibility(View.GONE);
        if (layoutProfileEditMode != null) layoutProfileEditMode.setVisibility(View.VISIBLE);

        if (etEditName != null) etEditName.setText(sessionManager.getName());
        if (etEditUsername != null) etEditUsername.setText(sessionManager.getUsername());
        if (etEditEmail != null) etEditEmail.setText(sessionManager.getEmail());

        if (tilEditName != null) tilEditName.setError(null);
        if (tilEditUsername != null) tilEditUsername.setError(null);
    }

    private void exitProfileEditMode() {
        if (layoutProfileEditMode != null) layoutProfileEditMode.setVisibility(View.GONE);
        if (layoutProfileViewMode != null) layoutProfileViewMode.setVisibility(View.VISIBLE);

        if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
        if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
        if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
    }

    private void handleSaveProfile() {
        if (tilEditName != null) tilEditName.setError(null);
        if (tilEditUsername != null) tilEditUsername.setError(null);

        String newName = etEditName != null && etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
        String newUsername = etEditUsername != null && etEditUsername.getText() != null ? etEditUsername.getText().toString().trim() : "";

        if (TextUtils.isEmpty(newName)) {
            if (tilEditName != null) tilEditName.setError("Full name is required");
            return;
        }

        if (TextUtils.isEmpty(newUsername)) {
            if (tilEditUsername != null) tilEditUsername.setError("Username is required");
            return;
        }

        if (newUsername.length() < 3) {
            if (tilEditUsername != null) tilEditUsername.setError("Username must be at least 3 characters");
            return;
        }

        if (!Pattern.compile("^[a-zA-Z0-9_]{3,}$").matcher(newUsername).matches()) {
            if (tilEditUsername != null) tilEditUsername.setError("Username can only contain letters, numbers, and underscores");
            return;
        }

        boolean nameChanged = !newName.equals(sessionManager.getName());
        boolean usernameChanged = !newUsername.equalsIgnoreCase(sessionManager.getUsername());

        if (!nameChanged && !usernameChanged) {
            exitProfileEditMode();
            Toast.makeText(requireContext(), "Profile is already up to date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressEditProfile != null) progressEditProfile.setVisibility(View.VISIBLE);
        if (btnSaveProfile != null) btnSaveProfile.setEnabled(false);
        if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(false);

        if (usernameChanged) {
            Map<String, String> usernameBody = new HashMap<>();
            usernameBody.put("username", newUsername);
            apiService.updateUsername(usernameBody).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        sessionManager.saveUsername(newUsername);
                        if (tvSettingsUsername != null) tvSettingsUsername.setText("@" + newUsername);

                        if (nameChanged) {
                            saveNameOnly(newName);
                        } else {
                            finishProfileSaveSuccess();
                        }
                    } else {
                        if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                        if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                        if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
                        if (tilEditUsername != null) tilEditUsername.setError("Username is already taken or invalid");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                    if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                    if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveNameOnly(newName);
        }
    }

    private void saveNameOnly(String newName) {
        Map<String, String> nameBody = new HashMap<>();
        nameBody.put("name", newName);
        apiService.updateName(nameBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    sessionManager.saveName(newName);
                    if (tvSettingsName != null) tvSettingsName.setText(newName);
                    finishProfileSaveSuccess();
                } else {
                    if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                    if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                    if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
                    if (tilEditName != null) tilEditName.setError("Failed to update name");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
                if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void finishProfileSaveSuccess() {
        if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
        if (btnSaveProfile != null) btnSaveProfile.setEnabled(true);
        if (btnCancelEditProfile != null) btnCancelEditProfile.setEnabled(true);
        exitProfileEditMode();
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void uploadProfilePhotoFromUri(@NonNull Uri uri) {
        if (getContext() == null) return;
        try {
            ContentResolver cr = requireContext().getContentResolver();
            String mimeType = cr.getType(uri);
            if (mimeType == null) mimeType = "image/jpeg";

            InputStream is = cr.openInputStream(uri);
            if (is == null) {
                Toast.makeText(requireContext(), "Failed to read selected image", Toast.LENGTH_SHORT).show();
                return;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[8192];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            is.close();

            if (bytes.length > 5 * 1024 * 1024) {
                Toast.makeText(requireContext(), "Image exceeds maximum allowable limit of 5 MB", Toast.LENGTH_LONG).show();
                return;
            }

            Bitmap previewBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (previewBmp != null && ivEditAvatar != null) {
                ivEditAvatar.setImageBitmap(previewBmp);
                ivEditAvatar.setPadding(0, 0, 0, 0);
                ivEditAvatar.setImageTintList(null);
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("photo", "profile_avatar.jpg", requestFile);

            if (progressEditProfile != null) progressEditProfile.setVisibility(View.VISIBLE);

            apiService.uploadProfilePhoto(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (!isAdded()) return;
                    if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        if (previewBmp != null && sessionManager != null) {
                            ProfilePhotoHelper.saveProfilePhoto(requireContext(), sessionManager.getEmail(), previewBmp);
                        }
                        Toast.makeText(requireContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                        loadProfilePhoto();
                    } else {
                        Toast.makeText(requireContext(), "Failed to upload photo", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error selecting photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleRemovePhoto() {
        if (progressEditProfile != null) progressEditProfile.setVisibility(View.VISIBLE);
        apiService.deleteProfilePhoto().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    if (sessionManager != null) {
                        ProfilePhotoHelper.deleteProfilePhoto(requireContext(), sessionManager.getEmail());
                    }
                    resetAvatarToDefault();
                    Toast.makeText(requireContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to remove photo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (progressEditProfile != null) progressEditProfile.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetAvatarToDefault() {
        if (!isAdded()) return;
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        if (ivSettingsAvatar != null) {
            ivSettingsAvatar.setImageResource(R.drawable.ic_lucide_user);
            ivSettingsAvatar.setPadding(pad, pad, pad, pad);
            ivSettingsAvatar.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.cv_primary));
        }
        if (ivEditAvatar != null) {
            ivEditAvatar.setImageResource(R.drawable.ic_lucide_user);
            ivEditAvatar.setPadding(pad, pad, pad, pad);
            ivEditAvatar.setImageTintList(ContextCompat.getColorStateList(requireContext(), R.color.cv_primary));
        }
        if (btnRemovePhoto != null) {
            btnRemovePhoto.setVisibility(View.GONE);
        }
    }

    private void loadProfilePhoto() {
        if (getContext() == null || sessionManager == null) return;
        String email = sessionManager.getEmail();

        // 1. Immediate local disk cache display
        Bitmap diskCached = ProfilePhotoHelper.getProfilePhoto(requireContext(), email);
        if (diskCached != null) {
            if (ivSettingsAvatar != null) {
                ivSettingsAvatar.setImageBitmap(diskCached);
                ivSettingsAvatar.setPadding(0, 0, 0, 0);
                ivSettingsAvatar.setImageTintList(null);
            }
            if (ivEditAvatar != null) {
                ivEditAvatar.setImageBitmap(diskCached);
                ivEditAvatar.setPadding(0, 0, 0, 0);
                ivEditAvatar.setImageTintList(null);
            }
            if (btnRemovePhoto != null) {
                btnRemovePhoto.setVisibility(View.VISIBLE);
            }
        }

        // 2. Refresh from server in background
        apiService.getProfilePhoto().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                Executors.newSingleThreadExecutor().execute(() -> {
                    try (InputStream is = response.body().byteStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null && getActivity() != null) {
                            ProfilePhotoHelper.saveProfilePhoto(requireContext(), email, bmp);
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                if (ivSettingsAvatar != null) {
                                    ivSettingsAvatar.setImageBitmap(bmp);
                                    ivSettingsAvatar.setPadding(0, 0, 0, 0);
                                    ivSettingsAvatar.setImageTintList(null);
                                }
                                if (ivEditAvatar != null) {
                                    ivEditAvatar.setImageBitmap(bmp);
                                    ivEditAvatar.setPadding(0, 0, 0, 0);
                                    ivEditAvatar.setImageTintList(null);
                                }
                                if (btnRemovePhoto != null) {
                                    btnRemovePhoto.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                });
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
        });
    }

    private void showEditNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvEditDialogTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvEditDialogDesc);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);

        tvTitle.setText("Update Full Name");
        tvDesc.setText("Enter your display name for welcome greetings and vault sharing");
        etInput.setText(sessionManager.getName());
        etInput.setSelection(etInput.getText().length());

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName)) {
                        Map<String, String> body = new HashMap<>();
                        body.put("name", newName);
                        apiService.updateName(body).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                                if (isAdded() && response.isSuccessful()) {
                                    sessionManager.saveName(newName);
                                    if (tvSettingsName != null) tvSettingsName.setText(newName);
                                    Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                                if (isAdded()) Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditUsernameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvEditDialogTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvEditDialogDesc);
        EditText etInput = dialogView.findViewById(R.id.etDialogInput);

        tvTitle.setText("Update Username");
        tvDesc.setText("Enter a unique username for your vault account");
        etInput.setText(sessionManager.getUsername());
        etInput.setSelection(etInput.getText().length());

        new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUsername = etInput.getText().toString().trim();
                    if (!TextUtils.isEmpty(newUsername)) {
                        Map<String, String> body = new HashMap<>();
                        body.put("username", newUsername);
                        apiService.updateUsername(body).enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                                if (isAdded() && response.isSuccessful()) {
                                    sessionManager.saveUsername(newUsername);
                                    if (tvSettingsUsername != null) tvSettingsUsername.setText("@" + newUsername);
                                    Toast.makeText(requireContext(), "Username updated successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Username unavailable or invalid", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                                if (isAdded()) Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmNewPassword);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancelChangePassword).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSubmitChangePassword).setOnClickListener(v -> {
            String oldPwd = etOldPassword.getText().toString();
            String newPwd = etNewPassword.getText().toString();
            String confirmPwd = etConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(oldPwd) || TextUtils.isEmpty(newPwd)) {
                Toast.makeText(requireContext(), "Please fill in all password fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPwd.length() < 8 || !Pattern.compile("[A-Z]").matcher(newPwd).find()
                    || !Pattern.compile("[a-z]").matcher(newPwd).find()
                    || !Pattern.compile("[0-9]").matcher(newPwd).find()
                    || !Pattern.compile("[^a-zA-Z0-9]").matcher(newPwd).find()) {
                Toast.makeText(requireContext(), "Password must be 8+ chars with uppercase, lowercase, number & symbol", Toast.LENGTH_LONG).show();
                return;
            }

            if (!newPwd.equals(confirmPwd)) {
                Toast.makeText(requireContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            ChangePasswordRequest req = new ChangePasswordRequest(oldPwd, newPwd);
            apiService.updateUserPassword(req).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                    if (isAdded() && response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Current password incorrect or validation failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    if (isAdded()) Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private int buildTapCount = 0;
    private long lastBuildTapTime = 0;

    private void handleBuildNumberTap() {
        long now = System.currentTimeMillis();
        if (now - lastBuildTapTime > 3500) {
            buildTapCount = 0;
        }
        lastBuildTapTime = now;
        buildTapCount++;
        if (buildTapCount >= 7) {
            buildTapCount = 0;
            Toast.makeText(requireContext(), "Developer UI Showcase unlocked!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireContext(), UiShowcaseActivity.class));
        } else if (buildTapCount >= 3) {
            int remaining = 7 - buildTapCount;
            Toast.makeText(requireContext(), "You are " + remaining + " steps away from developer UI showcase", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPurgeVaultDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_danger_zone, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDangerDialogTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvDangerDialogDesc);
        EditText etPassword = dialogView.findViewById(R.id.etDangerPassword);

        tvTitle.setText("Delete Vault Data");
        tvDesc.setText("This will permanently delete all encrypted files, thumbnails, and transfer history stored in your vault. Your account, profile, and login credentials will remain active. This action cannot be undone. Enter your password to confirm.");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_lucide_trash_2)
                .setView(dialogView)
                .setPositiveButton("Delete Vault Data", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button posBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (posBtn != null) {
                posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                posBtn.setOnClickListener(v -> {
                    String pwd = etPassword.getText().toString();
                    if (TextUtils.isEmpty(pwd)) {
                        Toast.makeText(requireContext(), "Password is required to delete vault data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> body = new HashMap<>();
                    body.put("password", pwd);
                    apiService.purgeVaultData(body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (isAdded() && response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Vault data deleted successfully", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadProfileData();
                            } else {
                                Toast.makeText(requireContext(), "Incorrect password or deletion failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            if (isAdded()) Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        dialog.show();
    }

    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_danger_zone, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDangerDialogTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvDangerDialogDesc);
        EditText etPassword = dialogView.findViewById(R.id.etDangerPassword);

        tvTitle.setText("Delete Vault Account");
        tvDesc.setText("This will permanently delete your account, authentication tokens, profile photo, and all encrypted vault storage from the server. This action cannot be undone. Enter your password to confirm.");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_lucide_trash_2)
                .setView(dialogView)
                .setPositiveButton("Delete Account", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button posBtn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            if (posBtn != null) {
                posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                posBtn.setOnClickListener(v -> {
                    String pwd = etPassword.getText().toString();
                    if (TextUtils.isEmpty(pwd)) {
                        Toast.makeText(requireContext(), "Password is required to delete account", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> body = new HashMap<>();
                    body.put("password", pwd);
                    apiService.deleteAccount(body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                            if (isAdded() && response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Account deleted permanently", Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                sessionManager.logout();
                                Intent intent = new Intent(requireContext(), LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                requireActivity().finish();
                            } else {
                                Toast.makeText(requireContext(), "Incorrect password or deletion failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            if (isAdded()) Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        dialog.show();
    }

    private void showDiagnosticsDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cryptographic & Network Diagnostics")
                .setMessage("Server URL: " + ApiClient.getBaseUrl(requireContext()) + "\n" +
                        "Cipher Suite: AES-256-GCM (128-bit Tag)\n" +
                        "Key Derivation: PBKDF2WithHmacSHA256 (65,536 iterations)\n" +
                        "Vault Capacity: 10 GB Authoritative Quota\n" +
                        "Streaming Buffer: 16 KB fixed chunking\n" +
                        "Integrity Check: SHA-256 local verification")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDisconnectServerDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Disconnect Server")
                .setMessage("Disconnect from the current backend server? The active server connection will be cleared. Your saved servers and authenticated login session will remain preserved.")
                .setIcon(R.drawable.ic_lucide_log_out)
                .setPositiveButton("Disconnect", (dialog, which) -> {
                    ServerConnectionManager.getInstance(requireContext()).disconnect(requireContext());
                    Toast.makeText(requireContext(), "Server disconnected", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), ConnectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSignOut() {
        try {
            apiService.logout().enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {}
                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
            });
        } catch (Exception ignored) {}

        sessionManager.logout();
        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
