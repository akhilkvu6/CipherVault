package com.ciphervault.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ciphervault.app.transfer.TransferManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Dedicated full-screen Metadata Search activity for CipherVault.
 * Provides a single unified search field matching indexed forensic metadata
 * (camera make/model, resolution, codecs, artist, author, document tags) without decrypting physical ciphertext.
 * Includes database-derived popular metadata suggestions and persistent recent searches.
 */
public class MetadataSearchActivity extends BaseActivity implements FilesAdapter.OnDownloadClickListener, FilesAdapter.OnDeleteClickListener {

    private ApiService apiService;
    private MaterialToolbar toolbarMetadataSearch;
    private LinearProgressIndicator progressMetadataLoading;
    private CircularProgressIndicator progressSuggestionsLoading;
    private TextInputEditText etMetadataSearchQuery;
    private View layoutPopularMetadata;
    private ChipGroup cgPopularMetadata;
    private View layoutRecentSearches;
    private ChipGroup cgRecentSearches;
    private TextView tvClearRecentSearches;
    private View layoutResultsHeader;
    private TextView tvResultsCount;
    private RecyclerView rvMetadataResults;
    private View layoutMetadataSearchEmpty;
    private TextView tvMetadataEmptyTitle;
    private TextView tvMetadataEmptyMessage;

    private FilesAdapter filesAdapter;
    private String currentQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata_search);

        apiService = ApiClient.getApiService(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchInput();
        loadPopularSuggestions();
        loadRecentSearches();

        // Check if an initial query was passed via Intent
        if (getIntent() != null && getIntent().hasExtra("query")) {
            String initialQuery = getIntent().getStringExtra("query");
            if (initialQuery != null && !initialQuery.trim().isEmpty()) {
                etMetadataSearchQuery.setText(initialQuery.trim());
                executeSearch(initialQuery.trim());
            }
        }
    }

    private void initViews() {
        toolbarMetadataSearch = findViewById(R.id.toolbarMetadataSearch);
        progressMetadataLoading = findViewById(R.id.progressMetadataLoading);
        progressSuggestionsLoading = findViewById(R.id.progressSuggestionsLoading);
        etMetadataSearchQuery = findViewById(R.id.etMetadataSearchQuery);
        layoutPopularMetadata = findViewById(R.id.layoutPopularMetadata);
        cgPopularMetadata = findViewById(R.id.cgPopularMetadata);
        layoutRecentSearches = findViewById(R.id.layoutRecentSearches);
        cgRecentSearches = findViewById(R.id.cgRecentSearches);
        tvClearRecentSearches = findViewById(R.id.tvClearRecentSearches);
        layoutResultsHeader = findViewById(R.id.layoutResultsHeader);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        rvMetadataResults = findViewById(R.id.rvMetadataResults);
        layoutMetadataSearchEmpty = findViewById(R.id.layoutMetadataSearchEmpty);
        tvMetadataEmptyTitle = findViewById(R.id.tvMetadataEmptyTitle);
        tvMetadataEmptyMessage = findViewById(R.id.tvMetadataEmptyMessage);
    }

    private void setupToolbar() {
        if (toolbarMetadataSearch != null) {
            toolbarMetadataSearch.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        if (rvMetadataResults != null) {
            rvMetadataResults.setLayoutManager(new LinearLayoutManager(this));
            filesAdapter = new FilesAdapter(this, this);
            rvMetadataResults.setAdapter(filesAdapter);
        }
    }

    private void setupSearchInput() {
        if (etMetadataSearchQuery == null) return;

        etMetadataSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etMetadataSearchQuery.getText() != null ? etMetadataSearchQuery.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    hideKeyboard();
                    executeSearch(q);
                }
                return true;
            }
            return false;
        });

        etMetadataSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                if (s == null || s.toString().trim().isEmpty()) {
                    currentQuery = "";
                    clearResultsView();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (tvClearRecentSearches != null) {
            tvClearRecentSearches.setOnClickListener(v -> {
                SearchHistoryManager.clearSearchHistory(this);
                loadRecentSearches();
            });
        }
    }

    private void loadPopularSuggestions() {
        if (progressSuggestionsLoading != null) {
            progressSuggestionsLoading.setVisibility(View.VISIBLE);
        }

        apiService.getSuggestions("").enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(@NonNull Call<List<String>> call, @NonNull Response<List<String>> response) {
                if (progressSuggestionsLoading != null) {
                    progressSuggestionsLoading.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    populateSuggestionChips(response.body());
                } else {
                    if (layoutPopularMetadata != null) {
                        layoutPopularMetadata.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<String>> call, @NonNull Throwable t) {
                if (progressSuggestionsLoading != null) {
                    progressSuggestionsLoading.setVisibility(View.GONE);
                }
                if (layoutPopularMetadata != null) {
                    layoutPopularMetadata.setVisibility(View.GONE);
                }
            }
        });
    }

    private void populateSuggestionChips(List<String> suggestions) {
        if (cgPopularMetadata == null) return;
        cgPopularMetadata.removeAllViews();

        int max = Math.min(suggestions.size(), 12);
        for (int i = 0; i < max; i++) {
            String text = suggestions.get(i);
            if (text == null || text.trim().isEmpty()) continue;

            Chip chip = new Chip(this);
            chip.setText(text.trim());
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setTextSize(13f);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainerLow)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_onSurface));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_light_outlineVariant)));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1f);

            chip.setOnClickListener(v -> {
                etMetadataSearchQuery.setText(text.trim());
                etMetadataSearchQuery.setSelection(text.trim().length());
                hideKeyboard();
                executeSearch(text.trim());
            });

            cgPopularMetadata.addView(chip);
        }

        if (layoutPopularMetadata != null) {
            layoutPopularMetadata.setVisibility(cgPopularMetadata.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void loadRecentSearches() {
        if (cgRecentSearches == null) return;
        cgRecentSearches.removeAllViews();

        List<String> latest = SearchHistoryManager.getLatestSearches(this);
        if (latest.isEmpty()) {
            if (layoutRecentSearches != null) {
                layoutRecentSearches.setVisibility(View.GONE);
            }
            return;
        }

        if (layoutRecentSearches != null) {
            layoutRecentSearches.setVisibility(View.VISIBLE);
        }

        for (String q : latest) {
            if (q == null || q.trim().isEmpty()) continue;

            Chip chip = new Chip(this);
            chip.setText(q.trim());
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setTextSize(13f);
            chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_light_surfaceContainer)));
            chip.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.md_theme_light_outlineVariant)));
            chip.setChipStrokeWidth(getResources().getDisplayMetrics().density * 1f);

            chip.setOnClickListener(v -> {
                etMetadataSearchQuery.setText(q.trim());
                etMetadataSearchQuery.setSelection(q.trim().length());
                hideKeyboard();
                executeSearch(q.trim());
            });

            cgRecentSearches.addView(chip);
        }
    }

    private void executeSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        currentQuery = query.trim();

        // Persist in search history
        SearchHistoryManager.recordSearch(this, currentQuery);
        loadRecentSearches();

        if (progressMetadataLoading != null) {
            progressMetadataLoading.setVisibility(View.VISIBLE);
        }

        apiService.searchFiles(currentQuery, null).enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (progressMetadataLoading != null) {
                    progressMetadataLoading.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<StoredFile> results = response.body();
                    displayResults(results);
                } else {
                    displayResults(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                if (progressMetadataLoading != null) {
                    progressMetadataLoading.setVisibility(View.GONE);
                }
                Toast.makeText(MetadataSearchActivity.this, "We couldn't connect to the server. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayResults(List<StoredFile> results) {
        if (results == null || results.isEmpty()) {
            if (layoutResultsHeader != null) layoutResultsHeader.setVisibility(View.VISIBLE);
            if (tvResultsCount != null) tvResultsCount.setText("0 files found");
            if (rvMetadataResults != null) rvMetadataResults.setVisibility(View.GONE);
            if (layoutMetadataSearchEmpty != null) {
                layoutMetadataSearchEmpty.setVisibility(View.VISIBLE);
                if (tvMetadataEmptyTitle != null) {
                    tvMetadataEmptyTitle.setText("No files match \"" + currentQuery + "\"");
                }
                if (tvMetadataEmptyMessage != null) {
                    tvMetadataEmptyMessage.setText("Try searching for a different camera model, format, resolution, or tag.");
                }
            }
            if (filesAdapter != null) filesAdapter.setFiles(new ArrayList<>());
        } else {
            if (layoutResultsHeader != null) layoutResultsHeader.setVisibility(View.VISIBLE);
            if (tvResultsCount != null) {
                int count = results.size();
                tvResultsCount.setText(count + (count == 1 ? " file found" : " files found"));
            }
            if (layoutMetadataSearchEmpty != null) layoutMetadataSearchEmpty.setVisibility(View.GONE);
            if (rvMetadataResults != null) {
                rvMetadataResults.setVisibility(View.VISIBLE);
                if (filesAdapter != null) {
                    filesAdapter.setFiles(results);
                }
            }
        }
    }

    private void clearResultsView() {
        if (layoutResultsHeader != null) layoutResultsHeader.setVisibility(View.GONE);
        if (rvMetadataResults != null) rvMetadataResults.setVisibility(View.GONE);
        if (layoutMetadataSearchEmpty != null) layoutMetadataSearchEmpty.setVisibility(View.GONE);
        if (filesAdapter != null) filesAdapter.setFiles(new ArrayList<>());
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onDownloadClick(StoredFile file) {
        if (file == null || file.getId() == null) return;
        showDownloadConfirmDialog(file);
    }

    private void showDownloadConfirmDialog(StoredFile file) {
        if (!file.isEncrypted()) {
            Toast.makeText(this, "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
            TransferManager.getInstance(this).enqueueDownload(file, false);
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_download_options, null);
        TextView tvDownloadFileName = dialogView.findViewById(R.id.tvDownloadFileName);
        android.widget.CompoundButton cbDecrypt = dialogView.findViewById(R.id.cbDecryptOption);
        if (tvDownloadFileName != null) {
            tvDownloadFileName.setText("Save \"" + file.getOriginalFilename() + "\" to Downloads folder.");
        }
        if (cbDecrypt != null) {
            cbDecrypt.setChecked(true); // Default ON
        }

        androidx.appcompat.app.AlertDialog downloadDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Download File")
                .setView(dialogView)
                .setPositiveButton("Download", (dialog, which) -> {
                    boolean decrypt = cbDecrypt == null || cbDecrypt.isChecked();
                    TransferManager.getInstance(this).enqueueDownload(file, decrypt);
                    Toast.makeText(this, "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();

        android.widget.Button downloadPosBtn = downloadDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (downloadPosBtn != null) {
            downloadPosBtn.setTextColor(ContextCompat.getColor(this, R.color.md_theme_light_primary));
        }
    }

    @Override
    public void onDeleteClick(StoredFile file) {
        if (file == null || file.getId() == null) return;
        androidx.appcompat.app.AlertDialog deleteDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you want to permanently delete \"" + file.getOriginalFilename() + "\" from your vault?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressMetadataLoading != null) progressMetadataLoading.setVisibility(View.VISIBLE);
                    apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (progressMetadataLoading != null) progressMetadataLoading.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(MetadataSearchActivity.this, "File deleted", Toast.LENGTH_SHORT).show();
                                if (!currentQuery.isEmpty()) {
                                    executeSearch(currentQuery);
                                }
                            } else {
                                Toast.makeText(MetadataSearchActivity.this, "Could not delete file. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                            if (progressMetadataLoading != null) progressMetadataLoading.setVisibility(View.GONE);
                            Toast.makeText(MetadataSearchActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();

        android.widget.Button deletePosBtn = deleteDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (deletePosBtn != null) {
            deletePosBtn.setTextColor(ContextCompat.getColor(this, R.color.status_error));
        }
    }
}
