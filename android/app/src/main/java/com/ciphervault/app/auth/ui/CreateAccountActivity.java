package com.ciphervault.app.auth.ui;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CreateAccountActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    
    private TextInputLayout tilUsername;
    private TextInputEditText etUsername;
    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private TextInputLayout tilPassword;
    private TextInputEditText etPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private CircularProgressIndicator pbRegister;
    private TextView tvErrorBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.createAccountRoot), (v, insets) -> {
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
        tilUsername = findViewById(R.id.tilUsername);
        etUsername = findViewById(R.id.etUsername);
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etPassword = findViewById(R.id.etPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        pbRegister = findViewById(R.id.pbRegister);
        tvErrorBanner = findViewById(R.id.tvErrorBanner);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            tilUsername.setError(null);
            tilEmail.setError(null);
            tilPassword.setError(null);
            tilConfirmPassword.setError(null);
            tvErrorBanner.setVisibility(View.GONE);

            String username = String.valueOf(etUsername.getText()).trim();
            String email = String.valueOf(etEmail.getText()).trim();
            String password = String.valueOf(etPassword.getText());
            String confirmPassword = String.valueOf(etConfirmPassword.getText());

            boolean valid = true;
            if (username.isEmpty()) {
                tilUsername.setError("Username is required");
                valid = false;
            }
            if (email.isEmpty()) {
                tilEmail.setError("Email is required");
                valid = false;
            }
            if (password.isEmpty()) {
                tilPassword.setError("Password is required");
                valid = false;
            } else if (password.length() < 6) {
                tilPassword.setError("Password must be at least 6 characters");
                valid = false;
            } else if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Passwords do not match");
                valid = false;
            }

            if (valid) {
                viewModel.register(username, email, password);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getRegisterState().observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    setLoadingState(true);
                    break;
                case SUCCESS:
                    setLoadingState(false);
                    Toast.makeText(this, "Account created successfully. Please sign in.", Toast.LENGTH_LONG).show();
                    finish(); // Registration does not return a token. Go back to SignIn.
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
        btnRegister.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        etConfirmPassword.setEnabled(!isLoading);
        pbRegister.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        if (isLoading) {
            btnRegister.setText("Processing...");
        } else {
            btnRegister.setText("Create Account");
        }
    }
}
