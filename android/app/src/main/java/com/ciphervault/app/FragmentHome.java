package com.ciphervault.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.ciphervault.app.transfer.TransferBatch;
import com.ciphervault.app.transfer.TransferItem;
import com.ciphervault.app.transfer.TransferListener;
import com.ciphervault.app.transfer.TransferManager;
import com.ciphervault.app.transfer.TransferType;

public class FragmentHome extends Fragment implements TransferListener {

    private static final long TOTAL_QUOTA_BYTES = SessionManager.DEFAULT_LIMIT; // 10 GB

    private TextView tvHomeUsername;
    private TextView tvStorageUsage;
    private LinearProgressIndicator progressStorage;
    private TextView tvSummaryFileCount;
    private TextView tvSummaryStorage;
    private TextView tvSummaryEncryptedCount;

    private TextView tvCatImagesCountSize;
    private TextView tvCatVideosCountSize;
    private TextView tvCatDocsCountSize;
    private TextView tvCatAudioCountSize;
    private TextView tvCatOtherCountSize;

    private RecyclerView rvRecentUploads;
    private View layoutHomeEmpty;

    private RecentFilesAdapter recentAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    private View cardHomeProfileAvatar;
    private ImageView ivHomeDefaultAvatar;
    private ImageView ivHomeProfilePhoto;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        tvHomeUsername = view.findViewById(R.id.tvHomeUsername);
        cardHomeProfileAvatar = view.findViewById(R.id.cardHomeProfileAvatar);
        ivHomeDefaultAvatar = view.findViewById(R.id.ivHomeDefaultAvatar);
        ivHomeProfilePhoto = view.findViewById(R.id.ivHomeProfilePhoto);

        if (cardHomeProfileAvatar != null) {
            cardHomeProfileAvatar.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(3);
                }
            });
        }

        tvStorageUsage = view.findViewById(R.id.tvStorageUsage);
        progressStorage = view.findViewById(R.id.progressStorage);

        tvSummaryFileCount = view.findViewById(R.id.tvSummaryFileCount);
        tvSummaryStorage = view.findViewById(R.id.tvSummaryStorage);
        tvSummaryEncryptedCount = view.findViewById(R.id.tvSummaryEncryptedCount);

        tvCatImagesCountSize = view.findViewById(R.id.tvCatImagesCountSize);
        tvCatVideosCountSize = view.findViewById(R.id.tvCatVideosCountSize);
        tvCatDocsCountSize = view.findViewById(R.id.tvCatDocsCountSize);
        tvCatAudioCountSize = view.findViewById(R.id.tvCatAudioCountSize);
        tvCatOtherCountSize = view.findViewById(R.id.tvCatOtherCountSize);

        rvRecentUploads = view.findViewById(R.id.rvRecentUploads);
        layoutHomeEmpty = view.findViewById(R.id.layoutHomeEmpty);

        MaterialCardView cardQuickUpload = view.findViewById(R.id.cardQuickUpload);
        MaterialCardView cardQuickCamera = view.findViewById(R.id.cardQuickCamera);
        MaterialCardView cardViewFiles = view.findViewById(R.id.cardViewFiles);
        MaterialCardView cardQuickTransfers = view.findViewById(R.id.cardQuickTransfers);
        Button btnUploadFirstFile = view.findViewById(R.id.btnUploadFirstFile);
        MaterialCardView cardVaultStorage = view.findViewById(R.id.cardVaultStorage);

        if (cardVaultStorage != null) {
            cardVaultStorage.setOnClickListener(v -> showStorageDetails());
        }

        if (cardQuickUpload != null) {
            cardQuickUpload.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UploadStagingActivity.class);
                startActivity(intent);
            });
        }

        if (cardQuickCamera != null) {
            cardQuickCamera.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CameraActivity.class);
                startActivity(intent);
            });
        }

        if (cardViewFiles != null) {
            cardViewFiles.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(1);
                }
            });
        }

        if (cardQuickTransfers != null) {
            cardQuickTransfers.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(2);
                }
            });
        }

        // Category Cards Click Listeners
        View cardCatImages = view.findViewById(R.id.cardCatImages);
        if (cardCatImages != null) {
            cardCatImages.setOnClickListener(v -> navigateToCategory("PHOTOS"));
        }

        View cardCatVideos = view.findViewById(R.id.cardCatVideos);
        if (cardCatVideos != null) {
            cardCatVideos.setOnClickListener(v -> navigateToCategory("VIDEOS"));
        }

        View cardCatDocs = view.findViewById(R.id.cardCatDocuments);
        if (cardCatDocs != null) {
            cardCatDocs.setOnClickListener(v -> navigateToCategory("DOCUMENTS"));
        }

        View cardCatAudio = view.findViewById(R.id.cardCatAudio);
        if (cardCatAudio != null) {
            cardCatAudio.setOnClickListener(v -> navigateToCategory("AUDIO"));
        }

        View cardCatOther = view.findViewById(R.id.cardCatOther);
        if (cardCatOther != null) {
            cardCatOther.setOnClickListener(v -> navigateToCategory("OTHER"));
        }

        if (btnUploadFirstFile != null) {
            btnUploadFirstFile.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), UploadStagingActivity.class);
                startActivity(intent);
            });
        }

        setupGreeting();
        setupRecentRecyclerView();
        loadDashboardData();

        return view;
    }

    private void navigateToCategory(String category) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateToVaultCategory(category);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            TransferManager.getInstance(requireContext()).registerListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null) {
            TransferManager.getInstance(requireContext()).unregisterListener(this);
        }
    }

    @Override
    public void onTransferStateChanged(TransferItem item) {}

    @Override
    public void onTransferProgress(TransferItem item) {}

    @Override
    public void onBatchProgress(TransferBatch batch) {}

    @Override
    public void onBatchCompleted(TransferBatch batch) {
        if (batch != null && batch.getType() == TransferType.UPLOAD) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        loadDashboardData();
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupGreeting();
        loadHomeAvatar(null);
        loadDashboardData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            setupGreeting();
            loadHomeAvatar(null);
            loadDashboardData();
        }
    }

    private void loadHomeAvatar(UserProfileResponse profile) {
        if (!isAdded() || getContext() == null) return;
        String email = profile != null ? profile.getEmail() : (sessionManager != null ? sessionManager.getEmail() : null);
        if (ivHomeProfilePhoto != null) {
            ProfilePhotoHelper.loadProfilePhotoInto(requireContext(), email, apiService, ivHomeProfilePhoto, ivHomeDefaultAvatar);
        }
    }

    private void setupGreeting() {
        if (tvHomeUsername != null) {
            String name = sessionManager.getName();
            if (name == null || name.trim().isEmpty()) {
                name = sessionManager.getUsername();
            }
            if (name == null || name.trim().isEmpty()) {
                name = "User";
            }
            tvHomeUsername.setText("Welcome back, " + name);
        }
    }

    private void setupRecentRecyclerView() {
        recentAdapter = new RecentFilesAdapter(this::handleFileClick, this::handleDeleteClick);
        rvRecentUploads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentUploads.setAdapter(recentAdapter);
    }

    private void showStorageDetails() {
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    StorageDetailsBottomSheet.show(requireContext(), response.body());
                } else {
                    Toast.makeText(requireContext(), "Failed to load storage details", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load storage details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadDashboardData() {
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    updateProfileMetrics(response.body());
                    loadHomeAvatar(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {}
        });

        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call,
                                   @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    updateRecentUploads(response.body());
                    updateCategoryMetrics(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryMetrics(List<StoredFile> files) {
        if (!isAdded()) return;
        int imgCount = 0; long imgSize = 0;
        int vidCount = 0; long vidSize = 0;
        int docCount = 0; long docSize = 0;
        int audCount = 0; long audSize = 0;
        int othCount = 0; long othSize = 0;

        if (files != null) {
            for (StoredFile file : files) {
                long size = file.getFileSize();
                StoredFile.FileCategory cat = file.getCategory();
                switch (cat) {
                    case IMAGES:
                        imgCount++;
                        imgSize += size;
                        break;
                    case VIDEOS:
                        vidCount++;
                        vidSize += size;
                        break;
                    case PDFS:
                    case DOCUMENTS:
                        docCount++;
                        docSize += size;
                        break;
                    case AUDIO:
                        audCount++;
                        audSize += size;
                        break;
                    case OTHER:
                    default:
                        othCount++;
                        othSize += size;
                        break;
                }
            }
        }

        if (tvCatImagesCountSize != null) {
            tvCatImagesCountSize.setText(imgCount + " files • " + FileUtils.formatStorageSize(requireContext(), imgSize));
        }
        if (tvCatVideosCountSize != null) {
            tvCatVideosCountSize.setText(vidCount + " files • " + FileUtils.formatStorageSize(requireContext(), vidSize));
        }
        if (tvCatDocsCountSize != null) {
            tvCatDocsCountSize.setText(docCount + " files • " + FileUtils.formatStorageSize(requireContext(), docSize));
        }
        if (tvCatAudioCountSize != null) {
            tvCatAudioCountSize.setText(audCount + " files • " + FileUtils.formatStorageSize(requireContext(), audSize));
        }
        if (tvCatOtherCountSize != null) {
            tvCatOtherCountSize.setText(othCount + " files • " + FileUtils.formatStorageSize(requireContext(), othSize));
        }
    }

    private void updateProfileMetrics(UserProfileResponse profile) {
        if (profile == null || !isAdded()) return;

        setupGreeting();

        long used = profile.getUsedStorage();
        long limit = profile.getStorageLimit() > 0 ? profile.getStorageLimit() : TOTAL_QUOTA_BYTES;

        if (tvSummaryFileCount != null) {
            tvSummaryFileCount.setText(String.valueOf(profile.getFileCount()));
        }

        if (tvSummaryStorage != null) {
            tvSummaryStorage.setText(FileUtils.formatStorageSize(requireContext(), used));
        }

        if (tvSummaryEncryptedCount != null) {
            tvSummaryEncryptedCount.setText(String.valueOf(profile.getEncryptedCount()));
        }

        if (tvStorageUsage != null) {
            String formattedUsed = FileUtils.formatStorageSize(requireContext(), used);
            String formattedLimit = FileUtils.formatStorageSize(requireContext(), limit);
            tvStorageUsage.setText(formattedUsed + " / " + formattedLimit);
        }

        if (progressStorage != null) {
            int progressPercent = (int) Math.min(100, Math.max(0, (used * 100) / limit));
            progressStorage.setProgress(progressPercent);
        }
    }

    private void updateRecentUploads(List<StoredFile> files) {
        if (files == null || files.isEmpty()) {
            if (layoutHomeEmpty != null) layoutHomeEmpty.setVisibility(View.VISIBLE);
            if (rvRecentUploads != null) rvRecentUploads.setVisibility(View.GONE);
            recentAdapter.setFiles(new ArrayList<>());
        } else {
            if (layoutHomeEmpty != null) layoutHomeEmpty.setVisibility(View.GONE);
            if (rvRecentUploads != null) rvRecentUploads.setVisibility(View.VISIBLE);

            int count = Math.min(5, files.size());
            List<StoredFile> recents = new ArrayList<>(files.subList(0, count));
            recentAdapter.setFiles(recents);
        }
    }

    private void handleFileClick(StoredFile file) {
        if (file == null || !isAdded()) return;

        FileDetailsBottomSheet.show(requireContext(), file,
                this::downloadSingleFile,
                this::handleDeleteClick);
    }

    private void downloadSingleFile(StoredFile file) {
        if (file == null || file.getId() == null || getContext() == null) return;

        if (!file.isEncrypted()) {
            Toast.makeText(requireContext(), "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
            TransferManager.getInstance(requireContext()).enqueueDownload(file, false);
            AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + file.getOriginalFilename());
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
        TextView tvDownloadFileName = dialogView.findViewById(R.id.tvDownloadFileName);
        android.widget.CompoundButton cbDecryptOption = dialogView.findViewById(R.id.cbDecryptOption);

        if (tvDownloadFileName != null) {
            tvDownloadFileName.setText("Save \"" + file.getOriginalFilename() + "\" to Downloads folder.");
        }
        if (cbDecryptOption != null) {
            cbDecryptOption.setChecked(true);
        }

        androidx.appcompat.app.AlertDialog downloadDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Download File")
                .setView(dialogView)
                .setPositiveButton("Download", (dialog, which) -> {
                    boolean decrypt = cbDecryptOption == null || cbDecryptOption.isChecked();
                    Toast.makeText(requireContext(), "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
                    TransferManager.getInstance(requireContext()).enqueueDownload(file, decrypt);
                    AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + file.getOriginalFilename() + " (decrypt=" + decrypt + ")");
                })
                .setNegativeButton("Cancel", null)
                .show();

        Button downloadPosBtn = downloadDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (downloadPosBtn != null) {
            downloadPosBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
        }
    }

    private void handleDeleteClick(StoredFile file) {
        if (file == null || file.getId() == null || !isAdded()) return;

        androidx.appcompat.app.AlertDialog deleteDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_file_dialog_title)
                .setMessage(String.format(getString(R.string.delete_file_dialog_msg), file.getOriginalFilename()))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), R.string.delete_file_success, Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.delete_file_failed) + " (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), getString(R.string.delete_file_failed) + ": " + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();

        Button deletePosBtn = deleteDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (deletePosBtn != null) {
            deletePosBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
        }
    }

    private static class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {
        interface OnItemClickListener { void onItemClick(StoredFile file); }
        interface OnItemDeleteListener { void onItemDelete(StoredFile file); }

        private final List<StoredFile> list = new ArrayList<>();
        private final OnItemClickListener listener;
        private final OnItemDeleteListener deleteListener;

        RecentFilesAdapter(OnItemClickListener listener, OnItemDeleteListener deleteListener) {
            this.listener = listener;
            this.deleteListener = deleteListener;
        }

        void setFiles(List<StoredFile> files) {
            list.clear();
            if (files != null) list.addAll(files);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoredFile file = list.get(position);
            Context context = holder.itemView.getContext();

            holder.tvFileName.setText(file.getOriginalFilename());

            String typeLabel;
            switch (file.getCategory()) {
                case IMAGES: typeLabel = "Image"; break;
                case VIDEOS: typeLabel = "Video"; break;
                case PDFS: typeLabel = "PDF"; break;
                default: typeLabel = "File"; break;
            }

            holder.tvFileSize.setText(typeLabel + " • " + FileUtils.formatStorageSize(context, file.getFileSize()));

            if (file.isEncrypted()) {
                int encColor = ThemeManager.getEncryptedColor(context);
                holder.tvEncryptionBadge.setText("Encrypted");
                holder.tvEncryptionBadge.setTextColor(encColor);
                holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lucide_lock, 0, 0, 0);
                holder.tvEncryptionBadge.setCompoundDrawableTintList(ColorStateList.valueOf(encColor));
                holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvEncryptionBadge.setText("Uploaded");
                holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
                holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            }

            switch (file.getCategory()) {
                case IMAGES: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_image); break;
                case VIDEOS: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_video); break;
                case PDFS: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file_text); break;
                case DOCUMENTS: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file_text); break;
                case AUDIO: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_music); break;
                case ARCHIVES: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_archive); break;
                default: holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file); break;
            }

            if (file.hasPreview() && file.getId() != null) {
                ThumbnailLoader.loadThumbnail(context, file.getId(), holder.ivThumbnail, holder.ivFileIcon,
                        file.getCategory() == StoredFile.FileCategory.VIDEOS ? holder.ivVideoBadge : null);
            } else {
                holder.ivThumbnail.setImageDrawable(null);
                holder.ivThumbnail.setVisibility(View.GONE);
                holder.ivFileIcon.setVisibility(View.VISIBLE);
                holder.ivVideoBadge.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < list.size() && listener != null) {
                    listener.onItemClick(list.get(pos));
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivFileIcon;
            final ImageView ivThumbnail;
            final ImageView ivVideoBadge;
            final TextView tvFileName;
            final TextView tvFileSize;
            final TextView tvEncryptionBadge;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
                ivVideoBadge = itemView.findViewById(R.id.ivVideoBadge);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
            }
        }
    }
}
