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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilesFragment extends Fragment implements FilesAdapter.OnDownloadClickListener, FilesAdapter.OnDeleteClickListener {

    private static final String PREF_SEARCH_HISTORY = "CipherVaultSearchHistory";
    private static final String KEY_HISTORY = "queries";

    private TextView tvFileCount;
    private View layoutEmptyState;
    private TextView tvEmptyMessage;
    private EditText etFileSearch;
    private Chip chipFilterEncrypted;
    private Chip chipFilterLarge;
    private View layoutRecentHeader;
    private ChipGroup chipGroupRecentSearches;
    private MaterialButton btnClearSearchHistory;

    private ChipGroup chipGroupCategory;
    private Chip chipAll;
    private Chip chipImages;
    private Chip chipVideos;
    private Chip chipPdfs;
    private Chip chipOther;

    private RecyclerView rvFiles;
    private FilesAdapter adapter;

    private ApiService apiService;
    private SharedPreferences historyPrefs;
    private final List<StoredFile> allFiles = new ArrayList<>();
    private StoredFile.FileCategory currentCategory = StoredFile.FileCategory.ALL;
    private String currentSearchQuery = "";
    private FileSortOption currentSortOption = FileSortOption.NAME_ASC;

    public static FilesFragment newInstance(String initialCategory) {
        FilesFragment fragment = new FilesFragment();
        if (initialCategory != null) {
            Bundle args = new Bundle();
            args.putString("EXTRA_INITIAL_CATEGORY", initialCategory);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);

        apiService = ApiClient.getApiService(requireContext());
        historyPrefs = requireContext().getSharedPreferences(PREF_SEARCH_HISTORY, Context.MODE_PRIVATE);
        currentSortOption = CipherVaultPreferences.getFileSortOption(requireContext());

        tvFileCount = view.findViewById(R.id.tvFileCount);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        etFileSearch = view.findViewById(R.id.etFileSearch);
        chipFilterEncrypted = view.findViewById(R.id.chipFilterEncrypted);
        chipFilterLarge = view.findViewById(R.id.chipFilterLarge);
        layoutRecentHeader = view.findViewById(R.id.layoutRecentHeader);
        chipGroupRecentSearches = view.findViewById(R.id.chipGroupRecentSearches);
        btnClearSearchHistory = view.findViewById(R.id.btnClearSearchHistory);

        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        chipAll = view.findViewById(R.id.chipAll);
        chipImages = view.findViewById(R.id.chipImages);
        chipVideos = view.findViewById(R.id.chipVideos);
        chipPdfs = view.findViewById(R.id.chipPdfs);
        chipOther = view.findViewById(R.id.chipOther);
        rvFiles = view.findViewById(R.id.rvFiles);

        MaterialButton btnSortFiles = view.findViewById(R.id.btnSortFiles);
        if (btnSortFiles != null) {
            btnSortFiles.setOnClickListener(v -> showSortDialog());
        }

        setupRecyclerView();
        setupSearchAndFilters();
        setupCategoryChips();
        applyInitialCategoryFromArgs();
        setupRecentHistory();

        loadFiles();
        return view;
    }

    private void applyInitialCategoryFromArgs() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("EXTRA_INITIAL_CATEGORY")) {
            String cat = args.getString("EXTRA_INITIAL_CATEGORY");
            if (cat != null) {
                if ("IMAGES".equalsIgnoreCase(cat) && chipImages != null) {
                    chipImages.setChecked(true);
                } else if ("VIDEOS".equalsIgnoreCase(cat) && chipVideos != null) {
                    chipVideos.setChecked(true);
                } else if ("PDFS".equalsIgnoreCase(cat) && chipPdfs != null) {
                    chipPdfs.setChecked(true);
                } else if ("OTHER".equalsIgnoreCase(cat) && chipOther != null) {
                    chipOther.setChecked(true);
                }
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new FilesAdapter(this, this);
        rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFiles.setAdapter(adapter);
    }

    private void setupSearchAndFilters() {
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
                public void afterTextChanged(Editable s) {
                    String query = s != null ? s.toString().trim() : "";
                    if (!query.isEmpty() && query.length() >= 3) {
                        saveSearchQuery(query);
                    }
                }
            });
        }

        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> filterAndDisplayFiles();

        if (chipFilterEncrypted != null) {
            chipFilterEncrypted.setOnCheckedChangeListener(filterListener);
        }
        if (chipFilterLarge != null) {
            chipFilterLarge.setOnCheckedChangeListener(filterListener);
        }

        if (btnClearSearchHistory != null) {
            btnClearSearchHistory.setOnClickListener(v -> clearSearchHistory());
        }
    }

    private void setupCategoryChips() {
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
                if (etFileSearch != null) {
                    etFileSearch.setText(q);
                    etFileSearch.setSelection(q.length());
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

    private void showSortDialog() {
        String[] sortLabels = new String[] {
                getString(R.string.sort_name_asc),
                getString(R.string.sort_name_desc),
                getString(R.string.sort_date_oldest),
                getString(R.string.sort_date_newest),
                getString(R.string.sort_size_smallest),
                getString(R.string.sort_size_largest)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sort_dialog_title)
                .setSingleChoiceItems(sortLabels, currentSortOption.ordinal(), (dialog, which) -> {
                    FileSortOption[] options = FileSortOption.values();
                    if (which >= 0 && which < options.length) {
                        FileSortOption selected = options[which];
                        if (selected != currentSortOption) {
                            currentSortOption = selected;
                            CipherVaultPreferences.saveFileSortOption(requireContext(), currentSortOption);
                            filterAndDisplayFiles();
                        }
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private void filterAndDisplayFiles() {
        boolean onlyEncrypted = chipFilterEncrypted != null && chipFilterEncrypted.isChecked();
        boolean onlyLarge = chipFilterLarge != null && chipFilterLarge.isChecked();

        List<StoredFile> filteredList = new ArrayList<>();

        for (StoredFile file : allFiles) {
            boolean matchesCategory = (currentCategory == StoredFile.FileCategory.ALL || file.getCategory() == currentCategory);
            if (!matchesCategory) continue;

            if (onlyEncrypted && !file.isEncrypted()) continue;
            if (onlyLarge && file.getFileSize() < (1024 * 1024)) continue;

            if (!currentSearchQuery.isEmpty()) {
                String name = file.getFilename().toLowerCase();
                String mime = file.getContentType().toLowerCase();
                String hash = file.getSha256Hash() != null ? file.getSha256Hash().toLowerCase() : "";
                if (!name.contains(currentSearchQuery) && !mime.contains(currentSearchQuery) && !hash.contains(currentSearchQuery)) {
                    continue;
                }
            }

            filteredList.add(file);
        }

        Collections.sort(filteredList, FileComparator.getComparator(currentSortOption));

        adapter.setFiles(filteredList);

        int count = filteredList.size();
        if (tvFileCount != null) {
            String countText = count + (count == 1 ? " file" : " files");
            tvFileCount.setText(countText);
        }

        boolean isEmpty = filteredList.isEmpty();
        if (layoutEmptyState != null) {
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        } else if (tvEmptyMessage != null) {
            tvEmptyMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDownloadClick(StoredFile file) {
        if (file == null || file.getId() == null) return;

        if (file.isEncrypted()) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
            CompoundButton cbDecrypt = dialogView.findViewById(R.id.cbDecrypt);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.download_dialog_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.btn_download, (dialog, which) -> {
                        boolean decrypt = cbDecrypt == null || cbDecrypt.isChecked();
                        executeDownload(file, decrypt);
                    })
                    .setNegativeButton(R.string.btn_close, null)
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

    @Override
    public void onDeleteClick(StoredFile file) {
        if (file == null || file.getId() == null || !isAdded()) return;

        String filename = file.getOriginalFilename();
        String message = String.format(getString(R.string.delete_file_dialog_msg), filename);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_file_dialog_title)
                .setMessage(message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> executeDelete(file))
                .setNegativeButton(R.string.btn_close, null)
                .show();
    }

    private void executeDelete(StoredFile file) {
        if (!isAdded()) return;

        apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    ThumbnailLoader.remove(file.getId());
                    Toast.makeText(requireContext(), R.string.delete_file_success, Toast.LENGTH_SHORT).show();
                    loadFiles();
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
