package com.ciphervault.app.main.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ciphervault.app.R;
import com.ciphervault.app.auth.ui.ConnectActivity;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.UserApi;
import com.ciphervault.app.main.model.UserProfileResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView tvSettingsUsername = view.findViewById(R.id.tvSettingsUsername);
        TextView tvSettingsEmail = view.findViewById(R.id.tvSettingsEmail);

        view.findViewById(R.id.cvProfile).setOnClickListener(v -> {
            ((MainAppActivity) requireActivity()).addFragment(new ProfileFragment());
        });

        view.findViewById(R.id.btnAppearance).setOnClickListener(v -> {
            new AppearanceBottomSheet().show(getChildFragmentManager(), "appearance");
        });

        View btnManageStorage = view.findViewById(R.id.btnManageStorage);
        if (btnManageStorage != null) {
            btnManageStorage.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), ManageStorageActivity.class));
            });
        }

        View btnAnalytics = view.findViewById(R.id.btnAnalytics);
        if (btnAnalytics != null) {
            btnAnalytics.setOnClickListener(v -> {
                ((MainAppActivity) requireActivity()).addFragment(new AnalyticsFragment());
            });
        }

        view.findViewById(R.id.btnConnectionCenter).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ConnectActivity.class));
        });

        view.findViewById(R.id.btnSecurityStatus).setOnClickListener(v -> {
            ((MainAppActivity) requireActivity()).addFragment(new SecurityStatusFragment());
        });

        view.findViewById(R.id.btnHealth).setOnClickListener(v -> {
            ((MainAppActivity) requireActivity()).addFragment(new HealthFragment());
        });

        view.findViewById(R.id.btnAccountManagement).setOnClickListener(v -> {
            ((MainAppActivity) requireActivity()).addFragment(new AccountManagementFragment());
        });

        view.findViewById(R.id.btnActivityHistory).setOnClickListener(v -> {
            ((MainAppActivity) requireActivity()).addFragment(new ActivityHistoryFragment());
        });

        UserApi api = ApiClient.getClient(getContext()).create(UserApi.class);
        api.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvSettingsUsername.setText(response.body().username);
                    tvSettingsEmail.setText(response.body().email);
                }
            }
            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                tvSettingsUsername.setText("Error loading profile");
            }
        });
    }
}
