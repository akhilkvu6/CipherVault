package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.TransferApi;
import com.ciphervault.app.main.model.PageResponse;
import com.ciphervault.app.main.model.Transfer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransfersFragment extends Fragment implements TransferAdapter.OnTransferActionListener {
    private TransferAdapter adapter;
    private TransferApi api;
    private ProgressBar pbTransfers;
    private View viewTransfersEmpty;
    private TextView tvTransfersEmpty;
    private RecyclerView rvTransfers;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transfers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvTransfers = view.findViewById(R.id.rvTransfers);
        pbTransfers = view.findViewById(R.id.pbTransfers);
        viewTransfersEmpty = view.findViewById(R.id.viewTransfersEmpty);
        tvTransfersEmpty = view.findViewById(R.id.tvTransfersEmpty);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        rvTransfers.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransferAdapter(this);
        rvTransfers.setAdapter(adapter);

        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.cv_primary);
            swipeRefresh.setOnRefreshListener(this::loadTransfers);
        }

        api = ApiClient.getClient(getContext()).create(TransferApi.class);
        loadTransfers();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransfers();
    }

    private void loadTransfers() {
        if (swipeRefresh == null || !swipeRefresh.isRefreshing()) {
            pbTransfers.setVisibility(View.VISIBLE);
        }
        viewTransfersEmpty.setVisibility(View.GONE);

        api.listTransfers(0, 50).enqueue(new Callback<PageResponse<Transfer>>() {
            @Override
            public void onResponse(Call<PageResponse<Transfer>> call, Response<PageResponse<Transfer>> response) { 
                if (!isAdded()) return;
                pbTransfers.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    adapter.setTransfers(response.body().content);
                    if (response.body().content == null || response.body().content.isEmpty()) {
                        if (tvTransfersEmpty != null) tvTransfersEmpty.setText("No transfers found");
                        viewTransfersEmpty.setVisibility(View.VISIBLE);
                        rvTransfers.setVisibility(View.GONE);
                    } else {
                        rvTransfers.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvTransfersEmpty != null) tvTransfersEmpty.setText("Failed to load transfers");
                    viewTransfersEmpty.setVisibility(View.VISIBLE);
                    rvTransfers.setVisibility(View.GONE);
                }
            }
            @Override
            public void onFailure(Call<PageResponse<Transfer>> call, Throwable t) { 
                if (!isAdded()) return;
                pbTransfers.setVisibility(View.GONE);
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (tvTransfersEmpty != null) tvTransfersEmpty.setText("Network error: " + t.getMessage());
                viewTransfersEmpty.setVisibility(View.VISIBLE);
                rvTransfers.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onCancel(Transfer transfer) {
        api.cancelTransfer(transfer.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { 
                if (!isAdded()) return;
                loadTransfers();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { 
                if (!isAdded()) return;
            }
        });
    }
    
    @Override
    public void onClick(Transfer transfer) {
        TransferDetailsBottomSheet sheet = TransferDetailsBottomSheet.newInstance(transfer);
        sheet.show(getChildFragmentManager(), "transfer_details");
    }
}
