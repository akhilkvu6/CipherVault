package com.ciphervault.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StorageDetailsActivity extends BaseActivity {

    private static final long TOTAL_QUOTA_BYTES = 1024L * 1024L * 1024L; // 1.0 GB

    private TextView tvStorageUsed;
    private TextView tvStorageAvailable;
    private LinearProgressIndicator progressStorageOverall;
    private TextView tvMetricFilesCount;
    private TextView tvMetricEncryptedCount;
    private TextView tvMetricUsagePercent;

    private MaterialCardView cardCategoryImages;
    private TextView tvImagesStats;
    private LinearProgressIndicator progressImages;

    private MaterialCardView cardCategoryVideos;
    private TextView tvVideosStats;
    private LinearProgressIndicator progressVideos;

    private MaterialCardView cardCategoryPdfs;
    private TextView tvPdfsStats;
    private LinearProgressIndicator progressPdfs;

    private MaterialCardView cardCategoryOther;
    private TextView tvOtherStats;
    private LinearProgressIndicator progressOther;

    private ImageView ivSecurityLock;
    private ImageView ivBannerLock;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_storage_details);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, ConnectionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        apiService = ApiClient.getApiService(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.storage_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        loadStorageMetrics();
    }

    private void initViews() {
        tvStorageUsed = findViewById(R.id.tvStorageUsed);
        tvStorageAvailable = findViewById(R.id.tvStorageAvailable);
        progressStorageOverall = findViewById(R.id.progressStorageOverall);
        tvMetricFilesCount = findViewById(R.id.tvMetricFilesCount);
        tvMetricEncryptedCount = findViewById(R.id.tvMetricEncryptedCount);
        tvMetricUsagePercent = findViewById(R.id.tvMetricUsagePercent);

        cardCategoryImages = findViewById(R.id.cardCategoryImages);
        tvImagesStats = findViewById(R.id.tvImagesStats);
        progressImages = findViewById(R.id.progressImages);

        cardCategoryVideos = findViewById(R.id.cardCategoryVideos);
        tvVideosStats = findViewById(R.id.tvVideosStats);
        progressVideos = findViewById(R.id.progressVideos);

        cardCategoryPdfs = findViewById(R.id.cardCategoryPdfs);
        tvPdfsStats = findViewById(R.id.tvPdfsStats);
        progressPdfs = findViewById(R.id.progressPdfs);

        cardCategoryOther = findViewById(R.id.cardCategoryOther);
        tvOtherStats = findViewById(R.id.tvOtherStats);
        progressOther = findViewById(R.id.progressOther);

        ivSecurityLock = findViewById(R.id.ivSecurityLock);
        ivBannerLock = findViewById(R.id.ivBannerLock);

        int encColor = ThemeManager.getEncryptedColor(this);
        if (ivSecurityLock != null) {
            ivSecurityLock.setImageTintList(ColorStateList.valueOf(encColor));
        }
        if (ivBannerLock != null) {
            ivBannerLock.setImageTintList(ColorStateList.valueOf(encColor));
        }
    }

    private void setupListeners() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (cardCategoryImages != null) {
            cardCategoryImages.setOnClickListener(v -> navigateToCategory("IMAGES"));
        }
        if (cardCategoryVideos != null) {
            cardCategoryVideos.setOnClickListener(v -> navigateToCategory("VIDEOS"));
        }
        if (cardCategoryPdfs != null) {
            cardCategoryPdfs.setOnClickListener(v -> navigateToCategory("PDFS"));
        }
        if (cardCategoryOther != null) {
            cardCategoryOther.setOnClickListener(v -> navigateToCategory("OTHER"));
        }
    }

    private void navigateToCategory(String categoryName) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("EXTRA_OPEN_FILES_TAB", true);
        intent.putExtra("EXTRA_FILTER_CATEGORY", categoryName);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void loadStorageMetrics() {
        apiService.getFiles().enqueue(new Callback<List<StoredFile>>() {
            @Override
            public void onResponse(@NonNull Call<List<StoredFile>> call, @NonNull Response<List<StoredFile>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    bindData(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<StoredFile>> call, @NonNull Throwable t) {
                Toast.makeText(StorageDetailsActivity.this, "Could not refresh storage metrics", Toast.LENGTH_SHORT).show();
                bindData(new ArrayList<>());
            }
        });
    }

    private void bindData(List<StoredFile> files) {
        long totalUsed = 0;
        long imagesUsed = 0;
        long videosUsed = 0;
        long docsUsed = 0;
        long otherUsed = 0;

        int imagesCount = 0;
        int videosCount = 0;
        int docsCount = 0;
        int otherCount = 0;
        int encryptedCount = 0;

        for (StoredFile file : files) {
            long size = file.getFileSize();
            totalUsed += size;
            if (file.isEncrypted()) {
                encryptedCount++;
            }

            switch (file.getCategory()) {
                case IMAGES:
                    imagesUsed += size;
                    imagesCount++;
                    break;
                case VIDEOS:
                    videosUsed += size;
                    videosCount++;
                    break;
                case PDFS:
                    docsUsed += size;
                    docsCount++;
                    break;
                default:
                    otherUsed += size;
                    otherCount++;
                    break;
            }
        }

        long availableBytes = Math.max(0L, TOTAL_QUOTA_BYTES - totalUsed);
        int totalPercent = (int) Math.min(100, (totalUsed * 100) / TOTAL_QUOTA_BYTES);

        if (tvStorageUsed != null) {
            String usedFormatted = Formatter.formatFileSize(this, totalUsed);
            tvStorageUsed.setText(String.format(Locale.US, "%s used of 1.0 GB", usedFormatted));
        }

        if (tvStorageAvailable != null) {
            String availFormatted = Formatter.formatFileSize(this, availableBytes);
            tvStorageAvailable.setText(String.format(Locale.US, "%s available", availFormatted));
        }

        if (tvMetricFilesCount != null) {
            tvMetricFilesCount.setText(String.valueOf(files.size()));
        }

        if (tvMetricEncryptedCount != null) {
            tvMetricEncryptedCount.setText(String.valueOf(encryptedCount));
        }

        if (tvMetricUsagePercent != null) {
            tvMetricUsagePercent.setText(String.format(Locale.US, "%d%% used", totalPercent));
        }

        if (progressStorageOverall != null) {
            animateProgress(progressStorageOverall, totalPercent);
        }

        // Category breakdown calculations relative to used bytes (or total quota if empty)
        long baseForPercent = totalUsed > 0 ? totalUsed : TOTAL_QUOTA_BYTES;

        int imagesPercent = totalUsed > 0 ? (int) Math.min(100, (imagesUsed * 100) / baseForPercent) : 0;
        int videosPercent = totalUsed > 0 ? (int) Math.min(100, (videosUsed * 100) / baseForPercent) : 0;
        int docsPercent = totalUsed > 0 ? (int) Math.min(100, (docsUsed * 100) / baseForPercent) : 0;
        int otherPercent = totalUsed > 0 ? (int) Math.min(100, (otherUsed * 100) / baseForPercent) : 0;

        if (tvImagesStats != null) {
            tvImagesStats.setText(String.format(Locale.US, "%s • %d%% • %d %s",
                    Formatter.formatFileSize(this, imagesUsed),
                    imagesPercent,
                    imagesCount,
                    imagesCount == 1 ? "file" : "files"));
        }
        if (progressImages != null) {
            animateProgress(progressImages, imagesPercent);
        }

        if (tvVideosStats != null) {
            tvVideosStats.setText(String.format(Locale.US, "%s • %d%% • %d %s",
                    Formatter.formatFileSize(this, videosUsed),
                    videosPercent,
                    videosCount,
                    videosCount == 1 ? "file" : "files"));
        }
        if (progressVideos != null) {
            animateProgress(progressVideos, videosPercent);
        }

        if (tvPdfsStats != null) {
            tvPdfsStats.setText(String.format(Locale.US, "%s • %d%% • %d %s",
                    Formatter.formatFileSize(this, docsUsed),
                    docsPercent,
                    docsCount,
                    docsCount == 1 ? "file" : "files"));
        }
        if (progressPdfs != null) {
            animateProgress(progressPdfs, docsPercent);
        }

        if (tvOtherStats != null) {
            tvOtherStats.setText(String.format(Locale.US, "%s • %d%% • %d %s",
                    Formatter.formatFileSize(this, otherUsed),
                    otherPercent,
                    otherCount,
                    otherCount == 1 ? "file" : "files"));
        }
        if (progressOther != null) {
            animateProgress(progressOther, otherPercent);
        }
    }

    private void animateProgress(LinearProgressIndicator indicator, int targetProgress) {
        if (indicator == null) return;
        ObjectAnimator animator = ObjectAnimator.ofInt(indicator, "progress", 0, targetProgress);
        animator.setDuration(600L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }
}
