package com.ciphervault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ThumbnailLoader {

    private static final int CACHE_SIZE_KB = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
    private static final LruCache<Long, Bitmap> memoryCache = new LruCache<Long, Bitmap>(Math.max(1024, CACHE_SIZE_KB)) {
        @Override
        protected int sizeOf(Long key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService decodeExecutor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "cv-thumb-decoder");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    public static void loadThumbnail(@NonNull Context context,
                                     @NonNull Long fileId,
                                     @NonNull ImageView ivThumbnail,
                                     @Nullable ImageView ivPlaceholder,
                                     @Nullable ImageView ivVideoBadge) {
        ivThumbnail.setTag(fileId);

        Bitmap cached = memoryCache.get(fileId);
        if (cached != null) {
            ivThumbnail.setImageBitmap(cached);
            ivThumbnail.setVisibility(View.VISIBLE);
            if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.GONE);
            if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.VISIBLE);
            return;
        }

        ivThumbnail.setImageDrawable(null);
        ivThumbnail.setVisibility(View.GONE);
        if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
        if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);

        ApiService apiService = ApiClient.getApiService(context);
        apiService.getFilePreview(fileId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    final ResponseBody body = response.body();
                    decodeExecutor.execute(() -> {
                        try (InputStream is = body.byteStream()) {
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            if (bitmap != null) {
                                memoryCache.put(fileId, bitmap);
                                mainHandler.post(() -> {
                                    Object currentTag = ivThumbnail.getTag();
                                    if (currentTag != null && currentTag.equals(fileId)) {
                                        ivThumbnail.setImageBitmap(bitmap);
                                        ivThumbnail.setVisibility(View.VISIBLE);
                                        if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.GONE);
                                        if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                // On failure, generic placeholder remains visible
            }
        });
    }

    public static void remove(@Nullable Long fileId) {
        if (fileId != null) {
            memoryCache.remove(fileId);
        }
    }

    public static void clearCache() {
        memoryCache.evictAll();
    }
}
