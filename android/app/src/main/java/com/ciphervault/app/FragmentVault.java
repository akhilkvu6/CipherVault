package com.ciphervault.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.ciphervault.app.transfer.TransferBatch;
import com.ciphervault.app.transfer.TransferItem;
import com.ciphervault.app.transfer.TransferListener;
import com.ciphervault.app.transfer.TransferManager;
import com.ciphervault.app.transfer.TransferType;

public class FragmentVault extends Fragment implements TransferListener {

    public enum SortOrder {
        DATE_DESC, DATE_ASC, NAME_ASC, NAME_DESC, SIZE_DESC, SIZE_ASC
    }

    private final List<StoredFile> allFiles = new ArrayList<>();
    private final List<StoredFile> displayedFiles = new ArrayList<>();
    private final Set<Long> selectedFileIds = new HashSet<>();
    private boolean isMultiSelectMode = false;

    private VaultFilesAdapter adapter;
    private ApiService apiService;

    private TextView tvVaultFileCount;
    private EditText etVaultSearch;
    private View layoutVaultHeader;
    private View layoutMultiSelectBar;
    private TextView tvSelectedCount;
    private MaterialButton btnSelectAll;
    private MaterialButton btnBatchDownload;
    private MaterialButton btnBatchDelete;
    private View layoutVaultEmpty;
    private ChipGroup chipGroupVaultCategories;
    private OnBackPressedCallback multiSelectBackCallback;

    private View layoutActiveFilter;
    private com.google.android.material.chip.Chip chipActiveMetadataFilter;
    private final MetadataSearchBottomSheet.FilterCriteria metadataCriteria =
            new MetadataSearchBottomSheet.FilterCriteria();

    private String currentCategoryFilter = "ALL";
    private SortOrder currentSort = SortOrder.DATE_DESC;
    private String currentSearchQuery = "";
    private boolean isLoadingVault = false;
    private com.google.android.material.progressindicator.LinearProgressIndicator progressVaultLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vault, container, false);

        if (savedInstanceState != null) {
            isMultiSelectMode = savedInstanceState.getBoolean("KEY_IS_MULTI_SELECT", false);
            long[] savedIds = savedInstanceState.getLongArray("KEY_SELECTED_IDS");
            if (savedIds != null) {
                for (long id : savedIds) {
                    selectedFileIds.add(id);
                }
            }
        }

        apiService = ApiClient.getApiService(requireContext());

        progressVaultLoading = view.findViewById(R.id.progressVaultLoading);
        tvVaultFileCount = view.findViewById(R.id.tvVaultFileCount);
        etVaultSearch = view.findViewById(R.id.etVaultSearch);
        layoutVaultHeader = view.findViewById(R.id.layoutVaultHeader);
        layoutMultiSelectBar = view.findViewById(R.id.layoutMultiSelectBar);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
        btnSelectAll = view.findViewById(R.id.btnSelectAll);
        btnBatchDownload = view.findViewById(R.id.btnBatchDownload);
        btnBatchDelete = view.findViewById(R.id.btnBatchDelete);
        layoutVaultEmpty = view.findViewById(R.id.layoutVaultEmpty);
        chipGroupVaultCategories = view.findViewById(R.id.chipGroupVaultCategories);

        RecyclerView rvVaultFiles = view.findViewById(R.id.rvVaultFiles);
        rvVaultFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VaultFilesAdapter();
        rvVaultFiles.setAdapter(adapter);

        layoutActiveFilter = view.findViewById(R.id.layoutActiveFilter);
        chipActiveMetadataFilter = view.findViewById(R.id.chipActiveMetadataFilter);

        if (chipActiveMetadataFilter != null) {
            chipActiveMetadataFilter.setOnCloseIconClickListener(v -> clearMetadataFilters());
            chipActiveMetadataFilter.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MetadataSearchActivity.class);
                if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
                    intent.putExtra("query", currentSearchQuery.trim());
                }
                startActivity(intent);
            });
        }

        View btnFilter = view.findViewById(R.id.btnFilterVault);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MetadataSearchActivity.class);
                if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
                    intent.putExtra("query", currentSearchQuery.trim());
                }
                startActivity(intent);
            });
        }

        view.findViewById(R.id.btnSortVault).setOnClickListener(v -> showSortBottomSheet());
        view.findViewById(R.id.btnCloseMultiSelect).setOnClickListener(v -> exitMultiSelectMode());
        if (btnSelectAll != null) btnSelectAll.setOnClickListener(v -> toggleSelectAll());
        if (btnBatchDownload != null) btnBatchDownload.setOnClickListener(v -> handleBatchDownload());
        if (btnBatchDelete != null) btnBatchDelete.setOnClickListener(v -> handleBatchDelete());
        view.findViewById(R.id.btnVaultEmptyUpload).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UploadStagingActivity.class);
            startActivity(intent);
        });

        setupSearchWatcher();
        setupCategoryChips();
        updateMultiSelectUI();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        multiSelectBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isMultiSelectMode) {
                    exitMultiSelectMode();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), multiSelectBackCallback);
        if (multiSelectBackCallback != null) {
            multiSelectBackCallback.setEnabled(isMultiSelectMode);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("KEY_IS_MULTI_SELECT", isMultiSelectMode);
        long[] ids = new long[selectedFileIds.size()];
        int i = 0;
        for (Long id : selectedFileIds) {
            ids[i++] = id;
        }
        outState.putLongArray("KEY_SELECTED_IDS", ids);
    }

    @Override
    public void onResume() {
        super.onResume();
        performMetadataSearchOrLoad();
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
                        performMetadataSearchOrLoad();
                    }
                });
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            performMetadataSearchOrLoad();
        } else {
            if (isMultiSelectMode) {
                exitMultiSelectMode();
            }
        }
    }

    public void applyCategoryFilter(String category) {
        currentCategoryFilter = category != null ? category.toUpperCase(Locale.US) : "ALL";
        if (chipGroupVaultCategories != null) {
            if ("PHOTOS".equals(currentCategoryFilter) || "IMAGES".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatPhotos);
            } else if ("VIDEOS".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatVideos);
            } else if ("DOCUMENTS".equals(currentCategoryFilter) || "PDFS".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatDocs);
            } else if ("ARCHIVES".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatArchives);
            } else if ("AUDIO".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatAudio);
            } else if ("OTHER".equals(currentCategoryFilter)) {
                chipGroupVaultCategories.check(R.id.chipCatOther);
            } else {
                chipGroupVaultCategories.check(R.id.chipCatAll);
            }
        }
        if (metadataCriteria.getActiveCount() > 0) {
            performMetadataSearchOrLoad();
        } else {
            applyFiltersAndSort();
        }
    }

    private void setupSearchWatcher() {
        etVaultSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.US) : "";
                applyFiltersAndSort();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryChips() {
        if (chipGroupVaultCategories == null) return;

        if ("PHOTOS".equals(currentCategoryFilter) || "IMAGES".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatPhotos);
        } else if ("VIDEOS".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatVideos);
        } else if ("DOCUMENTS".equals(currentCategoryFilter) || "PDFS".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatDocs);
        } else if ("ARCHIVES".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatArchives);
        } else if ("AUDIO".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatAudio);
        } else if ("OTHER".equals(currentCategoryFilter)) {
            chipGroupVaultCategories.check(R.id.chipCatOther);
        } else {
            chipGroupVaultCategories.check(R.id.chipCatAll);
        }

        chipGroupVaultCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipCatAll) currentCategoryFilter = "ALL";
            else if (id == R.id.chipCatPhotos) currentCategoryFilter = "PHOTOS";
            else if (id == R.id.chipCatVideos) currentCategoryFilter = "VIDEOS";
            else if (id == R.id.chipCatDocs) currentCategoryFilter = "DOCUMENTS";
            else if (id == R.id.chipCatArchives) currentCategoryFilter = "ARCHIVES";
            else if (id == R.id.chipCatAudio) currentCategoryFilter = "AUDIO";
            else if (id == R.id.chipCatOther) currentCategoryFilter = "OTHER";
            applyFiltersAndSort();
        });
    }

    private void showSortBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_sort, null);
        dialog.setContentView(sheetView);

        RadioGroup rg = sheetView.findViewById(R.id.rgSortOptions);
        switch (currentSort) {
            case DATE_DESC: rg.check(R.id.rbSortDateDesc); break;
            case DATE_ASC: rg.check(R.id.rbSortDateAsc); break;
            case NAME_ASC: rg.check(R.id.rbSortNameAsc); break;
            case NAME_DESC: rg.check(R.id.rbSortNameDesc); break;
            case SIZE_DESC: rg.check(R.id.rbSortSizeDesc); break;
            case SIZE_ASC: rg.check(R.id.rbSortSizeAsc); break;
        }

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSortDateDesc) currentSort = SortOrder.DATE_DESC;
            else if (checkedId == R.id.rbSortDateAsc) currentSort = SortOrder.DATE_ASC;
            else if (checkedId == R.id.rbSortNameAsc) currentSort = SortOrder.NAME_ASC;
            else if (checkedId == R.id.rbSortNameDesc) currentSort = SortOrder.NAME_DESC;
            else if (checkedId == R.id.rbSortSizeDesc) currentSort = SortOrder.SIZE_DESC;
            else if (checkedId == R.id.rbSortSizeAsc) currentSort = SortOrder.SIZE_ASC;
            applyFiltersAndSort();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showMetadataFilterBottomSheet() {
        MetadataSearchBottomSheet.show(requireContext(), metadataCriteria, criteria -> {
            metadataCriteria.copyFrom(criteria);
            updateMetadataFilterUI();
            performMetadataSearchOrLoad();
        });
    }

    private void clearMetadataFilters() {
        metadataCriteria.clear();
        updateMetadataFilterUI();
        performMetadataSearchOrLoad();
    }

    private void updateMetadataFilterUI() {
        if (layoutActiveFilter == null || chipActiveMetadataFilter == null) return;
        int activeCount = metadataCriteria.getActiveCount();
        if (activeCount > 0) {
            layoutActiveFilter.setVisibility(View.VISIBLE);
            chipActiveMetadataFilter.setText("Filters • " + activeCount);
        } else {
            layoutActiveFilter.setVisibility(View.GONE);
        }
    }

    public void performMetadataSearchOrLoad() {
        if (!isAdded() || getContext() == null) return;
        if (metadataCriteria.getActiveCount() == 0) {
            loadFiles();
            return;
        }

        if (isLoadingVault) return;
        isLoadingVault = true;
        if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.VISIBLE);

        String categoryParam = "ALL".equals(currentCategoryFilter) ? null : currentCategoryFilter;
        apiService.searchFiles(
                metadataCriteria.query,
                categoryParam,
                metadataCriteria.cameraMake,
                metadataCriteria.cameraModel,
                metadataCriteria.resolution,
                metadataCriteria.codec,
                metadataCriteria.artist,
                metadataCriteria.author,
                metadataCriteria.genre
        ).enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                isLoadingVault = false;
                if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.GONE);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    allFiles.clear();
                    allFiles.addAll(response.body());
                    applyFiltersAndSort();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "No matching files found", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                isLoadingVault = false;
                if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.GONE);
                if (!isAdded() || getContext() == null) return;
                String msg = (t != null && t.getMessage() != null) ? t.getMessage() : "Network error";
                Toast.makeText(getContext(), "Search error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void loadFiles() {
        if (!isAdded() || getContext() == null) return;
        if (isLoadingVault) return;
        isLoadingVault = true;
        if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.VISIBLE);

        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                isLoadingVault = false;
                if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.GONE);
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    allFiles.clear();
                    allFiles.addAll(response.body());
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                isLoadingVault = false;
                if (progressVaultLoading != null) progressVaultLoading.setVisibility(View.GONE);
                if (!isAdded() || getContext() == null) return;
                String msg = (t != null && t.getMessage() != null) ? t.getMessage() : "Network error";
                Toast.makeText(getContext(), "Error loading vault: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFiltersAndSort() {
        displayedFiles.clear();

        for (StoredFile file : allFiles) {
            // Category check
            boolean matchesCat = true;
            if (!"ALL".equals(currentCategoryFilter)) {
                StoredFile.FileCategory cat = file.getCategory();
                if (("PHOTOS".equals(currentCategoryFilter) || "IMAGES".equals(currentCategoryFilter))
                        && cat == StoredFile.FileCategory.IMAGES) matchesCat = true;
                else if ("VIDEOS".equals(currentCategoryFilter)
                        && cat == StoredFile.FileCategory.VIDEOS) matchesCat = true;
                else if (("DOCUMENTS".equals(currentCategoryFilter) || "PDFS".equals(currentCategoryFilter))
                        && (cat == StoredFile.FileCategory.PDFS || cat == StoredFile.FileCategory.DOCUMENTS)) matchesCat = true;
                else if ("AUDIO".equals(currentCategoryFilter)
                        && cat == StoredFile.FileCategory.AUDIO) matchesCat = true;
                else if ("ARCHIVES".equals(currentCategoryFilter)
                        && cat == StoredFile.FileCategory.ARCHIVES) matchesCat = true;
                else if ("OTHER".equals(currentCategoryFilter)
                        && cat == StoredFile.FileCategory.OTHER) matchesCat = true;
                else matchesCat = false;
            }

            // Search query check
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String name = file.getFilename().toLowerCase(Locale.US);
                String hash = file.getSha256Hash() != null ? file.getSha256Hash().toLowerCase(Locale.US) : "";
                matchesSearch = name.contains(currentSearchQuery) || hash.contains(currentSearchQuery);
            }

            if (matchesCat && matchesSearch) {
                displayedFiles.add(file);
            }
        }

        // Sort
        Collections.sort(displayedFiles, (a, b) -> {
            switch (currentSort) {
                case NAME_ASC: return a.getFilename().compareToIgnoreCase(b.getFilename());
                case NAME_DESC: return b.getFilename().compareToIgnoreCase(a.getFilename());
                case SIZE_DESC: return Long.compare(b.getFileSize(), a.getFileSize());
                case SIZE_ASC: return Long.compare(a.getFileSize(), b.getFileSize());
                case DATE_ASC: return Long.compare(a.getId() != null ? a.getId() : 0, b.getId() != null ? b.getId() : 0);
                case DATE_DESC:
                default:
                    return Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0);
            }
        });

        if (tvVaultFileCount != null) {
            tvVaultFileCount.setText(allFiles.size() + " files stored (" + displayedFiles.size() + " shown)");
        }

        if (layoutVaultEmpty != null) {
            layoutVaultEmpty.setVisibility(displayedFiles.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (isMultiSelectMode) {
            Set<Long> existingIds = new HashSet<>();
            for (StoredFile f : allFiles) {
                if (f.getId() != null) existingIds.add(f.getId());
            }
            selectedFileIds.retainAll(existingIds);
            updateMultiSelectUI();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void enterMultiSelectMode(StoredFile file) {
        isMultiSelectMode = true;
        selectedFileIds.clear();
        if (file != null && file.getId() != null) {
            selectedFileIds.add(file.getId());
        }
        updateMultiSelectUI();
    }

    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedFileIds.clear();
        updateMultiSelectUI();
    }

    private void toggleFileSelection(Long fileId) {
        if (fileId == null) return;
        if (selectedFileIds.contains(fileId)) {
            selectedFileIds.remove(fileId);
        } else {
            selectedFileIds.add(fileId);
        }
        updateMultiSelectUI();
    }

    private boolean areAllDisplayedFilesSelected() {
        if (displayedFiles.isEmpty()) return false;
        for (StoredFile f : displayedFiles) {
            if (f.getId() != null && !selectedFileIds.contains(f.getId())) {
                return false;
            }
        }
        return true;
    }

    private void updateMultiSelectUI() {
        if (multiSelectBackCallback != null) {
            multiSelectBackCallback.setEnabled(isMultiSelectMode);
        }
        if (isMultiSelectMode) {
            if (layoutVaultHeader != null) layoutVaultHeader.setVisibility(View.GONE);
            if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.VISIBLE);
            int count = selectedFileIds.size();
            if (tvSelectedCount != null) {
                tvSelectedCount.setText(count + " selected");
            }
            if (btnSelectAll != null) {
                btnSelectAll.setText(areAllDisplayedFilesSelected() ? "None" : "All");
            }
            if (btnBatchDownload != null) {
                btnBatchDownload.setEnabled(count > 0);
                btnBatchDownload.setAlpha(count > 0 ? 1.0f : 0.5f);
                btnBatchDownload.setText(count > 0 ? ("Download " + count + (count == 1 ? " File" : " Files")) : "Download");
            }
            if (btnBatchDelete != null) {
                btnBatchDelete.setEnabled(count > 0);
                btnBatchDelete.setAlpha(count > 0 ? 1.0f : 0.5f);
                btnBatchDelete.setText(count > 0 ? ("Delete " + count + (count == 1 ? " File" : " Files")) : "Delete");
            }
        } else {
            if (layoutVaultHeader != null) layoutVaultHeader.setVisibility(View.VISIBLE);
            if (layoutMultiSelectBar != null) layoutMultiSelectBar.setVisibility(View.GONE);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void toggleSelectAll() {
        if (displayedFiles.isEmpty()) return;
        if (areAllDisplayedFilesSelected()) {
            for (StoredFile f : displayedFiles) {
                if (f.getId() != null) selectedFileIds.remove(f.getId());
            }
        } else {
            for (StoredFile f : displayedFiles) {
                if (f.getId() != null) selectedFileIds.add(f.getId());
            }
        }
        updateMultiSelectUI();
    }

    private void handleBatchDownload() {
        if (selectedFileIds.isEmpty() || !isAdded() || getContext() == null) return;
        List<StoredFile> toDownload = new ArrayList<>();
        boolean hasEncrypted = false;
        for (StoredFile file : allFiles) {
            if (file.getId() != null && selectedFileIds.contains(file.getId())) {
                toDownload.add(file);
                if (file.isEncrypted()) hasEncrypted = true;
            }
        }
        if (toDownload.isEmpty()) {
            exitMultiSelectMode();
            return;
        }

        final int downloadCount = toDownload.size();
        final String downloadBtnLabel = "Download " + downloadCount + (downloadCount == 1 ? " File" : " Files");

        if (hasEncrypted) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
            TextView tvDownloadFileName = dialogView.findViewById(R.id.tvDownloadFileName);
            android.widget.CompoundButton cbDecryptOption = dialogView.findViewById(R.id.cbDecryptOption);

            tvDownloadFileName.setText("Save " + downloadCount + " file(s) to Downloads folder.");
            cbDecryptOption.setText("Decrypt downloaded files");
            cbDecryptOption.setChecked(true);

            androidx.appcompat.app.AlertDialog batchDownloadDialog = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(downloadBtnLabel)
                    .setView(dialogView)
                    .setPositiveButton(downloadBtnLabel, (dialog, which) -> {
                        boolean decrypt = cbDecryptOption.isChecked();
                        Toast.makeText(requireContext(), "Downloading " + downloadCount + " file(s) in background...", Toast.LENGTH_SHORT).show();
                        TransferManager.getInstance(requireContext()).enqueueDownloadBatch(toDownload, decrypt);
                        AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + downloadCount + " files (decrypt=" + decrypt + ")");
                        exitMultiSelectMode();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            android.widget.Button posBtn = batchDownloadDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
            if (posBtn != null) {
                posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
            }
        } else {
            Toast.makeText(requireContext(), "Downloading " + downloadCount + " file(s) in background...", Toast.LENGTH_SHORT).show();
            TransferManager.getInstance(requireContext()).enqueueDownloadBatch(toDownload, true);
            AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + downloadCount + " files");
            exitMultiSelectMode();
        }
    }

    private void handleBatchDelete() {
        if (selectedFileIds.isEmpty() || !isAdded() || getContext() == null) return;
        final List<Long> idsToDelete = new ArrayList<>(selectedFileIds);
        final int totalToDelete = idsToDelete.size();
        final String deleteBtnLabel = "Delete " + totalToDelete + (totalToDelete == 1 ? " File" : " Files");

        androidx.appcompat.app.AlertDialog deleteDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + totalToDelete + (totalToDelete == 1 ? " File?" : " Files?"))
                .setMessage("Are you sure you want to delete " + totalToDelete + " selected file(s) from your encrypted vault? This action cannot be undone.")
                .setPositiveButton(deleteBtnLabel, (dialog, which) -> {
                    exitMultiSelectMode();
                    if (!isAdded() || getContext() == null || idsToDelete.isEmpty()) return;

                    Toast.makeText(requireContext(), "Deleting " + totalToDelete + " files...", Toast.LENGTH_SHORT).show();
                    AtomicInteger remaining = new AtomicInteger(totalToDelete);
                    AtomicInteger successCount = new AtomicInteger(0);

                    for (Long id : idsToDelete) {
                        apiService.deleteFile(id).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                if (response.isSuccessful()) {
                                    successCount.incrementAndGet();
                                }
                                if (remaining.decrementAndGet() == 0) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), successCount.get() + " of " + totalToDelete + " files deleted", Toast.LENGTH_SHORT).show();
                                        performMetadataSearchOrLoad();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                if (remaining.decrementAndGet() == 0) {
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), successCount.get() + " of " + totalToDelete + " files deleted", Toast.LENGTH_SHORT).show();
                                        performMetadataSearchOrLoad();
                                    }
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();

        android.widget.Button posBtn = deleteDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
        }
    }

    private void downloadSingleFile(StoredFile file) {
        if (file == null || file.getId() == null) return;

        if (!file.isEncrypted()) {
            Toast.makeText(requireContext(), "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
            TransferManager.getInstance(requireContext()).enqueueDownload(file, false);
            AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + file.getOriginalFilename());
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
        TextView tvDownloadFileName = dialogView.findViewById(R.id.tvDownloadFileName);
        android.widget.CompoundButton cbDecryptOption = dialogView.findViewById(R.id.cbDecryptOption);

        tvDownloadFileName.setText("Save \"" + file.getOriginalFilename() + "\" to Downloads folder.");
        cbDecryptOption.setChecked(true);

        androidx.appcompat.app.AlertDialog downloadDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Download File")
                .setView(dialogView)
                .setPositiveButton("Download", (dialog, which) -> {
                    boolean decrypt = cbDecryptOption.isChecked();
                    Toast.makeText(requireContext(), "Downloading " + file.getFilename() + " in background...", Toast.LENGTH_SHORT).show();
                    TransferManager.getInstance(requireContext()).enqueueDownload(file, decrypt);
                    AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started download of " + file.getOriginalFilename() + " (decrypt=" + decrypt + ")");
                })
                .setNegativeButton("Cancel", null)
                .show();

        android.widget.Button posBtn = downloadDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
        }
    }

    private void confirmAndDeleteFile(StoredFile file) {
        if (file == null || file.getId() == null || getContext() == null) return;
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete File")
                .setMessage("Are you sure you want to delete \"" + file.getOriginalFilename() + "\" from your encrypted vault? This action cannot be undone.")
                .setPositiveButton("Delete", (d, which) -> {
                    apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "File deleted", Toast.LENGTH_SHORT).show();
                                performMetadataSearchOrLoad();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(requireContext(), "Delete failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();

        android.widget.Button posBtn = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (posBtn != null) {
            posBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_error));
        }
    }

    private int dpToPx(Context context, int dp) {
        if (context == null) return dp;
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private class VaultFilesAdapter extends RecyclerView.Adapter<VaultFilesAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StoredFile file = displayedFiles.get(position);
            Context ctx = holder.itemView.getContext();

            holder.tvFileName.setText(file.getFilename());
            holder.tvFileSize.setText(FileUtils.formatStorageSize(ctx, file.getFileSize()));

            if (file.isEncrypted()) {
                int encColor = ThemeManager.getEncryptedColor(ctx);
                holder.tvEncryptionBadge.setText("AES-256-GCM");
                holder.tvEncryptionBadge.setTextColor(encColor);
            } else {
                holder.tvEncryptionBadge.setText("Unencrypted");
                holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(ctx, R.color.vault_unencrypted));
            }

            // Thumbnail loading
            if (file.hasPreview() && file.getId() != null) {
                ThumbnailLoader.loadThumbnail(ctx, file.getId(), holder.ivThumbnail, holder.ivFileIcon,
                        file.getCategory() == StoredFile.FileCategory.VIDEOS ? holder.ivVideoBadge : null);
            } else {
                holder.ivThumbnail.setImageDrawable(null);
                holder.ivThumbnail.setVisibility(View.GONE);
                holder.ivFileIcon.setVisibility(View.VISIBLE);
                holder.ivVideoBadge.setVisibility(View.GONE);
            }

            boolean isSelected = isMultiSelectMode && file.getId() != null && selectedFileIds.contains(file.getId());

            // Multi-select checkbox & card highlight
            if (isMultiSelectMode) {
                holder.cbFileSelect.setVisibility(View.VISIBLE);
                holder.cbFileSelect.setChecked(isSelected);
                holder.cbFileSelect.setClickable(false);
                holder.cbFileSelect.setFocusable(false);

                if (isSelected) {
                    int primaryColor = MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary);
                    int containerColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSecondaryContainer);
                    holder.cardFileItem.setStrokeColor(ColorStateList.valueOf(primaryColor));
                    holder.cardFileItem.setStrokeWidth(dpToPx(ctx, 2));
                    holder.cardFileItem.setCardBackgroundColor(ColorStateList.valueOf(containerColor));
                } else {
                    int outlineColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOutlineVariant);
                    int defaultBgColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceContainerLow);
                    holder.cardFileItem.setStrokeColor(ColorStateList.valueOf(outlineColor));
                    holder.cardFileItem.setStrokeWidth(dpToPx(ctx, 1));
                    holder.cardFileItem.setCardBackgroundColor(ColorStateList.valueOf(defaultBgColor));
                }
            } else {
                holder.cbFileSelect.setVisibility(View.GONE);
                holder.cbFileSelect.setChecked(false);
                holder.cbFileSelect.setClickable(false);
                holder.cbFileSelect.setFocusable(false);

                int outlineColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOutlineVariant);
                int defaultBgColor = MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorSurfaceContainerLow);
                holder.cardFileItem.setStrokeColor(ColorStateList.valueOf(outlineColor));
                holder.cardFileItem.setStrokeWidth(dpToPx(ctx, 1));
                holder.cardFileItem.setCardBackgroundColor(ColorStateList.valueOf(defaultBgColor));
            }

            // Click listener: either toggle selection or open File Details Bottom Sheet
            View.OnClickListener openDetailsOrSelect = v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= displayedFiles.size()) return;
                StoredFile currentFile = displayedFiles.get(currentPos);
                if (currentFile == null) return;

                if (isMultiSelectMode) {
                    if (currentFile.getId() != null) {
                        toggleFileSelection(currentFile.getId());
                    }
                } else {
                    FileDetailsBottomSheet.show(ctx, currentFile,
                            FragmentVault.this::downloadSingleFile,
                            FragmentVault.this::confirmAndDeleteFile);
                }
            };

            // Long-click listener: enter multi-select or toggle selection
            View.OnLongClickListener openMultiSelect = v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= displayedFiles.size()) return true;
                StoredFile currentFile = displayedFiles.get(currentPos);
                if (currentFile == null) return true;

                if (!isMultiSelectMode) {
                    enterMultiSelectMode(currentFile);
                } else {
                    if (currentFile.getId() != null) {
                        toggleFileSelection(currentFile.getId());
                    }
                }
                return true;
            };

            // Card and child view click listeners
            holder.itemView.setOnClickListener(openDetailsOrSelect);
            holder.tvFileName.setOnClickListener(openDetailsOrSelect);
            holder.tvFileSize.setOnClickListener(openDetailsOrSelect);
            holder.tvEncryptionBadge.setOnClickListener(openDetailsOrSelect);
            holder.ivThumbnail.setOnClickListener(openDetailsOrSelect);
            holder.ivFileIcon.setOnClickListener(openDetailsOrSelect);

            // Card and child view long-click listeners to prevent touch swallow
            holder.itemView.setOnLongClickListener(openMultiSelect);
            holder.tvFileName.setOnLongClickListener(openMultiSelect);
            holder.tvFileSize.setOnLongClickListener(openMultiSelect);
            holder.tvEncryptionBadge.setOnLongClickListener(openMultiSelect);
            holder.ivThumbnail.setOnLongClickListener(openMultiSelect);
            holder.ivFileIcon.setOnLongClickListener(openMultiSelect);

            // Direct Actions: [Download] [Delete]
            if (holder.layoutCardActions != null) {
                holder.layoutCardActions.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
            }

            holder.btnDownload.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= displayedFiles.size()) return;
                downloadSingleFile(displayedFiles.get(currentPos));
            });

            holder.btnDelete.setOnClickListener(v -> {
                int currentPos = holder.getBindingAdapterPosition();
                if (currentPos == RecyclerView.NO_POSITION || currentPos >= displayedFiles.size()) return;
                confirmAndDeleteFile(displayedFiles.get(currentPos));
            });
        }

        @Override
        public int getItemCount() {
            return displayedFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView cardFileItem;
            final MaterialCheckBox cbFileSelect;
            final ImageView ivFileIcon;
            final ImageView ivThumbnail;
            final ImageView ivVideoBadge;
            final TextView tvFileName;
            final TextView tvFileSize;
            final TextView tvEncryptionBadge;
            final View layoutCardActions;
            final MaterialButton btnDownload;
            final MaterialButton btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardFileItem = (MaterialCardView) itemView;
                cbFileSelect = itemView.findViewById(R.id.cbFileSelect);
                ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
                ivVideoBadge = itemView.findViewById(R.id.ivVideoBadge);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileSize = itemView.findViewById(R.id.tvFileSize);
                tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
                layoutCardActions = itemView.findViewById(R.id.layoutCardActions);
                btnDownload = itemView.findViewById(R.id.btnDownload);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
