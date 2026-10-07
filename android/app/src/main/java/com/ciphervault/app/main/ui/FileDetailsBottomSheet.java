package com.ciphervault.app.main.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ciphervault.app.R;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FileDetailsBottomSheet extends BottomSheetDialogFragment {

    private FileResponse file;

    public FileDetailsBottomSheet() {}

    public FileDetailsBottomSheet(FileResponse file) {
        this.file = file;
    }

    public static FileDetailsBottomSheet newInstance(FileResponse file) {
        FileDetailsBottomSheet sheet = new FileDetailsBottomSheet(file);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_file_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (file == null) {
            dismiss();
            return;
        }

        TextView tvDetailFilename = view.findViewById(R.id.tvDetailFilename);
        TextView tvDetailSize = view.findViewById(R.id.tvDetailSize);
        TextView tvDetailType = view.findViewById(R.id.tvDetailType);
        TextView tvDetailEncryption = view.findViewById(R.id.tvDetailEncryption);
        TextView tvDetailHash = view.findViewById(R.id.tvDetailHash);

        if (tvDetailFilename != null) tvDetailFilename.setText(file.getSafeFilename());
        if (tvDetailSize != null) tvDetailSize.setText(String.format(java.util.Locale.US, "%,d bytes", file.getSafeFileSize()));
        if (tvDetailType != null) tvDetailType.setText(file.contentType != null ? file.contentType : "Unknown");
        
        if (tvDetailEncryption != null) tvDetailEncryption.setText(Boolean.TRUE.equals(file.encrypted) ? "AES-256-GCM" : "None");
        if (tvDetailHash != null) tvDetailHash.setText(file.sha256Hash != null ? file.sha256Hash : "Pending");

        View btnCopy = view.findViewById(R.id.btnCopyHash);
        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> {
                if (getContext() == null || tvDetailHash == null) return;
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("SHA-256", tvDetailHash.getText());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }
    }
}