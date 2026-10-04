package com.ciphervault.app;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadFragment extends Fragment {

    public static final long MAX_UPLOAD_SIZE_BYTES = 200L * 1024L * 1024L; // 200 MB

    private Uri fileUri;
    private String fileName;
    private long fileSize;
    private String fileSha256;

    private ApiService apiService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService hashExecutor = Executors.newSingleThreadExecutor();

    private View layoutUnselectedFile;
    private View layoutSelectedFile;
    private ImageView ivSelectedFileIcon;
    private TextView tvSelectedFileName;
    private TextView tvSelectedFileSize;
    private TextView tvSelectedFileHash;
    private Button btnChangeFile;

    private MaterialCardView cardUploadDuplicateWarning;
    private TextView tvUploadDuplicateMessage;

    private MaterialSwitch switchUploadEncrypt;
    private MaterialCardView cardUploadProgress;
    private TextView tvUploadStatus;
    private TextView tvUploadPercent;
    private LinearProgressIndicator progressUpload;
    private Button btnStartUpload;
    private Button btnUploadAnother;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    onFileSelected(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload, container, false);
        apiService = ApiClient.getApiService(requireContext());

        layoutUnselectedFile = view.findViewById(R.id.layoutUnselectedFile);
        layoutSelectedFile = view.findViewById(R.id.layoutSelectedFile);
        ivSelectedFileIcon = view.findViewById(R.id.ivSelectedFileIcon);
        tvSelectedFileName = view.findViewById(R.id.tvSelectedFileName);
        tvSelectedFileSize = view.findViewById(R.id.tvSelectedFileSize);
        tvSelectedFileHash = view.findViewById(R.id.tvSelectedFileHash);
        btnChangeFile = view.findViewById(R.id.btnChangeFile);

        cardUploadDuplicateWarning = view.findViewById(R.id.cardUploadDuplicateWarning);
        tvUploadDuplicateMessage = view.findViewById(R.id.tvUploadDuplicateMessage);

        switchUploadEncrypt = view.findViewById(R.id.switchUploadEncrypt);
        cardUploadProgress = view.findViewById(R.id.cardUploadProgress);
        tvUploadStatus = view.findViewById(R.id.tvUploadStatus);
        tvUploadPercent = view.findViewById(R.id.tvUploadPercent);
        progressUpload = view.findViewById(R.id.progressUpload);
        btnStartUpload = view.findViewById(R.id.btnStartUpload);
        btnUploadAnother = view.findViewById(R.id.btnUploadAnother);

        layoutUnselectedFile.setOnClickListener(v -> launchPicker());
        btnChangeFile.setOnClickListener(v -> launchPicker());
        btnStartUpload.setOnClickListener(v -> performUpload());
        btnUploadAnother.setOnClickListener(v -> resetUploadForm());

        return view;
    }

    public void launchPicker() {
        filePickerLauncher.launch("*/*");
    }

    public void selectFileUri(Uri uri) {
        if (uri != null) {
            onFileSelected(uri);
        }
    }

    private void onFileSelected(@NonNull Uri uri) {
        this.fileUri = uri;
        Context ctx = requireContext();
        fileName = FileUtils.getFileName(ctx, uri);
        fileSize = FileUtils.getFileSize(ctx, uri);

        layoutUnselectedFile.setVisibility(View.GONE);
        layoutSelectedFile.setVisibility(View.VISIBLE);
        cardUploadProgress.setVisibility(View.GONE);
        btnUploadAnother.setVisibility(View.GONE);
        cardUploadDuplicateWarning.setVisibility(View.GONE);

        tvSelectedFileName.setText(fileName);
        tvSelectedFileSize.setText(FileUtils.formatStorageSize(fileSize));
        tvSelectedFileSize.setTextColor(ContextCompat.getColor(ctx, R.color.vault_unencrypted));

        // Determine icon based on file extension
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
            ivSelectedFileIcon.setImageResource(R.drawable.ic_file_image);
        } else if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".mov")) {
            ivSelectedFileIcon.setImageResource(R.drawable.ic_file_video);
        } else if (lower.endsWith(".pdf")) {
            ivSelectedFileIcon.setImageResource(R.drawable.ic_file_pdf);
        } else {
            ivSelectedFileIcon.setImageResource(R.drawable.ic_file_general);
        }

        if (fileSize > MAX_UPLOAD_SIZE_BYTES) {
            btnStartUpload.setEnabled(false);
            tvSelectedFileSize.setText(FileUtils.formatStorageSize(fileSize) + " — Exceeds 200 MB limit!");
            tvSelectedFileSize.setTextColor(ContextCompat.getColor(ctx, R.color.status_error));
            tvSelectedFileHash.setText("File exceeds maximum allowable upload size.");
        } else {
            btnStartUpload.setEnabled(true);
            startChecksumAndDuplicateCheck(ctx, uri);
        }
    }

    private void startChecksumAndDuplicateCheck(Context context, Uri uri) {
        tvSelectedFileHash.setText("SHA-256: calculating checksum...");
        hashExecutor.execute(() -> {
            String hash = FileUtils.calculateSha256(context, uri);
            fileSha256 = hash;
            mainHandler.post(() -> {
                if (!isAdded()) return;
                if (hash != null) {
                    tvSelectedFileHash.setText("SHA-256: " + hash);
                    checkDuplicateOnServer(hash);
                } else {
                    tvSelectedFileHash.setText("SHA-256: calculation unavailable");
                }
            });
        });
    }

    private void checkDuplicateOnServer(String hash) {
        apiService.checkDuplicate(hash).enqueue(new Callback<DuplicateCheckResponse>() {
            @Override
            public void onResponse(@NonNull Call<DuplicateCheckResponse> call, @NonNull Response<DuplicateCheckResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isDuplicate()) {
                    String existingName = response.body().getFilename();
                    cardUploadDuplicateWarning.setVisibility(View.VISIBLE);
                    if (existingName != null && !existingName.isEmpty()) {
                        tvUploadDuplicateMessage.setText("An identical file already exists in your vault: \"" + existingName + "\"");
                    } else {
                        tvUploadDuplicateMessage.setText("An identical file already exists in your vault.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<DuplicateCheckResponse> call, @NonNull Throwable t) {
                // Background duplicate check failed silently
            }
        });
    }

    private void performUpload() {
        if (fileUri == null || !isAdded()) return;

        btnStartUpload.setEnabled(false);
        btnChangeFile.setEnabled(false);
        cardUploadProgress.setVisibility(View.VISIBLE);
        btnUploadAnother.setVisibility(View.GONE);
        progressUpload.setProgress(0);
        tvUploadPercent.setText("0%");
        tvUploadStatus.setText("Encrypting & uploading...");
        tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected));

        boolean isEncrypt = switchUploadEncrypt != null && switchUploadEncrypt.isChecked();

        String mimeType = requireContext().getContentResolver().getType(fileUri);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        StreamingProgressRequestBody requestBody = new StreamingProgressRequestBody(
                requireContext().getContentResolver(),
                fileUri,
                mimeType,
                fileSize,
                (bytesWritten, totalBytes) -> {
                    if (totalBytes > 0) {
                        int progress = (int) ((bytesWritten * 100) / totalBytes);
                        mainHandler.post(() -> {
                            if (!isAdded()) return;
                            progressUpload.setProgress(progress);
                            tvUploadPercent.setText(progress + "%");
                        });
                    }
                }
        );

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestBody);
        RequestBody encryptPart = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isEncrypt));

        apiService.uploadFile(filePart, encryptPart).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                if (!isAdded()) return;
                btnChangeFile.setEnabled(true);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    progressUpload.setProgress(100);
                    tvUploadPercent.setText("100%");
                    tvUploadStatus.setText("✓ Upload complete");
                    tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_connected));
                    btnUploadAnother.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Uploaded to vault: " + fileName, Toast.LENGTH_SHORT).show();

                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).refreshCurrentTab();
                    }
                } else {
                    btnStartUpload.setEnabled(true);
                    String errorMsg = "Upload failed";
                    if (response.code() == 409) {
                        errorMsg = "Duplicate file detected in vault";
                        cardUploadDuplicateWarning.setVisibility(View.VISIBLE);
                    } else if (response.code() == 507) {
                        errorMsg = "Storage quota exceeded";
                    } else {
                        errorMsg = "Upload failed (HTTP " + response.code() + ")";
                    }
                    tvUploadStatus.setText("✕ " + errorMsg);
                    tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                btnStartUpload.setEnabled(true);
                btnChangeFile.setEnabled(true);
                tvUploadStatus.setText("✕ Connection error");
                tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
                Toast.makeText(requireContext(), "Connection failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetUploadForm() {
        fileUri = null;
        fileName = null;
        fileSize = 0;
        fileSha256 = null;

        layoutSelectedFile.setVisibility(View.GONE);
        layoutUnselectedFile.setVisibility(View.VISIBLE);
        cardUploadProgress.setVisibility(View.GONE);
        cardUploadDuplicateWarning.setVisibility(View.GONE);
        btnStartUpload.setEnabled(false);
        btnUploadAnother.setVisibility(View.GONE);
    }
}
