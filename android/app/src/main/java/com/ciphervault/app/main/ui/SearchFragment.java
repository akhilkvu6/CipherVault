package com.ciphervault.app.main.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private EditText etSearchInput;
    private RecyclerView rvSearchResults;
    private ProgressBar pbSearch;
    private TextView tvSearchEmpty;
    private FileAdapter adapter;
    private FileApi fileApi;
    private String lastQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        etSearchInput = view.findViewById(R.id.etSearchInput);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        pbSearch = view.findViewById(R.id.pbSearch);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);

        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FileAdapter(this);
        rvSearchResults.setAdapter(adapter);

        fileApi = ApiClient.getClient(getContext()).create(FileApi.class);

        etSearchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = v.getText().toString().trim();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        etSearchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etSearchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void performSearch(String query) {
        lastQuery = query;
        pbSearch.setVisibility(View.VISIBLE);
        tvSearchEmpty.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.GONE);

        fileApi.searchFiles(query).enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) {
                if (!isAdded()) return;
                pbSearch.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<FileResponse> results = response.body();
                    adapter.setFiles(results);
                    if (results.isEmpty()) {
                        tvSearchEmpty.setText("No files match \"" + query + "\"");
                        tvSearchEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvSearchResults.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvSearchEmpty.setText("Search failed: " + response.code());
                    tvSearchEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) {
                if (!isAdded()) return;
                pbSearch.setVisibility(View.GONE);
                tvSearchEmpty.setText("Network error: " + t.getMessage());
                tvSearchEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onThumbnailClick(FileResponse file) {
        onClick(file);
    }

    @Override
    public void onClick(FileResponse file) {
        if (file == null || file.id == null || getContext() == null) return;
        Intent intent = new Intent(getContext(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override
    public void onDownload(FileResponse file) {
        if (file == null || getContext() == null) return;
        FileDownloadManager.showDownloadDialog(getContext(), file, null);
    }

    @Override
    public void onDelete(FileResponse file) {
        if (file == null || file.id == null || getContext() == null) return;
        fileApi.deleteFile(file.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Deleted: " + file.filename, Toast.LENGTH_SHORT).show();
                    if (!lastQuery.isEmpty()) {
                        performSearch(lastQuery);
                    }
                } else {
                    Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
