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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private View layoutVaultEmpty;
    private ChipGroup chipGroupVaultCategories;

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

        apiService = ApiClient.getApiService(requireContext());

        progressVaultLoading = view.findViewById(R.id.progressVaultLoading);
        tvVaultFileCount = view.findViewById(R.id.tvVaultFileCount);
        etVaultSearch = view.findViewById(R.id.etVaultSearch);
        layoutVaultHeader = view.findViewById(R.id.layoutVaultHeader);
        layoutMultiSelectBar = view.findViewById(R.id.layoutMultiSelectBar);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);
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
            chipActiveMetadataFilter.setOnClickListener(v -> showMetadataFilterBottomSheet());
        }

        View btnFilter = view.findViewById(R.id.btnFilterVault);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showMetadataFilterBottomSheet());
        }

        view.findViewById(R.id.btnSortVault).setOnClickListener(v -> showSortBottomSheet());
        view.findViewById(R.id.btnCloseMultiSelect).setOnClickListener(v -> exitMultiSelectMode());
        view.findViewById(R.id.btnSelectAll).setOnClickListener(v -> toggleSelectAll());
        view.findViewById(R.id.btnBatchDownload).setOnClickListener(v -> handleBatchDownload());
        view.findViewById(R.id.btnBatchDelete).setOnClickListener(v -> handleBatchDelete());
        view.findViewById(R.id.btnVaultEmptyUpload).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UploadStagingActivity.class);
            startActivity(intent);
        });

        setupSearchWatcher();
        setupCategoryChips();

        return view;
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

        adapter.notifyDataSetChanged();
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

    private void updateMultiSelectUI() {
        if (isMultiSelectMode) {
            layoutVaultHeader.setVisibility(View.GONE);
            layoutMultiSelectBar.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(selectedFileIds.size() + " selected");
        } else {
            layoutVaultHeader.setVisibility(View.VISIBLE);
            layoutMultiSelectBar.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    private void toggleSelectAll() {
        if (selectedFileIds.size() == displayedFiles.size()) {
            selectedFileIds.clear();
        } else {
            for (StoredFile f : displayedFiles) {
                if (f.getId() != null) selectedFileIds.add(f.getId());
            }
        }
        tvSelectedCount.setText(selectedFileIds.size() + " selected");
        adapter.notifyDataSetChanged();
    }

    private void handleBatchDownload() {
        if (selectedFileIds.isEmpty()) return;
        List<StoredFile> toDownload = new ArrayList<>();
        boolean hasEncrypted = false;
        for (StoredFile file : displayedFiles) {
            if (selectedFileIds.contains(file.getId())) {
                toDownload.add(file);
                if (file.isEncrypted()) hasEncrypted = true;
            }
        }
        if (toDownload.isEmpty()) {
            exitMultiSelectMode();
            return;
        }

        if (hasEncrypted) {
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_options, null);
            TextView tvDownloadFileName = dialogView.findViewById(R.id.tvDownloadFileName);
            MaterialCheckBox cbDecryptOption = dialogView.findViewById(R.id.cbDecryptOption);

            tvDownloadFileName.setText("Save " + toDownload.size() + " files to Downloads folder.");
            cbDecryptOption.setText("Decrypt downloaded files");
            cbDecryptOption.setChecked(true);

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Batch Download (" + toDownload.size() + " files)")
                    .setView(dialogView)
                    .setPositiveButton("Download All", (dialog, which) -> {
                        boolean decrypt = cbDecryptOption.isChecked();
                        Toast.makeText(requireContext(), "Downloading " + toDownload.size() + " files in background...", Toast.LENGTH_SHORT).show();
                        TransferManager.getInstance(requireContext()).enqueueDownloadBatch(toDownload, decrypt);
                        AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started batch download of " + toDownload.size() + " files (decrypt=" + decrypt + ")");
                        exitMultiSelectMode();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            Toast.makeText(requireContext(), "Downloading " + toDownload.size() + " files in background...", Toast.LENGTH_SHORT).show();
            TransferManager.getInstance(requireContext()).enqueueDownloadBatch(toDownload, true);
            AuditLogger.log(requireContext(), AuditLogger.ACTION_DOWNLOAD_START, "Started batch download of " + toDownload.size() + " files");
            exitMultiSelectMode();
        }
    }

    private void handleBatchDelete() {
        if (selectedFileIds.isEmpty()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + selectedFileIds.size() + " Files?")
                .setMessage("Are you sure you want to delete these files from your encrypted vault? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    for (Long id : selectedFileIds) {
                        apiService.deleteFile(id).enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                performMetadataSearchOrLoad();
                            }
                            @Override
                            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {}
                        });
                    }
                    exitMultiSelectMode();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        MaterialCheckBox cbDecryptOption = dialogView.findViewById(R.id.cbDecryptOption);

        tvDownloadFileName.setText("Save \"" + file.getOriginalFilename() + "\" to Downloads folder.");
        cbDecryptOption.setChecked(true);

        new MaterialAlertDialogBuilder(requireContext())
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

            // Multi-select checkbox
            if (isMultiSelectMode) {
                holder.cbFileSelect.setVisibility(View.VISIBLE);
                holder.cbFileSelect.setChecked(selectedFileIds.contains(file.getId()));
                holder.cbFileSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) selectedFileIds.add(file.getId());
                    else selectedFileIds.remove(file.getId());
                    tvSelectedCount.setText(selectedFileIds.size() + " selected");
                });
            } else {
                holder.cbFileSelect.setVisibility(View.GONE);
            }

            // Card click
            holder.itemView.setOnClickListener(v -> {
                if (isMultiSelectMode) {
                    if (selectedFileIds.contains(file.getId())) selectedFileIds.remove(file.getId());
                    else selectedFileIds.add(file.getId());
                    tvSelectedCount.setText(selectedFileIds.size() + " selected");
                    notifyItemChanged(position);
                } else {
                    Intent intent = new Intent(ctx, FileViewerActivity.class);
                    intent.putExtra(FileViewerActivity.EXTRA_FILE_ID, file.getId());
                    intent.putExtra(FileViewerActivity.EXTRA_FILE_NAME, file.getOriginalFilename());
                    intent.putExtra(FileViewerActivity.EXTRA_CONTENT_TYPE, file.getContentType());
                    intent.putExtra(FileViewerActivity.EXTRA_FILE_SIZE, file.getFileSize());
                    startActivity(intent);
                }
            });

            // Card long click -> enter multi-select
            holder.itemView.setOnLongClickListener(v -> {
                if (!isMultiSelectMode) {
                    enterMultiSelectMode(file);
                }
                return true;
            });

            // Direct Actions: [Download] [Delete] (matching screenshot reference)
            if (holder.layoutCardActions != null) {
                holder.layoutCardActions.setVisibility(isMultiSelectMode ? View.GONE : View.VISIBLE);
            }

            holder.btnDownload.setOnClickListener(v -> downloadSingleFile(file));

            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(ctx)
                        .setTitle("Delete File")
                        .setMessage("Are you sure you want to delete \"" + file.getOriginalFilename() + "\" from your encrypted vault? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            apiService.deleteFile(file.getId()).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                    performMetadataSearchOrLoad();
                                }
                                @Override
                                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                    Toast.makeText(ctx, "Delete failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return displayedFiles.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
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
