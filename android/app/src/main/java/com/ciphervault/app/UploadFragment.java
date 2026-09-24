package com.ciphervault.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadFragment extends Fragment {

    private static final long MIN_LOADING_MS = 2000L; // 2 seconds minimum loading floor

    private TextView tvSelectedFile;
    private TextView tvUploadSubtitle;
    private MaterialCardView cardDuplicateWarning;
    private CompoundButton cbEncrypt;
    private Button btnUpload;

    private Uri selectedUri;
    private String selectedFileName;
    private long selectedFileSize;
    private ApiService apiService;
    private final List<StoredFile> existingVaultFiles = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AlertDialog progressDialog;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedUri = uri;
                    selectedFileName = resolveFileName(uri);
                    selectedFileSize = resolveFileSize(uri);

                    tvSelectedFile.setText(selectedFileName);
                    if (selectedFileSize > 0) {
                        tvUploadSubtitle.setText(Formatter.formatFileSize(requireContext(), selectedFileSize));
                    } else {
                        tvUploadSubtitle.setText(R.string.upload_file_supported_formats);
                    }

                    checkForDuplicateFile(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);

        apiService = ApiClient.getApiService(requireContext());

        tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        tvUploadSubtitle = view.findViewById(R.id.tvUploadSubtitle);
        MaterialCardView btnChooseFile = view.findViewById(R.id.btnChooseFile);
        cardDuplicateWarning = view.findViewById(R.id.cardDuplicateWarning);
        cbEncrypt = view.findViewById(R.id.cbEncrypt);
        btnUpload = view.findViewById(R.id.btnUpload);

        if (btnChooseFile != null) {
            btnChooseFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        }

        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> performUpload());
            btnUpload.setEnabled(selectedUri != null);
        }

        fetchExistingVaultFiles();
        return view;
    }

    private void fetchExistingVaultFiles() {
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    existingVaultFiles.clear();
                    existingVaultFiles.addAll(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                // Ignore silent pre-fetch failures
            }
        });
    }

    private void checkForDuplicateFile(Uri uri) {
        if (btnUpload != null) {
            btnUpload.setEnabled(false);
        }

        new Thread(() -> {
            boolean isDuplicate = false;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int read;
                try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        while ((read = is.read(buffer)) != -1) {
                            digest.update(buffer, 0, read);
                        }
                    }
                }

                byte[] hashBytes = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : hashBytes) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                String computedHash = hexString.toString();

                for (StoredFile existingFile : existingVaultFiles) {
                    if (existingFile.getSha256Hash() != null && existingFile.getSha256Hash().equalsIgnoreCase(computedHash)) {
                        isDuplicate = true;
                        break;
                    }
                }
            } catch (Exception ignored) {}

            final boolean duplicateFound = isDuplicate;
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    if (duplicateFound) {
                        if (cardDuplicateWarning != null) {
                            cardDuplicateWarning.setVisibility(View.VISIBLE);
                        }
                        if (btnUpload != null) {
                            btnUpload.setEnabled(false);
                        }
                    } else {
                        if (cardDuplicateWarning != null) {
                            cardDuplicateWarning.setVisibility(View.GONE);
                        }
                        if (btnUpload != null) {
                            btnUpload.setEnabled(true);
                        }
                    }
                });
            }
        }).start();
    }

    private void performUpload() {
        if (selectedUri == null) return;

        showUploadProgressDialog();

        final long startTime = System.currentTimeMillis();

        try {
            String filename = selectedFileName != null ? selectedFileName : resolveFileName(selectedUri);
            String contentType = requireContext().getContentResolver().getType(selectedUri);
            if (contentType == null) contentType = "application/octet-stream";

            InputStream is = requireContext().getContentResolver().openInputStream(selectedUri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            if (is != null) {
                while ((read = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                is.close();
            }

            RequestBody fileBody = RequestBody.create(MediaType.parse(contentType), bos.toByteArray());
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, fileBody);

            boolean shouldEncrypt = cbEncrypt == null || cbEncrypt.isChecked();
            RequestBody encryptBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(shouldEncrypt));

            apiService.uploadFile(part, encryptBody).enqueue(new Callback<UploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                    if (!isAdded()) return;

                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long remainingDelay = Math.max(0, MIN_LOADING_MS - elapsedTime);

                    mainHandler.postDelayed(() -> {
                        if (!isAdded()) return;

                        dismissProgressDialog();

                        if (response.isSuccessful()) {
                            showUploadSuccessDialog();
                            fetchExistingVaultFiles();
                        } else if (response.code() == 409) {
                            showUploadFailedDialog("Duplicate File Detected: This exact file already exists in your vault.");
                        } else {
                            showUploadFailedDialog("Upload failed (HTTP " + response.code() + ")");
                        }
                    }, remainingDelay);
                }

                @Override
                public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                    if (!isAdded()) return;

                    long elapsedTime = System.currentTimeMillis() - startTime;
                    long remainingDelay = Math.max(0, MIN_LOADING_MS - elapsedTime);

                    mainHandler.postDelayed(() -> {
                        if (!isAdded()) return;
                        dismissProgressDialog();
                        showUploadFailedDialog("Upload error: " + t.getLocalizedMessage());
                    }, remainingDelay);
                }
            });

        } catch (Exception e) {
            dismissProgressDialog();
            showUploadFailedDialog("Failed reading file: " + e.getMessage());
        }
    }

    private void showUploadProgressDialog() {
        if (!isAdded()) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_upload_progress, null);
        TextView tvDialogFileName = dialogView.findViewById(R.id.tvDialogFileName);
        TextView tvDialogOperation = dialogView.findViewById(R.id.tvDialogOperation);

        if (tvDialogFileName != null) {
            tvDialogFileName.setText(selectedFileName != null ? selectedFileName : "File");
        }

        if (tvDialogOperation != null) {
            boolean isEncrypted = cbEncrypt == null || cbEncrypt.isChecked();
            tvDialogOperation.setText(isEncrypted ? R.string.status_uploading_encrypted : R.string.status_uploading_file);
        }

        dismissProgressDialog();

        progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            try {
                progressDialog.dismiss();
            } catch (Exception ignored) {}
            progressDialog = null;
        }
    }

    private void showUploadSuccessDialog() {
        if (!isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.upload_complete_title)
                .setMessage("Successfully uploaded " + (selectedFileName != null ? selectedFileName : "file") + " to your vault.")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    resetToSelectionState();
                })
                .setCancelable(false)
                .show();
    }

    private void showUploadFailedDialog(String errorMessage) {
        if (!isAdded()) return;

        if (cardDuplicateWarning != null && errorMessage != null && errorMessage.contains("Duplicate")) {
            cardDuplicateWarning.setVisibility(View.VISIBLE);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.upload_failed_title)
                .setMessage(errorMessage)
                .setPositiveButton(R.string.duplicate_dialog_positive, (dialog, which) -> dialog.dismiss())
                .show();

        if (btnUpload != null) {
            btnUpload.setEnabled(selectedUri != null);
            btnUpload.setText(R.string.btn_upload_to_vault);
        }
    }

    private void resetToSelectionState() {
        selectedUri = null;
        selectedFileName = null;
        selectedFileSize = 0;

        if (tvSelectedFile != null) {
            tvSelectedFile.setText(R.string.upload_file_prompt);
        }
        if (tvUploadSubtitle != null) {
            tvUploadSubtitle.setText(R.string.upload_file_supported_formats);
        }
        if (btnUpload != null) {
            btnUpload.setEnabled(false);
            btnUpload.setText(R.string.btn_upload_to_vault);
        }
        if (cardDuplicateWarning != null) {
            cardDuplicateWarning.setVisibility(View.GONE);
        }
    }

    private String resolveFileName(Uri uri) {
        String name = "file.bin";
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index != -1) name = cursor.getString(index);
            }
        } catch (Exception ignored) {}
        return name;
    }

    private long resolveFileSize(Uri uri) {
        long size = 0L;
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index != -1 && !cursor.isNull(index)) {
                    size = cursor.getLong(index);
                }
            }
        } catch (Exception ignored) {}
        if (size == 0L) {
            try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
                if (is != null) size = is.available();
            } catch (Exception ignored) {}
        }
        return size;
    }

    @Override
    public void onDestroyView() {
        dismissProgressDialog();
        super.onDestroyView();
    }
}