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
import com.ciphervault.app.main.api.ActivityApi;
import com.ciphervault.app.main.model.ActivityEvent;
import com.ciphervault.app.main.model.PageResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityHistoryFragment extends Fragment {

    private RecyclerView rvActivity;
    private RecentActivityAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar pbLoading;
    private View viewEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    private MaterialButton chipAll;
    private MaterialButton chipUploads;
    private MaterialButton chipDownloads;
    private MaterialButton chipDeletes;
    private MaterialButton chipDuplicates;
    private MaterialButton chipFailures;

    private List<ActivityEvent> allEvents = new ArrayList<>();
    private String selectedFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
        view.findViewById(R.id.btnRefresh).setOnClickListener(v -> loadActivity());

        rvActivity = view.findViewById(R.id.rvActivity);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        pbLoading = view.findViewById(R.id.pbLoading);
        viewEmpty = view.findViewById(R.id.viewEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);

        chipAll = view.findViewById(R.id.chipAll);
        chipUploads = view.findViewById(R.id.chipUploads);
        chipDownloads = view.findViewById(R.id.chipDownloads);
        chipDeletes = view.findViewById(R.id.chipDeletes);
        chipDuplicates = view.findViewById(R.id.chipDuplicates);
        chipFailures = view.findViewById(R.id.chipFailures);

        rvActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecentActivityAdapter();
        rvActivity.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.cv_primary);
        swipeRefresh.setOnRefreshListener(this::loadActivity);

        setupFilterChips();
        loadActivity();
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> setFilter("ALL"));
        chipUploads.setOnClickListener(v -> setFilter("UPLOAD"));
        chipDownloads.setOnClickListener(v -> setFilter("DOWNLOAD"));
        chipDeletes.setOnClickListener(v -> setFilter("DELETE"));
        chipDuplicates.setOnClickListener(v -> setFilter("DUPLICATE"));
        chipFailures.setOnClickListener(v -> setFilter("FAILURE"));
        updateChipStyles();
    }

    private void setFilter(String filter) {
        this.selectedFilter = filter;
        updateChipStyles();
        applyFilter();
    }

    private void updateChipStyles() {
        setChipActive(chipAll, "ALL".equals(selectedFilter));
        setChipActive(chipUploads, "UPLOAD".equals(selectedFilter));
        setChipActive(chipDownloads, "DOWNLOAD".equals(selectedFilter));
        setChipActive(chipDeletes, "DELETE".equals(selectedFilter));
        setChipActive(chipDuplicates, "DUPLICATE".equals(selectedFilter));
        setChipActive(chipFailures, "FAILURE".equals(selectedFilter));
    }

    private void setChipActive(MaterialButton chip, boolean active) {
        if (chip == null || getContext() == null) return;
        if (active) {
            chip.setBackgroundColor(requireContext().getColor(R.color.cv_primary));
            chip.setTextColor(requireContext().getColor(R.color.cv_on_primary));
            chip.setStrokeWidth(0);
        } else {
            chip.setBackgroundColor(requireContext().getColor(android.R.color.transparent));
            chip.setTextColor(requireContext().getColor(R.color.cv_text_secondary));
            chip.setStrokeColorResource(R.color.cv_border);
            chip.setStrokeWidth(1);
        }
    }

    private void loadActivity() {
        if (!swipeRefresh.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }
        viewEmpty.setVisibility(View.GONE);

        ActivityApi api = ApiClient.getClient(getContext()).create(ActivityApi.class);
        api.listActivity(0, 100, null, null).enqueue(new Callback<PageResponse<ActivityEvent>>() {
            @Override
            public void onResponse(Call<PageResponse<ActivityEvent>> call, Response<PageResponse<ActivityEvent>> response) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().content != null) {
                    allEvents = response.body().content;
                    applyFilter();
                } else {
                    viewEmpty.setVisibility(View.VISIBLE);
                    tvEmptyTitle.setText("Unable to load activity");
                    tvEmptySubtitle.setText("Server returned error code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ActivityEvent>> call, Throwable t) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                viewEmpty.setVisibility(View.VISIBLE);
                tvEmptyTitle.setText("Connection failed");
                tvEmptySubtitle.setText(t.getMessage() != null ? t.getMessage() : "Could not reach CipherVault server.");
            }
        });
    }

    private void applyFilter() {
        List<ActivityEvent> filtered = new ArrayList<>();
        for (ActivityEvent e : allEvents) {
            if ("ALL".equals(selectedFilter)) {
                filtered.add(e);
            } else if ("UPLOAD".equals(selectedFilter)) {
                if (e.eventType != null && e.eventType.toUpperCase(Locale.ROOT).contains("UPLOAD")) filtered.add(e);
            } else if ("DOWNLOAD".equals(selectedFilter)) {
                if (e.eventType != null && e.eventType.toUpperCase(Locale.ROOT).contains("DOWNLOAD")) filtered.add(e);
            } else if ("DELETE".equals(selectedFilter)) {
                if (e.eventType != null && e.eventType.toUpperCase(Locale.ROOT).contains("DELETE")) filtered.add(e);
            } else if ("DUPLICATE".equals(selectedFilter)) {
                if (e.eventType != null && e.eventType.toUpperCase(Locale.ROOT).contains("DUPLICATE")) filtered.add(e);
            } else if ("FAILURE".equals(selectedFilter)) {
                if ((e.eventType != null && e.eventType.toUpperCase(Locale.ROOT).contains("FAIL"))
                        || (e.status != null && e.status.toUpperCase(Locale.ROOT).contains("FAIL"))) {
                    filtered.add(e);
                }
            }
        }

        adapter.setEvents(filtered);
        if (filtered.isEmpty()) {
            viewEmpty.setVisibility(View.VISIBLE);
            tvEmptyTitle.setText("No " + selectedFilter.toLowerCase() + " events");
            tvEmptySubtitle.setText("No activity found for the selected filter.");
            rvActivity.setVisibility(View.GONE);
        } else {
            viewEmpty.setVisibility(View.GONE);
            rvActivity.setVisibility(View.VISIBLE);
        }
    }
}
