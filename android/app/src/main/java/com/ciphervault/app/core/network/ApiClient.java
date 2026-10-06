package com.ciphervault.app.core.network;

import android.content.Context;

import com.ciphervault.app.core.preferences.ConnectionPreferences;
import com.ciphervault.app.core.session.AuthSessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    public static Retrofit getClient(Context context) {
        ConnectionPreferences prefs = new ConnectionPreferences(context);
        String baseUrl = prefs.getServerUrl();
        
        if (baseUrl == null) {
            return null;
        }

        // Rebuild retrofit only if the base url changed or it's null
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            AuthSessionManager sessionManager = new AuthSessionManager(context);

            Interceptor authInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder();
                
                String token = sessionManager.getAuthToken();
                if (token != null && !token.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                }
                
                return chain.proceed(requestBuilder.build());
            };

            Interceptor unauthorizedInterceptor = chain -> {
                okhttp3.Response response = chain.proceed(chain.request());
                if (response.code() == 401) {
                    if (sessionManager.hasValidSession()) {
                        sessionManager.logout();
                        android.content.Intent intent = new android.content.Intent(context, com.ciphervault.app.auth.ui.SignInActivity.class);
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(intent);
                    }
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(unauthorizedInterceptor)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
                    
            currentBaseUrl = baseUrl;
        }

        return retrofit;
    }

    public static void invalidate() {
        retrofit = null;
        currentBaseUrl = null;
    }

    public static Retrofit createTempClient(String baseUrl) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
