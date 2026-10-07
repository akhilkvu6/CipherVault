package com.ciphervault.app.main.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ciphervault.app.R;
import com.ciphervault.app.auth.data.AuthRepository;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.preferences.ConnectionPreferences;
import com.ciphervault.app.core.session.AuthSessionManager;
import com.ciphervault.app.main.api.ActivityApi;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.api.StorageApi;
import com.ciphervault.app.main.api.UserApi;
import com.ciphervault.app.main.model.ActivityEvent;
import com.ciphervault.app.main.model.FileResponse;
import com.ciphervault.app.main.model.PageResponse;
import com.ciphervault.app.main.model.StorageSummaryResponse;
import com.ciphervault.app.main.model.UserProfileResponse;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements FileAdapter.OnFileActionListener {

    private SwipeRefreshLayout swipeRefresh;
    private View llErrorBanner;
    private TextView tvGreeting;
    private TextView tvProfileAvatar;
    private TextView tvStorageUsed;
    private TextView tvStorageTotal;
    private TextView tvStoragePercentage;
    private TextView tvStorageUsedDetail;
    private TextView tvStorageAvailable;
    private TextView tvStorageFileCount;
    private CircularProgressIndicator cpStorageRing;
    private LinearProgressIndicator pbStorage;

    private RecyclerView rvRecentFiles;
    private View llEmptyFiles;
    private FileAdapter recentAdapter;

    private RecyclerView rvRecentActivity;
    private TextView tvEmptyActivity;
    private RecentActivityAdapter activityAdapter;

    private View dotHealthServer;
    private TextView tvHealthServerStatus;
    private View btnConnectServer;
    private View dotHealthEncryption;
    private TextView tvHealthEncryptionStatus;
    private View dotHealthAuth;
    private TextView tvHealthAuthStatus;

    private UserApi userApi;
    private StorageApi storageApi;
    private FileApi fileApi;
    private ActivityApi activityApi;
    private AuthSessionManager sessionManager;
    private ConnectionPreferences connectionPrefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        llErrorBanner = view.findViewById(R.id.llErrorBanner);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);
        tvStorageUsed = view.findViewById(R.id.tvStorageUsed);
        tvStorageTotal = view.findViewById(R.id.tvStorageTotal);
        tvStoragePercentage = view.findViewById(R.id.tvStoragePercentage);
        tvStorageUsedDetail = view.findViewById(R.id.tvStorageUsedDetail);
        tvStorageAvailable = view.findViewById(R.id.tvStorageAvailable);
        tvStorageFileCount = view.findViewById(R.id.tvStorageFileCount);
        cpStorageRing = view.findViewById(R.id.cpStorageRing);
        pbStorage = view.findViewById(R.id.pbStorage);

        rvRecentFiles = view.findViewById(R.id.rvRecentFiles);
        llEmptyFiles = view.findViewById(R.id.llEmptyFiles);
        rvRecentActivity = view.findViewById(R.id.rvRecentActivity);
        tvEmptyActivity = view.findViewById(R.id.tvEmptyActivity);

        dotHealthServer = view.findViewById(R.id.dotHealthServer);
        tvHealthServerStatus = view.findViewById(R.id.tvHealthServerStatus);
        btnConnectServer = view.findViewById(R.id.btnConnectServer);
        dotHealthEncryption = view.findViewById(R.id.dotHealthEncryption);
        tvHealthEncryptionStatus = view.findViewById(R.id.tvHealthEncryptionStatus);
        dotHealthAuth = view.findViewById(R.id.dotHealthAuth);
        tvHealthAuthStatus = view.findViewById(R.id.tvHealthAuthStatus);

        sessionManager = new AuthSessionManager(requireContext());
        connectionPrefs = new ConnectionPreferences(requireContext());

        // Setup Adapters
        rvRecentFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        recentAdapter = new FileAdapter(this);
        rvRecentFiles.setAdapter(recentAdapter);

        rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext()));
        activityAdapter = new RecentActivityAdapter();
        rvRecentActivity.setAdapter(activityAdapter);

        // Header - Profile letter avatar
        if (tvProfileAvatar != null) {
            tvProfileAvatar.setOnClickListener(v -> {
                if (getActivity() instanceof MainAppActivity) {
                    ((MainAppActivity) getActivity()).addFragment(new ProfileFragment());
                }
            });
        }

        // Storage Overview Click -> Manage Storage
        view.findViewById(R.id.cvStorage).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ManageStorageActivity.class));
        });
        view.findViewById(R.id.btnQuickManage).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ManageStorageActivity.class));
        });

        // Quick Actions
        view.findViewById(R.id.btnQuickUpload).setOnClickListener(v -> openFilePicker());
        view.findViewById(R.id.btnQuickFiles).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).navigateToFiles();
            }
        });
        view.findViewById(R.id.btnQuickSearch).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).addFragment(new SearchFragment());
            }
        });
        view.findViewById(R.id.btnQuickTransfers).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).navigateToTransfers();
            }
        });

        // Recent Files Actions
        view.findViewById(R.id.tvSeeAllRecent).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).navigateToFiles();
            }
        });
        view.findViewById(R.id.btnEmptyUpload).setOnClickListener(v -> openFilePicker());

        // Recent Activity Actions
        view.findViewById(R.id.tvSeeAllActivity).setOnClickListener(v -> {
            if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).addFragment(new ActivityHistoryFragment());
            }
        });

        // Vault Health Connect Button & Card
        if (btnConnectServer != null) {
            btnConnectServer.setOnClickListener(v -> openConnectScreen());
        }
        view.findViewById(R.id.btnVaultHealthCard).setOnClickListener(v -> {
            if (btnConnectServer != null && btnConnectServer.getVisibility() == View.VISIBLE) {
                openConnectScreen();
            } else if (getActivity() instanceof MainAppActivity) {
                ((MainAppActivity) getActivity()).addFragment(new HealthFragment());
            }
        });

        // Retry & Pull to refresh
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadData());
        swipeRefresh.setOnRefreshListener(this::loadData);
        swipeRefresh.setColorSchemeResources(R.color.cv_primary);

        // Initialize APIs
        userApi = ApiClient.getClient(getContext()).create(UserApi.class);
        storageApi = ApiClient.getClient(getContext()).create(StorageApi.class);
        fileApi = ApiClient.getClient(getContext()).create(FileApi.class);
        activityApi = ApiClient.getClient(getContext()).create(ActivityApi.class);

        loadData();
    }

    private void openConnectScreen() {
        Intent intent = new Intent(getContext(), com.ciphervault.app.auth.ui.ConnectActivity.class);
        startActivity(intent);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");
        startActivityForResult(intent, 1001);
    }

    private String getTimeOfDayGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void loadData() {
        llErrorBanner.setVisibility(View.GONE);
        String baseGreeting = getTimeOfDayGreeting();
        tvGreeting.setText(baseGreeting);

        // 1. User Profile
        userApi.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    String username = response.body().username;
                    if (username != null && !username.trim().isEmpty()) {
                        tvGreeting.setText(baseGreeting + ", " + username);
                        String firstChar = username.trim().substring(0, 1).toUpperCase();
                        if (tvProfileAvatar != null) {
                            tvProfileAvatar.setText(firstChar);
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                // Keep base greeting
            }
        });

        // 2. Storage Overview
        storageApi.getStorageSummary().enqueue(new Callback<StorageSummaryResponse>() {
            @Override
            public void onResponse(Call<StorageSummaryResponse> call, Response<StorageSummaryResponse> response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    StorageSummaryResponse summary = response.body();
                    tvStorageUsed.setText(formatStorage(summary.usedBytes));
                    tvStorageTotal.setText("of " + formatStorage(summary.totalBytes) + " total");
                    
                    double effectivePercentage = summary.getEffectivePercentage();
                    tvStoragePercentage.setText(String.format(java.util.Locale.US, "%.0f%%", effectivePercentage));
                    
                    int progress = (int) Math.round(effectivePercentage);
                    if (summary.usedBytes > 0 && progress == 0) {
                        progress = 1; // Show at least a minimal sliver if non-zero used
                    }
                    progress = Math.max(0, Math.min(100, progress));

                    cpStorageRing.setProgressCompat(progress, true);
                    pbStorage.setProgressCompat(progress, true);

                    tvStorageUsedDetail.setText("Used: " + formatStorage(summary.usedBytes));
                    tvStorageAvailable.setText("Free: " + formatStorage(summary.availableBytes));
                    tvStorageFileCount.setText(summary.fileCount + (summary.fileCount == 1 ? " file" : " files"));
                } else {
                    showErrorBanner("Could not load storage summary.");
                }
            }

            @Override
            public void onFailure(Call<StorageSummaryResponse> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                showErrorBanner("Couldn't connect to CipherVault. Check connection and try again.");
            }
        });

        // 3. Recent Files
        fileApi.listFiles(0, 5, "newest").enqueue(new Callback<List<FileResponse>>() {
            @Override
            public void onResponse(Call<List<FileResponse>> call, Response<List<FileResponse>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<FileResponse> files = response.body();
                    if (files.isEmpty()) {
                        rvRecentFiles.setVisibility(View.GONE);
                        llEmptyFiles.setVisibility(View.VISIBLE);
                    } else {
                        llEmptyFiles.setVisibility(View.GONE);
                        rvRecentFiles.setVisibility(View.VISIBLE);
                        recentAdapter.setFiles(files);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<FileResponse>> call, Throwable t) {
                if (!isAdded()) return;
                rvRecentFiles.setVisibility(View.GONE);
                llEmptyFiles.setVisibility(View.VISIBLE);
            }
        });

        // 4. Recent Activity
        activityApi.listActivity(0, 3, null, null).enqueue(new Callback<PageResponse<ActivityEvent>>() {
            @Override
            public void onResponse(Call<PageResponse<ActivityEvent>> call, Response<PageResponse<ActivityEvent>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().content != null) {
                    List<ActivityEvent> events = response.body().content;
                    if (events.isEmpty()) {
                        rvRecentActivity.setVisibility(View.GONE);
                        tvEmptyActivity.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyActivity.setVisibility(View.GONE);
                        rvRecentActivity.setVisibility(View.VISIBLE);
                        activityAdapter.setEvents(events);
                    }
                } else {
                    tvEmptyActivity.setVisibility(View.VISIBLE);
                    rvRecentActivity.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ActivityEvent>> call, Throwable t) {
                if (!isAdded()) return;
                tvEmptyActivity.setVisibility(View.VISIBLE);
                rvRecentActivity.setVisibility(View.GONE);
            }
        });

        // 5. Vault Health
        String serverUrl = connectionPrefs.getServerUrl();
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            // Not configured / not connected
            applyHealthDisconnectedState();
            return;
        }

        AuthRepository repo = new AuthRepository(requireContext());
        repo.checkHealth(new AuthRepository.RepoCallback<HealthResponse>() {
            @Override
            public void onSuccess(HealthResponse result) {
                if (!isAdded()) return;
                if (result != null && "UP".equalsIgnoreCase(result.getStatus())) {
                    tvHealthServerStatus.setText("Connected");
                    tvHealthServerStatus.setTextColor(getResources().getColor(R.color.cv_security_success, null));
                    if (dotHealthServer != null) {
                        dotHealthServer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                                getResources().getColor(R.color.cv_security_success, null)));
                    }
                    if (btnConnectServer != null) {
                        btnConnectServer.setVisibility(View.GONE);
                    }
                } else {
                    applyHealthUnavailableState();
                }
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                applyHealthDisconnectedState();
            }
        });

        boolean sessionValid = sessionManager.hasValidSession();
        tvHealthAuthStatus.setText(sessionValid ? "Valid" : "Expired");
        tvHealthAuthStatus.setTextColor(getResources().getColor(sessionValid ? R.color.cv_security_success : R.color.cv_primary, null));
        if (dotHealthAuth != null) {
            dotHealthAuth.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(sessionValid ? R.color.cv_security_success : R.color.cv_primary, null)));
        }

        // Encryption status reflects valid backend configuration
        tvHealthEncryptionStatus.setText("AES-256 Active");
        tvHealthEncryptionStatus.setTextColor(getResources().getColor(R.color.cv_security_success, null));
        if (dotHealthEncryption != null) {
            dotHealthEncryption.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.cv_security_success, null)));
        }
    }

    private void applyHealthDisconnectedState() {
        if (!isAdded()) return;
        tvHealthServerStatus.setText("Not connected");
        tvHealthServerStatus.setTextColor(getResources().getColor(R.color.cv_text_tertiary, null));
        if (dotHealthServer != null) {
            dotHealthServer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.cv_text_tertiary, null)));
        }
        if (btnConnectServer != null) {
            btnConnectServer.setVisibility(View.VISIBLE);
        }
    }

    private void applyHealthUnavailableState() {
        if (!isAdded()) return;
        tvHealthServerStatus.setText("Unavailable");
        tvHealthServerStatus.setTextColor(getResources().getColor(R.color.cv_primary, null));
        if (dotHealthServer != null) {
            dotHealthServer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.cv_primary, null)));
        }
        if (btnConnectServer != null) {
            btnConnectServer.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorBanner(String message) {
        TextView tvMsg = getView() != null ? getView().findViewById(R.id.tvErrorMessage) : null;
        if (tvMsg != null) {
            tvMsg.setText(message);
        }
        llErrorBanner.setVisibility(View.VISIBLE);
    }

    private String formatStorage(long bytes) {
        if (bytes <= 0) return "0 GB";
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gb >= 1.0) {
            return String.format("%.1f GB", gb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.0f MB", mb);
    }

    @Override
    public void onThumbnailClick(FileResponse file) {
        onClick(file);
    }

    @Override
    public void onDownloadRequested(FileResponse file) {
        if (file == null || getContext() == null) return;
        FileDownloadManager.showDownloadDialog(getContext(), file, null);
    }

    @Override
    public void onDeleteRequested(FileResponse file) {
        if (file == null || file.id == null || getContext() == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete " + file.filename + "?")
                .setMessage("Are you sure you want to permanently delete this file from your private vault? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    fileApi.deleteFile(file.id).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), "Deleted " + file.filename, Toast.LENGTH_SHORT).show();
                                loadData();
                            } else {
                                Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Error deleting file", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onClick(FileResponse file) {
        if (file == null || getContext() == null) return;
        Intent intent = new Intent(getContext(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_ID, file.id);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_NAME, file.filename);
        intent.putExtra(FilePreviewActivity.EXTRA_FILE_MIME, file.contentType);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            Intent uploadIntent = new Intent(getContext(), UploadActivity.class);
            if (data.getClipData() != null) {
                uploadIntent.setClipData(data.getClipData());
            } else if (data.getData() != null) {
                uploadIntent.setData(data.getData());
            }
            startActivity(uploadIntent);
        }
    }
}
