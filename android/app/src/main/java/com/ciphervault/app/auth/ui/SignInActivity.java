package com.ciphervault.app.auth.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ciphervault.app.R;
import com.ciphervault.app.core.preferences.ConnectionPreferences;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignInActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private MaterialButton btnSignIn;
    private CircularProgressIndicator pbSignIn;
    private TextView tvErrorBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signInRoot), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        initViews();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        pbSignIn = findViewById(R.id.pbSignIn);
        tvErrorBanner = findViewById(R.id.tvErrorBanner);
    }

    private void setupListeners() {
        findViewById(R.id.btnConfig).setOnClickListener(v -> {
            startActivity(new Intent(this, ConnectActivity.class));
        });

        findViewById(R.id.btnCreateAccount).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateAccountActivity.class));
        });

        btnSignIn.setOnClickListener(v -> {
            tilEmail.setError(null);
            tilPassword.setError(null);
            tvErrorBanner.setVisibility(View.GONE);

            String email = String.valueOf(etEmail.getText()).trim();
            String password = String.valueOf(etPassword.getText());

            boolean valid = true;
            if (email.isEmpty()) {
                tilEmail.setError("Email is required");
                valid = false;
            }
            if (password.isEmpty()) {
                tilPassword.setError("Password is required");
                valid = false;
            }

            if (valid) {
                viewModel.login(email, password);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getLoginState().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    Toast.makeText(this, "Welcome " + resource.data.getUsername() + "!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignInActivity.this, com.ciphervault.app.main.ui.MainAppActivity.class);
                    startActivity(intent);
                    finishAffinity();
                    break;
                case ERROR:
                    setLoadingState(false);
                    tvErrorBanner.setText(resource.message);
                    tvErrorBanner.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        btnSignIn.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        pbSignIn.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        if (isLoading) {
            btnSignIn.setText("Processing...");
        } else {
            btnSignIn.setText("Sign In");
        }
    }
}
