package com.ciphervault.app.main.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileResponse;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ThumbnailLoader {

    private static final int CACHE_SIZE = 40;
    private static final LruCache<Long, Bitmap> memoryCache = new LruCache<>(CACHE_SIZE);
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadThumbnail(ImageView imageView, FileResponse file, Context context) {
        if (file == null || imageView == null || context == null) return;

        imageView.setTag(file.id);
        int defaultIcon = getDefaultIcon(file);

        Bitmap cached = memoryCache.get(file.id);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        // Show default vector icon while loading
        int pad = (int) (10 * context.getResources().getDisplayMetrics().density);
        imageView.setImageResource(defaultIcon);
        imageView.setPadding(pad, pad, pad, pad);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        boolean canHaveThumbnail = file.hasPreview || 
                (file.contentType != null && (file.contentType.startsWith("image/") || file.contentType.startsWith("video/")));

        if (!canHaveThumbnail) {
            return;
        }

        long fileId = file.id;
        executor.execute(() -> {
            try {
                FileApi api = ApiClient.getClient(context).create(FileApi.class);
                Response<ResponseBody> resp = api.getFilePreview(fileId).execute();
                if (resp.isSuccessful() && resp.body() != null) {
                    try (InputStream is = resp.body().byteStream()) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        if (bmp != null) {
                            memoryCache.put(fileId, bmp);
                            mainHandler.post(() -> {
                                Object currentTag = imageView.getTag();
                                if (currentTag instanceof Long && ((Long) currentTag) == fileId) {
                                    imageView.setImageBitmap(bmp);
                                    imageView.setPadding(0, 0, 0, 0);
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }
                            });
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public static int getDefaultIcon(FileResponse file) {
        if (file.contentType != null) {
            if (file.contentType.startsWith("image/")) return R.drawable.ic_lucide_image;
            if (file.contentType.startsWith("video/")) return R.drawable.ic_lucide_video;
            if (file.contentType.startsWith("audio/")) return R.drawable.ic_lucide_music;
            if (file.contentType.contains("pdf") || file.contentType.contains("text") || file.contentType.contains("document") || file.contentType.contains("msword")) {
                return R.drawable.ic_lucide_file_text;
            }
        }
        if (file.filename != null) {
            String lower = file.filename.toLowerCase();
            if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
                return R.drawable.ic_lucide_image;
            }
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".mov")) {
                return R.drawable.ic_lucide_video;
            }
            if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".flac")) {
                return R.drawable.ic_lucide_music;
            }
            if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".txt") || lower.endsWith(".csv")) {
                return R.drawable.ic_lucide_file_text;
            }
        }
        return R.drawable.ic_lucide_file;
    }
}
