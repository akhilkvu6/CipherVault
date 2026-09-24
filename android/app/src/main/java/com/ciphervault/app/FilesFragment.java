package com.ciphervault.app;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

public class FilesFragment extends Fragment implements FilesAdapter.OnDownloadClickListener {

    private TextView tvFileCount;
    private TextView tvEmptyMessage;
    private EditText etFileSearch;
    private ChipGroup chipGroupCategory;
    private Chip chipAll;
    private Chip chipImages;
    private Chip chipVideos;
    private Chip chipPdfs;
    private Chip chipOther;
    private RecyclerView rvFiles;
    private FilesAdapter adapter;

    private ApiService apiService;
    private final List<StoredFile> allFiles = new ArrayList<>();
    private StoredFile.FileCategory currentCategory = StoredFile.FileCategory.ALL;
    private String currentSearchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        tvFileCount = view.findViewById(R.id.tvFileCount);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        etFileSearch = view.findViewById(R.id.etFileSearch);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipAll = view.findViewById(R.id.chipAll);
        chipImages = view.findViewById(R.id.chipImages);
        chipVideos = view.findViewById(R.id.chipVideos);
        chipPdfs = view.findViewById(R.id.chipPdfs);
        chipOther = view.findViewById(R.id.chipOther);
        rvFiles = view.findViewById(R.id.rvFiles);

        apiService = ApiClient.getApiService(requireContext());

        setupRecyclerView();
        setupSearchInput();
        setupFilterChips();

        loadFiles();
        return view;
    }

    private void setupRecyclerView() {
        adapter = new FilesAdapter(this);
        rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFiles.setAdapter(adapter);
    }

    private void setupSearchInput() {
        if (etFileSearch != null) {
            etFileSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                    filterAndDisplayFiles();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void setupFilterChips() {
        if (chipGroupCategory == null) return;

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) {
                currentCategory = StoredFile.FileCategory.ALL;
            } else if (checkedId == R.id.chipImages) {
                currentCategory = StoredFile.FileCategory.IMAGES;
            } else if (checkedId == R.id.chipVideos) {
                currentCategory = StoredFile.FileCategory.VIDEOS;
            } else if (checkedId == R.id.chipPdfs) {
                currentCategory = StoredFile.FileCategory.PDFS;
            } else if (checkedId == R.id.chipOther) {
                currentCategory = StoredFile.FileCategory.OTHER;
            } else {
                currentCategory = StoredFile.FileCategory.ALL;
            }

            filterAndDisplayFiles();
        });
    }

    private void loadFiles() {
        if (tvFileCount != null) {
            tvFileCount.setText("Loading files...");
        }

        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    allFiles.clear();
                    allFiles.addAll(response.body());
                    updateCategoryChipCounts();
                    filterAndDisplayFiles();
                } else {
                    if (tvFileCount != null) {
                        tvFileCount.setText("Failed to load files (HTTP " + response.code() + ")");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (tvFileCount != null) {
                    tvFileCount.setText("Error loading files");
                }
                Toast.makeText(requireContext(), "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryChipCounts() {
        int all = allFiles.size();
        int images = 0, videos = 0, pdfs = 0, other = 0;

        for (StoredFile file : allFiles) {
            switch (file.getCategory()) {
                case IMAGES:
                    images++;
                    break;
                case VIDEOS:
                    videos++;
                    break;
                case PDFS:
                    pdfs++;
                    break;
                default:
                    other++;
                    break;
            }
        }

        if (chipAll != null) chipAll.setText("All (" + all + ")");
        if (chipImages != null) chipImages.setText("Images (" + images + ")");
        if (chipVideos != null) chipVideos.setText("Videos (" + videos + ")");
        if (chipPdfs != null) chipPdfs.setText("PDFs (" + pdfs + ")");
        if (chipOther != null) chipOther.setText("Other (" + other + ")");
    }

    private void filterAndDisplayFiles() {
        List<StoredFile> filteredList = new ArrayList<>();

        for (StoredFile file : allFiles) {
            boolean matchesCategory = (currentCategory == StoredFile.FileCategory.ALL || file.getCategory() == currentCategory);

            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String name = file.getFilename().toLowerCase();
                String mime = file.getContentType().toLowerCase();
                String hash = file.getSha256Hash() != null ? file.getSha256Hash().toLowerCase() : "";
                matchesSearch = name.contains(currentSearchQuery) || mime.contains(currentSearchQuery) || hash.contains(currentSearchQuery);
            }

            if (matchesCategory && matchesSearch) {
                filteredList.add(file);
            }
        }

        adapter.setFiles(filteredList);

        int count = filteredList.size();
        if (tvFileCount != null) {
            String countText = count + (count == 1 ? " file" : " files");
            tvFileCount.setText(countText);
        }

        if (tvEmptyMessage != null) {
            tvEmptyMessage.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDownloadClick(StoredFile file) {
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
}