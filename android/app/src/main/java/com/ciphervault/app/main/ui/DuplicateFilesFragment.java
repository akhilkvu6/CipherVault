package com.ciphervault.app.main.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.model.DuplicateFileGroup;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DuplicateFilesFragment extends BottomSheetDialogFragment implements FileAdapter.OnFileActionListener {

    private TextView tvState;
    private RecyclerView rvDuplicates;
    private FileAdapter adapter;
    private StorageApi storageApi;
    private FileApi fileApi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_duplicate_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        tvState = view.findViewById(R.id.tvState);
        rvDuplicates = view.findViewById(R.id.rvDuplicates);

        rvDuplicates.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FileAdapter(this);
        rvDuplicates.setAdapter(adapter);

        storageApi = ApiClient.getClient(getContext()).create(StorageApi.class);
        fileApi = ApiClient.getClient(getContext()).create(FileApi.class);

        loadDuplicates();
    }

    private void loadDuplicates() {
        if (storageApi == null) return;
        tvState.setText("Checking vault for SHA-256 duplicate clusters...");

        storageApi.getDuplicateFiles().enqueue(new Callback<List<DuplicateFileGroup>>() {
            @Override
            public void onResponse(Call<List<DuplicateFileGroup>> call, Response<List<DuplicateFileGroup>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<DuplicateFileGroup> groups = response.body();
                    if (groups.isEmpty()) {
                        tvState.setText("No duplicate files found in your vault.");
                        adapter.setFiles(new ArrayList<>());
                    } else {
                        List<FileResponse> allDuplicates = new ArrayList<>();
                        for (DuplicateFileGroup g : groups) {
                            if (g.files != null) {
                                allDuplicates.addAll(g.files);
                            }
                        }
                        tvState.setText(String.format(java.util.Locale.US, "%d duplicate file(s) across %d hash groups.", allDuplicates.size(), groups.size()));
                        adapter.setFiles(allDuplicates);
                    }
                } else {
                    tvState.setText("Failed to load duplicate files.");
                }
            }

            @Override
            public void onFailure(Call<List<DuplicateFileGroup>> call, Throwable t) {
                if (!isAdded()) return;
                tvState.setText("Network error: " + t.getMessage());
            }
        });
    }

    @Override
    public void onThumbnailClick(FileResponse file) {
        onClick(file);
    }

    @Override
    public void onClick(FileResponse file) {
        if (file == null || file.id == null || getContext() == null) return;
        Intent intent = new Intent(getContext(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override
    public void onDownload(FileResponse file) {
        if (file == null || getContext() == null) return;
        FileDownloadManager.showDownloadDialog(getContext(), file, null);
    }

    @Override
    public void onDelete(FileResponse file) {
        if (file == null || file.id == null || getContext() == null) return;
        fileApi.deleteFile(file.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Duplicate deleted: " + file.filename, Toast.LENGTH_SHORT).show();
                    loadDuplicates();
                } else {
                    Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
