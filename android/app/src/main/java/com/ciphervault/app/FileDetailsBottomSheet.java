package com.ciphervault.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

    public interface OnPreviewRequestedListener {
        void onPreviewRequested(StoredFile file);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file, OnDownloadRequestedListener downloadListener) {
        show(context, file, downloadListener, null, null);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file, OnDownloadRequestedListener downloadListener, OnDeleteRequestedListener deleteListener) {
        show(context, file, downloadListener, deleteListener, null);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file,
                            @Nullable OnDownloadRequestedListener downloadListener,
                            @Nullable OnDeleteRequestedListener deleteListener,
                            @Nullable OnPreviewRequestedListener previewListener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_file_details, null);

        ImageView ivBottomSheetIcon = view.findViewById(R.id.ivBottomSheetIcon);
        ImageView ivBottomSheetThumbnail = view.findViewById(R.id.ivBottomSheetThumbnail);
        ImageView ivBottomSheetVideoBadge = view.findViewById(R.id.ivBottomSheetVideoBadge);
        View cardBottomSheetPreview = view.findViewById(R.id.cardBottomSheetPreview);
        TextView tvBottomSheetFileName = view.findViewById(R.id.tvBottomSheetFileName);
        TextView tvBottomSheetBadge = view.findViewById(R.id.tvBottomSheetBadge);
        TextView tvBottomSheetHash = view.findViewById(R.id.tvBottomSheetHash);
        TextView tvBottomSheetExactSize = view.findViewById(R.id.tvBottomSheetExactSize);
        TextView tvBottomSheetMime = view.findViewById(R.id.tvBottomSheetMime);
        View rowBottomSheetUploadDate = view.findViewById(R.id.rowBottomSheetUploadDate);
        TextView tvBottomSheetUploadDate = view.findViewById(R.id.tvBottomSheetUploadDate);
        Button btnBottomSheetPreview = view.findViewById(R.id.btnBottomSheetPreview);
        Button btnBottomSheetDownload = view.findViewById(R.id.btnBottomSheetDownload);
        Button btnBottomSheetDelete = view.findViewById(R.id.btnBottomSheetDelete);
        Button btnBottomSheetClose = view.findViewById(R.id.btnBottomSheetClose);

        View.OnClickListener openPreviewAction = v -> {
            dialog.dismiss();
            if (previewListener != null) {
                previewListener.onPreviewRequested(file);
            } else {
                Intent intent = new Intent(context, FileViewerActivity.class);
                intent.putExtra(FileViewerActivity.EXTRA_FILE_ID, file.getId());
                intent.putExtra(FileViewerActivity.EXTRA_FILE_NAME, file.getOriginalFilename());
                intent.putExtra(FileViewerActivity.EXTRA_CONTENT_TYPE, file.getContentType());
                intent.putExtra(FileViewerActivity.EXTRA_FILE_SIZE, file.getFileSize());
                context.startActivity(intent);
            }
        };

        if (cardBottomSheetPreview != null) {
            cardBottomSheetPreview.setOnClickListener(openPreviewAction);
        }

        if (btnBottomSheetPreview != null) {
            btnBottomSheetPreview.setOnClickListener(openPreviewAction);
        }

        if (ivBottomSheetIcon != null) {
            switch (file.getCategory()) {
                case IMAGES:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_image);
                    break;
                case VIDEOS:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_video);
                    break;
                case PDFS:
                case DOCUMENTS:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_file_text);
                    break;
                case AUDIO:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_music);
                    break;
                case ARCHIVES:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_archive);
                    break;
                default:
                    ivBottomSheetIcon.setImageResource(R.drawable.ic_lucide_file);
                    break;
            }
        }

        boolean canLoadThumb = file.getId() != null && (file.hasPreview() || ThumbnailLoader.hasCachedThumbnail(context, file.getId(), file.getSha256Checksum()));
        if (canLoadThumb && ivBottomSheetThumbnail != null) {
            ThumbnailLoader.loadThumbnail(context, file.getId(), file.getSha256Checksum(), file.hasPreview(), ivBottomSheetThumbnail, ivBottomSheetIcon,
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
            if (!file.hasEncryptionStatus()) {
                tvBottomSheetBadge.setVisibility(View.GONE);
            } else if (file.isEncrypted()) {
                int encColor = ThemeManager.getEncryptedColor(context);
                tvBottomSheetBadge.setVisibility(View.VISIBLE);
                tvBottomSheetBadge.setText("Encrypted");
                tvBottomSheetBadge.setTextColor(encColor);
                tvBottomSheetBadge.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_lucide_lock, 0, 0, 0);
                tvBottomSheetBadge.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(encColor));
                tvBottomSheetBadge.setCompoundDrawablePadding((int) (4 * context.getResources().getDisplayMetrics().density));
            } else {
                tvBottomSheetBadge.setVisibility(View.VISIBLE);
                tvBottomSheetBadge.setText("Not encrypted");
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

        if (tvBottomSheetMime != null) {
            String mime = file.getContentType();
            tvBottomSheetMime.setText(mime != null && !mime.trim().isEmpty() ? mime : "application/octet-stream");
        }

        String uploadDate = file.getCreatedAt();
        if (uploadDate != null && !uploadDate.trim().isEmpty() && rowBottomSheetUploadDate != null && tvBottomSheetUploadDate != null) {
            rowBottomSheetUploadDate.setVisibility(View.VISIBLE);
            tvBottomSheetUploadDate.setText(FileUtils.formatDisplayDateTime(uploadDate));
        } else if (rowBottomSheetUploadDate != null) {
            rowBottomSheetUploadDate.setVisibility(View.GONE);
        }

        FileMetadataDTO metadata = file.getMetadata();
        boolean hasAnyMetadata = false;

        if (metadata != null) {
            // Images: Camera make and model, lens, focal length, ISO, exposure time, F-number, date taken, and resolution
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataCamera), view.findViewById(R.id.tvMetadataCamera), file.getCameraInfo())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataLens), view.findViewById(R.id.tvMetadataLens), metadata.getLens())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataFocalLength), view.findViewById(R.id.tvMetadataFocalLength), metadata.getFocalLength())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataIso), view.findViewById(R.id.tvMetadataIso), metadata.getIso())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataExposure), view.findViewById(R.id.tvMetadataExposure), metadata.getExposureTime())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataFNumber), view.findViewById(R.id.tvMetadataFNumber), metadata.getFNumber())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataDateTaken), view.findViewById(R.id.tvMetadataDateTaken), metadata.getDateTaken())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataResolution), view.findViewById(R.id.tvMetadataResolution), file.getResolution())) {
                hasAnyMetadata = true;
            }

            // Videos: Duration, video codec, audio codec, framerate, and bitrate
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataDuration), view.findViewById(R.id.tvMetadataDuration), file.getDuration())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataVideoCodec), view.findViewById(R.id.tvMetadataVideoCodec), metadata.getVideoCodec())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataAudioCodec), view.findViewById(R.id.tvMetadataAudioCodec), metadata.getAudioCodec())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataFrameRate), view.findViewById(R.id.tvMetadataFrameRate), metadata.getFrameRate())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataBitrate), view.findViewById(R.id.tvMetadataBitrate), metadata.getBitrate())) {
                hasAnyMetadata = true;
            }

            // Audio: Title, artist, album, genre, release year
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataTitle), view.findViewById(R.id.tvMetadataTitle), metadata.getTitle())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataArtist), view.findViewById(R.id.tvMetadataArtist), metadata.getArtist())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataAlbum), view.findViewById(R.id.tvMetadataAlbum), metadata.getAlbum())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataGenre), view.findViewById(R.id.tvMetadataGenre), metadata.getGenre())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataReleaseYear), view.findViewById(R.id.tvMetadataReleaseYear), metadata.getReleaseYear())) {
                hasAnyMetadata = true;
            }

            // Documents: Author, creator, subject, keywords, creation date, modification date
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataAuthor), view.findViewById(R.id.tvMetadataAuthor), metadata.getAuthor())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataCreator), view.findViewById(R.id.tvMetadataCreator), metadata.getCreator())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataSubject), view.findViewById(R.id.tvMetadataSubject), metadata.getSubject())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataKeywords), view.findViewById(R.id.tvMetadataKeywords), metadata.getKeywords())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataDocCreated), view.findViewById(R.id.tvMetadataDocCreated), metadata.getDocCreatedDate())) {
                hasAnyMetadata = true;
            }
            if (bindMetadataRow(view.findViewById(R.id.rowMetadataDocModified), view.findViewById(R.id.tvMetadataDocModified), metadata.getDocModifiedDate())) {
                hasAnyMetadata = true;
            }
        }

        View layoutMetadataSection = view.findViewById(R.id.layoutMetadataSection);
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

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            BottomSheetBehavior<?> behavior = d.getBehavior();
            if (behavior != null) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private static boolean bindMetadataRow(View row, TextView tv, String value) {
        if (value != null && !value.trim().isEmpty() && row != null && tv != null) {
            row.setVisibility(View.VISIBLE);
            tv.setText(value.trim());
            return true;
        } else if (row != null) {
            row.setVisibility(View.GONE);
        }
        return false;
    }
}
