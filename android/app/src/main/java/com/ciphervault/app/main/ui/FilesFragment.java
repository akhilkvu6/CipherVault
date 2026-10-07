package com.ciphervault.app.main.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilesFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private static final String PREFS_NAME = "ciphervault_files_prefs";
    private RecyclerView rvFiles;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View viewEmpty;
    private TextView tvEmptyMessage;
    private TextView tvSectionTitle;
    private EditText etSearch;
    private ImageButton btnSort;
    private ImageButton btnBackFilter;

    private View llMultiSelectActions;
    private CheckBox cbSelectAll;
    private TextView tvSelectedCount;

    private MaterialButton btnCatAll;
    private MaterialButton btnCatImages;
    private MaterialButton btnCatVideos;
    private MaterialButton btnCatDocs;
    private MaterialButton btnCatAudio;
    private MaterialButton btnCatArchives;
    private MaterialButton btnCatOther;
    private MaterialButton btnCatLarge;

    private FileApi fileApi;
    private StorageApi storageApi;

    private String currentSort = "createdAtDesc";
    private String currentTimeFilter = "";
    private String currentTypeFilter = "";
    private String currentEncFilter = "";
    private String activeCategory = null;
    private boolean isFromManageStorage = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        fileApi = ApiClient.getClient(getContext()).create(FileApi.class);
        storageApi = ApiClient.getClient(getContext()).create(StorageApi.class);

        rvFiles = view.findViewById(R.id.rvFiles);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        viewEmpty = view.findViewById(R.id.viewEmpty);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvSectionTitle = view.findViewById(R.id.tvSectionTitle);
        etSearch = view.findViewById(R.id.etSearch);
        btnSort = view.findViewById(R.id.btnSort);
        btnBackFilter = view.findViewById(R.id.btnBackFilter);

        llMultiSelectActions = view.findViewById(R.id.llMultiSelectActions);
        cbSelectAll = view.findViewById(R.id.cbSelectAll);
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount);

        btnCatAll = view.findViewById(R.id.btnCatAll);
        btnCatImages = view.findViewById(R.id.btnCatImages);
        btnCatVideos = view.findViewById(R.id.btnCatVideos);
        btnCatDocs = view.findViewById(R.id.btnCatDocs);
        btnCatAudio = view.findViewById(R.id.btnCatAudio);
        btnCatArchives = view.findViewById(R.id.btnCatArchives);
        btnCatOther = view.findViewById(R.id.btnCatOther);
        btnCatLarge = view.findViewById(R.id.btnCatLarge);

        setupRecyclerView();

        swipeRefresh.setOnRefreshListener(this::loadFiles);

        etSearch.setFocusable(false);
        etSearch.setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).addFragment(new SearchFragment());
            }
        });

        btnSort.setOnClickListener(v -> showSortMenu());

        btnBackFilter.setOnClickListener(v -> {
            if (isFromManageStorage && getActivity() != null) {
                getActivity().finish();
            } else {
                setCategory(null);
            }
        });

        btnCatAll.setOnClickListener(v -> setCategory(null));
        btnCatImages.setOnClickListener(v -> setCategory("image"));
        btnCatVideos.setOnClickListener(v -> setCategory("video"));
        btnCatDocs.setOnClickListener(v -> setCategory("document"));
        btnCatAudio.setOnClickListener(v -> setCategory("audio"));
        btnCatArchives.setOnClickListener(v -> setCategory("archive"));
        btnCatOther.setOnClickListener(v -> setCategory("other"));
        btnCatLarge.setOnClickListener(v -> setCategory("large"));

        cbSelectAll.setOnClickListener(v -> adapter.selectAll());
        view.findViewById(R.id.btnMultiClose).setOnClickListener(v -> adapter.clearSelection());

        view.findViewById(R.id.btnMultiDownload).setOnClickListener(v -> {
            List<FileResponse> selected = adapter.getSelectedFiles();
            if (selected.isEmpty()) return;
            showBatchDownloadDialog(selected);
        });

        view.findViewById(R.id.btnMultiDelete).setOnClickListener(v -> {
            List<FileResponse> selected = adapter.getSelectedFiles();
            if (selected.isEmpty()) return;
            showBatchDeleteDialog(selected);
        });

        if (getActivity() instanceof MainAppActivity) {
            MainAppActivity act = (MainAppActivity) getActivity();
            String initialFilter = act.getFilterCategory();
            isFromManageStorage = act.getIntent().getBooleanExtra("FROM_MANAGE_STORAGE", false);
            if (initialFilter != null && !initialFilter.isEmpty()) {
                activeCategory = initialFilter;
            }
        }

        updateCategoryButtonsUI();
        loadFiles();
    }

    private void setupRecyclerView() {
        adapter = new FileAdapter(this);
        rvFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFiles.setAdapter(adapter);
    }

    public void setCategory(String category) {
        this.activeCategory = category;
        updateCategoryButtonsUI();
        loadFiles();
    }

    private void updateCategoryButtonsUI() {
        boolean hasFilter = activeCategory != null && !activeCategory.isEmpty();
        btnBackFilter.setVisibility(hasFilter ? View.VISIBLE : View.GONE);

        resetButtonStyle(btnCatAll, false);
        resetButtonStyle(btnCatImages, false);
        resetButtonStyle(btnCatVideos, false);
        resetButtonStyle(btnCatDocs, false);
        resetButtonStyle(btnCatAudio, false);
        resetButtonStyle(btnCatArchives, false);
        resetButtonStyle(btnCatOther, false);
        resetButtonStyle(btnCatLarge, false);

        if (activeCategory == null) {
            resetButtonStyle(btnCatAll, true);
            tvSectionTitle.setText("All files");
        } else if ("image".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatImages, true);
            tvSectionTitle.setText("Images");
        } else if ("video".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatVideos, true);
            tvSectionTitle.setText("Videos");
        } else if ("document".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatDocs, true);
            tvSectionTitle.setText("Documents");
        } else if ("audio".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatAudio, true);
            tvSectionTitle.setText("Audio files");
        } else if ("archive".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatArchives, true);
            tvSectionTitle.setText("Archives & compressed");
        } else if ("other".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatOther, true);
            tvSectionTitle.setText("Other files");
        } else if ("large".equalsIgnoreCase(activeCategory)) {
            resetButtonStyle(btnCatLarge, true);
            tvSectionTitle.setText("Large files (>100 MB)");
        } else {
            tvSectionTitle.setText("Filtered files");
        }
    }

    private void resetButtonStyle(MaterialButton button, boolean selected) {
        if (button == null) return;
        if (selected) {
            button.setBackgroundColor(requireContext().getColor(R.color.cv_primary));
            button.setTextColor(requireContext().getColor(R.color.cv_on_primary));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundColor(requireContext().getColor(android.R.color.transparent));
            button.setTextColor(requireContext().getColor(R.color.cv_text_secondary));
            button.setStrokeColorResource(R.color.cv_border);
            button.setStrokeWidth(1);
        }
    }

    private void loadFiles() {
        swipeRefresh.setRefreshing(true);

        if ("large".equalsIgnoreCase(activeCategory)) {
            storageApi.getLargeFiles().enqueue(new Callback<List<FileResponse>>() {
                @Override
                public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) {
                    if (!isAdded()) return;
                    swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        List<FileResponse> files = response.body();
                        adapter.setFiles(files);
                        viewEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                        tvEmptyMessage.setText("No files over 100 MB");
                        long totalBytes = 0;
                        for (FileResponse f : files) {
                            if (f.fileSize != null) totalBytes += f.fileSize;
                        }
                        double gb = totalBytes / (1024.0 * 1024.0 * 1024.0);
                        tvSectionTitle.setText(String.format(Locale.US, "Large files (>100 MB) • %d files (%.1f GB)", files.size(), gb));
                    } else {
                        viewEmpty.setVisibility(View.VISIBLE);
                        tvEmptyMessage.setText("Failed to load large files");
                    }
                }

                @Override
                public void onFailure(Call<List<FileResponse>> call, Throwable t) {
                    if (!isAdded()) return;
                    swipeRefresh.setRefreshing(false);
                    viewEmpty.setVisibility(View.VISIBLE);
                    tvEmptyMessage.setText("Network error: " + t.getMessage());
                }
            });
            return;
        }

        fileApi.listFiles(0, 100, currentSort).enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<FileResponse> all = response.body();
                    List<FileResponse> filtered = applyAllFilters(all);
                    adapter.setFiles(filtered);
                    viewEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                    tvEmptyMessage.setText("No files match filters");
                    if (activeCategory == null) {
                        tvSectionTitle.setText(String.format(Locale.US, "All files (%d)", filtered.size()));
                    }
                } else {
                    viewEmpty.setVisibility(View.VISIBLE);
                    tvEmptyMessage.setText("Failed to load files");
                }
            }

            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                viewEmpty.setVisibility(View.VISIBLE);
                tvEmptyMessage.setText("Network error: " + t.getMessage());
            }
        });
    }

    private List<FileResponse> applyAllFilters(List<FileResponse> input) {
        if (input == null) return new ArrayList<>();
        List<FileResponse> result = new ArrayList<>();

        for (FileResponse f : input) {
            // Category / type filter
            if (activeCategory != null && !activeCategory.isEmpty()) {
                String mime = f.contentType != null ? f.contentType.toLowerCase(Locale.ROOT) : "";
                String name = f.filename != null ? f.filename.toLowerCase(Locale.ROOT) : "";

                if ("image".equalsIgnoreCase(activeCategory)) {
                    if (!mime.startsWith("image/") && !name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png") && !name.endsWith(".webp") && !name.endsWith(".gif")) {
                        continue;
                    }
                } else if ("video".equalsIgnoreCase(activeCategory)) {
                    if (!mime.startsWith("video/") && !name.endsWith(".mp4") && !name.endsWith(".mkv") && !name.endsWith(".avi") && !name.endsWith(".mov")) {
                        continue;
                    }
                } else if ("document".equalsIgnoreCase(activeCategory)) {
                    if (!mime.contains("pdf") && !mime.contains("document") && !mime.contains("msword") && !mime.contains("sheet") && !mime.contains("presentation") && !mime.startsWith("text/")
                            && !name.endsWith(".pdf") && !name.endsWith(".doc") && !name.endsWith(".docx") && !name.endsWith(".txt") && !name.endsWith(".xlsx") && !name.endsWith(".csv")) {
                        continue;
                    }
                } else if ("audio".equalsIgnoreCase(activeCategory)) {
                    if (!mime.startsWith("audio/") && !name.endsWith(".mp3") && !name.endsWith(".wav") && !name.endsWith(".m4a") && !name.endsWith(".aac") && !name.endsWith(".flac")) {
                        continue;
                    }
                } else if ("archive".equalsIgnoreCase(activeCategory)) {
                    boolean isArchive = mime.contains("zip") || mime.contains("compressed") || mime.contains("tar") || mime.contains("7z") || mime.contains("rar")
                            || name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".tgz") || name.endsWith(".rar") || name.endsWith(".7z");
                    if (!isArchive) continue;
                } else if ("other".equalsIgnoreCase(activeCategory)) {
                    boolean isImg = mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif");
                    boolean isVid = mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") || name.endsWith(".mov");
                    boolean isDoc = mime.contains("pdf") || mime.contains("document") || mime.contains("msword") || mime.contains("sheet") || mime.contains("presentation") || mime.startsWith("text/")
                            || name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".txt") || name.endsWith(".xlsx") || name.endsWith(".csv");
                    boolean isAud = mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".flac");
                    boolean isArch = mime.contains("zip") || mime.contains("compressed") || mime.contains("tar") || mime.contains("7z") || mime.contains("rar")
                            || name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".tgz") || name.endsWith(".rar") || name.endsWith(".7z");
                    if (isImg || isVid || isDoc || isAud || isArch) continue;
                } else if ("large".equalsIgnoreCase(activeCategory)) {
                    if (f.fileSize == null || f.fileSize < 100L * 1024L * 1024L) {
                        continue;
                    }
                }
            }

            // Encryption filter
            if ("encrypted".equals(currentEncFilter)) {
                if (!f.encrypted) continue;
            } else if ("unencrypted".equals(currentEncFilter)) {
                if (f.encrypted) continue;
            }

            result.add(f);
        }

        // Client-side sort fallback / consistency
        if ("nameAsc".equals(currentSort)) {
            result.sort((a, b) -> a.filename != null && b.filename != null ? a.filename.compareToIgnoreCase(b.filename) : 0);
        } else if ("nameDesc".equals(currentSort)) {
            result.sort((a, b) -> a.filename != null && b.filename != null ? b.filename.compareToIgnoreCase(a.filename) : 0);
        } else if ("largest".equals(currentSort)) {
            result.sort((a, b) -> Long.compare(b.fileSize != null ? b.fileSize : 0, a.fileSize != null ? a.fileSize : 0));
        } else if ("smallest".equals(currentSort)) {
            result.sort((a, b) -> Long.compare(a.fileSize != null ? a.fileSize : 0, b.fileSize != null ? b.fileSize : 0));
        }

        return result;
    }

    private void showSortMenu() {
        BottomSheetFilters sheet = new BottomSheetFilters();
        sheet.setInitialValues(currentSort, currentTimeFilter, currentTypeFilter, currentEncFilter);
        sheet.setListener((sort, time, type, enc) -> {
            currentSort = sort;
            currentTimeFilter = time;
            currentTypeFilter = type;
            currentEncFilter = enc;

            if (type != null && !type.isEmpty()) {
                activeCategory = type;
                updateCategoryButtonsUI();
            }
            loadFiles();
        });
        sheet.show(getChildFragmentManager(), "filters");
    }

    @Override
    public void onThumbnailClick(FileResponse file) {
        if (file == null || file.id == null) return;
        Intent intent = new Intent(getContext(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override
    public void onDownloadRequested(FileResponse file) {
        if (file == null) return;
        FileDownloadManager.showDownloadDialog(getContext(), file, new FileDownloadManager.DownloadCallback() {
            @Override
            public void onDownloadStarted(FileResponse f, boolean decrypted) {}
            @Override
            public void onDownloadFinished(FileResponse f, boolean success, File targetFile) {}
        });
    }

    @Override
    public void onDeleteRequested(FileResponse file) {
        if (file == null || file.id == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + file.filename + "?")
                .setMessage("Are you sure you want to permanently delete this file from your private vault? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    fileApi.deleteFile(file.id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "File deleted", Toast.LENGTH_SHORT).show();
                                loadFiles();
                            } else {
                                Toast.makeText(getContext(), "Failed to delete file", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Delete error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBatchDownloadDialog(List<FileResponse> selected) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Download " + selected.size() + " files")
                .setMessage("Choose download mode for selected files:")
                .setPositiveButton("Decrypt for normal download", (dialog, which) -> {
                    for (FileResponse f : selected) {
                        FileDownloadManager.startDownload(getContext(), f, true, null);
                    }
                    adapter.clearSelection();
                })
                .setNeutralButton("Download encrypted files", (dialog, which) -> {
                    for (FileResponse f : selected) {
                        FileDownloadManager.startDownload(getContext(), f, false, null);
                    }
                    adapter.clearSelection();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showBatchDeleteDialog(List<FileResponse> selected) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + selected.size() + " files?")
                .setMessage("Permanently remove these files from your private vault? This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    for (FileResponse f : selected) {
                        if (f.id != null) {
                            fileApi.deleteFile(f.id).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (isAdded()) loadFiles();
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {}
                            });
                        }
                    }
                    adapter.clearSelection();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSelectionChanged(int count) {
        if (count > 0) {
            llMultiSelectActions.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(count + " selected");
            cbSelectAll.setChecked(count == adapter.getItemCount());
        } else {
            llMultiSelectActions.setVisibility(View.GONE);
            cbSelectAll.setChecked(false);
        }
    }
}
