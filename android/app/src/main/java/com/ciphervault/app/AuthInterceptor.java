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

        return chain.proceed(
                requestBuilder.build()
        );
    }
}