package com.ciphervault.app.auth.ui;

import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ciphervault.app.auth.model.LoginResponse;
import com.ciphervault.app.auth.model.RegisterResponse;
import com.ciphervault.app.auth.repository.AuthRepository;
import com.ciphervault.app.auth.validation.AuthValidator;
import com.ciphervault.app.auth.validation.PasswordRequirements;
import com.ciphervault.app.core.network.ConnectionStatus;
import com.ciphervault.app.core.network.ServerConnectionPreferences;
import com.ciphervault.app.core.ui.Event;

/**
 * ViewModel coordinating authentication, validation, rate-limiting, and server connection (Section 32).
 */
public class AuthViewModel extends ViewModel {

    public enum Mode {
        LOGIN,
        CREATE_ACCOUNT
    }

    private final AuthRepository authRepository;
    private final ServerConnectionPreferences serverPreferences;

    private final MutableLiveData<Mode> mode = new MutableLiveData<>(Mode.LOGIN);
    private final MutableLiveData<PasswordRequirements> passwordRequirements = new MutableLiveData<>(
            new PasswordRequirements(false, false, false, false, false)
    );
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> lockoutRemainingSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>(ConnectionStatus.CHECKING);

    private final MutableLiveData<Event<Boolean>> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> registrationSuccess = new MutableLiveData<>();

    private CountDownTimer lockoutTimer;

    public AuthViewModel(
            @NonNull AuthRepository authRepository,
            @NonNull ServerConnectionPreferences serverPreferences
    ) {
        this.authRepository = authRepository;
        this.serverPreferences = serverPreferences;
        checkServerConnection();
    }

    public LiveData<Mode> getMode() {
        return mode;
    }

    public void setMode(Mode newMode) {
        mode.setValue(newMode);
        errorMessage.setValue(null);
    }

    public LiveData<PasswordRequirements> getPasswordRequirements() {
        return passwordRequirements;
    }

    public void onPasswordChanged(String password) {
        passwordRequirements.setValue(PasswordRequirements.check(password));
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    public LiveData<Integer> getLockoutRemainingSeconds() {
        return lockoutRemainingSeconds;
    }

    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<Event<Boolean>> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<Event<String>> getRegistrationSuccess() {
        return registrationSuccess;
    }

    public String getCurrentServerUrl() {
        return serverPreferences.getServerUrl();
    }

    public void checkServerConnection() {
        connectionStatus.setValue(ConnectionStatus.CHECKING);
        authRepository.checkHealth(null, new AuthRepository.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isUp) {
                connectionStatus.setValue(isUp ? ConnectionStatus.CONNECTED : ConnectionStatus.UNAVAILABLE);
            }

            @Override
            public void onError(String message, @Nullable Integer retryAfterSeconds) {
                connectionStatus.setValue(ConnectionStatus.UNAVAILABLE);
            }
        });
    }

    public void testCandidateUrl(
            @NonNull String candidateUrl,
            @NonNull AuthRepository.ResultCallback<Boolean> callback
    ) {
        authRepository.checkHealth(candidateUrl, callback);
    }

    public void saveServerUrl(@NonNull String url) {
        serverPreferences.setServerUrl(url);
        checkServerConnection();
    }

    public void login(@NonNull String email, @NonNull String password) {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }

        Integer remainingLockout = lockoutRemainingSeconds.getValue();
        if (remainingLockout != null && remainingLockout > 0) {
            errorMessage.setValue("Too many attempts. Please wait " + remainingLockout + " seconds.");
            return;
        }

        String emailErr = AuthValidator.validateEmail(email);
        if (emailErr != null) {
            errorMessage.setValue(emailErr);
            return;
        }

        if (password.trim().isEmpty()) {
            errorMessage.setValue("Password is required");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.login(email.trim(), password, new AuthRepository.ResultCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse result) {
                isLoading.setValue(false);
                loginSuccess.setValue(new Event<>(true));
            }

            @Override
            public void onError(String message, @Nullable Integer retryAfterSeconds) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
                if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                    startLockoutCountdown(retryAfterSeconds);
                }
            }
        });
    }

    public void register(
            @NonNull String name,
            @NonNull String username,
            @NonNull String email,
            @NonNull String password,
            @NonNull String confirmPassword
    ) {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }

        String nameErr = AuthValidator.validateName(name);
        if (nameErr != null) {
            errorMessage.setValue(nameErr);
            return;
        }

        String usernameErr = AuthValidator.validateUsername(username);
        if (usernameErr != null) {
            errorMessage.setValue(usernameErr);
            return;
        }

        String emailErr = AuthValidator.validateEmail(email);
        if (emailErr != null) {
            errorMessage.setValue(emailErr);
            return;
        }

        String passwordErr = AuthValidator.validatePassword(password);
        if (passwordErr != null) {
            errorMessage.setValue(passwordErr);
            return;
        }

        String confirmErr = AuthValidator.validateConfirmPassword(password, confirmPassword);
        if (confirmErr != null) {
            errorMessage.setValue(confirmErr);
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.register(
                name.trim(),
                username.trim(),
                email.trim(),
                password,
                new AuthRepository.ResultCallback<RegisterResponse>() {
                    @Override
                    public void onSuccess(RegisterResponse result) {
                        isLoading.setValue(false);
                        // On success: switch to login mode and preserve email (Section 20)
                        mode.setValue(Mode.LOGIN);
                        registrationSuccess.setValue(new Event<>(email.trim()));
                    }

                    @Override
                    public void onError(String message, @Nullable Integer retryAfterSeconds) {
                        isLoading.setValue(false);
                        errorMessage.setValue(message);
                    }
                }
        );
    }

    private void startLockoutCountdown(int seconds) {
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
        }
        lockoutRemainingSeconds.setValue(seconds);

        lockoutTimer = new CountDownTimer((long) seconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000L);
                lockoutRemainingSeconds.setValue(secs);
            }

            @Override
            public void onFinish() {
                lockoutRemainingSeconds.setValue(0);
                errorMessage.setValue(null);
            }
        }.start();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (lockoutTimer != null) {
            lockoutTimer.cancel();
            lockoutTimer = null;
        }
    }
}
