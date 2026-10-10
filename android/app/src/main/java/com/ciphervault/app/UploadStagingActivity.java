package com.ciphervault.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.content.res.ColorStateList;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import android.graphics.Bitmap;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.ciphervault.app.transfer.TransferBatch;
import com.ciphervault.app.transfer.TransferItem;
import com.ciphervault.app.transfer.TransferListener;
import com.ciphervault.app.transfer.TransferManager;
import com.ciphervault.app.transfer.TransferState;
import com.ciphervault.app.transfer.TransferType;

public class UploadStagingActivity extends BaseActivity implements TransferListener {

    public static final long MAX_SINGLE_FILE_BYTES = 1073741824L; // 1 GB
    public static final long MAX_BATCH_BYTES = 1073741824L; // 1 GB
    public static final long AUTHORITATIVE_CAPACITY_BYTES = SessionManager.DEFAULT_LIMIT; // 10 GB

    public enum StagingStatus {
        READY,
        DUPLICATE_EXCLUDED,
        EXCEEDS_QUOTA,
        UPLOADING,
        COMPLETED,
        FAILED
    }

    public static class StagedFile {
        public Uri uri;
        public String name;
        public long size;
        public String sha256;
        public String transferId;
        public StagingStatus status = StagingStatus.READY;
        public String statusReason = "Ready";
        public int progress = 0;

        public StagedFile(Uri uri, String name, long size) {
            this.uri = uri;
            this.name = name;
            this.size = size;
        }
    }

    private final List<StagedFile> stagedFiles = new ArrayList<>();
    private StagingAdapter adapter;
    private ApiService apiService;
    private final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(3);

    private LinearProgressIndicator progressVaultCapacity;
    private LinearProgressIndicator progressBatchSize;
    private LinearProgressIndicator progressOverallUpload;
    private TextView tvVaultCapacityText;
    private TextView tvBatchSizeText;
    private TextView tvValidFilesCount;
    private TextView tvUploadStatusMsg;
    private Button btnUploadValidFiles;
    private View layoutStagingEmpty;

    private long usedStorageBytes = 0L;
    private boolean isUploading = false;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    addUrisToStaging(uris);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri capturedUri = result.getData().getData();
                    if (capturedUri != null) {
                        List<Uri> list = new ArrayList<>();
                        list.add(capturedUri);
                        addUrisToStaging(list);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload_staging);

        apiService = ApiClient.getApiService(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbarStaging);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressVaultCapacity = findViewById(R.id.progressVaultCapacity);
        progressBatchSize = findViewById(R.id.progressBatchSize);
        progressOverallUpload = findViewById(R.id.progressOverallUpload);
        tvVaultCapacityText = findViewById(R.id.tvVaultCapacityText);
        tvBatchSizeText = findViewById(R.id.tvBatchSizeText);
        tvValidFilesCount = findViewById(R.id.tvValidFilesCount);
        tvUploadStatusMsg = findViewById(R.id.tvUploadStatusMsg);
        btnUploadValidFiles = findViewById(R.id.btnUploadValidFiles);
        layoutStagingEmpty = findViewById(R.id.layoutStagingEmpty);

        RecyclerView rvStagingFiles = findViewById(R.id.rvStagingFiles);
        adapter = new StagingAdapter(stagedFiles, new StagingAdapter.OnItemActionListener() {
            @Override
            public void onRemove(int position) {
                removeStagedFile(position);
            }

            @Override
            public void onPreview(int position) {
                previewStagedFile(position);
            }
        });
        rvStagingFiles.setLayoutManager(new LinearLayoutManager(this));
        rvStagingFiles.setAdapter(adapter);

        findViewById(R.id.btnAddFiles).setOnClickListener(v -> {
            if (!isUploading) {
                filePickerLauncher.launch("*/*");
            }
        });

        findViewById(R.id.btnOpenCamera).setOnClickListener(v -> {
            if (!isUploading) {
                Intent intent = new Intent(UploadStagingActivity.this, CameraActivity.class);
                cameraLauncher.launch(intent);
            }
        });

        btnUploadValidFiles.setOnClickListener(v -> startStreamingUpload());

        loadCurrentStorageUsage();

        // Handle initial intent if files were shared or passed
        if (getIntent() != null && getIntent().getData() != null) {
            List<Uri> single = new ArrayList<>();
            single.add(getIntent().getData());
            addUrisToStaging(single);
        }

        TransferManager.getInstance(this).registerListener(this);
    }

    private void loadCurrentStorageUsage() {
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    usedStorageBytes = response.body().getUsedStorage();
                    updateQuotaMetrics();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                updateQuotaMetrics();
            }
        });
    }

    private void addUrisToStaging(List<Uri> uris) {
        for (Uri uri : uris) {
            String name = FileUtils.getFileName(this, uri);
            long size = FileUtils.getFileSize(this, uri);
            StagedFile file = new StagedFile(uri, name, size);
            stagedFiles.add(file);
            AuditLogger.log(this, "File Staged", "SUCCESS", "Staged: " + file.name + " (" + FileUtils.formatStorageSize(this, file.size) + ")");
            validateAndCheckDuplicate(file);
        }
        adapter.notifyDataSetChanged();
        updateQuotaMetrics();
    }

    private void validateAndCheckDuplicate(StagedFile file) {
        // Check 1: Individual file limit (1 GB)
        if (file.size > MAX_SINGLE_FILE_BYTES) {
            file.status = StagingStatus.EXCEEDS_QUOTA;
            file.statusReason = "Exceeds 1 GB file limit";
            adapter.notifyDataSetChanged();
            return;
        }

        // Check 2: Total batch limit (1 GB)
        long currentBatch = calculateBatchSize();
        if (currentBatch > MAX_BATCH_BYTES) {
            file.status = StagingStatus.EXCEEDS_QUOTA;
            file.statusReason = "Exceeds 1 GB batch limit";
            adapter.notifyDataSetChanged();
            return;
        }

        // Check 3: Vault capacity limit (10 GB)
        if (usedStorageBytes + currentBatch > AUTHORITATIVE_CAPACITY_BYTES) {
            file.status = StagingStatus.EXCEEDS_QUOTA;
            file.statusReason = "Exceeds 10 GB vault limit";
            adapter.notifyDataSetChanged();
            return;
        }

        // Check 4: Duplicate Check via SHA-256
        backgroundExecutor.execute(() -> {
            String sha256 = FileUtils.calculateSha256(this, file.uri);
            file.sha256 = sha256;
            if (sha256 != null && !sha256.isEmpty()) {
                apiService.checkDuplicate(sha256).enqueue(new Callback<DuplicateCheckResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DuplicateCheckResponse> call, @NonNull Response<DuplicateCheckResponse> response) {
                        runOnUiThread(() -> {
                            if (response.isSuccessful() && response.body() != null && response.body().isDuplicate()) {
                                file.status = StagingStatus.DUPLICATE_EXCLUDED;
                                file.statusReason = "Duplicate Excluded";
                            } else {
                                if (file.status != StagingStatus.EXCEEDS_QUOTA) {
                                    file.status = StagingStatus.READY;
                                    file.statusReason = "Ready";
                                }
                            }
                            adapter.notifyDataSetChanged();
                            updateQuotaMetrics();
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<DuplicateCheckResponse> call, @NonNull Throwable t) {
                        runOnUiThread(() -> {
                            file.status = StagingStatus.READY;
                            file.statusReason = "Ready";
                            adapter.notifyDataSetChanged();
                            updateQuotaMetrics();
                        });
                    }
                });
            }
        });
    }

    private void removeStagedFile(int position) {
        if (position >= 0 && position < stagedFiles.size() && !isUploading) {
            StagedFile removed = stagedFiles.remove(position);
            adapter.notifyDataSetChanged();
            updateQuotaMetrics();
            AuditLogger.log(this, "Delete", "SUCCESS", "Removed staged file: " + removed.name);
            Toast.makeText(this, "Deleted from staging: " + removed.name, Toast.LENGTH_SHORT).show();
        }
    }

    private void previewStagedFile(int position) {
        if (position >= 0 && position < stagedFiles.size()) {
            showStagedFilePreviewDialog(stagedFiles.get(position));
        }
    }

    private void showStagedFilePreviewDialog(StagedFile file) {
        if (file == null || isFinishing() || isDestroyed()) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_staged_file_preview, null);
        dialog.setContentView(sheetView);

        ImageView ivImage = sheetView.findViewById(R.id.ivDialogPreviewImage);
        View scrollText = sheetView.findViewById(R.id.scrollDialogPreviewText);
        TextView tvText = sheetView.findViewById(R.id.tvDialogPreviewText);
        View layoutGeneric = sheetView.findViewById(R.id.layoutDialogPreviewGeneric);
        ImageView ivGenericIcon = sheetView.findViewById(R.id.ivDialogPreviewGenericIcon);
        TextView tvGenericType = sheetView.findViewById(R.id.tvDialogPreviewGenericType);

        TextView tvFileName = sheetView.findViewById(R.id.tvDialogPreviewFileName);
        TextView tvFileSize = sheetView.findViewById(R.id.tvDialogPreviewFileSize);
        TextView tvStatus = sheetView.findViewById(R.id.tvDialogPreviewStatus);

        Button btnDelete = sheetView.findViewById(R.id.btnDialogPreviewDelete);
        Button btnFullscreen = sheetView.findViewById(R.id.btnDialogPreviewFullscreen);
        View btnClose = sheetView.findViewById(R.id.btnDialogPreviewClose);

        tvFileName.setText(file.name);
        tvFileSize.setText(FileUtils.formatStorageSize(this, file.size));
        tvStatus.setText(file.statusReason);

        String mimeType = null;
        if ("content".equalsIgnoreCase(file.uri.getScheme())) {
            try {
                mimeType = getContentResolver().getType(file.uri);
            } catch (Exception ignored) {}
        }
        String ext = "";
        if (file.name != null) {
            int dot = file.name.lastIndexOf('.');
            if (dot >= 0) ext = file.name.substring(dot + 1).toLowerCase(Locale.US);
        }
        if (mimeType == null) {
            mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }
        if (mimeType == null) mimeType = "application/octet-stream";

        boolean isImage = (mimeType != null && mimeType.startsWith("image/"))
                || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")
                || ext.equals("webp") || ext.equals("heic") || ext.equals("bmp") || ext.equals("gif");

        boolean isVideo = (mimeType != null && mimeType.startsWith("video/"))
                || ext.equals("mp4") || ext.equals("mkv") || ext.equals("webm") || ext.equals("mov");

        boolean isText = (mimeType != null && mimeType.startsWith("text/"))
                || ext.equals("txt") || ext.equals("json") || ext.equals("xml")
                || ext.equals("csv") || ext.equals("md") || ext.equals("log");

        if (isImage) {
            Bitmap bmp = ThumbnailLoader.decodeSampledBitmapFromUri(this, file.uri, 1080, 1920);
            if (bmp != null) {
                ivImage.setImageBitmap(bmp);
                ivImage.setVisibility(View.VISIBLE);
                layoutGeneric.setVisibility(View.GONE);
                scrollText.setVisibility(View.GONE);
            } else {
                ivImage.setVisibility(View.GONE);
                layoutGeneric.setVisibility(View.VISIBLE);
                scrollText.setVisibility(View.GONE);
                ivGenericIcon.setImageResource(R.drawable.ic_lucide_image);
                tvGenericType.setText("Image (" + ext.toUpperCase(Locale.US) + ")");
            }
        } else if (isVideo) {
            Bitmap frame = ThumbnailLoader.extractVideoFrame(this, file.uri, 720, 720);
            if (frame != null) {
                ivImage.setImageBitmap(frame);
                ivImage.setVisibility(View.VISIBLE);
                layoutGeneric.setVisibility(View.GONE);
                scrollText.setVisibility(View.GONE);
            } else {
                ivImage.setVisibility(View.GONE);
                layoutGeneric.setVisibility(View.VISIBLE);
                scrollText.setVisibility(View.GONE);
                ivGenericIcon.setImageResource(R.drawable.ic_lucide_video);
                tvGenericType.setText("Video (" + ext.toUpperCase(Locale.US) + ")");
            }
        } else if (isText) {
            try (InputStream is = getContentResolver().openInputStream(file.uri);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                int lines = 0;
                while ((line = br.readLine()) != null && lines < 100) {
                    sb.append(line).append("\n");
                    lines++;
                }
                tvText.setText(sb.toString());
                scrollText.setVisibility(View.VISIBLE);
                ivImage.setVisibility(View.GONE);
                layoutGeneric.setVisibility(View.GONE);
            } catch (Exception e) {
                ivImage.setVisibility(View.GONE);
                layoutGeneric.setVisibility(View.VISIBLE);
                scrollText.setVisibility(View.GONE);
                ivGenericIcon.setImageResource(R.drawable.ic_lucide_file_text);
                tvGenericType.setText("Text File");
            }
        } else if ("pdf".equals(ext) || "application/pdf".equalsIgnoreCase(mimeType)) {
            ivImage.setVisibility(View.GONE);
            scrollText.setVisibility(View.GONE);
            layoutGeneric.setVisibility(View.VISIBLE);
            ivGenericIcon.setImageResource(R.drawable.ic_lucide_file_text);
            tvGenericType.setText("PDF Document");
        } else {
            ivImage.setVisibility(View.GONE);
            scrollText.setVisibility(View.GONE);
            layoutGeneric.setVisibility(View.VISIBLE);
            ivGenericIcon.setImageResource(R.drawable.ic_lucide_file);
            tvGenericType.setText(ext.isEmpty() ? "Binary File" : ext.toUpperCase(Locale.US) + " File");
        }

        final String finalMimeType = mimeType;
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            int currentPos = stagedFiles.indexOf(file);
            if (currentPos >= 0) {
                removeStagedFile(currentPos);
            }
        });

        btnFullscreen.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, FileViewerActivity.class);
            intent.putExtra(FileViewerActivity.EXTRA_FILE_NAME, file.name);
            intent.putExtra(FileViewerActivity.EXTRA_FILE_SIZE, file.size);
            intent.putExtra(FileViewerActivity.EXTRA_CONTENT_TYPE, finalMimeType);
            intent.putExtra(FileViewerActivity.EXTRA_LOCAL_URI, file.uri.toString());
            intent.setDataAndType(file.uri, finalMimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        });

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }
        View btnBottomClose = sheetView.findViewById(R.id.btnDialogPreviewBottomClose);
        if (btnBottomClose != null) {
            btnBottomClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private long calculateBatchSize() {
        long sum = 0;
        for (StagedFile f : stagedFiles) {
            if (f.status != StagingStatus.DUPLICATE_EXCLUDED) {
                sum += f.size;
            }
        }
        return sum;
    }

    private void updateQuotaMetrics() {
        long batchSize = calculateBatchSize();
        long totalAfterBatch = usedStorageBytes + batchSize;

        // Vault Capacity (10 GB)
        int vaultPercent = (int) Math.min(100, (totalAfterBatch * 100) / AUTHORITATIVE_CAPACITY_BYTES);
        progressVaultCapacity.setProgress(vaultPercent);
        tvVaultCapacityText.setText(FileUtils.formatStorageSize(this, totalAfterBatch) + " / " +
                FileUtils.formatStorageSize(this, AUTHORITATIVE_CAPACITY_BYTES));

        // Batch Size (1 GB)
        int batchPercent = (int) Math.min(100, (batchSize * 100) / MAX_BATCH_BYTES);
        progressBatchSize.setProgress(batchPercent);
        tvBatchSizeText.setText(FileUtils.formatStorageSize(this, batchSize) + " / " +
                FileUtils.formatStorageSize(this, MAX_BATCH_BYTES));

        // Valid ready count
        int readyCount = 0;
        for (StagedFile f : stagedFiles) {
            if (f.status == StagingStatus.READY) {
                readyCount++;
            }
        }

        tvValidFilesCount.setText(readyCount + " files ready");
        tvUploadStatusMsg.setText("Batch size: " + FileUtils.formatStorageSize(this, batchSize));
        btnUploadValidFiles.setEnabled(readyCount > 0 && !isUploading);

        layoutStagingEmpty.setVisibility(stagedFiles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void startStreamingUpload() {
        if (isUploading) return;

        List<StagedFile> duplicates = new ArrayList<>();
        for (StagedFile f : stagedFiles) {
            if (f.status == StagingStatus.DUPLICATE_EXCLUDED) {
                duplicates.add(f);
            }
        }

        if (!duplicates.isEmpty()) {
            showDuplicateReviewDialog(duplicates);
            return;
        }

        executeUpload();
    }

    private void showDuplicateReviewDialog(List<StagedFile> duplicates) {
        StringBuilder sb = new StringBuilder();
        sb.append("The following file(s) are duplicates of existing vault items or other staged files:\n\n");
        for (StagedFile d : duplicates) {
            sb.append("• ").append(d.name).append("\n");
        }
        sb.append("\nRemove duplicates to proceed with uploading remaining files.");

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Duplicate Files (" + duplicates.size() + ")")
                .setMessage(sb.toString().trim())
                .setPositiveButton("Remove Duplicates", (dialog, which) -> {
                    for (StagedFile dup : duplicates) {
                        int pos = stagedFiles.indexOf(dup);
                        if (pos >= 0) {
                            stagedFiles.remove(pos);
                            adapter.notifyItemRemoved(pos);
                        }
                    }
                    updateQuotaMetrics();
                    Toast.makeText(this, "Duplicates removed", Toast.LENGTH_SHORT).show();
                    executeUpload();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeUpload() {
        List<StagedFile> toUpload = new ArrayList<>();
        List<TransferItem> transferItems = new ArrayList<>();

        for (StagedFile f : stagedFiles) {
            if (f.status == StagingStatus.READY) {
                toUpload.add(f);
                TransferItem item = new TransferItem(
                        null,
                        TransferType.UPLOAD,
                        f.name,
                        f.uri,
                        null,
                        f.size
                );
                f.transferId = item.getId();
                transferItems.add(item);
            }
        }

        if (toUpload.isEmpty()) {
            Toast.makeText(this, "No valid files ready to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        isUploading = true;
        btnUploadValidFiles.setEnabled(false);
        progressOverallUpload.setVisibility(View.VISIBLE);
        progressOverallUpload.setIndeterminate(true);
        tvUploadStatusMsg.setText("Preparing encrypted upload...");

        for (StagedFile f : toUpload) {
            f.status = StagingStatus.UPLOADING;
            f.statusReason = "Queued for upload...";
        }
        adapter.notifyDataSetChanged();

        Toast.makeText(this, "Uploading " + toUpload.size() + " files in background", Toast.LENGTH_SHORT).show();
        AuditLogger.log(this, "Upload Started", "SUCCESS", "Queued " + toUpload.size() + " files for encrypted upload");
        TransferManager.getInstance(this).enqueueUploadItems("Upload (" + toUpload.size() + " files)", transferItems);
    }

    @Override
    public void onTransferStateChanged(TransferItem item) {
        if (item.getType() != TransferType.UPLOAD) return;
        for (int i = 0; i < stagedFiles.size(); i++) {
            StagedFile f = stagedFiles.get(i);
            if ((f.transferId != null && f.transferId.equals(item.getId())) || f.name.equals(item.getFileName())) {
                f.transferId = item.getId();
                if (item.getState() == TransferState.UPLOADING) {
                    f.status = StagingStatus.UPLOADING;
                    f.statusReason = "Encrypting & Uploading...";
                } else if (item.getState() == TransferState.COMPLETED) {
                    f.status = StagingStatus.COMPLETED;
                    f.statusReason = "Uploaded (AES-256-GCM)";
                    f.progress = 100;
                } else if (item.getState() == TransferState.FAILED) {
                    f.status = StagingStatus.FAILED;
                    f.statusReason = item.getErrorMessage() != null ? item.getErrorMessage() : "Upload failed";
                } else if (item.getState() == TransferState.CANCELLED) {
                    f.status = StagingStatus.FAILED;
                    f.statusReason = "Cancelled";
                }
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onTransferProgress(TransferItem item) {
        if (item.getType() != TransferType.UPLOAD) return;
        for (int i = 0; i < stagedFiles.size(); i++) {
            StagedFile f = stagedFiles.get(i);
            if ((f.transferId != null && f.transferId.equals(item.getId())) || f.name.equals(item.getFileName())) {
                f.progress = item.getProgressPercentage();
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onBatchProgress(TransferBatch batch) {
        if (batch.getType() != TransferType.UPLOAD) return;
        int overall = batch.getOverallProgress();
        if (overall > 0) {
            progressOverallUpload.setIndeterminate(false);
            progressOverallUpload.setProgress(overall);
        } else {
            progressOverallUpload.setIndeterminate(true);
        }
        tvUploadStatusMsg.setText("Uploading " + overall + "% • " + batch.getCompletedCount() + "/" + batch.getTotalCount() + " completed");
    }

    @Override
    public void onBatchCompleted(TransferBatch batch) {
        if (batch.getType() != TransferType.UPLOAD) return;
        isUploading = false;
        progressOverallUpload.setIndeterminate(false);
        progressOverallUpload.setVisibility(View.GONE);
        if (batch.getState() == TransferState.CANCELLED) {
            tvUploadStatusMsg.setText("Upload batch cancelled");
            Toast.makeText(this, "Batch upload cancelled", Toast.LENGTH_SHORT).show();
        } else if (batch.getFailedCount() > 0) {
            tvUploadStatusMsg.setText("Completed with " + batch.getFailedCount() + " error(s)");
            Toast.makeText(this, "Upload completed with " + batch.getFailedCount() + " error(s)", Toast.LENGTH_LONG).show();
        } else {
            tvUploadStatusMsg.setText("Upload completed successfully!");
            AuditLogger.log(this, "Upload Completed", "SUCCESS", "Uploaded batch (" + batch.getTotalCount() + " files)");
            AuditLogger.log(this, "File Moved/Uploaded to Vault", "SUCCESS", "Batch indexed into vault");
            Toast.makeText(this, "Uploading completed", Toast.LENGTH_LONG).show();
        }
        updateQuotaMetrics();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TransferManager.getInstance(this).unregisterListener(this);
        backgroundExecutor.shutdown();
    }

    private static class StagingAdapter extends RecyclerView.Adapter<StagingAdapter.ViewHolder> {
        private final List<StagedFile> items;
        private final OnItemActionListener actionListener;

        interface OnItemActionListener {
            void onRemove(int position);
            void onPreview(int position);
        }

        StagingAdapter(List<StagedFile> items, OnItemActionListener actionListener) {
            this.items = items;
            this.actionListener = actionListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staging_file, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StagedFile file = items.get(position);
            Context ctx = holder.itemView.getContext();

            holder.tvStagingFileName.setText(file.name);
            holder.tvStagingFileSize.setText(FileUtils.formatStorageSize(ctx, file.size));
            holder.tvStagingStatus.setText(file.statusReason);

            ThumbnailLoader.loadStagingThumbnail(ctx, file.uri, file.name, holder.ivStagingThumbnail, holder.ivStagingIcon);

            // Preview clicks: both the dedicated Preview button and tapping the card
            holder.btnPreviewStagingFile.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    actionListener.onPreview(pos);
                }
            });
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    actionListener.onPreview(pos);
                }
            });

            switch (file.status) {
                case READY:
                    holder.tvStagingStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_connected));
                    holder.progressStagingFile.setVisibility(View.GONE);
                    holder.btnPreviewStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setVisibility(View.VISIBLE);
                    holder.btnRemoveStagingFile.setIconResource(R.drawable.ic_lucide_trash_2);
                    holder.btnRemoveStagingFile.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_error)));
                    holder.btnRemoveStagingFile.setContentDescription("Delete staged file");
                    holder.btnRemoveStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setOnClickListener(v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            actionListener.onRemove(pos);
                        }
                    });
                    break;
                case DUPLICATE_EXCLUDED:
                    holder.tvStagingStatus.setTextColor(ContextCompat.getColor(ctx, R.color.vault_unencrypted));
                    holder.progressStagingFile.setVisibility(View.GONE);
                    holder.btnPreviewStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setVisibility(View.VISIBLE);
                    holder.btnRemoveStagingFile.setIconResource(R.drawable.ic_lucide_trash_2);
                    holder.btnRemoveStagingFile.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_error)));
                    holder.btnRemoveStagingFile.setContentDescription("Delete staged file");
                    holder.btnRemoveStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setOnClickListener(v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            actionListener.onRemove(pos);
                        }
                    });
                    break;
                case EXCEEDS_QUOTA:
                case FAILED:
                    holder.tvStagingStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_error));
                    holder.progressStagingFile.setVisibility(View.GONE);
                    holder.btnPreviewStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setVisibility(View.VISIBLE);
                    holder.btnRemoveStagingFile.setIconResource(R.drawable.ic_lucide_trash_2);
                    holder.btnRemoveStagingFile.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_error)));
                    holder.btnRemoveStagingFile.setContentDescription("Delete staged file");
                    holder.btnRemoveStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setOnClickListener(v -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            actionListener.onRemove(pos);
                        }
                    });
                    break;
                case UPLOADING:
                    holder.tvStagingStatus.setTextColor(ContextCompat.getColor(ctx, R.color.cv_primary));
                    holder.progressStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setEnabled(true);
                    if (file.progress > 0) {
                        holder.progressStagingFile.setIndeterminate(false);
                        holder.progressStagingFile.setProgress(file.progress);
                    } else {
                        holder.progressStagingFile.setIndeterminate(true);
                    }
                    holder.btnRemoveStagingFile.setVisibility(View.VISIBLE);
                    holder.btnRemoveStagingFile.setIconResource(R.drawable.ic_lucide_x);
                    holder.btnRemoveStagingFile.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_error)));
                    holder.btnRemoveStagingFile.setContentDescription("Cancel upload");
                    holder.btnRemoveStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setOnClickListener(v -> {
                        if (file.transferId != null) {
                            TransferManager.getInstance(ctx).cancelTransfer(file.transferId);
                        }
                        file.status = StagingStatus.FAILED;
                        file.statusReason = "Cancelled";
                        notifyItemChanged(holder.getBindingAdapterPosition());
                    });
                    break;
                case COMPLETED:
                    holder.tvStagingStatus.setTextColor(ContextCompat.getColor(ctx, R.color.vault_encrypted));
                    holder.progressStagingFile.setVisibility(View.GONE);
                    holder.btnPreviewStagingFile.setVisibility(View.VISIBLE);
                    holder.btnPreviewStagingFile.setEnabled(true);
                    holder.btnRemoveStagingFile.setVisibility(View.VISIBLE);
                    holder.btnRemoveStagingFile.setIconResource(R.drawable.ic_lucide_check_circle);
                    holder.btnRemoveStagingFile.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_connected)));
                    holder.btnRemoveStagingFile.setContentDescription("Upload completed");
                    holder.btnRemoveStagingFile.setEnabled(false);
                    holder.btnRemoveStagingFile.setOnClickListener(null);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivStagingIcon;
            final ImageView ivStagingThumbnail;
            final TextView tvStagingFileName;
            final TextView tvStagingFileSize;
            final TextView tvStagingStatus;
            final MaterialButton btnPreviewStagingFile;
            final MaterialButton btnRemoveStagingFile;
            final LinearProgressIndicator progressStagingFile;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivStagingIcon = itemView.findViewById(R.id.ivStagingIcon);
                ivStagingThumbnail = itemView.findViewById(R.id.ivStagingThumbnail);
                tvStagingFileName = itemView.findViewById(R.id.tvStagingFileName);
                tvStagingFileSize = itemView.findViewById(R.id.tvStagingFileSize);
                tvStagingStatus = itemView.findViewById(R.id.tvStagingStatus);
                btnPreviewStagingFile = itemView.findViewById(R.id.btnPreviewStagingFile);
                btnRemoveStagingFile = itemView.findViewById(R.id.btnRemoveStagingFile);
                progressStagingFile = itemView.findViewById(R.id.progressStagingFile);
            }
        }
    }
}
