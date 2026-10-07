package com.ciphervault.app.main.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.ciphervault.app.R;
import com.ciphervault.app.main.model.Transfer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.ViewHolder> {
    private List<Transfer> transfers = new ArrayList<>();
    
    public interface OnTransferActionListener {
        void onCancel(Transfer transfer);
        void onClick(Transfer transfer);
    }
    
    private final OnTransferActionListener listener;

    public TransferAdapter(OnTransferActionListener listener) {
        this.listener = listener;
    }

    public void setTransfers(List<Transfer> newTransfers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return transfers.size(); }
            @Override
            public int getNewListSize() { return newTransfers.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(transfers.get(oldItemPosition).id, newTransfers.get(newItemPosition).id);
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Transfer oldT = transfers.get(oldItemPosition);
                Transfer newT = newTransfers.get(newItemPosition);
                return Objects.equals(oldT.status, newT.status) &&
                       Objects.equals(oldT.bytesTransferred, newT.bytesTransferred);
            }
        });
        
        this.transfers = new ArrayList<>(newTransfers);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transfer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transfer t = transfers.get(position);
        holder.tvTransferName.setText(t.filename != null ? t.filename : "File");
        
        boolean isDownload = "DOWNLOAD".equalsIgnoreCase(t.transferType);
        holder.ivTransferIcon.setImageResource(isDownload ? R.drawable.ic_lucide_download : R.drawable.ic_lucide_upload);

        boolean isCompleted = "COMPLETED".equalsIgnoreCase(t.status);
        boolean isFailed = "FAILED".equalsIgnoreCase(t.status);
        boolean isCancelled = "CANCELLED".equalsIgnoreCase(t.status);

        holder.tvTransferStatus.setText(t.status != null ? t.status : "In progress");
        
        long total = t.totalBytes != null && t.totalBytes > 0 ? t.totalBytes : 1;
        long current = t.bytesTransferred != null ? t.bytesTransferred : 0;
        int pct = (int)((current * 100) / total);
        if (isCompleted) pct = 100;
        holder.pbTransfer.setProgress(pct);
        
        if (isCompleted) {
            holder.btnTransferAction.setImageResource(R.drawable.ic_lucide_check);
            holder.btnTransferAction.setColorFilter(android.graphics.Color.parseColor("#2E7D32"));
            holder.btnTransferAction.setEnabled(false);
        } else if (isFailed || isCancelled) {
            holder.btnTransferAction.setVisibility(View.GONE);
        } else {
            holder.btnTransferAction.setVisibility(View.VISIBLE);
            holder.btnTransferAction.setImageResource(R.drawable.ic_lucide_x);
            holder.btnTransferAction.setColorFilter(android.graphics.Color.parseColor("#8E8E93"));
            holder.btnTransferAction.setEnabled(true);
            holder.btnTransferAction.setOnClickListener(v -> listener.onCancel(t));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(t));
    }

    @Override
    public int getItemCount() {
        return transfers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivTransferIcon;
        TextView tvTransferName;
        TextView tvTransferStatus;
        ProgressBar pbTransfer;
        ImageButton btnTransferAction;
        ViewHolder(View v) {
            super(v);
            ivTransferIcon = v.findViewById(R.id.ivTransferIcon);
            tvTransferName = v.findViewById(R.id.tvTransferName);
            tvTransferStatus = v.findViewById(R.id.tvTransferStatus);
            pbTransfer = v.findViewById(R.id.pbTransfer);
            btnTransferAction = v.findViewById(R.id.btnTransferAction);
        }
    }
}
