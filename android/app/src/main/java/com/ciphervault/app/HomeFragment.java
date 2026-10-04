package com.ciphervault.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final long TOTAL_QUOTA_BYTES = SessionManager.DEFAULT_LIMIT; // 1 GB / 1073741824L

    private TextView tvHomeUsername;
    private TextView tvStorageUsage;
    private LinearProgressIndicator progressStorage;
    private TextView tvSummaryFileCount;
    private TextView tvSummaryStorage;
    private TextView tvSummaryEncryptedCount;

    private RecyclerView rvRecentUploads;
    private View layoutHomeEmpty;

    private RecentFilesAdapter recentAdapter;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        apiService = ApiClient.getApiService(requireContext());

        tvHomeUsername = view.findViewById(R.id.tvHomeUsername);
        tvStorageUsage = view.findViewById(R.id.tvStorageUsage);
        progressStorage = view.findViewById(R.id.progressStorage);

        tvSummaryFileCount = view.findViewById(R.id.tvSummaryFileCount);
        tvSummaryStorage = view.findViewById(R.id.tvSummaryStorage);
        tvSummaryEncryptedCount = view.findViewById(R.id.tvSummaryEncryptedCount);

        rvRecentUploads = view.findViewById(R.id.rvRecentUploads);
        layoutHomeEmpty = view.findViewById(R.id.layoutHomeEmpty);

        MaterialCardView cardQuickUpload = view.findViewById(R.id.cardQuickUpload);
        MaterialCardView cardViewFiles = view.findViewById(R.id.cardViewFiles);
        Button btnUploadFirstFile = view.findViewById(R.id.btnUploadFirstFile);
        MaterialCardView cardVaultStorage = view.findViewById(R.id.cardVaultStorage);

        if (cardVaultStorage != null) {
            cardVaultStorage.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(1);
                }
            });
        }
        if (cardQuickUpload != null) {
            cardQuickUpload.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(2);
                }
            });
        }
        if (cardViewFiles != null) {
            cardViewFiles.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(1);
                }
            });
        }
        if (btnUploadFirstFile != null) {
            btnUploadFirstFile.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToTab(2);
                }
            });
        }

        setupGreeting();
        setupRecentRecyclerView();
        loadDashboardData();

        return view;
    }

    private void setupGreeting() {
        if (tvHomeUsername != null) {
            String username = sessionManager.getUsername();
            if (username == null || username.trim().isEmpty()) {
                username = "User";
            }
            tvHomeUsername.setText(username);
        }
    }

    private void setupRecentRecyclerView() {
        recentAdapter = new RecentFilesAdapter(this::handleFileClick, this::handleDeleteClick);
        rvRecentUploads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentUploads.setAdapter(recentAdapter);
    }

    public void loadDashboardData() {
        // 1. Fetch server-authoritative profile metrics
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call, @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    updateProfileMetrics(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                // Ignore fallback to getFiles
            }
        });

        // 2. Fetch recent files list
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<StoredFile> files = response.body();
                    updateRecentUploads(files);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load dashboard files", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfileMetrics(UserProfileResponse profile) {
        if (profile == null || !isAdded()) return;

        if (tvHomeUsername != null && !profile.getUsername().isEmpty()) {
            tvHomeUsername.setText(profile.getUsername());
        }

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

            // Display top 5 most recent files
            int count = Math.min(5, files.size());
            List<StoredFile> recents = new ArrayList<>(files.subList(0, count));
            recentAdapter.setFiles(recents);
        }
    }

    private void handleFileClick(StoredFile file) {
        if (file == null || !isAdded()) return;
        FileDetailsBottomSheet.show(requireContext(), file, this::handleDownloadClick, this::handleDeleteClick);
    }

    private void handleDownloadClick(StoredFile file) {
        if (file == null || file.getId() == null || !isAdded()) return;

        String filename = file.getOriginalFilename();
        Toast.makeText(requireContext(), "Downloading " + filename + "...", Toast.LENGTH_SHORT).show();

        apiService.downloadFile(file.getId(), true).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    saveFileToDisk(filename, response.body());
                } else {
                    Toast.makeText(requireContext(), "Download failed (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Download error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFileToDisk(String filename, ResponseBody body) {
        FileUtils.saveResponseBodyToDownloads(requireContext(), filename, body, new FileUtils.DownloadCallback() {
            @Override
            public void onSuccess(File savedFile) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Saved to Download/CipherVault: " + savedFile.getName(), Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed saving: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void handleDeleteClick(StoredFile file) {
        if (file == null || file.getId() == null || !isAdded()) return;

        String filename = file.getOriginalFilename();
        String message = String.format(getString(R.string.delete_file_dialog_msg), filename);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_file_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), R.string.delete_file_success, Toast.LENGTH_SHORT).show();
                                loadDashboardData();
                            } else {
                                Toast.makeText(requireContext(), getString(R.string.delete_file_failed) + " (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
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
    }

    // Dedicated Adapter for Recent Uploads
    private static class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {

        interface OnItemClickListener {
            void onItemClick(StoredFile file);
        }

        interface OnItemDeleteListener {
            void onItemDelete(StoredFile file);
        }

        private final List<StoredFile> list = new ArrayList<>();
        private final OnItemClickListener listener;
        private final OnItemDeleteListener deleteListener;

        RecentFilesAdapter(OnItemClickListener listener, OnItemDeleteListener deleteListener) {
            this.listener = listener;
            this.deleteListener = deleteListener;
        }

        void setFiles(List<StoredFile> files) {
            list.clear();
            if (files != null) {
                list.addAll(files);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_file, parent, false);
            return new ViewHolder(v);
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
                holder.tvEncryptionBadge.setText("AES-256-GCM");
                holder.tvEncryptionBadge.setTextColor(encColor);
                holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0);
                holder.tvEncryptionBadge.setCompoundDrawableTintList(ColorStateList.valueOf(encColor));
                holder.tvEncryptionBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density));
                holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvEncryptionBadge.setText("Unencrypted");
                holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
                holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            }

            switch (file.getCategory()) {
                case IMAGES:
                    holder.ivFileIcon.setImageResource(R.drawable.ic_file_image);
                    break;
                case VIDEOS:
                    holder.ivFileIcon.setImageResource(R.drawable.ic_file_video);
                    break;
                case PDFS:
                    holder.ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
                    break;
                default:
                    holder.ivFileIcon.setImageResource(R.drawable.ic_file_general);
                    break;
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
                if (listener != null) listener.onItemClick(file);
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