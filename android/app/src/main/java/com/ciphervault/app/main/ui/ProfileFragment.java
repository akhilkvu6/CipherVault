package com.ciphervault.app.main.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.UserApi;
import com.ciphervault.app.main.model.UserProfileResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        TextView tvUsername = view.findViewById(R.id.tvUsername);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        TextView tvStorage = view.findViewById(R.id.tvStorage);
        TextView tvFiles = view.findViewById(R.id.tvFiles);

        UserApi api = ApiClient.getClient(getContext()).create(UserApi.class);
        api.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvUsername.setText(response.body().username != null ? response.body().username : "User");
                    tvEmail.setText(response.body().email != null ? response.body().email : "");
                    long used = response.body().usedStorage != null ? response.body().usedStorage : 0L;
                    long count = response.body().fileCount != null ? response.body().fileCount : 0L;
                    double usedMb = used / (1024.0 * 1024.0);
                    tvStorage.setText(String.format(java.util.Locale.US, "%.1f MB used", usedMb));
                    tvFiles.setText(count + " files");
                }
            }
            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                tvUsername.setText("Error");
            }
        });
    }
}
