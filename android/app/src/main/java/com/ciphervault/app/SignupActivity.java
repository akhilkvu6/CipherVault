package com.ciphervault.app;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends BaseActivity {

    private ShapeableImageView ivSignupAvatar;
    private FloatingActionButton btnSelectPhoto;
    private MaterialButton btnServerConfig;
    private TextInputEditText etFullName;
    private TextInputEditText etUsername;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    private ImageView ivReqLength;
    private TextView tvReqLength;
    private ImageView ivReqUpper;
    private TextView tvReqUpper;
    private ImageView ivReqLower;
    private TextView tvReqLower;
    private ImageView ivReqNumber;
    private TextView tvReqNumber;
    private ImageView ivReqSymbol;
    private TextView tvReqSymbol;

    private Uri selectedPhotoUri = null;
    private byte[] selectedPhotoBytes = null;
    private ActivityResultLauncher<String> photoPickerLauncher;

    private static final Pattern PATTERN_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PATTERN_LOWER = Pattern.compile("[a-z]");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("[0-9]");
    private static final Pattern PATTERN_SYMBOL = Pattern.compile("[^a-zA-Z0-9]");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        initViews();
        setupPhotoPicker();
        setupPasswordWatcher();
        setupListeners();
        updatePasswordChecklist("");
        validateAllFields();
    }

    private void initViews() {
        ivSignupAvatar = findViewById(R.id.ivSignupAvatar);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnServerConfig = findViewById(R.id.btnServerConfig);
        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        ivReqLength = findViewById(R.id.ivReqLength);
        tvReqLength = findViewById(R.id.tvReqLength);
        ivReqUpper = findViewById(R.id.ivReqUpper);
        tvReqUpper = findViewById(R.id.tvReqUpper);
        ivReqLower = findViewById(R.id.ivReqLower);
        tvReqLower = findViewById(R.id.tvReqLower);
        ivReqNumber = findViewById(R.id.ivReqNumber);
        tvReqNumber = findViewById(R.id.tvReqNumber);
        ivReqSymbol = findViewById(R.id.ivReqSymbol);
        tvReqSymbol = findViewById(R.id.tvReqSymbol);
    }

    private void setupPhotoPicker() {
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedPhotoUri = uri;
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            if (is != null) {
                                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                                byte[] data = new byte[8192];
                                int nRead;
                                while ((nRead = is.read(data, 0, data.length)) != -1) {
                                    buffer.write(data, 0, nRead);
                                }
                                selectedPhotoBytes = buffer.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(selectedPhotoBytes, 0, selectedPhotoBytes.length);
                                ivSignupAvatar.setImageBitmap(bitmap);
                                ivSignupAvatar.setPadding(0, 0, 0, 0);
                                ivSignupAvatar.setImageTintList(null);
                            }
                        } catch (Exception e) {
                            ivSignupAvatar.setImageURI(uri);
                        }
                    }
                }
        );
    }

    private void setupPasswordWatcher() {
        TextWatcher allFieldsWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAllFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etFullName.addTextChangedListener(allFieldsWatcher);
        etUsername.addTextChangedListener(allFieldsWatcher);
        etEmail.addTextChangedListener(allFieldsWatcher);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordChecklist(s != null ? s.toString() : "");
                validateAllFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pwd = etPassword.getText() != null ? etPassword.getText().toString() : "";
                String cpwd = s != null ? s.toString() : "";
                if (!cpwd.isEmpty() && !cpwd.equals(pwd)) {
                    etConfirmPassword.setError("Passwords do not match");
                } else {
                    etConfirmPassword.setError(null);
                }
                validateAllFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordChecklist(String password) {
        boolean isEmpty = password == null || password.isEmpty();
        boolean hasLength = !isEmpty && password.length() >= 8;
        boolean hasUpper = !isEmpty && PATTERN_UPPER.matcher(password).find();
        boolean hasLower = !isEmpty && PATTERN_LOWER.matcher(password).find();
        boolean hasNumber = !isEmpty && PATTERN_NUMBER.matcher(password).find();
        boolean hasSymbol = !isEmpty && PATTERN_SYMBOL.matcher(password).find();

        int colorSatisfied = ContextCompat.getColor(this, R.color.status_connected);
        int colorUnsatisfied = ContextCompat.getColor(this, R.color.status_error);
        int colorNeutral = ContextCompat.getColor(this, R.color.cv_border);

        int textSatisfied = ContextCompat.getColor(this, R.color.status_connected);
        int textUnsatisfied = ContextCompat.getColor(this, R.color.status_error);
        int textNeutral = ContextCompat.getColor(this, R.color.cv_text_secondary);

        setRequirementState(ivReqLength, tvReqLength, hasLength, isEmpty, colorSatisfied, colorUnsatisfied, colorNeutral, textSatisfied, textUnsatisfied, textNeutral);
        setRequirementState(ivReqUpper, tvReqUpper, hasUpper, isEmpty, colorSatisfied, colorUnsatisfied, colorNeutral, textSatisfied, textUnsatisfied, textNeutral);
        setRequirementState(ivReqLower, tvReqLower, hasLower, isEmpty, colorSatisfied, colorUnsatisfied, colorNeutral, textSatisfied, textUnsatisfied, textNeutral);
        setRequirementState(ivReqNumber, tvReqNumber, hasNumber, isEmpty, colorSatisfied, colorUnsatisfied, colorNeutral, textSatisfied, textUnsatisfied, textNeutral);
        setRequirementState(ivReqSymbol, tvReqSymbol, hasSymbol, isEmpty, colorSatisfied, colorUnsatisfied, colorNeutral, textSatisfied, textUnsatisfied, textNeutral);
    }

    private void setRequirementState(ImageView iv, TextView tv, boolean satisfied, boolean neutral,
                                     int colorSatisfied, int colorUnsatisfied, int colorNeutral,
                                     int textSatisfied, int textUnsatisfied, int textNeutral) {
        if (iv == null || tv == null) return;
        if (neutral) {
            iv.setImageResource(R.drawable.ic_lucide_circle);
            iv.setImageTintList(ColorStateList.valueOf(colorNeutral));
            tv.setTextColor(textNeutral);
        } else if (satisfied) {
            iv.setImageResource(R.drawable.ic_lucide_check_circle);
            iv.setImageTintList(ColorStateList.valueOf(colorSatisfied));
            tv.setTextColor(textSatisfied);
        } else {
            iv.setImageResource(R.drawable.ic_lucide_alert_triangle);
            iv.setImageTintList(ColorStateList.valueOf(colorUnsatisfied));
            tv.setTextColor(textUnsatisfied);
        }
    }

    private boolean validateAllFields() {
        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

        boolean hasName = fullName.length() >= 2;
        boolean hasUsername = username.length() >= 3;
        boolean hasEmail = !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();

        boolean hasLength = password.length() >= 8;
        boolean hasUpper = PATTERN_UPPER.matcher(password).find();
        boolean hasLower = PATTERN_LOWER.matcher(password).find();
        boolean hasNumber = PATTERN_NUMBER.matcher(password).find();
        boolean hasSymbol = PATTERN_SYMBOL.matcher(password).find();
        boolean isPasswordValid = hasLength && hasUpper && hasLower && hasNumber && hasSymbol;

        boolean matchesConfirm = !confirmPassword.isEmpty() && password.equals(confirmPassword);

        boolean allValid = hasName && hasUsername && hasEmail && isPasswordValid && matchesConfirm;
        if (btnRegister != null) {
            btnRegister.setEnabled(allValid);
        }
        return allValid;
    }

    private void setupListeners() {
        if (btnSelectPhoto != null) {
            btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        }
        if (ivSignupAvatar != null) {
            ivSignupAvatar.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        }

        if (btnServerConfig != null) {
            btnServerConfig.setOnClickListener(v -> {
                Intent intent = new Intent(SignupActivity.this, ConnectionActivity.class);
                startActivity(intent);
            });
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        if (!validateAllFields()) {
            Toast.makeText(this, "Please satisfy all registration requirements", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        RegisterRequest request = new RegisterRequest(fullName, username, email, password);

        ApiClient.getApiService(this).register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(@NonNull Call<RegisterResponse> call, @NonNull Response<RegisterResponse> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Create Account");

                if (response.isSuccessful()) {
                    if (selectedPhotoBytes != null && selectedPhotoBytes.length > 0) {
                        ProfilePhotoHelper.savePendingSignupPhotoBytes(SignupActivity.this, email, selectedPhotoBytes);
                    } else if (selectedPhotoUri != null) {
                        ProfilePhotoHelper.savePendingSignupPhoto(SignupActivity.this, email, selectedPhotoUri);
                    }
                    Toast.makeText(SignupActivity.this, "Account created successfully! Please sign in.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Registration failed (" + response.code() + ")";
                    if (response.code() == 409) {
                        errorMsg = "Username or email is already registered.";
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                String body = response.errorBody().string();
                                if (!body.isEmpty()) {
                                    errorMsg = body;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RegisterResponse> call, @NonNull Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Create Account");
                com.google.android.material.snackbar.Snackbar.make(
                        findViewById(android.R.id.content),
                        "Server unreachable: " + t.getMessage(),
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).setAction("Server IP", v -> {
                    Intent intent = new Intent(SignupActivity.this, ConnectionActivity.class);
                    startActivity(intent);
                }).show();
            }
        });
    }
}
