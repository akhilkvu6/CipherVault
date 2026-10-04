package com.ciphervault.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountBottomSheet {

    public static void show(@NonNull Context context) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_account, null);

        SessionManager sessionManager = new SessionManager(context);
        String username = sessionManager.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = "User";
        }
        String email = sessionManager.getEmail();

        TextView tvAccountInitials = view.findViewById(R.id.tvAccountInitials);
        TextView tvAccountHeaderUsername = view.findViewById(R.id.tvAccountHeaderUsername);
        TextView tvAccountDetailUsername = view.findViewById(R.id.tvAccountDetailUsername);
        TextView tvAccountDetailEmail = view.findViewById(R.id.tvAccountDetailEmail);
        Button btnAccountChangePassword = view.findViewById(R.id.btnAccountChangePassword);
        Button btnAccountClose = view.findViewById(R.id.btnAccountClose);

        if (tvAccountInitials != null) {
            tvAccountInitials.setText(calculateInitials(username));
        }
        if (tvAccountHeaderUsername != null) {
            tvAccountHeaderUsername.setText(username);
        }
        if (tvAccountDetailUsername != null) {
            tvAccountDetailUsername.setText(username);
        }
        if (tvAccountDetailEmail != null) {
            tvAccountDetailEmail.setText(email);
        }

        if (btnAccountChangePassword != null) {
            btnAccountChangePassword.setOnClickListener(v -> showChangePasswordDialog(context));
        }

        if (btnAccountClose != null) {
            btnAccountClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setContentView(view);
        dialog.show();
    }

    private static void showChangePasswordDialog(@NonNull Context context) {
        final SessionManager sessionManager = new SessionManager(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null);
        EditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmNewPassword = dialogView.findViewById(R.id.etConfirmNewPassword);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitChangePassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelChangePassword);

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> alertDialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String currentPass = etCurrentPassword != null ? etCurrentPassword.getText().toString().trim() : "";
                String newPass = etNewPassword != null ? etNewPassword.getText().toString().trim() : "";
                String confirmPass = etConfirmNewPassword != null ? etConfirmNewPassword.getText().toString().trim() : "";

                if (currentPass.isEmpty()) {
                    if (etCurrentPassword != null) etCurrentPassword.setError("Current password is required");
                    return;
                }

                if (newPass.isEmpty()) {
                    if (etNewPassword != null) etNewPassword.setError("New password is required");
                    return;
                }

                if (newPass.length() < 6) {
                    if (etNewPassword != null) etNewPassword.setError("Password must be at least 6 characters");
                    return;
                }

                if (newPass.equals(currentPass)) {
                    if (etNewPassword != null) etNewPassword.setError("New password cannot be same as current");
                    return;
                }

                if (confirmPass.isEmpty()) {
                    if (etConfirmNewPassword != null) etConfirmNewPassword.setError("Please confirm new password");
                    return;
                }

                if (!newPass.equals(confirmPass)) {
                    if (etConfirmNewPassword != null) etConfirmNewPassword.setError("Passwords do not match");
                    return;
                }

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Updating...");

                ApiService apiService = ApiClient.getApiService(context);
                ChangePasswordRequest req = new ChangePasswordRequest(currentPass, newPass, confirmPass);

                apiService.changePassword(req).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Change Password");

                        if (response.isSuccessful()) {
                            if (response.body() != null && response.body().containsKey("token")) {
                                Object tokenObj = response.body().get("token");
                                if (tokenObj != null && !tokenObj.toString().isEmpty()) {
                                    sessionManager.saveAuthToken(tokenObj.toString());
                                }
                            }
                            alertDialog.dismiss();
                            Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = "Failed to update password";
                            try {
                                if (response.errorBody() != null) {
                                    String errJson = response.errorBody().string();
                                    if (errJson.contains("message")) {
                                        org.json.JSONObject obj = new org.json.JSONObject(errJson);
                                        if (obj.has("message")) errorMsg = obj.getString("message");
                                    }
                                }
                            } catch (Exception ignored) {}
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Change Password");
                        Toast.makeText(context, "Connection error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        alertDialog.show();
    }

    private static String calculateInitials(String name) {
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
}
