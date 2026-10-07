package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ciphervault.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;

public class BottomSheetFilters extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterApply(String sort, String timeFilter, String typeFilter, String encryptionFilter);
    }

    private FilterListener listener;
    private String initialSort = "createdAtDesc";
    private String initialTime = "";
    private String initialType = "";
    private String initialEnc = "";

    public void setListener(FilterListener listener) {
        this.listener = listener;
    }

    public void setInitialValues(String sort, String time, String type, String enc) {
        this.initialSort = sort != null ? sort : "createdAtDesc";
        this.initialTime = time != null ? time : "";
        this.initialType = type != null ? type : "";
        this.initialEnc = enc != null ? enc : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ChipGroup cgSort = view.findViewById(R.id.cgSort);
        ChipGroup cgTime = view.findViewById(R.id.cgTime);
        ChipGroup cgType = view.findViewById(R.id.cgType);
        ChipGroup cgEnc = view.findViewById(R.id.cgEncryption);

        // Apply initial selections
        if ("createdAtAsc".equals(initialSort)) cgSort.check(R.id.chipSortOldest);
        else if ("largest".equals(initialSort)) cgSort.check(R.id.chipSortLargest);
        else if ("smallest".equals(initialSort)) cgSort.check(R.id.chipSortSmallest);
        else if ("nameAsc".equals(initialSort)) cgSort.check(R.id.chipSortNameAsc);
        else if ("nameDesc".equals(initialSort)) cgSort.check(R.id.chipSortNameDesc);
        else cgSort.check(R.id.chipSortNewest);

        if ("yesterday".equals(initialTime)) cgTime.check(R.id.chipTimeYesterday);
        else if ("7days".equals(initialTime)) cgTime.check(R.id.chipTime7Days);
        else if ("30days".equals(initialTime)) cgTime.check(R.id.chipTime30Days);
        else cgTime.check(R.id.chipTimeAny);

        if ("image".equals(initialType)) cgType.check(R.id.chipTypeImage);
        else if ("video".equals(initialType)) cgType.check(R.id.chipTypeVideo);
        else if ("document".equals(initialType)) cgType.check(R.id.chipTypeDoc);
        else if ("audio".equals(initialType)) cgType.check(R.id.chipTypeAudio);
        else cgType.check(R.id.chipTypeAll);

        if ("encrypted".equals(initialEnc)) cgEnc.check(R.id.chipEncYes);
        else if ("unencrypted".equals(initialEnc)) cgEnc.check(R.id.chipEncNo);
        else cgEnc.check(R.id.chipEncAll);

        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            cgSort.check(R.id.chipSortNewest);
            cgTime.check(R.id.chipTimeAny);
            cgType.check(R.id.chipTypeAll);
            cgEnc.check(R.id.chipEncAll);
        });

        view.findViewById(R.id.btnApply).setOnClickListener(v -> {
            if (listener != null) {
                String sort = "createdAtDesc";
                int sortId = cgSort.getCheckedChipId();
                if (sortId == R.id.chipSortOldest) sort = "createdAtAsc";
                else if (sortId == R.id.chipSortLargest) sort = "largest";
                else if (sortId == R.id.chipSortSmallest) sort = "smallest";
                else if (sortId == R.id.chipSortNameAsc) sort = "nameAsc";
                else if (sortId == R.id.chipSortNameDesc) sort = "nameDesc";

                String time = "";
                int timeId = cgTime.getCheckedChipId();
                if (timeId == R.id.chipTimeYesterday) time = "yesterday";
                else if (timeId == R.id.chipTime7Days) time = "7days";
                else if (timeId == R.id.chipTime30Days) time = "30days";

                String type = "";
                int typeId = cgType.getCheckedChipId();
                if (typeId == R.id.chipTypeImage) type = "image";
                else if (typeId == R.id.chipTypeVideo) type = "video";
                else if (typeId == R.id.chipTypeDoc) type = "document";
                else if (typeId == R.id.chipTypeAudio) type = "audio";

                String enc = "";
                int encId = cgEnc.getCheckedChipId();
                if (encId == R.id.chipEncYes) enc = "encrypted";
                else if (encId == R.id.chipEncNo) enc = "unencrypted";

                listener.onFilterApply(sort, time, type, enc);
            }
            dismiss();
        });
    }
}
