package com.ciphervault.app;

import android.content.Context;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.NumberFormat;
import java.util.Locale;

public class FileDetailsBottomSheet {

    public interface OnDownloadRequestedListener {
        void onDownloadRequested(StoredFile file);
    }

    public static void show(@NonNull Context context, @NonNull StoredFile file, OnDownloadRequestedListener downloadListener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_file_details, null);

        ImageView ivBottomSheetIcon = view.findViewById(R.id.ivBottomSheetIcon);
        TextView tvBottomSheetFileName = view.findViewById(R.id.tvBottomSheetFileName);
        TextView tvBottomSheetBadge = view.findViewById(R.id.tvBottomSheetBadge);
        TextView tvBottomSheetHash = view.findViewById(R.id.tvBottomSheetHash);
        TextView tvBottomSheetTimestamp = view.findViewById(R.id.tvBottomSheetTimestamp);
        TextView tvBottomSheetExactSize = view.findViewById(R.id.tvBottomSheetExactSize);
        TextView tvBottomSheetAesSpec = view.findViewById(R.id.tvBottomSheetAesSpec);
        TextView tvBottomSheetMime = view.findViewById(R.id.tvBottomSheetMime);
        Button btnBottomSheetDownload = view.findViewById(R.id.btnBottomSheetDownload);
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

        if (tvBottomSheetFileName != null) {
            tvBottomSheetFileName.setText(file.getOriginalFilename());
        }

        if (tvBottomSheetBadge != null) {
            if (file.isEncrypted()) {
                tvBottomSheetBadge.setText(R.string.encrypted_badge_label);
                tvBottomSheetBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_encrypted));
            } else {
                tvBottomSheetBadge.setText(R.string.unencrypted_badge_label);
                tvBottomSheetBadge.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
            }
        }

        if (tvBottomSheetHash != null) {
            String hash = file.getSha256Hash();
            tvBottomSheetHash.setText(hash != null && !hash.trim().isEmpty() ? hash : "Not available");
        }

        if (tvBottomSheetTimestamp != null) {
            String createdAt = file.getCreatedAt();
            tvBottomSheetTimestamp.setText(createdAt != null && !createdAt.trim().isEmpty() ? createdAt : "Recently uploaded");
        }

        if (tvBottomSheetExactSize != null) {
            long bytes = file.getFileSize();
            String formattedFormatted = Formatter.formatFileSize(context, bytes);
            String numberString = NumberFormat.getNumberInstance(Locale.US).format(bytes);
            tvBottomSheetExactSize.setText(String.format(Locale.US, "%s bytes (%s)", numberString, formattedFormatted));
        }

        if (tvBottomSheetAesSpec != null) {
            if (file.isEncrypted()) {
                tvBottomSheetAesSpec.setText("AES-256-GCM / 128-bit Auth Tag");
                tvBottomSheetAesSpec.setTextColor(ContextCompat.getColor(context, R.color.vault_encrypted));
            } else {
                tvBottomSheetAesSpec.setText("None (Unencrypted)");
                tvBottomSheetAesSpec.setTextColor(ContextCompat.getColor(context, R.color.vault_unencrypted));
            }
        }

        if (tvBottomSheetMime != null) {
            String mime = file.getContentType();
            tvBottomSheetMime.setText(mime != null && !mime.trim().isEmpty() ? mime : "application/octet-stream");
        }

        if (btnBottomSheetDownload != null) {
            btnBottomSheetDownload.setOnClickListener(v -> {
                dialog.dismiss();
                if (downloadListener != null) {
                    downloadListener.onDownloadRequested(file);
                }
            });
        }

        if (btnBottomSheetClose != null) {
            btnBottomSheetClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setContentView(view);
        dialog.show();
    }
}