package com.ciphervault.app;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import com.ciphervault.app.transfer.TransferBatch;
import com.ciphervault.app.transfer.TransferItem;
import com.ciphervault.app.transfer.TransferListener;
import com.ciphervault.app.transfer.TransferManager;
import com.ciphervault.app.transfer.TransferState;
import com.ciphervault.app.transfer.TransferType;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentTransfers extends Fragment implements TransferListener {

    private TabLayout tabLayoutTransfers;
    private View containerActiveTransfers;
    private View containerAuditLog;
    private View layoutTransfersEmpty;
    private View layoutAuditEmpty;
    private RecyclerView rvActiveTransfers;
    private RecyclerView rvAuditLog;

    private ApiService apiService;
    private final List<TransferItem> localTransfersList = new ArrayList<>();
    private final List<Map<String, Object>> remoteTransfersList = new ArrayList<>();
    private final List<Map<String, Object>> auditLogList = new ArrayList<>();
    private ActiveTransfersAdapter transfersAdapter;
    private AuditLogAdapter auditAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transfers, container, false);

        apiService = ApiClient.getApiService(requireContext());

        tabLayoutTransfers = view.findViewById(R.id.tabLayoutTransfers);
        containerActiveTransfers = view.findViewById(R.id.containerActiveTransfers);
        containerAuditLog = view.findViewById(R.id.containerAuditLog);
        layoutTransfersEmpty = view.findViewById(R.id.layoutTransfersEmpty);
        layoutAuditEmpty = view.findViewById(R.id.layoutAuditEmpty);

        rvActiveTransfers = view.findViewById(R.id.rvActiveTransfers);
        rvActiveTransfers.setLayoutManager(new LinearLayoutManager(requireContext()));
        transfersAdapter = new ActiveTransfersAdapter();
        rvActiveTransfers.setAdapter(transfersAdapter);

        rvAuditLog = view.findViewById(R.id.rvAuditLog);
        rvAuditLog.setLayoutManager(new LinearLayoutManager(requireContext()));
        auditAdapter = new AuditLogAdapter();
        rvAuditLog.setAdapter(auditAdapter);

        tabLayoutTransfers.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    containerActiveTransfers.setVisibility(View.VISIBLE);
                    containerAuditLog.setVisibility(View.GONE);
                    loadActiveTransfers();
                } else {
                    containerActiveTransfers.setVisibility(View.GONE);
                    containerAuditLog.setVisibility(View.VISIBLE);
                    loadAuditLog();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadActiveTransfers();

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
        if (tabLayoutTransfers.getSelectedTabPosition() == 0) {
            loadActiveTransfers();
        } else {
            loadAuditLog();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            if (tabLayoutTransfers.getSelectedTabPosition() == 0) {
                loadActiveTransfers();
            } else {
                loadAuditLog();
            }
        }
    }

    private void loadActiveTransfers() {
        if (!isAdded()) return;

        localTransfersList.clear();
        localTransfersList.addAll(TransferManager.getInstance(requireContext()).getAllTransfers());

        if (!localTransfersList.isEmpty()) {
            layoutTransfersEmpty.setVisibility(View.GONE);
            transfersAdapter.notifyDataSetChanged();
            return;
        }

        apiService.getTransfers().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                remoteTransfersList.clear();
                if (response.isSuccessful() && response.body() != null) {
                    Object content = response.body().get("content");
                    if (content instanceof List) {
                        for (Object o : (List<?>) content) {
                            if (o instanceof Map) {
                                remoteTransfersList.add((Map<String, Object>) o);
                            }
                        }
                    }
                }
                boolean isEmpty = localTransfersList.isEmpty() && remoteTransfersList.isEmpty();
                layoutTransfersEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                transfersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                boolean isEmpty = localTransfersList.isEmpty() && remoteTransfersList.isEmpty();
                layoutTransfersEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadAuditLog() {
        if (!isAdded()) return;
        List<Map<String, Object>> localEvents = AuditLogger.getAuditEvents(requireContext());
        auditLogList.clear();
        auditLogList.addAll(localEvents);

        apiService.getActivity().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call,
                                   @NonNull Response<Map<String, Object>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Object content = response.body().get("content");
                    if (content instanceof List) {
                        for (Object o : (List<?>) content) {
                            if (o instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> serverMap = (Map<String, Object>) o;
                                Map<String, Object> converted = new HashMap<>();
                                Object act = serverMap.get("action");
                                if (act == null) act = serverMap.get("eventType");
                                converted.put("action", act != null ? act : "Security Event");

                                Object det = serverMap.get("details");
                                if (det == null) det = serverMap.get("summary");
                                converted.put("details", det != null ? det : "Vault operation");

                                Object st = serverMap.get("status");
                                converted.put("status", st != null ? st : "SUCCESS");

                                Object ts = serverMap.get("timestamp");
                                converted.put("timestamp", ts != null ? ts : "Recent");
                                auditLogList.add(converted);
                            }
                        }
                    }
                }
                layoutAuditEmpty.setVisibility(auditLogList.isEmpty() ? View.VISIBLE : View.GONE);
                auditAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                layoutAuditEmpty.setVisibility(auditLogList.isEmpty() ? View.VISIBLE : View.GONE);
                auditAdapter.notifyDataSetChanged();
            }
        });

        layoutAuditEmpty.setVisibility(auditLogList.isEmpty() ? View.VISIBLE : View.GONE);
        auditAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTransferStateChanged(TransferItem item) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::loadActiveTransfers);
    }

    @Override
    public void onTransferProgress(TransferItem item) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> transfersAdapter.notifyDataSetChanged());
    }

    @Override
    public void onBatchProgress(TransferBatch batch) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> transfersAdapter.notifyDataSetChanged());
    }

    @Override
    public void onBatchCompleted(TransferBatch batch) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::loadActiveTransfers);
    }

    private class ActiveTransfersAdapter extends RecyclerView.Adapter<ActiveTransfersAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (!localTransfersList.isEmpty()) {
                TransferItem item = localTransfersList.get(position);
                holder.tvTransferFileName.setText(item.getFileName());

                Context ctx = holder.itemView.getContext();
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
            } else if (!remoteTransfersList.isEmpty()) {
                Map<String, Object> item = remoteTransfersList.get(position);
                String name = item.get("filename") != null ? String.valueOf(item.get("filename")) : "Transfer " + position;
                String speed = item.get("speed") != null ? String.valueOf(item.get("speed")) : "Completed";
                String eta = item.get("eta") != null ? String.valueOf(item.get("eta")) : "100%";
                int progress = item.get("progress") instanceof Number ? ((Number) item.get("progress")).intValue() : 100;

                holder.tvTransferFileName.setText(name);
                holder.tvTransferSpeed.setText(speed);
                holder.tvTransferEta.setText(eta);
                holder.progressTransfer.setIndeterminate(false);
                holder.progressTransfer.setProgress(progress);
                holder.btnCancelTransfer.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return !localTransfersList.isEmpty() ? localTransfersList.size() : remoteTransfersList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivTransferTypeIcon;
            final TextView tvTransferFileName;
            final TextView tvTransferSpeed;
            final TextView tvTransferEta;
            final MaterialButton btnCancelTransfer;
            final LinearProgressIndicator progressTransfer;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivTransferTypeIcon = itemView.findViewById(R.id.ivTransferTypeIcon);
                tvTransferFileName = itemView.findViewById(R.id.tvTransferFileName);
                tvTransferSpeed = itemView.findViewById(R.id.tvTransferSpeed);
                tvTransferEta = itemView.findViewById(R.id.tvTransferEta);
                btnCancelTransfer = itemView.findViewById(R.id.btnCancelTransfer);
                progressTransfer = itemView.findViewById(R.id.progressTransfer);
            }
        }
    }

    private class AuditLogAdapter extends RecyclerView.Adapter<AuditLogAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity_row, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = auditLogList.get(position);
            String action = item.get("action") != null ? String.valueOf(item.get("action")) : "Security Event";
            String details = item.get("details") != null ? String.valueOf(item.get("details")) : "Encrypted transaction";
            String timestamp = item.get("timestamp") != null ? String.valueOf(item.get("timestamp")) : "Recent";
            String status = item.get("status") != null ? String.valueOf(item.get("status")) : "SUCCESS";

            holder.tvActivityAction.setText(action);
            holder.tvActivityDetails.setText(details);
            holder.tvActivityTimestamp.setText(timestamp);
            holder.tvActivityStatus.setText(status);

            if ("SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) {
                holder.tvActivityStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_connected));
            } else {
                holder.tvActivityStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_error));
            }
        }

        @Override
        public int getItemCount() {
            return auditLogList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvActivityAction;
            final TextView tvActivityDetails;
            final TextView tvActivityTimestamp;
            final TextView tvActivityStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvActivityAction = itemView.findViewById(R.id.tvActivityAction);
                tvActivityDetails = itemView.findViewById(R.id.tvActivityDetails);
                tvActivityTimestamp = itemView.findViewById(R.id.tvActivityTimestamp);
                tvActivityStatus = itemView.findViewById(R.id.tvActivityStatus);
            }
        }
    }
}
