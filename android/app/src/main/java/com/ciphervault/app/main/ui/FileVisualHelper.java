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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class FileVisualHelper {

    private static final int CACHE_SIZE = 50;
    private static final LruCache<Long, Bitmap> memoryCache = new LruCache<>(CACHE_SIZE);
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void bindFileVisual(ImageView imageView, FileResponse file, Context context) {
        if (file == null || imageView == null || context == null) return;

        imageView.setTag(file.id);
        int defaultIcon = getFileIconResource(file.filename, file.contentType);

        Bitmap cached = file.id != null ? memoryCache.get(file.id) : null;
        if (cached != null) {
            imageView.setImageBitmap(cached);
            imageView.setPadding(0, 0, 0, 0);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return;
        }

        // Show default outline vector icon inside the rounded container
        int pad = (int) (10 * context.getResources().getDisplayMetrics().density);
        imageView.setImageResource(defaultIcon);
        imageView.setPadding(pad, pad, pad, pad);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        boolean canHaveThumbnail = file.hasPreview ||
                (file.contentType != null && (file.contentType.startsWith("image/") || file.contentType.startsWith("video/")));

        if (!canHaveThumbnail || file.id == null) {
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
            } catch (Exception ignored) {}
        });
    }

    public static int getFileIconResource(String filename, String contentType) {
        String mime = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        String name = filename != null ? filename.toLowerCase(Locale.ROOT) : "";

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif")) {
            return R.drawable.ic_lucide_image;
        }
        if (mime.startsWith("video/") || name.endsWith(".mp4") || name.endsWith(".mkv")
                || name.endsWith(".avi") || name.endsWith(".mov")) {
            return R.drawable.ic_lucide_video;
        }
        if (mime.startsWith("audio/") || name.endsWith(".mp3") || name.endsWith(".wav")
                || name.endsWith(".m4a") || name.endsWith(".aac") || name.endsWith(".flac")) {
            return R.drawable.ic_lucide_music;
        }
        if (mime.contains("zip") || mime.contains("compressed") || mime.contains("tar") || mime.contains("7z")
                || name.endsWith(".zip") || name.endsWith(".tar") || name.endsWith(".gz") || name.endsWith(".7z") || name.endsWith(".rar")) {
            return R.drawable.ic_lucide_archive;
        }
        if (name.endsWith(".apk")) {
            return R.drawable.ic_lucide_package;
        }
        if (mime.contains("pdf") || mime.contains("document") || mime.contains("msword")
                || mime.contains("sheet") || mime.contains("presentation") || mime.startsWith("text/")
                || name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx")
                || name.endsWith(".txt") || name.endsWith(".xlsx") || name.endsWith(".csv")
                || name.endsWith(".json") || name.endsWith(".xml")) {
            return R.drawable.ic_lucide_file_text;
        }
        return R.drawable.ic_lucide_file;
    }
}
