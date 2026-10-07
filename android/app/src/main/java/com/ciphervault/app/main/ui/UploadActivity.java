package com.ciphervault.app.main.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UploadActivity extends AppCompatActivity implements UploadQueueManager.QueueListener {

    private TextView tvBatchTitle;
    private TextView tvBatchProgressText;
    private ProgressBar pbBatchOverall;
    private TextView tvBatchBytes;
    private TextView tvBatchPercentage;
    private MaterialButton btnUploadAll;
    private MaterialButton btnCancelAll;
    private MaterialButton btnClearCompleted;

    private View cardBatchSummary;
    private View viewEmptyQueue;
    private RecyclerView rvUploadQueue;
    private UploadQueueAdapter adapter;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleIncomingIntent(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.uploadRoot), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            int extraSpace = (int) (8 * getResources().getDisplayMetrics().density);
            toolbar.setPadding(0, sb.top + extraSpace, 0, 0);
            return insets;
        });

        cardBatchSummary = findViewById(R.id.cardBatchSummary);
        tvBatchTitle = findViewById(R.id.tvBatchTitle);
        tvBatchProgressText = findViewById(R.id.tvBatchProgressText);
        pbBatchOverall = findViewById(R.id.pbBatchOverall);
        tvBatchBytes = findViewById(R.id.tvBatchBytes);
        tvBatchPercentage = findViewById(R.id.tvBatchPercentage);
        btnUploadAll = findViewById(R.id.btnUploadAll);
        btnCancelAll = findViewById(R.id.btnCancelAll);
        btnClearCompleted = findViewById(R.id.btnClearCompleted);

        viewEmptyQueue = findViewById(R.id.viewEmptyQueue);
        rvUploadQueue = findViewById(R.id.rvUploadQueue);

        findViewById(R.id.btnAddFilesEmpty).setOnClickListener(v -> openFilePicker());

        adapter = new UploadQueueAdapter(new UploadQueueAdapter.OnQueueItemActionListener() {
            @Override
            public void onCancel(UploadQueueManager.UploadItem item) {
                UploadQueueManager.getInstance().cancelItem(item.id);
            }

            @Override
            public void onRetry(UploadQueueManager.UploadItem item) {
                UploadQueueManager.getInstance().retryItem(item.id, UploadActivity.this);
            }
        });

        rvUploadQueue.setLayoutManager(new LinearLayoutManager(this));
        rvUploadQueue.setAdapter(adapter);

        btnUploadAll.setOnClickListener(v -> UploadQueueManager.getInstance().startUploadAll(this));
        btnCancelAll.setOnClickListener(v -> UploadQueueManager.getInstance().cancelAll());
        btnClearCompleted.setOnClickListener(v -> UploadQueueManager.getInstance().removeCompleted());

        handleIncomingIntent(getIntent());

        UploadQueueManager.getInstance().addListener(this);
        refreshUI();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        UploadQueueManager.getInstance().removeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem addItem = menu.add(0, 1001, 0, "Add Files");
        addItem.setIcon(R.drawable.ic_lucide_plus);
        addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1001) {
            openFilePicker();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        List<Uri> uris = new ArrayList<>();

        if (intent.getClipData() != null) {
            ClipData clipData = intent.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri u = clipData.getItemAt(i).getUri();
                if (u != null) uris.add(u);
            }
        } else if (intent.getData() != null) {
            uris.add(intent.getData());
        } else if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            ArrayList<Uri> extraUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (extraUris != null) {
                uris.addAll(extraUris);
            } else {
                Uri singleExtra = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (singleExtra != null) uris.add(singleExtra);
            }
        }

        if (!uris.isEmpty()) {
            UploadQueueManager.getInstance().addFiles(uris, this);
            UploadQueueManager.getInstance().startUploadAll(this);
            refreshUI();
        }
    }

    private void refreshUI() {
        UploadQueueManager qm = UploadQueueManager.getInstance();
        List<UploadQueueManager.UploadItem> items = qm.getItems();

        if (items.isEmpty()) {
            cardBatchSummary.setVisibility(View.GONE);
            rvUploadQueue.setVisibility(View.GONE);
            viewEmptyQueue.setVisibility(View.VISIBLE);
        } else {
            cardBatchSummary.setVisibility(View.VISIBLE);
            rvUploadQueue.setVisibility(View.VISIBLE);
            viewEmptyQueue.setVisibility(View.GONE);

            int total = qm.getTotalCount();
            int completed = qm.getCompletedCount();
            int overallPct = qm.getOverallBatchProgress();
            long transferredBytes = qm.getTransferredBatchBytes();
            long totalBytes = qm.getTotalBatchBytes();

            tvBatchProgressText.setText(String.format(Locale.US, "%d of %d completed", completed, total));
            pbBatchOverall.setProgress(overallPct);
            tvBatchPercentage.setText(String.format(Locale.US, "%d%%", overallPct));
            tvBatchBytes.setText(String.format(Locale.US, "%s / %s", formatBytes(transferredBytes), formatBytes(totalBytes)));
        }

        adapter.setItems(items);
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

    @Override
    public void onQueueChanged() {
        refreshUI();
    }

    @Override
    public void onItemUpdated(UploadQueueManager.UploadItem item) {
        refreshUI();
    }
}
