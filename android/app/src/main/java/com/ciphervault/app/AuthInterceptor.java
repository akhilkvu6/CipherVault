package com.ciphervault.app;

import android.content.Context;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = null;
        if (context != null) {
            try {
                token = SessionManager.getInstance(context).getToken();
            } catch (Throwable t) {
                Log.w("AuthInterceptor", "Could not read auth token: " + t.getMessage());
            }
        }

        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        if (originalRequest.header("Authorization") == null && token != null && !token.trim().isEmpty()) {
            requestBuilder.addHeader(
                    "Authorization",
                    "Bearer " + token.trim()
            );
        }

        Response response = chain.proceed(
                requestBuilder.build()
        );

        if (response.code() == 401 && token != null && !token.trim().isEmpty() && context != null) {
            AuditLogger.log(context, "Session Expired", "FAILED", "Authorization token expired or invalid (HTTP 401)");
            try {
                SessionManager.getInstance(context).logout();
            } catch (Throwable t) {
                Log.w("AuthInterceptor", "Could not clear session on 401: " + t.getMessage());
            }
        }

        return response;
    }
}