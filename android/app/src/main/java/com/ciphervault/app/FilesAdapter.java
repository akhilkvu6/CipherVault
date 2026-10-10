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

    public interface OnDeleteClickListener {
        void onDeleteClick(StoredFile file);
    }

    private final List<StoredFile> files = new ArrayList<>();
    private final OnDownloadClickListener downloadListener;
    private final OnDeleteClickListener deleteListener;

    public FilesAdapter(OnDownloadClickListener downloadListener) {
        this(downloadListener, null);
    }

    public FilesAdapter(OnDownloadClickListener downloadListener, OnDeleteClickListener deleteListener) {
        this.downloadListener = downloadListener;
        this.deleteListener = deleteListener;
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

        String typeLabel;
        switch (file.getCategory()) {
            case IMAGES: typeLabel = "Image"; break;
            case VIDEOS: typeLabel = "Video"; break;
            case PDFS: typeLabel = "PDF"; break;
            default: typeLabel = "File"; break;
        }
        holder.tvFileSize.setText(typeLabel + " • " + FileUtils.formatStorageSize(file.getFileSize()));

        if (file.isEncrypted()) {
            int encColor = ThemeManager.getEncryptedColor(context);
            holder.tvEncryptionBadge.setText("Encrypted");
            holder.tvEncryptionBadge.setTextColor(encColor);
            holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lucide_lock, 0, 0, 0);
            holder.tvEncryptionBadge.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(encColor));
            holder.tvEncryptionBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density));
        } else {
            holder.tvEncryptionBadge.setText("Not encrypted");
            holder.tvEncryptionBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
            holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        switch (file.getCategory()) {
            case IMAGES:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_image);
                break;
            case VIDEOS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_video);
                break;
            case PDFS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file_text);
                break;
            default:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file);
                break;
        }

        if (file.hasPreview() && file.getId() != null) {
            ThumbnailLoader.loadThumbnail(context, file.getId(), holder.ivThumbnail, holder.ivFileIcon,
                    file.getCategory() == StoredFile.FileCategory.VIDEOS ? holder.ivVideoBadge : null);
        } else {
            holder.ivThumbnail.setVisibility(View.GONE);
            holder.ivFileIcon.setVisibility(View.VISIBLE);
            if (holder.ivVideoBadge != null) {
                holder.ivVideoBadge.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= files.size()) return;
            StoredFile currentFile = files.get(pos);
            FileDetailsBottomSheet.show(context, currentFile, f -> {
                if (downloadListener != null) {
                    downloadListener.onDownloadClick(f);
                }
            }, f -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(f);
                }
            });
        });

        holder.btnDownload.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || pos >= files.size()) return;
            if (downloadListener != null) {
                downloadListener.onDownloadClick(files.get(pos));
            }
        });

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= files.size()) return;
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(files.get(pos));
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivFileIcon;
        final ImageView ivThumbnail;
        final ImageView ivVideoBadge;
        final TextView tvFileName;
        final TextView tvFileSize;
        final TextView tvEncryptionBadge;
        final MaterialButton btnDownload;
        final MaterialButton btnDelete;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivVideoBadge = itemView.findViewById(R.id.ivVideoBadge);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}