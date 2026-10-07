package com.ciphervault.app.main.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UploadQueueAdapter extends RecyclerView.Adapter<UploadQueueAdapter.ViewHolder> {

    public interface OnQueueItemActionListener {
        void onCancel(UploadQueueManager.UploadItem item);
        void onRetry(UploadQueueManager.UploadItem item);
    }

    private final List<UploadQueueManager.UploadItem> items = new ArrayList<>();
    private final OnQueueItemActionListener listener;

    public UploadQueueAdapter(OnQueueItemActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<UploadQueueManager.UploadItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upload_queue, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UploadQueueManager.UploadItem item = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvFilename.setText(item.filename);

        // Icon based on centralized FileVisualHelper
        int iconRes = FileVisualHelper.getFileIconResource(item.filename, item.mimeType);
        holder.ivIcon.setImageResource(iconRes);

        // Setup stage, progress, buttons, details
        holder.tvError.setVisibility(View.GONE);
        holder.ivSuccess.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.btnRetry.setVisibility(View.GONE);
        holder.pbItem.setIndeterminate(false);

        String humanTotal = item.getHumanReadableSize();

        switch (item.stage) {
            case QUEUED:
                holder.tvStage.setText("Queued");
                holder.tvStage.setTextColor(Color.parseColor("#6E6E73"));
                holder.pbItem.setProgress(0);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case PREPARING:
                holder.tvStage.setText("Preparing file...");
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setIndeterminate(true);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case HASHING:
                holder.tvStage.setText(String.format(Locale.US, "Hashing (SHA-256) %d%%", item.progress));
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setProgress(item.progress);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText(formatMetrics(item));
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case DUPLICATE_CHECK:
                holder.tvStage.setText("Checking SHA-256 duplicate...");
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setIndeterminate(true);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case ENCRYPTING:
                holder.tvStage.setText(String.format(Locale.US, "Encrypting %d%%", item.progress));
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setProgress(item.progress);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText(formatMetrics(item));
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case UPLOADING:
                holder.tvStage.setText(String.format(Locale.US, "Uploading %d%%", item.progress));
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setProgress(item.progress);
                String transferredStr = formatBytes(item.transferredBytes);
                holder.tvBytes.setText(String.format(Locale.US, "%s / %s", transferredStr, humanTotal));
                holder.tvMetrics.setText(formatMetrics(item));
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case FINALIZING:
                holder.tvStage.setText("Finalizing & Securing...");
                holder.tvStage.setTextColor(Color.parseColor("#9E6A38"));
                holder.pbItem.setIndeterminate(true);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnCancel.setVisibility(View.VISIBLE);
                break;

            case COMPLETED:
                holder.tvStage.setText("Completed & Encrypted");
                holder.tvStage.setTextColor(Color.parseColor("#2E7D32"));
                holder.pbItem.setProgress(100);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.ivSuccess.setVisibility(View.VISIBLE);
                break;

            case DUPLICATE:
                holder.tvStage.setText("Duplicate already in vault");
                holder.tvStage.setTextColor(Color.parseColor("#E65100"));
                holder.pbItem.setProgress(100);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.ivSuccess.setVisibility(View.VISIBLE);
                if (item.errorMessage != null) {
                    holder.tvError.setText(item.errorMessage);
                    holder.tvError.setVisibility(View.VISIBLE);
                }
                break;

            case FAILED:
                holder.tvStage.setText("Upload Failed");
                holder.tvStage.setTextColor(Color.parseColor("#C62828"));
                holder.pbItem.setProgress(item.progress);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnRetry.setVisibility(View.VISIBLE);
                if (item.errorMessage != null) {
                    holder.tvError.setText(item.errorMessage);
                    holder.tvError.setVisibility(View.VISIBLE);
                }
                break;

            case CANCELLED:
                holder.tvStage.setText("Cancelled");
                holder.tvStage.setTextColor(Color.parseColor("#8E8E93"));
                holder.pbItem.setProgress(0);
                holder.tvBytes.setText(humanTotal);
                holder.tvMetrics.setText("");
                holder.btnRetry.setVisibility(View.VISIBLE);
                break;
        }

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(item);
        });

        holder.btnRetry.setOnClickListener(v -> {
            if (listener != null) listener.onRetry(item);
        });
    }

    private String formatMetrics(UploadQueueManager.UploadItem item) {
        StringBuilder sb = new StringBuilder();
        if (item.speedBytesPerSec > 0) {
            sb.append(formatBytes(item.speedBytesPerSec)).append("/s");
        }
        if (item.etaSeconds > 0) {
            if (sb.length() > 0) sb.append(" • ");
            if (item.etaSeconds >= 60) {
                sb.append(String.format(Locale.US, "ETA %dm %ds", item.etaSeconds / 60, item.etaSeconds % 60));
            } else {
                sb.append(String.format(Locale.US, "ETA %ds", item.etaSeconds));
            }
        }
        return sb.toString();
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
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvFilename;
        TextView tvStage;
        ImageView ivSuccess;
        ImageButton btnCancel;
        ImageButton btnRetry;
        ProgressBar pbItem;
        TextView tvBytes;
        TextView tvMetrics;
        TextView tvError;

        ViewHolder(View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivUploadIcon);
            tvFilename = v.findViewById(R.id.tvUploadFilename);
            tvStage = v.findViewById(R.id.tvUploadStage);
            ivSuccess = v.findViewById(R.id.ivUploadStatusSuccess);
            btnCancel = v.findViewById(R.id.btnUploadCancel);
            btnRetry = v.findViewById(R.id.btnUploadRetry);
            pbItem = v.findViewById(R.id.pbUploadItem);
            tvBytes = v.findViewById(R.id.tvUploadBytes);
            tvMetrics = v.findViewById(R.id.tvUploadMetrics);
            tvError = v.findViewById(R.id.tvUploadError);
        }
    }
}
