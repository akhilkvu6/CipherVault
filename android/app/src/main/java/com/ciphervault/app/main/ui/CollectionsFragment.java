package com.ciphervault.app.main.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileResponse;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CollectionsFragment extends Fragment implements FileAdapter.OnFileActionListener {
    private FileAdapter adapter;
    private FileApi api;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_collections, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        RecyclerView rvCollections = view.findViewById(R.id.rvCollections);
        rvCollections.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        adapter = new FileAdapter(this);
        rvCollections.setAdapter(adapter);

        api = ApiClient.getClient(getContext()).create(FileApi.class);
        loadMedia();
    }

    private void loadMedia() {
        api.listFiles(0, 100, "newest").enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) { if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<FileResponse> media = new ArrayList<>();
                    for (FileResponse f : response.body()) {
                        if (f.contentType != null && (f.contentType.startsWith("image") || f.contentType.startsWith("video"))) {
                            media.add(f);
                        }
                    }
                    adapter.setFiles(media);
                }
            }
            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) { if (!isAdded()) return;}
        });
    }

    @Override
    public void onClick(FileResponse file) {
        Intent intent = new Intent(getContext(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override public void onDownload(FileResponse file) {}
    @Override public void onDelete(FileResponse file) {}
    @Override public void onSelectionChanged(int count) {}
}

