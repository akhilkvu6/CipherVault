package com.ciphervault.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.Button;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements FilesAdapter.OnDownloadClickListener {

    private static final String PREF_SEARCH_HISTORY = "CipherVaultSearchHistory";
    private static final String KEY_HISTORY = "queries";

    private EditText etSearchQuery;
    private View layoutRecentHeader;
    private ChipGroup chipGroupRecentSearches;
    private Chip chipFilterEncrypted;
    private Chip chipFilterLarge;
    private RecyclerView rvSearchResults;
    private TextView tvSearchEmpty;

    private FilesAdapter adapter;
    private ApiService apiService;
    private final List<StoredFile> allFiles = new ArrayList<>();
    private SharedPreferences historyPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        apiService = ApiClient.getApiService(requireContext());
        historyPrefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);

        etSearchQuery = view.findViewById(R.id.etSearchQuery);
        layoutRecentHeader = view.findViewById(R.id.layoutRecentHeader);
        chipGroupRecentSearches = view.findViewById(R.id.chipGroupRecentSearches);
        chipFilterEncrypted = view.findViewById(R.id.chipFilterEncrypted);
        chipFilterLarge = view.findViewById(R.id.chipFilterLarge);
        Button btnClearSearchHistory = view.findViewById(R.id.btnClearSearchHistory);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);

        setupRecyclerView();
        setupSearchInput(btnClearSearchHistory);
        setupFilterChips();
        setupRecentHistory();
        loadFiles();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new FilesAdapter(this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupFilterChips() {
        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> {
            if (etSearchQuery != null) {
                filterFiles(etSearchQuery.getText().toString());
            }
        };

        if (chipFilterEncrypted != null) {
            chipFilterEncrypted.setOnCheckedChangeListener(filterListener);
        }
        if (chipFilterLarge != null) {
            chipFilterLarge.setOnCheckedChangeListener(filterListener);
        }
    }

    private void setupSearchInput(Button btnClearSearchHistory) {
        if (etSearchQuery != null) {
            etSearchQuery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFiles(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String query = s.toString().trim();
                    if (!query.isEmpty() && query.length() >= 3) {
                        saveSearchQuery(query);
                    }
                }
            });
        }

        if (btnClearSearchHistory != null) {
            btnClearSearchHistory.setOnClickListener(v -> clearSearchHistory());
        }
    }

    private void loadFiles() {
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    allFiles.clear();
                    allFiles.addAll(response.body());
                    if (etSearchQuery != null) {
                        filterFiles(etSearchQuery.getText().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Failed to load files for search", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterFiles(String query) {
        String trimmed = query != null ? query.trim().toLowerCase() : "";
        boolean onlyEncrypted = chipFilterEncrypted != null && chipFilterEncrypted.isChecked();
        boolean onlyLarge = chipFilterLarge != null && chipFilterLarge.isChecked();

        List<StoredFile> matched = new ArrayList<>();

        for (StoredFile file : allFiles) {
            if (onlyEncrypted && !file.isEncrypted()) {
                continue;
            }
            if (onlyLarge && file.getFileSize() < (1024 * 1024)) { // < 1MB
                continue;
            }

            if (trimmed.isEmpty()) {
                matched.add(file);
            } else {
                String name = file.getFilename().toLowerCase();
                String mime = file.getContentType().toLowerCase();
                String hash = file.getSha256Hash() != null ? file.getSha256Hash().toLowerCase() : "";

                if (name.contains(trimmed) || mime.contains(trimmed) || hash.contains(trimmed)) {
                    matched.add(file);
                }
            }
        }

        adapter.setFiles(matched);

        if (tvSearchEmpty != null) {
            tvSearchEmpty.setVisibility(matched.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void setupRecentHistory() {
        if (chipGroupRecentSearches == null) return;
        chipGroupRecentSearches.removeAllViews();

        Set<String> queries = historyPrefs.getStringSet(KEY_HISTORY, new HashSet<>());
        if (queries.isEmpty()) {
            if (layoutRecentHeader != null) layoutRecentHeader.setVisibility(View.GONE);
            chipGroupRecentSearches.setVisibility(View.GONE);
            return;
        }

        if (layoutRecentHeader != null) layoutRecentHeader.setVisibility(View.VISIBLE);
        chipGroupRecentSearches.setVisibility(View.VISIBLE);

        for (String q : queries) {
            Chip chip = new Chip(requireContext());
            chip.setText(q);
            chip.setCloseIconVisible(true);
            chip.setClickable(true);

            chip.setOnClickListener(v -> {
                if (etSearchQuery != null) {
                    etSearchQuery.setText(q);
                    etSearchQuery.setSelection(q.length());
                }
            });

            chip.setOnCloseIconClickListener(v -> {
                removeSearchQuery(q);
                setupRecentHistory();
            });

            chipGroupRecentSearches.addView(chip);
        }
    }

    private void saveSearchQuery(String query) {
        Set<String> queries = new HashSet<>(historyPrefs.getStringSet(KEY_HISTORY, new HashSet<>()));
        queries.add(query);
        historyPrefs.edit().putStringSet(KEY_HISTORY, queries).apply();
        setupRecentHistory();
    }

    private void removeSearchQuery(String query) {
        Set<String> queries = new HashSet<>(historyPrefs.getStringSet(KEY_HISTORY, new HashSet<>()));
        queries.remove(query);
        historyPrefs.edit().putStringSet(KEY_HISTORY, queries).apply();
    }

    private void clearSearchHistory() {
        historyPrefs.edit().remove(KEY_HISTORY).apply();
        setupRecentHistory();
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