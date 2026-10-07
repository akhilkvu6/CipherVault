package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.AnalyticsApi;
import com.ciphervault.app.main.model.AnalyticsResponse;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsFragment extends Fragment {

    private TextView tvStorageGrowth;
    private TextView tvStorageInOut;
    private TextView tvUploads;
    private TextView tvUploadedVolume;
    private TextView tvDownloads;
    private TextView tvDownloadedVolume;
    private TextView tvFilesAdded;
    private TextView tvFilesDeleted;
    private ChipGroup chipGroupPeriod;

    private AnalyticsApi api;
    private String currentPeriod = "today";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        tvStorageGrowth = view.findViewById(R.id.tvStorageGrowth);
        tvStorageInOut = view.findViewById(R.id.tvStorageInOut);
        tvUploads = view.findViewById(R.id.tvUploads);
        tvUploadedVolume = view.findViewById(R.id.tvUploadedVolume);
        tvDownloads = view.findViewById(R.id.tvDownloads);
        tvDownloadedVolume = view.findViewById(R.id.tvDownloadedVolume);
        tvFilesAdded = view.findViewById(R.id.tvFilesAdded);
        tvFilesDeleted = view.findViewById(R.id.tvFilesDeleted);
        chipGroupPeriod = view.findViewById(R.id.chipGroupPeriod);

        api = ApiClient.getClient(getContext()).create(AnalyticsApi.class);

        chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipToday)) {
                currentPeriod = "today";
            } else if (checkedIds.contains(R.id.chipWeek)) {
                currentPeriod = "week";
            } else if (checkedIds.contains(R.id.chipMonth)) {
                currentPeriod = "month";
            }
            loadAnalytics(currentPeriod);
        });

        loadAnalytics(currentPeriod);
    }

    private void loadAnalytics(String period) {
        if (api == null || getContext() == null) return;

        api.getAnalytics(period).enqueue(new Callback<AnalyticsResponse>() {
            @Override
            public void onResponse(Call<AnalyticsResponse> call, Response<AnalyticsResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    AnalyticsResponse data = response.body();

                    long growth = data.storageGrowth != null ? data.storageGrowth : 0L;
                    long added = data.storageAdded != null ? data.storageAdded : 0L;
                    long removed = data.storageRemoved != null ? data.storageRemoved : 0L;
                    long upCount = data.uploads != null ? data.uploads : 0L;
                    long downCount = data.downloads != null ? data.downloads : 0L;
                    long upBytes = data.uploadedBytes != null ? data.uploadedBytes : 0L;
                    long downBytes = data.downloadedBytes != null ? data.downloadedBytes : 0L;
                    long addedCount = data.filesAdded != null ? data.filesAdded : 0L;
                    long deletedCount = data.filesDeleted != null ? data.filesDeleted : 0L;

                    tvStorageGrowth.setText(formatBytes(growth));
                    tvStorageInOut.setText("+" + formatBytes(added) + " added  •  -" + formatBytes(removed) + " deleted");

                    tvUploads.setText(String.valueOf(upCount));
                    tvUploadedVolume.setText(formatBytes(upBytes) + " total");

                    tvDownloads.setText(String.valueOf(downCount));
                    tvDownloadedVolume.setText(formatBytes(downBytes) + " total");

                    tvFilesAdded.setText(String.valueOf(addedCount));
                    tvFilesDeleted.setText(String.valueOf(deletedCount));
                } else {
                    Toast.makeText(getContext(), "Failed to load analytics: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnalyticsResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1.0) return String.format(Locale.US, "%.1f GB", gb);
        if (mb >= 1.0) return String.format(Locale.US, "%.1f MB", mb);
        if (kb >= 1.0) return String.format(Locale.US, "%.1f KB", kb);
        return bytes + " B";
    }
}
