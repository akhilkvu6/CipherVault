package com.ciphervault.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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

    private View layoutSuggestionsHeader;
    private View scrollSuggestions;
    private ChipGroup chipGroupSuggestions;
    private final Handler suggestionDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable suggestionDebounceRunnable;

    private ChipGroup chipGroupCategory;
    private Chip chipAll;
    private Chip chipImages;
    private Chip chipVideos;
    private Chip chipPdfs;
    private Chip chipOther;

    private RecyclerView rvFiles;
    private FilesAdapter adapter;

    // Inline Download Panel
    private MaterialCardView cardDownloadPanel;
    private ImageView ivDownloadFileIcon;
    private TextView tvDownloadFileName;
    private TextView tvDownloadFileSize;
    private TextView tvDownloadEncryptionBadge;
    private MaterialCardView cardDownloadDecryptOption;
    private MaterialSwitch switchDownloadDecrypt;
    private View layoutDownloadProgress;
    private TextView tvDownloadStatus;
    private TextView tvDownloadPercent;
    private LinearProgressIndicator progressDownload;
    private View layoutDownloadActions;
    private MaterialButton btnCancelDownload;
    private MaterialButton btnStartDownload;
    private StoredFile pendingDownloadFile;
    private Call<ResponseBody> activeDownloadCall;

    private ApiService apiService;
    private SharedPreferences historyPrefs;
    private final List<StoredFile> allFiles = new ArrayList<>();
    private StoredFile.FileCategory currentCategory = StoredFile.FileCategory.ALL;
    private String currentSearchQuery = "";
    private FileSortOption currentSortOption = FileSortOption.NAME_ASC;
    private final Handler searchDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchDebounceRunnable;

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

        layoutSuggestionsHeader = view.findViewById(R.id.layoutSuggestionsHeader);
        scrollSuggestions = view.findViewById(R.id.scrollSuggestions);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);

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
        setupDownloadPanel(view);

        loadFiles();
        return view;
    }

    private void applyInitialCategoryFromArgs() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("EXTRA_INITIAL_CATEGORY")) {
            applyCategoryFilter(args.getString("EXTRA_INITIAL_CATEGORY"));
        }
    }

    public void applyCategoryFilter(String cat) {
        if (cat == null) return;
        if (getArguments() == null) {
            setArguments(new Bundle());
        }
        getArguments().putString("EXTRA_INITIAL_CATEGORY", cat);

        if ("IMAGES".equalsIgnoreCase(cat) && chipImages != null) {
            chipImages.setChecked(true);
        } else if ("VIDEOS".equalsIgnoreCase(cat) && chipVideos != null) {
            chipVideos.setChecked(true);
        } else if ("PDFS".equalsIgnoreCase(cat) && chipPdfs != null) {
            chipPdfs.setChecked(true);
        } else if ("OTHER".equalsIgnoreCase(cat) && chipOther != null) {
            chipOther.setChecked(true);
        } else if (chipAll != null) {
            chipAll.setChecked(true);
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
                    currentSearchQuery = s != null ? s.toString().trim() : "";
                    filterAndDisplayFiles();
                    scheduleServerSearch();
                    scheduleSuggestionsLookup(currentSearchQuery);
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
            scheduleServerSearch();
        });
    }

    private String getHistoryKey() {
        if (getContext() == null) return KEY_HISTORY;
        try {
            SessionManager session = new SessionManager(requireContext());
            String email = session.getEmail();
            return KEY_HISTORY + "_" + (email != null ? email.replace("@", "_").replace(".", "_") : "default");
        } catch (Exception e) {
            return KEY_HISTORY;
        }
    }

    private List<String> getSearchHistoryList() {
        String key = getHistoryKey();
        String raw = historyPrefs.getString(key, null);
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            try {
                org.json.JSONArray arr = new org.json.JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }
            } catch (Exception ignored) {}
        }
        return list;
    }

    private void setupRecentHistory() {
        if (chipGroupRecentSearches == null || !isAdded()) return;
        chipGroupRecentSearches.removeAllViews();

        List<String> queries = getSearchHistoryList();
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
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        List<String> list = getSearchHistoryList();
        list.remove(query);
        list.add(0, query);
        while (list.size() > 10) {
            list.remove(list.size() - 1);
        }
        org.json.JSONArray arr = new org.json.JSONArray(list);
        historyPrefs.edit().putString(getHistoryKey(), arr.toString()).apply();
        setupRecentHistory();
    }

    private void removeSearchQuery(String query) {
        List<String> list = getSearchHistoryList();
        list.remove(query);
        org.json.JSONArray arr = new org.json.JSONArray(list);
        historyPrefs.edit().putString(getHistoryKey(), arr.toString()).apply();
    }

    private void clearSearchHistory() {
        historyPrefs.edit().remove(getHistoryKey()).apply();
        setupRecentHistory();
    }

    private void loadInitialSuggestions() {
        apiService.getSuggestions(null).enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    displaySuggestions(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                // Silently ignore suggestion errors
            }
        });
    }

    private void scheduleSuggestionsLookup(String prefix) {
        suggestionDebounceHandler.removeCallbacksAndMessages(null);
        suggestionDebounceRunnable = () -> {
            String p = (prefix != null && !prefix.trim().isEmpty()) ? prefix.trim() : null;
            apiService.getSuggestions(p).enqueue(new Callback<List<String>>() {
                @Override
                public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null) {
                        displaySuggestions(response.body());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {}
            });
        };
        suggestionDebounceHandler.postDelayed(suggestionDebounceRunnable, 250);
    }

    private void displaySuggestions(List<String> suggestions) {
        if (chipGroupSuggestions == null || !isAdded()) return;
        chipGroupSuggestions.removeAllViews();

        if (suggestions == null || suggestions.isEmpty()) {
            if (layoutSuggestionsHeader != null) layoutSuggestionsHeader.setVisibility(View.GONE);
            if (scrollSuggestions != null) scrollSuggestions.setVisibility(View.GONE);
            return;
        }

        if (layoutSuggestionsHeader != null) layoutSuggestionsHeader.setVisibility(View.VISIBLE);
        if (scrollSuggestions != null) scrollSuggestions.setVisibility(View.VISIBLE);

        for (String suggestion : suggestions) {
            Chip chip = new Chip(requireContext());
            chip.setText(suggestion);
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setChipIconResource(R.drawable.ic_nav_search);
            chip.setIconStartPadding(12f);

            chip.setOnClickListener(v -> {
                if (etFileSearch != null) {
                    etFileSearch.setText(suggestion);
                    etFileSearch.setSelection(suggestion.length());
                }
                saveSearchQuery(suggestion);
            });

            chipGroupSuggestions.addView(chip);
        }
    }

    public void loadFiles() {
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
                    loadInitialSuggestions();
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
        renderFileList(allFiles, true);
    }

    private void scheduleServerSearch() {
        searchDebounceHandler.removeCallbacksAndMessages(null);
        searchDebounceRunnable = () -> performServerSearch(currentSearchQuery, currentCategory);
        searchDebounceHandler.postDelayed(searchDebounceRunnable, 300);
    }

    private void performServerSearch(String query, StoredFile.FileCategory category) {
        if (!isAdded()) return;

        if (query.isEmpty() && category == StoredFile.FileCategory.ALL) {
            filterAndDisplayFiles();
            return;
        }

        String catParam = (category == StoredFile.FileCategory.ALL) ? null : category.name();
        String queryParam = query.isEmpty() ? null : query;

        apiService.searchFiles(queryParam, catParam).enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    renderFileList(response.body(), false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                // Retain local client-side filtered view on network error
            }
        });
    }

    private void renderFileList(List<StoredFile> sourceList, boolean applyTextAndCategoryFilters) {
        if (!isAdded()) return;

        boolean onlyEncrypted = chipFilterEncrypted != null && chipFilterEncrypted.isChecked();
        boolean onlyLarge = chipFilterLarge != null && chipFilterLarge.isChecked();

        List<StoredFile> filteredList = new ArrayList<>();

        for (StoredFile file : sourceList) {
            if (applyTextAndCategoryFilters) {
                boolean matchesCategory = (currentCategory == StoredFile.FileCategory.ALL || file.getCategory() == currentCategory);
                if (!matchesCategory) continue;

                if (!currentSearchQuery.isEmpty()) {
                    String name = file.getFilename().toLowerCase();
                    String mime = file.getContentType().toLowerCase();
                    String hash = file.getSha256Hash() != null ? file.getSha256Hash().toLowerCase() : "";
                    String q = currentSearchQuery.toLowerCase();
                    boolean matchesMetadata = false;
                    if (file.getMetadata() != null) {
                        FileMetadataDTO m = file.getMetadata();
                        if ((m.getCameraMake() != null && m.getCameraMake().toLowerCase().contains(q)) ||
                                (m.getCameraModel() != null && m.getCameraModel().toLowerCase().contains(q)) ||
                                (m.getResolution() != null && m.getResolution().toLowerCase().contains(q)) ||
                                (m.getVideoCodec() != null && m.getVideoCodec().toLowerCase().contains(q)) ||
                                (m.getAudioCodec() != null && m.getAudioCodec().toLowerCase().contains(q)) ||
                                (m.getArtist() != null && m.getArtist().toLowerCase().contains(q)) ||
                                (m.getAuthor() != null && m.getAuthor().toLowerCase().contains(q)) ||
                                (m.getTitle() != null && m.getTitle().toLowerCase().contains(q)) ||
                                (m.getGenre() != null && m.getGenre().toLowerCase().contains(q))) {
                            matchesMetadata = true;
                        }
                    }
                    if (!name.contains(q) && !mime.contains(q) && !hash.contains(q) && !matchesMetadata) {
                        continue;
                    }
                }
            }

            if (onlyEncrypted && !file.isEncrypted()) continue;
            if (onlyLarge && file.getFileSize() < (1024 * 1024)) continue;

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

    private void setupDownloadPanel(View view) {
        cardDownloadPanel = view.findViewById(R.id.cardDownloadPanel);
        ivDownloadFileIcon = view.findViewById(R.id.ivDownloadFileIcon);
        tvDownloadFileName = view.findViewById(R.id.tvDownloadFileName);
        tvDownloadFileSize = view.findViewById(R.id.tvDownloadFileSize);
        tvDownloadEncryptionBadge = view.findViewById(R.id.tvDownloadEncryptionBadge);
        cardDownloadDecryptOption = view.findViewById(R.id.cardDownloadDecryptOption);
        switchDownloadDecrypt = view.findViewById(R.id.switchDownloadDecrypt);
        layoutDownloadProgress = view.findViewById(R.id.layoutDownloadProgress);
        tvDownloadStatus = view.findViewById(R.id.tvDownloadStatus);
        tvDownloadPercent = view.findViewById(R.id.tvDownloadPercent);
        progressDownload = view.findViewById(R.id.progressDownload);
        layoutDownloadActions = view.findViewById(R.id.layoutDownloadActions);
        btnCancelDownload = view.findViewById(R.id.btnCancelDownload);
        btnStartDownload = view.findViewById(R.id.btnStartDownload);

        if (btnCancelDownload != null) {
            btnCancelDownload.setOnClickListener(v -> cancelOrCloseDownload());
        }
        if (btnStartDownload != null) {
            btnStartDownload.setOnClickListener(v -> {
                if (pendingDownloadFile != null) {
                    boolean decrypt = pendingDownloadFile.isEncrypted() && (switchDownloadDecrypt != null && switchDownloadDecrypt.isChecked());
                    executeDownload(pendingDownloadFile, decrypt);
                }
            });
        }
    }

    private void showDownloadPanel(StoredFile file) {
        if (file == null || cardDownloadPanel == null || !isAdded()) return;

        this.pendingDownloadFile = file;

        // Reset state
        cardDownloadPanel.setVisibility(View.VISIBLE);
        if (layoutDownloadProgress != null) layoutDownloadProgress.setVisibility(View.GONE);
        if (layoutDownloadActions != null) layoutDownloadActions.setVisibility(View.VISIBLE);
        if (btnStartDownload != null) btnStartDownload.setEnabled(true);
        if (btnCancelDownload != null) {
            btnCancelDownload.setEnabled(true);
            btnCancelDownload.setText(R.string.btn_cancel);
        }

        // File info
        if (tvDownloadFileName != null) {
            tvDownloadFileName.setText(file.getOriginalFilename());
        }
        if (tvDownloadFileSize != null) {
            tvDownloadFileSize.setText(FileUtils.formatStorageSize(file.getFileSize()));
        }

        // Icon
        if (ivDownloadFileIcon != null) {
            switch (file.getCategory()) {
                case IMAGES:
                    ivDownloadFileIcon.setImageResource(R.drawable.ic_file_image);
                    break;
                case VIDEOS:
                    ivDownloadFileIcon.setImageResource(R.drawable.ic_file_video);
                    break;
                case PDFS:
                    ivDownloadFileIcon.setImageResource(R.drawable.ic_file_pdf);
                    break;
                default:
                    ivDownloadFileIcon.setImageResource(R.drawable.ic_file_general);
                    break;
            }
        }

        // Encryption Badge & Decrypt Switch
        if (file.isEncrypted()) {
            if (tvDownloadEncryptionBadge != null) {
                tvDownloadEncryptionBadge.setText(R.string.encrypted_badge_label);
                tvDownloadEncryptionBadge.setTextColor(ThemeManager.getEncryptedColor(requireContext()));
            }
            if (cardDownloadDecryptOption != null) {
                cardDownloadDecryptOption.setVisibility(View.VISIBLE);
            }
            if (switchDownloadDecrypt != null) {
                switchDownloadDecrypt.setChecked(true);
            }
        } else {
            if (tvDownloadEncryptionBadge != null) {
                tvDownloadEncryptionBadge.setText(R.string.unencrypted_badge_label);
                tvDownloadEncryptionBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.vault_unencrypted));
            }
            if (cardDownloadDecryptOption != null) {
                cardDownloadDecryptOption.setVisibility(View.GONE);
            }
            if (switchDownloadDecrypt != null) {
                switchDownloadDecrypt.setChecked(false);
            }
        }

        if (rvFiles != null) {
            rvFiles.smoothScrollToPosition(0);
        }
    }

    private void cancelOrCloseDownload() {
        if (activeDownloadCall != null && !activeDownloadCall.isCanceled()) {
            activeDownloadCall.cancel();
            activeDownloadCall = null;
        }
        if (cardDownloadPanel != null) {
            cardDownloadPanel.setVisibility(View.GONE);
        }
        pendingDownloadFile = null;
    }

    @Override
    public void onDownloadClick(StoredFile file) {
        if (file == null || file.getId() == null) return;
        showDownloadPanel(file);
    }

    private void executeDownload(StoredFile file, boolean decrypt) {
        String filename = file.getOriginalFilename();
        if (file.isEncrypted() && !decrypt && !filename.toLowerCase().endsWith(".encrypted")) {
            filename = filename + ".encrypted";
        }

        final String finalFilename = filename;

        if (layoutDownloadProgress != null) layoutDownloadProgress.setVisibility(View.VISIBLE);
        if (btnStartDownload != null) btnStartDownload.setEnabled(false);
        if (btnCancelDownload != null) btnCancelDownload.setEnabled(true);
        if (tvDownloadStatus != null) tvDownloadStatus.setText(decrypt ? "Preparing and decrypting..." : "Preparing...");
        if (tvDownloadPercent != null) tvDownloadPercent.setText("0%");
        if (progressDownload != null) {
            progressDownload.setIndeterminate(false);
            progressDownload.setProgress(0);
        }

        activeDownloadCall = apiService.downloadFile(file.getId(), decrypt);
        activeDownloadCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    if (tvDownloadStatus != null) tvDownloadStatus.setText(decrypt ? "Decrypting & Downloading..." : "Downloading...");
                    saveFileToDisk(finalFilename, file.getFileSize(), response.body());
                } else {
                    if (tvDownloadStatus != null) tvDownloadStatus.setText("Download failed (HTTP " + response.code() + ")");
                    if (btnStartDownload != null) btnStartDownload.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (call.isCanceled()) {
                    if (tvDownloadStatus != null) tvDownloadStatus.setText("Download cancelled");
                } else {
                    if (tvDownloadStatus != null) tvDownloadStatus.setText("Download error: " + t.getLocalizedMessage());
                    if (btnStartDownload != null) btnStartDownload.setEnabled(true);
                }
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

    private void saveFileToDisk(String filename, long totalBytesExpected, ResponseBody body) {
        FileUtils.saveResponseBodyToDownloads(
                requireContext(),
                filename,
                totalBytesExpected,
                body,
                percent -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if (progressDownload != null) progressDownload.setProgress(percent);
                            if (tvDownloadPercent != null) tvDownloadPercent.setText(percent + "%");
                        });
                    }
                },
                new FileUtils.DownloadCallback() {
                    @Override
                    public void onSuccess(File targetFile) {
                        activeDownloadCall = null;
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (progressDownload != null) progressDownload.setProgress(100);
                                if (tvDownloadPercent != null) tvDownloadPercent.setText("100%");
                                if (tvDownloadStatus != null) tvDownloadStatus.setText("Saved to Download/CipherVault");
                                if (btnCancelDownload != null) {
                                    btnCancelDownload.setText(R.string.btn_close);
                                    btnCancelDownload.setEnabled(true);
                                }
                                Toast.makeText(requireContext(), "Saved to Download/CipherVault: " + targetFile.getName(), Toast.LENGTH_LONG).show();
                                searchDebounceHandler.postDelayed(() -> {
                                    if (isAdded() && cardDownloadPanel != null) {
                                        cardDownloadPanel.setVisibility(View.GONE);
                                    }
                                }, 2500);
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        activeDownloadCall = null;
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (tvDownloadStatus != null) tvDownloadStatus.setText("Failed saving: " + e.getMessage());
                                if (btnStartDownload != null) btnStartDownload.setEnabled(true);
                                Toast.makeText(requireContext(), "Failed saving: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        searchDebounceHandler.removeCallbacksAndMessages(null);
        suggestionDebounceHandler.removeCallbacksAndMessages(null);
    }
}
