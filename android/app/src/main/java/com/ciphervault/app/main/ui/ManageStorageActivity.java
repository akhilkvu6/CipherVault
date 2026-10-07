package com.ciphervault.app.main.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.model.StorageCategoryResponse;
import com.ciphervault.app.main.model.StorageSummaryResponse;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.View;

public class ManageStorageActivity extends AppCompatActivity {
    private StorageApi storageApi;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_storage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.cv_primary);
            swipeRefresh.setOnRefreshListener(this::loadStorageData);
        }

        View appBar = findViewById(R.id.appBarLayout);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manageStorageRoot), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            int extraSpace = (int) (8 * getResources().getDisplayMetrics().density);
            if (appBar != null) {
                appBar.setPadding(0, sb.top + extraSpace, 0, 0);
            }
            return insets;
        });

        storageApi = ApiClient.getClient(this).create(StorageApi.class);
        loadStorageData();
    }

    private void loadStorageData() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        storageApi.getStorageSummary().enqueue(new Callback<StorageSummaryResponse>() {
            @Override
            public void onResponse(Call<StorageSummaryResponse> call, Response<StorageSummaryResponse> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    StorageSummaryResponse summary = response.body();
                    long usedBytes = summary.getSafeUsedBytes();
                    long totalBytes = summary.getSafeTotalBytes();
                    double usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0);
                    double totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0);
                    
                    TextView tvStoragePercentage = findViewById(R.id.tvStoragePercentage);
                    TextView tvStorageTotal = findViewById(R.id.tvStorageTotal);
                    
                    tvStoragePercentage.setText(String.format(java.util.Locale.US, "%.0f%%", summary.getEffectivePercentage()));
                    tvStorageTotal.setText(String.format(java.util.Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb));
                }
            }
            @Override
            public void onFailure(Call<StorageSummaryResponse> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
                Toast.makeText(ManageStorageActivity.this, "Failed to load storage", Toast.LENGTH_SHORT).show();
            }
        });

        storageApi.getStorageCategories().enqueue(new Callback<List<StorageCategoryResponse>>() {
            @Override
            public void onResponse(Call<List<StorageCategoryResponse>> call, Response<List<StorageCategoryResponse>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    android.widget.LinearLayout llCategories = findViewById(R.id.llCategories);
                    llCategories.removeAllViews();
                    android.widget.LinearLayout llProgressBar = findViewById(R.id.llProgressBar);
                    llProgressBar.removeAllViews();
                    
                    int[] colors = {
                        android.graphics.Color.parseColor("#9E6A38"), // Primary
                        android.graphics.Color.parseColor("#B88A58"),
                        android.graphics.Color.parseColor("#D2AA78"),
                        android.graphics.Color.parseColor("#ECCA98"),
                        android.graphics.Color.parseColor("#80532B"),
                        android.graphics.Color.parseColor("#633D1F"),
                        android.graphics.Color.parseColor("#A87A4A")
                    };
                    
                    int colorIndex = 0;
                    for (StorageCategoryResponse cat : response.body()) {
                        if (cat != null && cat.getSafePercentage() > 0) {
                            int color = colors[colorIndex % colors.length];
                            
                            // Add to progress bar
                            View segment = new View(ManageStorageActivity.this);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) cat.getSafePercentage());
                            segment.setLayoutParams(lp);
                            segment.setBackgroundColor(color);
                            llProgressBar.addView(segment);
                            
                            // Add to list
                            addCategoryRow(cat, color);
                            
                            colorIndex++;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<StorageCategoryResponse>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
            }
        });

        storageApi.getLargeFiles().enqueue(new Callback<List<com.ciphervault.app.main.model.FileResponse>>() {
            @Override
            public void onResponse(Call<List<com.ciphervault.app.main.model.FileResponse>> call, Response<List<com.ciphervault.app.main.model.FileResponse>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
                TextView tvSize = findViewById(R.id.tvLargeFilesSize);
                if (response.isSuccessful() && response.body() != null) {
                    long totalSize = 0;
                    for (com.ciphervault.app.main.model.FileResponse f : response.body()) {
                        if (f != null) {
                            totalSize += f.getSafeFileSize();
                        }
                    }
                    double gb = totalSize / (1024.0 * 1024.0 * 1024.0);
                    tvSize.setText(String.format(java.util.Locale.US, "%.1f GB", gb));
                } else {
                    tvSize.setText("0 GB");
                }
            }
            @Override
            public void onFailure(Call<List<com.ciphervault.app.main.model.FileResponse>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (isDestroyed() || isFinishing()) return;
                TextView tvSize = findViewById(R.id.tvLargeFilesSize);
                tvSize.setText("Error");
            }
        });
        
        findViewById(R.id.llLargeFiles).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainAppActivity.class);
            intent.putExtra("START_TAB", "FILES");
            intent.putExtra("FILTER_CATEGORY", "large");
            intent.putExtra("FROM_MANAGE_STORAGE", true);
            startActivity(intent);
        });
    }

    private void addCategoryRow(StorageCategoryResponse cat, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        row.setPadding(0, padding/2, 0, padding/2);
        
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        row.setBackgroundResource(outValue.resourceId);
        
        // Dot
        View dot = new View(this);
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotLp.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density));
        dot.setLayoutParams(dotLp);
        
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(color);
        dot.setBackground(gd);
        row.addView(dot);
        
        // Name
        TextView tvName = new TextView(this);
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tvName.setText(cat.category);
        tvName.setTextColor(getColor(R.color.cv_text_primary));
        row.addView(tvName);
        
        // Size
        TextView tvSize = new TextView(this);
        tvSize.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        long catBytes = cat.getSafeTotalBytes();
        double sizeGb = catBytes / (1024.0 * 1024.0 * 1024.0);
        double sizeMb = catBytes / (1024.0 * 1024.0);
        if (sizeGb >= 1.0) {
            tvSize.setText(String.format(java.util.Locale.US, "%.1f GB", sizeGb));
        } else {
            tvSize.setText(String.format(java.util.Locale.US, "%.1f MB", sizeMb));
        }
        tvSize.setTextColor(getColor(R.color.cv_text_secondary));
        row.addView(tvSize);
        
        // Chevron
        android.widget.ImageView ivChevron = new android.widget.ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
            (int)(16 * getResources().getDisplayMetrics().density), 
            (int)(16 * getResources().getDisplayMetrics().density)
        );
        ivLp.setMarginStart((int)(8 * getResources().getDisplayMetrics().density));
        ivChevron.setLayoutParams(ivLp);
        ivChevron.setImageResource(R.drawable.ic_lucide_chevron_right);
        ivChevron.setColorFilter(getColor(R.color.cv_text_tertiary));
        row.addView(ivChevron);
        
        row.setOnClickListener(v -> {
            String filter = "all";
            String catName = cat.category != null ? cat.category.toLowerCase() : "";
            if (catName.contains("image")) {
                filter = "image";
            } else if (catName.contains("video")) {
                filter = "video";
            } else if (catName.contains("doc")) {
                filter = "document";
            } else if (catName.contains("audio")) {
                filter = "audio";
            } else if (catName.contains("large")) {
                filter = "large";
            } else {
                filter = cat.category;
            }

            android.content.Intent intent = new android.content.Intent(this, MainAppActivity.class);
            intent.putExtra("START_TAB", "FILES");
            intent.putExtra("FILTER_CATEGORY", filter);
            intent.putExtra("FROM_MANAGE_STORAGE", true);
            startActivity(intent);
        });
        
        android.widget.LinearLayout llCategories = findViewById(R.id.llCategories);
        llCategories.addView(row);
    }
}

