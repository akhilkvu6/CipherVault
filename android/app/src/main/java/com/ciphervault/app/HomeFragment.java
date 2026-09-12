package com.ciphervault.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private TextView tvImagesUsage;
    private TextView tvVideosUsage;
    private TextView tvDocsUsage;
    private TextView tvOtherUsage;
    private RecyclerView rvRecentUploads;
    private TextView tvRecentEmpty;

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
        tvImagesUsage = view.findViewById(R.id.tvImagesUsage);
        tvVideosUsage = view.findViewById(R.id.tvVideosUsage);
        tvDocsUsage = view.findViewById(R.id.tvDocsUsage);
        tvOtherUsage = view.findViewById(R.id.tvOtherUsage);
        rvRecentUploads = view.findViewById(R.id.rvRecentUploads);
        tvRecentEmpty = view.findViewById(R.id.tvRecentEmpty);

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
        recentAdapter = new RecentFilesAdapter(file -> handleFileClick(file));
        rvRecentUploads.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentUploads.setAdapter(recentAdapter);
    }

    private void loadDashboardData() {
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<StoredFile> files = response.body();
                    updateStorageStats(files);
                    updateRecentUploads(files);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load dashboard stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStorageStats(List<StoredFile> files) {
        long totalUsed = 0L;
        long imagesUsed = 0L;
        long videosUsed = 0L;
        long docsUsed = 0L;
        long otherUsed = 0L;

        for (StoredFile file : files) {
            long size = file.getFileSize();
            totalUsed += size;

            switch (file.getCategory()) {
                case IMAGES:
                    imagesUsed += size;
                    break;
                case VIDEOS:
                    videosUsed += size;
                    break;
                case PDFS:
                    docsUsed += size;
                    break;
                default:
                    otherUsed += size;
                    break;
            }
        }

        if (tvStorageUsage != null) {
            String formattedUsed = Formatter.formatFileSize(requireContext(), totalUsed);
            String text = formattedUsed + " used of 1.0 GB";
            tvStorageUsage.setText(text);
        }

        if (progressStorage != null) {
            int percentage = (int) Math.min(100, (totalUsed * 100) / TOTAL_QUOTA_BYTES);
            progressStorage.setProgress(percentage);
        }

        if (tvImagesUsage != null) {
            tvImagesUsage.setText(Formatter.formatFileSize(requireContext(), imagesUsed));
        }
        if (tvVideosUsage != null) {
            tvVideosUsage.setText(Formatter.formatFileSize(requireContext(), videosUsed));
        }
        if (tvDocsUsage != null) {
            tvDocsUsage.setText(Formatter.formatFileSize(requireContext(), docsUsed));
        }
        if (tvOtherUsage != null) {
            tvOtherUsage.setText(Formatter.formatFileSize(requireContext(), otherUsed));
        }
    }

    private void updateRecentUploads(List<StoredFile> files) {
        List<StoredFile> recent = new ArrayList<>();
        int count = Math.min(5, files.size());
        for (int i = 0; i < count; i++) {
            recent.add(files.get(files.size() - 1 - i));
        }

        recentAdapter.setFiles(recent);

        if (tvRecentEmpty != null) {
            tvRecentEmpty.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void handleFileClick(StoredFile file) {
        if (file == null || file.getId() == null) return;

        if (file.isEncrypted()) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
            CompoundButton cbDecrypt = dialogView.findViewById(R.id.cbDecrypt);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Download File")
                    .setView(dialogView)
                    .setPositiveButton("Download", (dialog, which) -> {
                        boolean decrypt = cbDecrypt == null || cbDecrypt.isChecked();
                        executeDownload(file, decrypt);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            executeDownload(file, false);
        }
    }

    private void executeDownload(StoredFile file, boolean decrypt) {
        String filename = file.getOriginalFilename();
        if (file.isEncrypted() && !decrypt && !filename.toLowerCase().endsWith(".encrypted")) {
            filename = filename + ".encrypted";
        }

        final String finalFilename = filename;
        Toast.makeText(requireContext(), "Downloading " + finalFilename + "...", Toast.LENGTH_SHORT).show();

        apiService.downloadFile(file.getId(), decrypt).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    saveFileToDisk(finalFilename, response.body());
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
        new Thread(() -> {
            try {
                File cipherVaultDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        "CipherVault"
                );
                if (!cipherVaultDir.exists()) {
                    cipherVaultDir.mkdirs();
                }

                File targetFile = new File(cipherVaultDir, filename);
                OutputStream os = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CipherVault");
                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        os = requireContext().getContentResolver().openOutputStream(uri);
                    }
                }

                if (os == null) {
                    os = new FileOutputStream(targetFile);
                }

                try (InputStream is = body.byteStream();
                     OutputStream targetOs = os) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        targetOs.write(buffer, 0, read);
                    }
                    targetOs.flush();
                }

                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Saved to Download/CipherVault: " + targetFile.getName(), Toast.LENGTH_LONG).show()
                    );
                }
            } catch (Exception e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed saving: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    // Dedicated Adapter for Recent Uploads (NO Download button)
    private static class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {

        interface OnItemClickListener {
            void onItemClick(StoredFile file);
        }

        private final List<StoredFile> list = new ArrayList<>();
        private final OnItemClickListener listener;

        RecentFilesAdapter(OnItemClickListener listener) {
            this.listener = listener;
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
            holder.tvFileSize.setText(Formatter.formatFileSize(context, file.getFileSize()));

            if (file.isEncrypted()) {
                holder.tvEncryptionBadge.setText(R.string.encrypted_badge_label);
                holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_encrypted));
            } else {
                holder.tvEncryptionBadge.setText(R.string.unencrypted_badge_label);
                holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
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
            final TextView tvFileName;
            final TextView tvFileSize;
            final TextView tvEncryptionBadge;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
            }
        }
    }
}