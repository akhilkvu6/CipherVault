package com.ciphervault.app;

import android.content.Context;
import android.content.res.ColorStateList;
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
            case DOCUMENTS: typeLabel = "Document"; break;
            case AUDIO: typeLabel = "Audio"; break;
            case ARCHIVES: typeLabel = "Archive"; break;
            default: typeLabel = "File"; break;
        }

        String sizeStr = FileUtils.formatStorageSize(context, file.getFileSize());
        String dateStr = file.getCreatedAt();
        String formattedDate = (dateStr != null && !dateStr.trim().isEmpty())
                ? FileUtils.formatDisplayDateOnly(dateStr) : null;

        StringBuilder subtitle = new StringBuilder();
        subtitle.append(typeLabel).append(" • ").append(sizeStr);
        if (formattedDate != null && !formattedDate.isEmpty()) {
            subtitle.append(" • ").append(formattedDate);
        }
        holder.tvFileSize.setText(subtitle.toString());

        if (!file.hasEncryptionStatus()) {
            holder.tvEncryptionBadge.setVisibility(View.GONE);
            if (holder.tvEncryptionDot != null) holder.tvEncryptionDot.setVisibility(View.GONE);
        } else if (file.isEncrypted()) {
            int encColor = ThemeManager.getEncryptedColor(context);
            holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            holder.tvEncryptionBadge.setText("Encrypted");
            holder.tvEncryptionBadge.setTextColor(encColor);
            holder.tvEncryptionBadge.setBackgroundResource(R.drawable.bg_badge_encrypted);
            holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_badge_lock, 0, 0, 0);
            holder.tvEncryptionBadge.setCompoundDrawableTintList(ColorStateList.valueOf(encColor));
            holder.tvEncryptionBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density + 0.5f));
            if (holder.tvEncryptionDot != null) holder.tvEncryptionDot.setVisibility(View.GONE);
        } else {
            int unencColor = ContextCompat.getColor(context, R.color.vault_unencrypted);
            holder.tvEncryptionBadge.setVisibility(View.VISIBLE);
            holder.tvEncryptionBadge.setText("Not encrypted");
            holder.tvEncryptionBadge.setTextColor(unencColor);
            holder.tvEncryptionBadge.setBackgroundResource(R.drawable.bg_badge_unencrypted);
            holder.tvEncryptionBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_badge_unlock, 0, 0, 0);
            holder.tvEncryptionBadge.setCompoundDrawableTintList(ColorStateList.valueOf(unencColor));
            holder.tvEncryptionBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density + 0.5f));
            if (holder.tvEncryptionDot != null) holder.tvEncryptionDot.setVisibility(View.GONE);
        }

        switch (file.getCategory()) {
            case IMAGES:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_image);
                break;
            case VIDEOS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_video);
                break;
            case PDFS:
            case DOCUMENTS:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file_text);
                break;
            case AUDIO:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_music);
                break;
            case ARCHIVES:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_archive);
                break;
            default:
                holder.ivFileIcon.setImageResource(R.drawable.ic_lucide_file);
                break;
        }

        boolean canLoadThumb = file.getId() != null && (file.hasPreview() || ThumbnailLoader.hasCachedThumbnail(context, file.getId(), file.getSha256Checksum()));
        if (canLoadThumb) {
            ThumbnailLoader.loadThumbnail(context, file.getId(), file.getSha256Checksum(), file.hasPreview(), holder.ivThumbnail, holder.ivFileIcon,
                    file.getCategory() == StoredFile.FileCategory.VIDEOS ? holder.ivVideoBadge : null);
        } else {
            holder.ivThumbnail.setImageDrawable(null);
            holder.ivThumbnail.setVisibility(View.GONE);
            holder.ivFileIcon.setVisibility(View.VISIBLE);
            if (holder.ivVideoBadge != null) {
                holder.ivVideoBadge.setVisibility(View.GONE);
            }
        }

        if (holder.tvFileMetadataSummary != null) {
            List<String> metaParts = new ArrayList<>();
            String camera = file.getCameraInfo();
            if (camera != null && !camera.trim().isEmpty()) metaParts.add(camera.trim());
            String res = file.getResolution();
            if (res != null && !res.trim().isEmpty()) metaParts.add(res.trim());
            String codec = file.getCodec();
            if (codec != null && !codec.trim().isEmpty()) metaParts.add(codec.trim());
            String artistAuthor = file.getArtistOrAuthor();
            if (artistAuthor != null && !artistAuthor.trim().isEmpty()) metaParts.add(artistAuthor.trim());
            String duration = file.getDuration();
            if (duration != null && !duration.trim().isEmpty()) metaParts.add(duration.trim());

            if (!metaParts.isEmpty()) {
                holder.tvFileMetadataSummary.setVisibility(View.VISIBLE);
                holder.tvFileMetadataSummary.setText(android.text.TextUtils.join(" • ", metaParts));
            } else {
                holder.tvFileMetadataSummary.setVisibility(View.GONE);
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
        final TextView tvEncryptionDot;
        final TextView tvEncryptionBadge;
        final TextView tvFileMetadataSummary;
        final MaterialButton btnDownload;
        final MaterialButton btnDelete;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFileIcon = itemView.findViewById(R.id.ivFileIcon);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            ivVideoBadge = itemView.findViewById(R.id.ivVideoBadge);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvEncryptionDot = itemView.findViewById(R.id.tvEncryptionDot);
            tvEncryptionBadge = itemView.findViewById(R.id.tvEncryptionBadge);
            tvFileMetadataSummary = itemView.findViewById(R.id.tvFileMetadataSummary);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}