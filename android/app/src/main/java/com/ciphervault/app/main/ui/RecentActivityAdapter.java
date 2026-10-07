package com.ciphervault.app.main.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.ciphervault.app.main.model.ActivityEvent;

import java.util.ArrayList;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    private List<ActivityEvent> events = new ArrayList<>();

    public void setEvents(List<ActivityEvent> newEvents) {
        this.events = newEvents != null ? newEvents : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityEvent event = events.get(position);
        
        String summary = event.summary;
        if (summary == null || summary.trim().isEmpty()) {
            if (event.affectedFiles != null && !event.affectedFiles.isEmpty()) {
                summary = event.affectedFiles + " " + (event.eventType != null ? event.eventType.toLowerCase() : "");
            } else {
                summary = event.eventType != null ? event.eventType : "Vault event";
            }
        }
        holder.tvSummary.setText(summary);

        String time = formatTimestamp(event.timestamp);
        holder.tvTime.setText(time);

        if (event.eventType != null) {
            String type = event.eventType.toUpperCase();
            if (type.contains("UPLOAD")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_upload);
            } else if (type.contains("DOWNLOAD")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_download);
            } else if (type.contains("DELETE")) {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_trash);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_lucide_shield_check);
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_lucide_shield_check);
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.trim().isEmpty()) {
            return "Recently";
        }
        try {
            java.time.Instant instant;
            if (rawTimestamp.endsWith("Z") || rawTimestamp.contains("+")) {
                instant = java.time.Instant.parse(rawTimestamp);
            } else {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(rawTimestamp);
                instant = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant();
            }
            long epochMillis = instant.toEpochMilli();
            long diffMillis = System.currentTimeMillis() - epochMillis;
            if (diffMillis < 60_000) {
                return "Just now";
            } else if (diffMillis < 3600_000) {
                long mins = diffMillis / 60_000;
                return mins + (mins == 1 ? " minute ago" : " minutes ago");
            } else if (diffMillis < 86400_000) {
                long hours = diffMillis / 3600_000;
                return hours + (hours == 1 ? " hour ago" : " hours ago");
            } else {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
                        .withZone(java.time.ZoneId.systemDefault());
                return formatter.format(instant);
            }
        } catch (Exception e) {
            // Fallback: cleanup raw string if ISO-like (e.g. 2026-10-07T04:21:00 -> 2026-10-07 04:21)
            return rawTimestamp.replace("T", " ").split("\\.")[0];
        }
    }


    @Override
    public int getItemCount() {
        return Math.min(events.size(), 3);
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvSummary;
        TextView tvTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivActivityIcon);
            tvSummary = itemView.findViewById(R.id.tvActivitySummary);
            tvTime = itemView.findViewById(R.id.tvActivityTime);
        }
    }
}
