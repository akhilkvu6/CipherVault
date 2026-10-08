package com.ciphervault.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.util.Locale;

public class FileDetailsBottomSheet {

    public interface OnDownloadRequestedListener {
        void onDownloadRequested(StoredFile file);
    }

    public interface OnDeleteRequestedListener {
        void onDeleteRequested(StoredFile file);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file, OnDownloadRequestedListener downloadListener) {
        show(context, file, downloadListener, null);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file, OnDownloadRequestedListener downloadListener, OnDeleteRequestedListener deleteListener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_file_details, null);

        ImageView ivBottomSheetIcon = view.findViewById(R.id.ivBottomSheetIcon);
        ImageView ivBottomSheetThumbnail = view.findViewById(R.id.ivBottomSheetThumbnail);
        ImageView ivBottomSheetVideoBadge = view.findViewById(R.id.ivBottomSheetVideoBadge);
        TextView tvBottomSheetFileName = view.findViewById(R.id.tvBottomSheetFileName);
        TextView tvBottomSheetBadge = view.findViewById(R.id.tvBottomSheetBadge);
        TextView tvBottomSheetHash = view.findViewById(R.id.tvBottomSheetHash);
        TextView tvBottomSheetExactSize = view.findViewById(R.id.tvBottomSheetExactSize);
        TextView tvBottomSheetAesSpec = view.findViewById(R.id.tvBottomSheetAesSpec);
        TextView tvBottomSheetMime = view.findViewById(R.id.tvBottomSheetMime);
        Button btnBottomSheetDownload = view.findViewById(R.id.btnBottomSheetDownload);
        Button btnBottomSheetDelete = view.findViewById(R.id.btnBottomSheetDelete);
        Button btnBottomSheetClose = view.findViewById(R.id.btnBottomSheetClose);

        if (ivBottomSheetIcon != null) {
            switch (file.getCategory()) {
                case IMAGES:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_file_image);
                    break;
                case VIDEOS:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_file_video);
                    break;
                case PDFS:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_file_pdf);
                    break;
                default:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_file_general);
                    break;
            }
        }

        if (file.hasPreview() && file.getId() != null && ivBottomSheetThumbnail != null) {
            ThumbnailLoader.loadThumbnail(context, file.getId(), ivBottomSheetThumbnail, ivBottomSheetIcon,
                    file.getCategory() == StoredFile.FileCategory.VIDEOS ? ivBottomSheetVideoBadge : null);
        } else {
            if (ivBottomSheetThumbnail != null) ivBottomSheetThumbnail.setVisibility(View.GONE);
            if (ivBottomSheetVideoBadge != null) ivBottomSheetVideoBadge.setVisibility(View.GONE);
            if (ivBottomSheetIcon != null) ivBottomSheetIcon.setVisibility(View.VISIBLE);
        }

        if (tvBottomSheetFileName != null) {
            tvBottomSheetFileName.setText(file.getOriginalFilename());
        }

        if (tvBottomSheetBadge != null) {
            if (file.isEncrypted()) {
                int encColor = ThemeManager.getEncryptedColor(context);
                tvBottomSheetBadge.setText("AES-256-GCM");
                tvBottomSheetBadge.setTextColor(encColor);
                tvBottomSheetBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lock, 0, 0, 0);
                tvBottomSheetBadge.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(encColor));
                tvBottomSheetBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density));
            } else {
                tvBottomSheetBadge.setText(R.string.unencrypted_badge_label);
                tvBottomSheetBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
                tvBottomSheetBadge.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        }

        if (tvBottomSheetHash != null) {
            String hash = file.getSha256Hash();
            final String validHash = (hash != null && !hash.trim().isEmpty()) ? hash : "Not available";
            tvBottomSheetHash.setText(validHash);
            tvBottomSheetHash.setOnClickListener(v -> {
                if (hash != null && !hash.trim().isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        ClipData clip = ClipData.newPlainText("SHA-256 Checksum", hash);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        if (tvBottomSheetExactSize != null) {
            long bytes = file.getFileSize();
            String formattedFormatted = FileUtils.formatStorageSize(bytes);
            String numberString = NumberFormat.getNumberInstance(Locale.US).format(bytes);
            tvBottomSheetExactSize.setText(String.format(Locale.US, "%s bytes (%s)", numberString, formattedFormatted));
        }

        if (tvBottomSheetAesSpec != null) {
            if (file.isEncrypted()) {
                tvBottomSheetAesSpec.setText("AES-256-GCM / 128-bit Auth Tag");
                tvBottomSheetAesSpec.setTextColor(ThemeManager.getEncryptedColor(context));
            } else {
                tvBottomSheetAesSpec.setText("None (Unencrypted)");
                tvBottomSheetAesSpec.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
            }
        }

        if (tvBottomSheetMime != null) {
            String mime = file.getContentType();
            tvBottomSheetMime.setText(mime != null && !mime.trim().isEmpty() ? mime : "application/octet-stream");
        }

        View layoutMetadataSection = view.findViewById(R.id.layoutMetadataSection);
        View rowMetadataCamera = view.findViewById(R.id.rowMetadataCamera);
        TextView tvMetadataCamera = view.findViewById(R.id.tvMetadataCamera);
        View rowMetadataResolution = view.findViewById(R.id.rowMetadataResolution);
        TextView tvMetadataResolution = view.findViewById(R.id.tvMetadataResolution);
        View rowMetadataDate = view.findViewById(R.id.rowMetadataDate);
        TextView tvMetadataDate = view.findViewById(R.id.tvMetadataDate);
        View rowMetadataCodec = view.findViewById(R.id.rowMetadataCodec);
        TextView tvMetadataCodec = view.findViewById(R.id.tvMetadataCodec);
        View rowMetadataDuration = view.findViewById(R.id.rowMetadataDuration);
        TextView tvMetadataDuration = view.findViewById(R.id.tvMetadataDuration);
        View rowMetadataAuthor = view.findViewById(R.id.rowMetadataAuthor);
        TextView tvMetadataAuthor = view.findViewById(R.id.tvMetadataAuthor);
        View rowMetadataTitle = view.findViewById(R.id.rowMetadataTitle);
        TextView tvMetadataTitle = view.findViewById(R.id.tvMetadataTitle);

        FileMetadataDTO metadata = file.getMetadata();
        boolean hasAnyMetadata = false;

        if (metadata != null) {
            String camera = file.getCameraInfo();
            if (camera != null && !camera.trim().isEmpty() && rowMetadataCamera != null && tvMetadataCamera != null) {
                rowMetadataCamera.setVisibility(View.VISIBLE);
                tvMetadataCamera.setText(camera);
                hasAnyMetadata = true;
            }

            String resolution = file.getResolution();
            if (resolution != null && !resolution.trim().isEmpty() && rowMetadataResolution != null && tvMetadataResolution != null) {
                rowMetadataResolution.setVisibility(View.VISIBLE);
                tvMetadataResolution.setText(resolution);
                hasAnyMetadata = true;
            }

            String date = metadata.getDateTaken() != null ? metadata.getDateTaken() : metadata.getDocCreatedDate();
            if (date != null && !date.trim().isEmpty() && rowMetadataDate != null && tvMetadataDate != null) {
                rowMetadataDate.setVisibility(View.VISIBLE);
                tvMetadataDate.setText(date);
                hasAnyMetadata = true;
            }

            String codec = file.getCodec();
            if (codec != null && !codec.trim().isEmpty() && rowMetadataCodec != null && tvMetadataCodec != null) {
                rowMetadataCodec.setVisibility(View.VISIBLE);
                tvMetadataCodec.setText(codec);
                hasAnyMetadata = true;
            }

            String duration = file.getDuration();
            if (duration != null && !duration.trim().isEmpty() && rowMetadataDuration != null && tvMetadataDuration != null) {
                rowMetadataDuration.setVisibility(View.VISIBLE);
                tvMetadataDuration.setText(duration);
                hasAnyMetadata = true;
            }

            String author = file.getArtistOrAuthor();
            if (author != null && !author.trim().isEmpty() && rowMetadataAuthor != null && tvMetadataAuthor != null) {
                rowMetadataAuthor.setVisibility(View.VISIBLE);
                tvMetadataAuthor.setText(author);
                hasAnyMetadata = true;
            }

            String title = metadata.getTitle() != null ? metadata.getTitle() : metadata.getSubject();
            if (title != null && !title.trim().isEmpty() && rowMetadataTitle != null && tvMetadataTitle != null) {
                rowMetadataTitle.setVisibility(View.VISIBLE);
                tvMetadataTitle.setText(title);
                hasAnyMetadata = true;
            }
        }

        if (layoutMetadataSection != null) {
            layoutMetadataSection.setVisibility(hasAnyMetadata ? View.VISIBLE : View.GONE);
        }

        if (btnBottomSheetDownload != null) {
            btnBottomSheetDownload.setOnClickListener(v -> {
                dialog.dismiss();
                if (downloadListener != null) {
                    downloadListener.onDownloadRequested(file);
                }
            });
        }

        if (btnBottomSheetDelete != null) {
            if (deleteListener != null) {
                btnBottomSheetDelete.setVisibility(View.VISIBLE);
                btnBottomSheetDelete.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteListener.onDeleteRequested(file);
                });
            } else {
                btnBottomSheetDelete.setVisibility(View.GONE);
            }
        }

        if (btnBottomSheetClose != null) {
            btnBottomSheetClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setContentView(view);
        dialog.show();
    }
}
