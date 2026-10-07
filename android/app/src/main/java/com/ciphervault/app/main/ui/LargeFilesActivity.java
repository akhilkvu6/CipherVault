package com.ciphervault.app.main.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.session.AuthSessionManager;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.model.FileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LargeFilesActivity extends AppCompatActivity implements FileAdapter.OnFileActionListener {
    private RecyclerView rvFiles;
    private FileAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View viewEmpty;
    private TextView tvSubtitle;
    private LinearLayout llMultiSelectActions;
    private TextView tvSelectedCount;

    private StorageApi storageApi;
    private FileApi fileApi;
    private AuthSessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_large_files);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        storageApi = ApiClient.getClient(this).create(StorageApi.class);
        fileApi = ApiClient.getClient(this).create(FileApi.class);
        sessionManager = new AuthSessionManager(this);

        rvFiles = findViewById(R.id.rvFiles);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        viewEmpty = findViewById(R.id.viewEmpty);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        llMultiSelectActions = findViewById(R.id.llMultiSelectActions);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);

        adapter = new FileAdapter(this);
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadFiles);

        findViewById(R.id.btnMultiClose).setOnClickListener(v -> adapter.clearSelection());
        findViewById(R.id.btnMultiDownload).setOnClickListener(v -> {
            for (FileResponse f : adapter.getSelectedFiles()) {
                onDownload(f);
            }
            adapter.clearSelection();
        });
        findViewById(R.id.btnMultiDelete).setOnClickListener(v -> {
            for (FileResponse f : adapter.getSelectedFiles()) {
                onDelete(f);
            }
            adapter.clearSelection();
        });

        loadFiles();
    }

    private void loadFiles() {
        swipeRefresh.setRefreshing(true);
        storageApi.getLargeFiles().enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) { if (isDestroyed() || isFinishing()) return;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<FileResponse> files = response.body();
                    adapter.setFiles(files);
                    viewEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
                    
                    long totalSize = 0;
                    for (FileResponse f : files) {
                        totalSize += f.getSafeFileSize();
                    }
                    double gb = totalSize / (1024.0 * 1024.0 * 1024.0);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(files.size() + " large files");
                    }
                    tvSubtitle.setText(String.format("Clear space quickly by deleting large files. (%.1f GB total)", gb));
                }
            }
            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) { if (isDestroyed() || isFinishing()) return;
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    @Override
    public void onThumbnailClick(FileResponse file) {
        if (file == null || file.id == null) return;
        Intent intent = new Intent(this, FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override
    public void onDownloadRequested(FileResponse file) {
        if (file == null) return;
        FileDownloadManager.showDownloadDialog(this, file, null);
    }

    @Override
    public void onDeleteRequested(FileResponse file) {
        if (file == null || file.id == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + file.filename + "?")
                .setMessage("Are you sure you want to permanently delete this file? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    fileApi.deleteFile(file.id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (isDestroyed() || isFinishing()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(LargeFilesActivity.this, "File deleted", Toast.LENGTH_SHORT).show();
                                loadFiles();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDownload(FileResponse file) {
        onDownloadRequested(file);
    }

    @Override
    public void onDelete(FileResponse file) {
        onDeleteRequested(file);
    }

    @Override
    public void onClick(FileResponse file) {
        onThumbnailClick(file);
    }
    
    @Override
    public void onSelectionChanged(int count) {
        if (count > 0) {
            llMultiSelectActions.setVisibility(View.VISIBLE);
            tvSelectedCount.setText(count + " selected");
        } else {
            llMultiSelectActions.setVisibility(View.GONE);
        }
    }
}
