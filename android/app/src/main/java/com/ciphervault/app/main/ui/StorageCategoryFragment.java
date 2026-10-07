package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ciphervault.app.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class StorageCategoryFragment extends BottomSheetDialogFragment {
    private String category;
    
    public static StorageCategoryFragment newInstance(String category) {
        StorageCategoryFragment fragment = new StorageCategoryFragment();
        fragment.category = category;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_storage_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (category == null) category = "Unknown";
        ((TextView) view.findViewById(R.id.tvCategoryName)).setText(category);
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
    }
}

