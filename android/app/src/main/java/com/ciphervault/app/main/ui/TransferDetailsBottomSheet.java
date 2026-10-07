package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ciphervault.app.R;
import com.ciphervault.app.main.model.Transfer;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TransferDetailsBottomSheet extends BottomSheetDialogFragment {
    
    private Transfer transfer;
    
    public static TransferDetailsBottomSheet newInstance(Transfer transfer) {
        TransferDetailsBottomSheet fragment = new TransferDetailsBottomSheet();
        fragment.transfer = transfer;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_transfer_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (transfer == null) {
            dismiss();
            return;
        }
        
        ((TextView) view.findViewById(R.id.tvFilename)).setText(transfer.filename != null ? transfer.filename : "Unknown");
        ((TextView) view.findViewById(R.id.tvOperation)).setText(transfer.transferType != null ? transfer.transferType : "Unknown");
        ((TextView) view.findViewById(R.id.tvStatus)).setText(transfer.status != null ? transfer.status : "Unknown");
        
        long total = transfer.totalBytes != null ? transfer.totalBytes : 0;
        long current = transfer.bytesTransferred != null ? transfer.bytesTransferred : 0;
        
        ((TextView) view.findViewById(R.id.tvProgress)).setText(current + " / " + total + " bytes");
        
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
    }
}
