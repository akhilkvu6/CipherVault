package com.ciphervault.app.main.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ciphervault.app.R;
import com.ciphervault.app.auth.api.AuthApi;
import com.ciphervault.app.auth.ui.SignInActivity;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.session.AuthSessionManager;
import com.ciphervault.app.core.session.SecureTokenStorage;
import com.ciphervault.app.main.api.UserApi;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountManagementFragment extends Fragment {

    private SecureTokenStorage tokenStorage;
    private UserApi userApi;
    private AuthApi authApi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tokenStorage = new SecureTokenStorage(requireContext());
        userApi = ApiClient.getClient(requireContext()).create(UserApi.class);
        authApi = ApiClient.getClient(requireContext()).create(AuthApi.class);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        view.findViewById(R.id.btnChangeUsername).setOnClickListener(v -> showChangeUsernameDialog());
        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.btnDeleteAllData).setOnClickListener(v -> showDeleteAllDataDialog());
        view.findViewById(R.id.btnDeleteAccount).setOnClickListener(v -> showDeleteAccountDialog());

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AuthSessionManager(getContext()).logout();
            Intent intent = new Intent(getContext(), SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showChangeUsernameDialog() {
        if (getContext() == null) return;
        EditText input = new EditText(requireContext());
        input.setHint("New username");
        input.setText(tokenStorage.getUsername());
        input.setSingleLine(true);
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Username")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.length() < 3) {
                        Toast.makeText(getContext(), "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, String> body = new HashMap<>();
                    body.put("username", newName);
                    userApi.updateUsername(body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                tokenStorage.saveSession(tokenStorage.getToken(), newName);
                                Toast.makeText(getContext(), "Username updated to " + newName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to update username", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        if (getContext() == null) return;
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText etCurrent = new EditText(requireContext());
        etCurrent.setHint("Current Password");
        etCurrent.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etCurrent);

        EditText etNew = new EditText(requireContext());
        etNew.setHint("New Password");
        etNew.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNew);

        EditText etConfirm = new EditText(requireContext());
        etConfirm.setHint("Confirm New Password");
        etConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirm);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Change Password")
                .setView(layout)
                .setPositiveButton("Update", (dialog, which) -> {
                    String cur = etCurrent.getText().toString().trim();
                    String np = etNew.getText().toString().trim();
                    String cp = etConfirm.getText().toString().trim();

                    if (cur.isEmpty() || np.isEmpty()) {
                        Toast.makeText(getContext(), "Passwords cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!np.equals(cp)) {
                        Toast.makeText(getContext(), "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> body = new HashMap<>();
                    body.put("currentPassword", cur);
                    body.put("newPassword", np);
                    body.put("confirmPassword", cp);

                    authApi.changePassword(body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null) {
                                Object tokenObj = response.body().get("token");
                                if (tokenObj instanceof String) {
                                    tokenStorage.saveSession((String) tokenObj, tokenStorage.getUsername());
                                }
                                Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Incorrect password or weak new password", Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAllDataDialog() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete All Vault Data?")
                .setMessage("This will permanently delete all encrypted files, previews, and transfer logs in your vault. Your account will remain active with 0 B used.")
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    userApi.deleteAllData().enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "All vault data has been deleted", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to delete data: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account?")
                .setMessage("This will permanently delete your account, authentication credentials, and all encrypted data. This action is irreversible.")
                .setPositiveButton("Permanently Delete", (dialog, which) -> {
                    userApi.deleteAccount().enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_LONG).show();
                                new AuthSessionManager(getContext()).logout();
                                Intent intent = new Intent(getContext(), SignInActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            } else {
                                Toast.makeText(getContext(), "Failed to delete account: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
