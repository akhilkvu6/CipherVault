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
import com.ciphervault.app.core.session.SecureTokenStorage;

public class SecurityStatusFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_security_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        TextView tvUserSession = view.findViewById(R.id.tvUserSession);
        TextView tvJwtStatus = view.findViewById(R.id.tvJwtStatus);

        SecureTokenStorage tokenStorage = new SecureTokenStorage(requireContext());
        String username = tokenStorage.getUsername();
        String token = tokenStorage.getToken();

        if (username != null && !username.isEmpty()) {
            tvUserSession.setText("Active vault for: " + username);
        } else {
            tvUserSession.setText("Authenticated session active");
        }

        if (token != null && !token.isEmpty()) {
            tvJwtStatus.setText("ACTIVE");
        } else {
            tvJwtStatus.setText("INACTIVE");
        }
    }
}
