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

        Request.Builder requestBuilder = chain.request().newBuilder();

        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader(
                    "Authorization",
                    "Bearer " + token
            );
        }

        Response response = chain.proceed(
                requestBuilder.build()
        );

        if (response.code() == 401 && token != null && !token.isEmpty() && context != null) {
            AuditLogger.log(context, "Session Expired", "FAILED", "Authorization token expired or invalid (HTTP 401)");
        }

        return response;
    }
}