package com.ciphervault.app.auth;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.ciphervault.app.R;
import com.ciphervault.app.auth.repository.AuthRepository;
import com.ciphervault.app.auth.ui.AuthViewModel;
import com.ciphervault.app.auth.validation.PasswordRequirements;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.core.network.ConnectionStatus;
import com.ciphervault.app.core.network.ServerConnectionPreferences;
import com.ciphervault.app.core.session.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Unified Authentication Screen (Sections 15, 16, 17, 18, 19, 21, 26, 27).
 * Supports Login and Create Account modes, live password checklist, connection status modal,
 * and seamless navigation to the authenticated root upon successful login.
 */
public class AuthFragment extends Fragment {

    private AuthViewModel viewModel;

    private View cardConnectionStatus;
    private View viewStatusIndicator;
    private TextView tvConnectionStatus;
    private ImageView btnConfigureServer;
    private TextView tvAuthTitle;
    private TextView tvAuthSubtitle;
    private TextView tvErrorBanner;
    private TextView tvLockoutBanner;

    private TextInputLayout layoutName;
    private TextInputEditText inputName;
    private TextInputLayout layoutUsername;
    private TextInputEditText inputUsername;
    private TextInputLayout layoutEmail;
    private TextInputEditText inputEmail;
    private TextInputLayout layoutPassword;
    private TextInputEditText inputPassword;
    private TextInputLayout layoutConfirmPassword;
    private TextInputEditText inputConfirmPassword;

    private View layoutPasswordChecklist;
    private ImageView ivReqLength;
    private TextView tvReqLength;
    private ImageView ivReqUppercase;
    private TextView tvReqUppercase;
    private ImageView ivReqLowercase;
    private TextView tvReqLowercase;
    private ImageView ivReqNumber;
    private TextView tvReqNumber;
    private ImageView ivReqSpecial;
    private TextView tvReqSpecial;

    private MaterialButton btnSubmit;
    private ProgressBar progressAuth;
    private MaterialButton btnToggleMode;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(requireContext().getApplicationContext());
        ServerConnectionPreferences serverPrefs = new ServerConnectionPreferences(requireContext().getApplicationContext());
        ApiClient apiClient = new ApiClient(serverPrefs, sessionManager);
        AuthRepository authRepository = new AuthRepository(apiClient, sessionManager);

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AuthViewModel(authRepository, serverPrefs);
            }
        };

        viewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_auth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupListeners();
        observeViewModel();
    }

    private void bindViews(View view) {
        cardConnectionStatus = view.findViewById(R.id.card_connection_status);
        viewStatusIndicator = view.findViewById(R.id.view_status_indicator);
        tvConnectionStatus = view.findViewById(R.id.tv_connection_status);
        btnConfigureServer = view.findViewById(R.id.btn_configure_server);
        tvAuthTitle = view.findViewById(R.id.tv_auth_title);
        tvAuthSubtitle = view.findViewById(R.id.tv_auth_subtitle);
        tvErrorBanner = view.findViewById(R.id.tv_error_banner);
        tvLockoutBanner = view.findViewById(R.id.tv_lockout_banner);

        layoutName = view.findViewById(R.id.layout_name);
        inputName = view.findViewById(R.id.input_name);
        layoutUsername = view.findViewById(R.id.layout_username);
        inputUsername = view.findViewById(R.id.input_username);
        layoutEmail = view.findViewById(R.id.layout_email);
        inputEmail = view.findViewById(R.id.input_email);
        layoutPassword = view.findViewById(R.id.layout_password);
        inputPassword = view.findViewById(R.id.input_password);
        layoutConfirmPassword = view.findViewById(R.id.layout_confirm_password);
        inputConfirmPassword = view.findViewById(R.id.input_confirm_password);

        layoutPasswordChecklist = view.findViewById(R.id.layout_password_checklist);
        ivReqLength = view.findViewById(R.id.iv_req_length);
        tvReqLength = view.findViewById(R.id.tv_req_length);
        ivReqUppercase = view.findViewById(R.id.iv_req_uppercase);
        tvReqUppercase = view.findViewById(R.id.tv_req_uppercase);
        ivReqLowercase = view.findViewById(R.id.iv_req_lowercase);
        tvReqLowercase = view.findViewById(R.id.tv_req_lowercase);
        ivReqNumber = view.findViewById(R.id.iv_req_number);
        tvReqNumber = view.findViewById(R.id.tv_req_number);
        ivReqSpecial = view.findViewById(R.id.iv_req_special);
        tvReqSpecial = view.findViewById(R.id.tv_req_special);

        btnSubmit = view.findViewById(R.id.btn_submit);
        progressAuth = view.findViewById(R.id.progress_auth);
        btnToggleMode = view.findViewById(R.id.btn_toggle_mode);
    }

    private void setupListeners() {
        // Mode toggle
        btnToggleMode.setOnClickListener(v -> {
            AuthViewModel.Mode current = viewModel.getMode().getValue();
            if (current == AuthViewModel.Mode.LOGIN) {
                viewModel.setMode(AuthViewModel.Mode.CREATE_ACCOUNT);
            } else {
                viewModel.setMode(AuthViewModel.Mode.LOGIN);
            }
        });

        // Live password text watcher for requirements checklist
        inputPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onPasswordChanged(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Submit action
        btnSubmit.setOnClickListener(v -> {
            String email = inputEmail.getText() != null ? inputEmail.getText().toString() : "";
            String password = inputPassword.getText() != null ? inputPassword.getText().toString() : "";

            if (viewModel.getMode().getValue() == AuthViewModel.Mode.LOGIN) {
                viewModel.login(email, password);
            } else {
                String name = inputName.getText() != null ? inputName.getText().toString() : "";
                String username = inputUsername.getText() != null ? inputUsername.getText().toString() : "";
                String confirmPassword = inputConfirmPassword.getText() != null ? inputConfirmPassword.getText().toString() : "";
                viewModel.register(name, username, email, password, confirmPassword);
            }
        });

        // Server connection configuration dialog
        btnConfigureServer.setOnClickListener(v -> showConnectionDialog());
        if (cardConnectionStatus != null) {
            cardConnectionStatus.setOnClickListener(v -> showConnectionDialog());
        }
    }

    private void observeViewModel() {
        viewModel.getMode().observe(getViewLifecycleOwner(), mode -> {
            boolean isCreate = mode == AuthViewModel.Mode.CREATE_ACCOUNT;

            tvAuthTitle.setText(isCreate ? "Create Account" : "CipherVault");
            tvAuthSubtitle.setText(isCreate ? "Establish your local encryption credentials" : "Sign in to access your secure vault");

            layoutName.setVisibility(isCreate ? View.VISIBLE : View.GONE);
            layoutUsername.setVisibility(isCreate ? View.VISIBLE : View.GONE);
            layoutConfirmPassword.setVisibility(isCreate ? View.VISIBLE : View.GONE);
            layoutPasswordChecklist.setVisibility(isCreate ? View.VISIBLE : View.GONE);

            btnSubmit.setText(isCreate ? "Create Account" : "Sign In");
            btnToggleMode.setText(isCreate ? "Already have an account? Sign in" : "Don't have an account? Create one");
        });

        viewModel.getPasswordRequirements().observe(getViewLifecycleOwner(), reqs -> {
            updateRequirementRow(ivReqLength, tvReqLength, reqs.hasMinLength());
            updateRequirementRow(ivReqUppercase, tvReqUppercase, reqs.hasUppercase());
            updateRequirementRow(ivReqLowercase, tvReqLowercase, reqs.hasLowercase());
            updateRequirementRow(ivReqNumber, tvReqNumber, reqs.hasNumber());
            updateRequirementRow(ivReqSpecial, tvReqSpecial, reqs.hasSpecialChar());
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progressAuth.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnSubmit.setEnabled(!loading && (viewModel.getLockoutRemainingSeconds().getValue() == null || viewModel.getLockoutRemainingSeconds().getValue() <= 0));
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), err -> {
            if (err != null && !err.trim().isEmpty()) {
                tvErrorBanner.setText(err);
                tvErrorBanner.setVisibility(View.VISIBLE);
            } else {
                tvErrorBanner.setVisibility(View.GONE);
            }
        });

        viewModel.getLockoutRemainingSeconds().observe(getViewLifecycleOwner(), seconds -> {
            if (seconds != null && seconds > 0) {
                tvLockoutBanner.setText("Too many failed attempts. Locked for " + seconds + " seconds.");
                tvLockoutBanner.setVisibility(View.VISIBLE);
                btnSubmit.setEnabled(false);
            } else {
                tvLockoutBanner.setVisibility(View.GONE);
                btnSubmit.setEnabled(!Boolean.TRUE.equals(viewModel.getIsLoading().getValue()));
            }
        });

        viewModel.getConnectionStatus().observe(getViewLifecycleOwner(), status -> {
            int color;
            String label;
            switch (status) {
                case CONNECTED:
                    color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary);
                    label = "Server: Connected";
                    break;
                case CHECKING:
                    color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_outline);
                    label = "Server: Checking...";
                    break;
                case UNAVAILABLE:
                    color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_error);
                    label = "Server: Unavailable";
                    break;
                case NOT_CONFIGURED:
                default:
                    color = ContextCompat.getColor(requireContext(), R.color.md_theme_light_outline);
                    label = "Server: Not configured";
                    break;
            }
            tvConnectionStatus.setText(label);
            viewStatusIndicator.setBackgroundTintList(ColorStateList.valueOf(color));
        });

        viewModel.getLoginSuccess().observe(getViewLifecycleOwner(), event -> {
            if (event.getContentIfNotHandled() != null) {
                // Section 21: Navigate to existing authenticated root and remove Auth from back-stack
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.dest_auth, true)
                        .build();

                NavHostFragment.findNavController(AuthFragment.this)
                        .navigate(R.id.action_dest_auth_to_nav_main, null, navOptions);
            }
        });

        viewModel.getRegistrationSuccess().observe(getViewLifecycleOwner(), event -> {
            String email = event.getContentIfNotHandled();
            if (email != null) {
                inputEmail.setText(email);
                inputPassword.setText("");
                inputConfirmPassword.setText("");
                Snackbar.make(requireView(), "Account created successfully. Please sign in.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void updateRequirementRow(ImageView iv, TextView tv, boolean satisfied) {
        int color = satisfied
                ? ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary)
                : ContextCompat.getColor(requireContext(), R.color.md_theme_light_outline);

        iv.setImageResource(satisfied ? R.drawable.ic_check : R.drawable.ic_circle_outline);
        iv.setImageTintList(ColorStateList.valueOf(color));
        tv.setTextColor(color);
    }

    private void showConnectionDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_server_connection, null);

        TextInputEditText inputUrl = dialogView.findViewById(R.id.input_server_url);
        MaterialButton btnTest = dialogView.findViewById(R.id.btn_test_connection);
        ProgressBar progressTest = dialogView.findViewById(R.id.progress_test_connection);
        TextView tvResult = dialogView.findViewById(R.id.tv_test_result);

        inputUrl.setText(viewModel.getCurrentServerUrl());

        btnTest.setOnClickListener(v -> {
            String candidateUrl = inputUrl.getText() != null ? inputUrl.getText().toString().trim() : "";
            progressTest.setVisibility(View.VISIBLE);
            tvResult.setText("Testing...");
            tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_outline));

            viewModel.testCandidateUrl(candidateUrl, new AuthRepository.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean isUp) {
                    if (!isAdded()) return;
                    progressTest.setVisibility(View.GONE);
                    if (isUp) {
                        tvResult.setText("Connected (UP)");
                        tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary));
                    } else {
                        tvResult.setText("Unavailable");
                        tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_error));
                    }
                }

                @Override
                public void onError(String message, @Nullable Integer retryAfterSeconds) {
                    if (!isAdded()) return;
                    progressTest.setVisibility(View.GONE);
                    tvResult.setText("Unavailable");
                    tvResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_error));
                }
            });
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String url = inputUrl.getText() != null ? inputUrl.getText().toString().trim() : "";
                    viewModel.saveServerUrl(url);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }
}
