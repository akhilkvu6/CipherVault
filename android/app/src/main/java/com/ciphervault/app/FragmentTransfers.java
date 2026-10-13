package com.ciphervault.app;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.transfer.TransferBatch;
import com.ciphervault.app.transfer.TransferItem;
import com.ciphervault.app.transfer.TransferListener;
import com.ciphervault.app.transfer.TransferManager;
import com.ciphervault.app.transfer.TransferState;
import com.ciphervault.app.transfer.TransferType;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Unified Log screen displaying all file transfers, security actions, account events,
 * and system operations in a single persistent chronological timeline.
 */
public class FragmentTransfers extends Fragment implements TransferListener {

    private TextView tvTransfersSubtitle;
    private RecyclerView rvUnifiedLog;
    private View layoutLogEmpty;

    private ApiService apiService;
    private final List<UnifiedTimelineItem> timelineItems = new ArrayList<>();
    private UnifiedLogAdapter adapter;

    // Timeline item wrapper for unifying active transfers and historical audit entries
    public static class UnifiedTimelineItem {
        public static final int TYPE_ACTIVE_TRANSFER = 0;
        public static final int TYPE_AUDIT_EVENT = 1;

        public final int itemType;
        public TransferItem transferItem;
        public AuditLogger.AuditEntry auditEntry;
        public long timestampMillis;

        public UnifiedTimelineItem(TransferItem transferItem) {
            this.itemType = TYPE_ACTIVE_TRANSFER;
            this.transferItem = transferItem;
            this.timestampMillis = System.currentTimeMillis();
        }

        public UnifiedTimelineItem(AuditLogger.AuditEntry auditEntry) {
            this.itemType = TYPE_AUDIT_EVENT;
            this.auditEntry = auditEntry;
            this.timestampMillis = auditEntry.timestampMillis > 0 ? auditEntry.timestampMillis : System.currentTimeMillis();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        apiService = ApiClient.getApiService(requireContext());

        tvTransfersSubtitle = view.findViewById(R.id.tvTransfersSubtitle);
        if (tvTransfersSubtitle != null) {
            tvTransfersSubtitle.setText("Your activity and security history");
        }

        rvUnifiedLog = view.findViewById(R.id.rvUnifiedLog);
        layoutLogEmpty = view.findViewById(R.id.layoutLogEmpty);

        if (rvUnifiedLog != null) {
            rvUnifiedLog.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new UnifiedLogAdapter();
            rvUnifiedLog.setAdapter(adapter);
        }

        loadUnifiedTimeline();

        return view;
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
    public void onResume() {
        super.onResume();
        loadUnifiedTimeline();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            loadUnifiedTimeline();
        }
    }

    /**
     * Loads active in-progress transfers from TransferManager and persisted audit history from AuditLogger,
     * merges them into a single timeline sorted newest first, and queries the backend audit log.
     */
    public void loadUnifiedTimeline() {
        if (!isAdded() || getContext() == null) return;

        List<UnifiedTimelineItem> newItems = new ArrayList<>();

        // 1. In-progress & active transfers
        List<TransferItem> activeTransfers = TransferManager.getInstance(requireContext()).getAllTransfers();
        for (TransferItem t : activeTransfers) {
            if (t.getState() == TransferState.UPLOADING || t.getState() == TransferState.DOWNLOADING
                    || t.getState() == TransferState.STARTING || t.getState() == TransferState.QUEUED) {
                newItems.add(new UnifiedTimelineItem(t));
            }
        }

        // 2. Local persistent audit events (survives app restarts)
        List<AuditLogger.AuditEntry> localEvents = AuditLogger.getAuditEntries(requireContext());
        for (AuditLogger.AuditEntry entry : localEvents) {
            newItems.add(new UnifiedTimelineItem(entry));
        }

        // Update list
        timelineItems.clear();
        timelineItems.addAll(newItems);
        updateEmptyState();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        // 3. Query backend audit events to merge and display any remote records
        apiService.getActivity().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded() || getContext() == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    Object content = response.body().get("content");
                    if (content instanceof List) {
                        boolean modified = false;
                        for (Object o : (List<?>) content) {
                            if (o instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> serverMap = (Map<String, Object>) o;

                                String action = serverMap.get("eventType") != null ? String.valueOf(serverMap.get("eventType")) : "Security Event";
                                String summary = serverMap.get("summary") != null ? String.valueOf(serverMap.get("summary")) : "Vault operation";
                                String status = serverMap.get("status") != null ? String.valueOf(serverMap.get("status")) : "SUCCESS";
                                String timestamp = serverMap.get("timestamp") != null ? String.valueOf(serverMap.get("timestamp")) : "";

                                // Check if this remote record is already present in our local history
                                boolean exists = false;
                                for (UnifiedTimelineItem existing : timelineItems) {
                                    if (existing.itemType == UnifiedTimelineItem.TYPE_AUDIT_EVENT && existing.auditEntry != null) {
                                        if (action.equalsIgnoreCase(existing.auditEntry.action) && summary.equalsIgnoreCase(existing.auditEntry.details)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                }

                                if (!exists) {
                                    AuditLogger.AuditEntry remoteEntry = new AuditLogger.AuditEntry(action, status, summary, timestamp);
                                    if (serverMap.get("totalBytes") instanceof Number) {
                                        remoteEntry.fileSize = ((Number) serverMap.get("totalBytes")).longValue();
                                    }
                                    if (serverMap.get("errorMessage") != null) {
                                        remoteEntry.errorMessage = String.valueOf(serverMap.get("errorMessage"));
                                    }
                                    timelineItems.add(new UnifiedTimelineItem(remoteEntry));
                                    modified = true;
                                }
                            }
                        }
                        if (modified && adapter != null) {
                            adapter.notifyDataSetChanged();
                            updateEmptyState();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                // Keep local records visible even if offline
            }
        });
    }

    private void updateEmptyState() {
        if (layoutLogEmpty != null) {
            layoutLogEmpty.setVisibility(timelineItems.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onTransferStateChanged(TransferItem item) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::loadUnifiedTimeline);
    }

    @Override
    public void onTransferProgress(TransferItem item) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (adapter != null) adapter.notifyItemChanged(findTransferItemPosition(item));
        });
    }

    @Override
    public void onBatchProgress(TransferBatch batch) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onBatchCompleted(TransferBatch batch) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::loadUnifiedTimeline);
    }

    private int findTransferItemPosition(TransferItem item) {
        if (item == null) return -1;
        for (int i = 0; i < timelineItems.size(); i++) {
            UnifiedTimelineItem ti = timelineItems.get(i);
            if (ti.itemType == UnifiedTimelineItem.TYPE_ACTIVE_TRANSFER && ti.transferItem != null) {
                if (item.getId() != null && item.getId().equals(ti.transferItem.getId())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private class UnifiedLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Set<String> expandedEntryIds = new HashSet<>();

        @Override
        public int getItemViewType(int position) {
            return timelineItems.get(position).itemType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == UnifiedTimelineItem.TYPE_ACTIVE_TRANSFER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_row, parent, false);
                return new ActiveTransferViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_row, parent, false);
                return new AuditEventViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder rawHolder, int position) {
            UnifiedTimelineItem item = timelineItems.get(position);

            if (rawHolder instanceof ActiveTransferViewHolder && item.transferItem != null) {
                bindActiveTransfer((ActiveTransferViewHolder) rawHolder, item.transferItem);
            } else if (rawHolder instanceof AuditEventViewHolder && item.auditEntry != null) {
                bindAuditEvent((AuditEventViewHolder) rawHolder, item.auditEntry, position);
            }
        }

        private void bindActiveTransfer(ActiveTransferViewHolder holder, TransferItem item) {
            Context ctx = holder.itemView.getContext();
            holder.tvTransferFileName.setText(item.getFileName());

            String sizeStr = item.getTotalBytes() > 0 ? FileUtils.formatStorageSize(ctx, item.getTotalBytes()) : "";
            if (item.getType() == TransferType.UPLOAD) {
                holder.ivTransferTypeIcon.setImageResource(R.drawable.ic_lucide_upload);
                holder.ivTransferTypeIcon.setContentDescription("Upload");
            } else {
                holder.ivTransferTypeIcon.setImageResource(R.drawable.ic_lucide_download);
                holder.ivTransferTypeIcon.setContentDescription("Download");
            }

            if (item.getState() == TransferState.COMPLETED) {
                holder.tvTransferSpeed.setText((!sizeStr.isEmpty() ? sizeStr + " • " : "") + "Completed");
                holder.tvTransferEta.setText("100%");
                holder.progressTransfer.setIndeterminate(false);
                holder.progressTransfer.setProgress(100);
                holder.btnCancelTransfer.setVisibility(View.GONE);
            } else if (item.getState() == TransferState.FAILED) {
                holder.tvTransferSpeed.setText((!sizeStr.isEmpty() ? sizeStr + " • " : "") + "Failed");
                holder.tvTransferEta.setText(item.getErrorMessage() != null ? item.getErrorMessage() : "Error");
                holder.progressTransfer.setIndeterminate(false);
                holder.progressTransfer.setProgress(item.getProgressPercentage());
                holder.btnCancelTransfer.setVisibility(View.GONE);
            } else if (item.getState() == TransferState.CANCELLED) {
                holder.tvTransferSpeed.setText((!sizeStr.isEmpty() ? sizeStr + " • " : "") + "Cancelled");
                holder.tvTransferEta.setText("Stopped");
                holder.progressTransfer.setIndeterminate(false);
                holder.progressTransfer.setProgress(item.getProgressPercentage());
                holder.btnCancelTransfer.setVisibility(View.GONE);
            } else {
                holder.tvTransferSpeed.setText((!sizeStr.isEmpty() ? sizeStr + " • " : "") + item.getFormattedSpeed());
                holder.tvTransferEta.setText(item.getProgressPercentage() + "% • " + item.getFormattedEta());
                if (item.getProgressPercentage() > 0) {
                    holder.progressTransfer.setIndeterminate(false);
                    holder.progressTransfer.setProgress(item.getProgressPercentage());
                } else {
                    holder.progressTransfer.setIndeterminate(true);
                }
                holder.btnCancelTransfer.setVisibility(View.VISIBLE);
                holder.btnCancelTransfer.setOnClickListener(v -> {
                    TransferManager.getInstance(requireContext()).cancelTransfer(item.getId());
                });
            }
        }

        private void bindAuditEvent(AuditEventViewHolder holder, AuditLogger.AuditEntry entry, int position) {
            Context ctx = holder.itemView.getContext();

            holder.tvActivityAction.setText(entry.action != null ? entry.action : "Security Event");
            holder.tvActivityDetails.setText(entry.details != null ? entry.details : "");
            holder.tvActivityTimestamp.setText(entry.timestamp != null ? entry.timestamp : "");

            // Status indication
            String status = entry.status != null ? entry.status : "SUCCESS";
            if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status)) {
                holder.tvActivityStatus.setVisibility(View.VISIBLE);
                holder.tvActivityStatus.setText("FAILED");
                holder.tvActivityStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_error));
            } else if ("WARNING".equalsIgnoreCase(status)) {
                holder.tvActivityStatus.setVisibility(View.VISIBLE);
                holder.tvActivityStatus.setText("WARNING");
                holder.tvActivityStatus.setTextColor(ContextCompat.getColor(ctx, R.color.vault_encrypted));
            } else {
                // Clean UI: omit redundant green SUCCESS badge
                holder.tvActivityStatus.setVisibility(View.GONE);
            }

            // Semantic event-specific Lucide icons
            setEventIcon(holder.ivActivityIcon, entry.action);

            // Expandable details container
            String entryId = entry.id != null ? entry.id : String.valueOf(position);
            boolean isExpanded = expandedEntryIds.contains(entryId);
            holder.layoutExpandedDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            holder.ivExpandCollapseChevron.setRotation(isExpanded ? 180f : 0f);

            if (isExpanded) {
                // Full timestamp
                holder.tvDetailFullTime.setText(entry.timestamp != null ? entry.timestamp : "Recorded");

                // Type & Outcome
                holder.tvDetailTypeOutcome.setText((entry.action != null ? entry.action.toUpperCase(Locale.ROOT) : "EVENT") + " • " + status.toUpperCase(Locale.ROOT));

                // File Details
                if (entry.fileSize != null && entry.fileSize > 0) {
                    holder.layoutDetailFileRow.setVisibility(View.VISIBLE);
                    String sizeText = entry.fileSize + " bytes (" + FileUtils.formatStorageSize(ctx, entry.fileSize) + ")";
                    if (entry.primaryEntity != null && !entry.primaryEntity.isEmpty()) {
                        holder.tvDetailFile.setText(entry.primaryEntity + " • " + sizeText);
                    } else {
                        holder.tvDetailFile.setText(sizeText);
                    }
                } else if (entry.primaryEntity != null && !entry.primaryEntity.isEmpty() && entry.action != null && (entry.action.contains("File") || entry.action.contains("Upload") || entry.action.contains("Download"))) {
                    holder.layoutDetailFileRow.setVisibility(View.VISIBLE);
                    holder.tvDetailFile.setText(entry.primaryEntity);
                } else {
                    holder.layoutDetailFileRow.setVisibility(View.GONE);
                }

                // Server target
                if (entry.serverTarget != null && !entry.serverTarget.isEmpty()) {
                    holder.layoutDetailServerRow.setVisibility(View.VISIBLE);
                    holder.tvDetailServer.setText(entry.serverTarget);
                } else {
                    holder.layoutDetailServerRow.setVisibility(View.GONE);
                }

                // Settings / Profile Change details
                if (entry.settingKey != null || (entry.oldValue != null && entry.newValue != null)) {
                    holder.layoutDetailSettingRow.setVisibility(View.VISIBLE);
                    String changeText = "";
                    if (entry.settingKey != null) changeText += entry.settingKey + ": ";
                    if (entry.oldValue != null) changeText += entry.oldValue + " -> ";
                    if (entry.newValue != null) changeText += entry.newValue;
                    holder.tvDetailSettingChange.setText(changeText);
                } else {
                    holder.layoutDetailSettingRow.setVisibility(View.GONE);
                }

                // Auth method
                if (entry.authMethod != null && !entry.authMethod.isEmpty()) {
                    holder.layoutDetailAuthRow.setVisibility(View.VISIBLE);
                    holder.tvDetailAuthMethod.setText(entry.authMethod);
                } else {
                    holder.layoutDetailAuthRow.setVisibility(View.GONE);
                }

                // Error details
                if (entry.errorMessage != null && !entry.errorMessage.isEmpty()) {
                    holder.layoutDetailErrorRow.setVisibility(View.VISIBLE);
                    holder.tvDetailError.setText(entry.errorMessage);
                } else {
                    holder.layoutDetailErrorRow.setVisibility(View.GONE);
                }

                // Batch breakdown
                if (entry.batchItems != null && !entry.batchItems.isEmpty()) {
                    holder.layoutDetailBatchRow.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < entry.batchItems.size(); i++) {
                        AuditLogger.BatchItemDetail b = entry.batchItems.get(i);
                        sb.append("• ").append(b.filename);
                        if (b.sizeBytes > 0) {
                            sb.append(" (").append(FileUtils.formatStorageSize(ctx, b.sizeBytes)).append(")");
                        }
                        sb.append(" - ").append(b.status);
                        if (b.errorReason != null && !b.errorReason.isEmpty()) {
                            sb.append(" [").append(b.errorReason).append("]");
                        }
                        if (i < entry.batchItems.size() - 1) sb.append("\n");
                    }
                    holder.tvDetailBatchItems.setText(sb.toString());
                } else {
                    holder.layoutDetailBatchRow.setVisibility(View.GONE);
                }
            }

            // Click listener for expand / collapse in-place
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                if (expandedEntryIds.contains(entryId)) {
                    expandedEntryIds.remove(entryId);
                } else {
                    expandedEntryIds.add(entryId);
                }
                notifyItemChanged(pos);
            });
        }

        private void setEventIcon(ImageView iv, String action) {
            if (iv == null) return;
            if (action == null) {
                iv.setImageResource(R.drawable.ic_lucide_activity);
                return;
            }
            String a = action.toLowerCase(Locale.ROOT);
            if (a.contains("upload")) {
                iv.setImageResource(R.drawable.ic_lucide_upload);
            } else if (a.contains("download")) {
                iv.setImageResource(R.drawable.ic_lucide_download);
            } else if (a.contains("delete") || a.contains("purge")) {
                iv.setImageResource(R.drawable.ic_lucide_trash_2);
            } else if (a.contains("preview") || a.contains("eye")) {
                iv.setImageResource(R.drawable.ic_lucide_eye);
            } else if (a.contains("decrypt")) {
                iv.setImageResource(R.drawable.ic_lucide_unlock);
            } else if (a.contains("duplicate") || a.contains("staged")) {
                iv.setImageResource(R.drawable.ic_lucide_file);
            } else if (a.contains("login") || a.contains("sign in") || a.contains("auth")) {
                iv.setImageResource(R.drawable.ic_lucide_user);
            } else if (a.contains("logout") || a.contains("sign out") || a.contains("expired")) {
                iv.setImageResource(R.drawable.ic_lucide_log_out);
            } else if (a.contains("server") || a.contains("connect")) {
                iv.setImageResource(R.drawable.ic_lucide_server);
            } else if (a.contains("lock") || a.contains("security") || a.contains("vault")) {
                iv.setImageResource(R.drawable.ic_lucide_shield_check);
            } else if (a.contains("biometric") || a.contains("fingerprint")) {
                iv.setImageResource(R.drawable.ic_lucide_fingerprint);
            } else if (a.contains("pass") || a.contains("key")) {
                iv.setImageResource(R.drawable.ic_lucide_key);
            } else if (a.contains("profile") || a.contains("name") || a.contains("photo")) {
                iv.setImageResource(R.drawable.ic_lucide_edit_3);
            } else if (a.contains("theme") || a.contains("appearance")) {
                iv.setImageResource(R.drawable.ic_lucide_sliders);
            } else if (a.contains("notif")) {
                iv.setImageResource(R.drawable.ic_lucide_info);
            } else if (a.contains("fail") || a.contains("error")) {
                iv.setImageResource(R.drawable.ic_lucide_alert_triangle);
            } else {
                iv.setImageResource(R.drawable.ic_lucide_activity);
            }
        }

        @Override
        public int getItemCount() {
            return timelineItems.size();
        }

        class ActiveTransferViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivTransferTypeIcon;
            final TextView tvTransferFileName;
            final TextView tvTransferSpeed;
            final TextView tvTransferEta;
            final MaterialButton btnCancelTransfer;
            final LinearProgressIndicator progressTransfer;

            ActiveTransferViewHolder(@NonNull View itemView) {
                super(itemView);
                ivTransferTypeIcon = itemView.findViewById(R.id.ivTransferTypeIcon);
                tvTransferFileName = itemView.findViewById(R.id.tvTransferFileName);
                tvTransferSpeed = itemView.findViewById(R.id.tvTransferSpeed);
                tvTransferEta = itemView.findViewById(R.id.tvTransferEta);
                btnCancelTransfer = itemView.findViewById(R.id.btnCancelTransfer);
                progressTransfer = itemView.findViewById(R.id.progressTransfer);
            }
        }

        class AuditEventViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivActivityIcon;
            final TextView tvActivityAction;
            final TextView tvActivityDetails;
            final TextView tvActivityTimestamp;
            final TextView tvActivityStatus;
            final ImageView ivExpandCollapseChevron;

            final LinearLayout layoutExpandedDetails;
            final TextView tvDetailFullTime;
            final TextView tvDetailTypeOutcome;
            final LinearLayout layoutDetailFileRow;
            final TextView tvDetailFile;
            final LinearLayout layoutDetailServerRow;
            final TextView tvDetailServer;
            final LinearLayout layoutDetailSettingRow;
            final TextView tvDetailSettingChange;
            final LinearLayout layoutDetailAuthRow;
            final TextView tvDetailAuthMethod;
            final LinearLayout layoutDetailErrorRow;
            final TextView tvDetailError;
            final LinearLayout layoutDetailBatchRow;
            final TextView tvDetailBatchItems;

            AuditEventViewHolder(@NonNull View itemView) {
                super(itemView);
                ivActivityIcon = itemView.findViewById(R.id.ivActivityIcon);
                tvActivityAction = itemView.findViewById(R.id.tvActivityAction);
                tvActivityDetails = itemView.findViewById(R.id.tvActivityDetails);
                tvActivityTimestamp = itemView.findViewById(R.id.tvActivityTimestamp);
                tvActivityStatus = itemView.findViewById(R.id.tvActivityStatus);
                ivExpandCollapseChevron = itemView.findViewById(R.id.ivExpandCollapseChevron);

                layoutExpandedDetails = itemView.findViewById(R.id.layoutExpandedDetails);
                tvDetailFullTime = itemView.findViewById(R.id.tvDetailFullTime);
                tvDetailTypeOutcome = itemView.findViewById(R.id.tvDetailTypeOutcome);
                layoutDetailFileRow = itemView.findViewById(R.id.layoutDetailFileRow);
                tvDetailFile = itemView.findViewById(R.id.tvDetailFile);
                layoutDetailServerRow = itemView.findViewById(R.id.layoutDetailServerRow);
                tvDetailServer = itemView.findViewById(R.id.tvDetailServer);
                layoutDetailSettingRow = itemView.findViewById(R.id.layoutDetailSettingRow);
                tvDetailSettingChange = itemView.findViewById(R.id.tvDetailSettingChange);
                layoutDetailAuthRow = itemView.findViewById(R.id.layoutDetailAuthRow);
                tvDetailAuthMethod = itemView.findViewById(R.id.tvDetailAuthMethod);
                layoutDetailErrorRow = itemView.findViewById(R.id.layoutDetailErrorRow);
                tvDetailError = itemView.findViewById(R.id.tvDetailError);
                layoutDetailBatchRow = itemView.findViewById(R.id.layoutDetailBatchRow);
                tvDetailBatchItems = itemView.findViewById(R.id.tvDetailBatchItems);
            }
        }
    }
}
