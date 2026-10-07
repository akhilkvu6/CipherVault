package com.ciphervault.app.main.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ListViewHolder> {

    public interface OnFileActionListener {
        default void onThumbnailClick(FileResponse file) { onClick(file); }
        default void onDownloadRequested(FileResponse file) { onDownload(file); }
        default void onDeleteRequested(FileResponse file) { onDelete(file); }
        default void onClick(FileResponse file) {}
        default void onDownload(FileResponse file) {}
        default void onDelete(FileResponse file) {}
        default void onSelectionChanged(int count) {}
    }

    private List<FileResponse> files = new ArrayList<>();
    private final OnFileActionListener listener;
    private final Set<Long> selectedIds = new HashSet<>();
    private boolean isSelectionMode = false;
    private Long expandedFileId = null;

    public FileAdapter(OnFileActionListener listener) {
        this.listener = listener;
    }

    public void setFiles(List<FileResponse> newFiles) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return files.size(); }
            @Override
            public int getNewListSize() { return newFiles.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(files.get(oldItemPosition).id, newFiles.get(newItemPosition).id);
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                FileResponse o = files.get(oldItemPosition);
                FileResponse n = newFiles.get(newItemPosition);
                return Objects.equals(o.filename, n.filename) &&
                       Objects.equals(o.fileSize, n.fileSize) &&
                       o.encrypted == n.encrypted;
            }
        });

        this.files = new ArrayList<>(newFiles);
        diffResult.dispatchUpdatesTo(this);
    }

    public void selectAll() {
        if (selectedIds.size() == files.size()) {
            selectedIds.clear();
            isSelectionMode = false;
        } else {
            isSelectionMode = true;
            selectedIds.clear();
            for (FileResponse f : files) {
                if (f.id != null) selectedIds.add(f.id);
            }
        }
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedIds.clear();
        isSelectionMode = false;
        listener.onSelectionChanged(0);
        notifyDataSetChanged();
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public List<FileResponse> getSelectedFiles() {
        List<FileResponse> selected = new ArrayList<>();
        for (FileResponse f : files) {
            if (f.id != null && selectedIds.contains(f.id)) {
                selected.add(f);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_file, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        FileResponse file = files.get(position);
        boolean isSelected = file.id != null && selectedIds.contains(file.id);
        bindListView(holder, file, isSelected);
    }

    private void bindListView(ListViewHolder holder, FileResponse file, boolean isSelected) {
        holder.tvFilename.setText(file.filename != null ? file.filename : "Unnamed file");

        double mb = (file.fileSize != null ? file.fileSize : 0L) / (1024.0 * 1024.0);
        String sizeStr = mb >= 1000.0 ? String.format(Locale.US, "%.1f GB", mb / 1024.0) : String.format(Locale.US, "%.1f MB", mb);
        String typeStr = getFileTypeString(file);
        holder.tvFileSizeAndType.setText(sizeStr + " • " + typeStr);

        if (file.encrypted) {
            holder.tvEncryptionStatus.setText("● Encrypted");
            holder.tvEncryptionStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            holder.tvEncryptionStatus.setText("○ Not encrypted");
            holder.tvEncryptionStatus.setTextColor(Color.parseColor("#C62828"));
        }

        FileVisualHelper.bindFileVisual(holder.ivThumbnail, file, holder.itemView.getContext());

        if (isSelectionMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(isSelected);
            holder.ivExpandChevron.setVisibility(View.GONE);
        } else {
            holder.cbSelect.setVisibility(View.GONE);
            holder.cbSelect.setChecked(false);
            holder.ivExpandChevron.setVisibility(View.VISIBLE);
        }

        boolean isExpanded = !isSelectionMode && file.id != null && file.id.equals(expandedFileId);
        holder.llExpandedDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpandChevron.setRotation(isExpanded ? 90f : 0f);

        if (isExpanded) {
            holder.tvDetailMime.setText(file.contentType != null ? file.contentType : "Unknown");
            holder.tvDetailByteSize.setText(String.format(Locale.US, "%,d bytes", file.fileSize != null ? file.fileSize : 0L));
            holder.tvDetailUploaded.setText(file.createdAt != null ? file.createdAt.replace("T", " ") : "Unknown");
            holder.tvDetailHash.setText(file.sha256Hash != null ? "# " + file.sha256Hash : "# None");
            holder.tvDetailSecurity.setText(file.encrypted ? "AES-256-GCM • Integrity Verified" : "Unencrypted storage");

            StringBuilder extra = new StringBuilder();
            if (file.metadata != null) {
                if (file.metadata.width != null && file.metadata.height != null) {
                    extra.append("Dimensions: ").append(file.metadata.width).append(" × ").append(file.metadata.height).append("\n");
                }
                if (file.metadata.duration != null && !file.metadata.duration.isEmpty()) {
                    extra.append("Duration: ").append(file.metadata.duration).append("\n");
                }
                if (file.metadata.cameraMake != null || file.metadata.cameraModel != null) {
                    extra.append("Camera: ").append(file.metadata.cameraMake != null ? file.metadata.cameraMake : "")
                         .append(" ").append(file.metadata.cameraModel != null ? file.metadata.cameraModel : "").append("\n");
                }
                if (file.metadata.videoCodec != null) {
                    extra.append("Codec: ").append(file.metadata.videoCodec).append("\n");
                }
            }
            if (extra.length() > 0) {
                holder.llDetailExtraRow.setVisibility(View.VISIBLE);
                holder.tvDetailExtra.setText(extra.toString().trim());
            } else {
                holder.llDetailExtraRow.setVisibility(View.GONE);
            }

            holder.btnActionPreview.setOnClickListener(v -> listener.onThumbnailClick(file));
            holder.btnActionDownload.setOnClickListener(v -> listener.onDownloadRequested(file));
            holder.btnActionDelete.setOnClickListener(v -> listener.onDeleteRequested(file));
        }

        holder.ivThumbnail.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(file.id);
            } else {
                listener.onThumbnailClick(file);
            }
        });

        holder.rowFileMain.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(file.id);
            } else {
                if (file.id != null && file.id.equals(expandedFileId)) {
                    expandedFileId = null;
                } else {
                    expandedFileId = file.id;
                }
                notifyItemChanged(holder.getBindingAdapterPosition());
            }
        });

        holder.rowFileMain.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                expandedFileId = null;
                toggleSelection(file.id);
            }
            return true;
        });

        holder.cbSelect.setOnClickListener(v -> toggleSelection(file.id));
    }

    private void toggleSelection(Long id) {
        if (id == null) return;
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false;
        }
        listener.onSelectionChanged(selectedIds.size());
        notifyDataSetChanged();
    }

    private String getFileTypeString(FileResponse file) {
        if (file.contentType != null) {
            if (file.contentType.startsWith("image/")) return "Image";
            if (file.contentType.startsWith("video/")) return "Video";
            if (file.contentType.startsWith("audio/")) return "Audio";
            if (file.contentType.contains("pdf")) return "PDF Document";
            if (file.contentType.contains("word") || file.contentType.contains("document")) return "Word Document";
            if (file.contentType.contains("sheet") || file.contentType.contains("excel")) return "Spreadsheet";
            if (file.contentType.contains("presentation") || file.contentType.contains("powerpoint")) return "Presentation";
            if (file.contentType.contains("text/")) return "Text file";
            if (file.contentType.contains("zip") || file.contentType.contains("compressed") || file.contentType.contains("tar")) return "Archive";
        }
        if (file.filename != null) {
            int dot = file.filename.lastIndexOf('.');
            if (dot != -1 && dot < file.filename.length() - 1) {
                return file.filename.substring(dot + 1).toUpperCase(Locale.ROOT) + " file";
            }
        }
        return "File";
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ListViewHolder extends RecyclerView.ViewHolder {
        View rowFileMain;
        CheckBox cbSelect;
        ImageView ivThumbnail;
        TextView tvFilename;
        TextView tvFileSizeAndType;
        TextView tvEncryptionStatus;
        ImageView ivExpandChevron;

        LinearLayout llExpandedDetails;
        TextView tvDetailMime;
        TextView tvDetailByteSize;
        TextView tvDetailUploaded;
        TextView tvDetailHash;
        TextView tvDetailSecurity;
        View llDetailExtraRow;
        TextView tvDetailExtra;

        MaterialButton btnActionPreview;
        MaterialButton btnActionDownload;
        MaterialButton btnActionDelete;

        public ListViewHolder(@NonNull View v) {
            super(v);
            rowFileMain = v.findViewById(R.id.rowFileMain);
            cbSelect = v.findViewById(R.id.cbSelect);
            ivThumbnail = v.findViewById(R.id.ivThumbnail);
            tvFilename = v.findViewById(R.id.tvFilename);
            tvFileSizeAndType = v.findViewById(R.id.tvFileSizeAndType);
            tvEncryptionStatus = v.findViewById(R.id.tvEncryptionStatus);
            ivExpandChevron = v.findViewById(R.id.ivExpandChevron);

            llExpandedDetails = v.findViewById(R.id.llExpandedDetails);
            tvDetailMime = v.findViewById(R.id.tvDetailMime);
            tvDetailByteSize = v.findViewById(R.id.tvDetailByteSize);
            tvDetailUploaded = v.findViewById(R.id.tvDetailUploaded);
            tvDetailHash = v.findViewById(R.id.tvDetailHash);
            tvDetailSecurity = v.findViewById(R.id.tvDetailSecurity);
            llDetailExtraRow = v.findViewById(R.id.llDetailExtraRow);
            tvDetailExtra = v.findViewById(R.id.tvDetailExtra);

            btnActionPreview = v.findViewById(R.id.btnActionPreview);
            btnActionDownload = v.findViewById(R.id.btnActionDownload);
            btnActionDelete = v.findViewById(R.id.btnActionDelete);
        }
    }
}
