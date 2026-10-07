package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LargeFilesFragment extends BottomSheetDialogFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_large_files, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());
        TextView tvState = view.findViewById(R.id.tvState);
        
        StorageApi api = ApiClient.getClient(getContext()).create(StorageApi.class);
        api.getLargeFiles().enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) { if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().isEmpty()) {
                        tvState.setText("No large files found");
                    } else {
                        tvState.setText(response.body().size() + " large files found.");
                    }
                }
            }
            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) { if (!isAdded()) return;
                tvState.setText("Error loading large files");
            }
        });
    }
}

