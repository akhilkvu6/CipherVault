package com.ciphervault.app;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FileViewHolder> {

    public interface OnDownloadClickListener {
        void onDownloadClick(StoredFile file);
    }

    private final List<StoredFile> files = new ArrayList<>();
    private final OnDownloadClickListener downloadListener;

    public FilesAdapter(OnDownloadClickListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public void setFiles(List<StoredFile> newFiles) {
        this.files.clear();
        if (newFiles != null) {
            this.files.addAll(newFiles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        StoredFile file = files.get(position);
        Context context = holder.itemView.getContext();

        holder.tvFileName.setText(file.getOriginalFilename());

        long size = file.getFileSize();
        holder.tvFileSize.setText(Formatter.formatFileSize(context, size));

        if (file.isEncrypted()) {
            holder.tvEncryptionBadge.setText(R.string.encrypted_badge_label);
            holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_encrypted));
        } else {
            holder.tvEncryptionBadge.setText(R.string.unencrypted_badge_label);
            holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
        }

        switch (file.getCategory()) {
            case IMAGES:
                holder.ivFileIcon.setImageResource(R.drawable.ic_file_image);
                break;
            case VIDEOS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_file_video);
                break;
            case PDFS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_file_pdf);
                break;
            default:
                holder.ivFileIcon.setImageResource(R.drawable.ic_file_general);
                break;
        }

        holder.itemView.setOnClickListener(v -> {
            FileDetailsBottomSheet.show(context, file, f -> {
                if (downloadListener != null) {
                    downloadListener.onDownloadClick(f);
                }
            });
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (downloadListener != null) {
                downloadListener.onDownloadClick(file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivFileIcon;
        final TextView tvFileName;
        final TextView tvFileSize;
        final TextView tvEncryptionBadge;
        final MaterialButton btnDownload;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }
    }
}