package com.ciphervault.app.auth.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ciphervault.app.auth.data.AuthRepository;
import com.ciphervault.app.auth.model.HealthResponse;
import com.ciphervault.app.auth.model.LoginResponse;
import com.ciphervault.app.auth.model.RegisterResponse;
import com.ciphervault.app.core.preferences.ConnectionPreferences;
import com.ciphervault.app.core.session.AuthSessionManager;
import com.ciphervault.app.core.network.ApiClient;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final AuthSessionManager sessionManager;
    private final ConnectionPreferences connectionPreferences;

    private final MutableLiveData<Resource<String>> connectionState = new MutableLiveData<>();
    private final MutableLiveData<Resource<LoginResponse>> loginState = new MutableLiveData<>();
    private final MutableLiveData<Resource<RegisterResponse>> registerState = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
        sessionManager = new AuthSessionManager(application);
        connectionPreferences = new ConnectionPreferences(application);
    }

    public LiveData<Resource<String>> getConnectionState() { return connectionState; }
    public LiveData<Resource<LoginResponse>> getLoginState() { return loginState; }
    public LiveData<Resource<RegisterResponse>> getRegisterState() { return registerState; }

    public void testConnection(String url) {
        connectionState.setValue(Resource.loading());
        
        // Temporarily set it to test
        connectionPreferences.setServerUrl(url);
        ApiClient.invalidate(); // force rebuild
        
        repository.checkHealth(new AuthRepository.RepoCallback<HealthResponse>() {
            @Override
            public void onSuccess(HealthResponse result) {
                if ("UP".equals(result.getStatus())) {
                    connectionState.postValue(Resource.success("Connected to CipherVault"));
                } else {
                    connectionState.postValue(Resource.error("Server returned unknown status"));
                }
            }

            @Override
            public void onError(String error) {
                connectionPreferences.setServerUrl(null); // revert on fail
                ApiClient.invalidate();
                connectionState.postValue(Resource.error("Unable to connect to CipherVault.\nCheck the server address and try again."));
            }
        });
    }

    public void login(String email, String password) {
        loginState.setValue(Resource.loading());
        repository.login(email, password, new AuthRepository.RepoCallback<LoginResponse>() {
            @Override
            public void onSuccess(LoginResponse result) {
                if (result.isSuccess() && result.getToken() != null) {
                    sessionManager.createSession(result.getToken(), result.getUsername());
                    loginState.postValue(Resource.success(result));
                } else {
                    loginState.postValue(Resource.error(result.getMessage() != null ? result.getMessage() : "Invalid credentials"));
                }
            }

            @Override
            public void onError(String error) {
                loginState.postValue(Resource.error(error));
            }
        });
    }

    public void register(String username, String email, String password) {
        registerState.setValue(Resource.loading());
        repository.register(username, email, password, new AuthRepository.RepoCallback<RegisterResponse>() {
            @Override
            public void onSuccess(RegisterResponse result) {
                registerState.postValue(Resource.success(result));
            }

            @Override
            public void onError(String error) {
                registerState.postValue(Resource.error(error));
            }
        });
    }

    public static class Resource<T> {
        public enum Status { SUCCESS, ERROR, LOADING }
        public final Status status;
        public final T data;
        public final String message;

        private Resource(Status status, T data, String message) {
            this.status = status;
            this.data = data;
            this.message = message;
        }

        public static <T> Resource<T> success(T data) { return new Resource<>(Status.SUCCESS, data, null); }
        public static <T> Resource<T> error(String msg) { return new Resource<>(Status.ERROR, null, msg); }
        public static <T> Resource<T> loading() { return new Resource<>(Status.LOADING, null, null); }
    }
}
