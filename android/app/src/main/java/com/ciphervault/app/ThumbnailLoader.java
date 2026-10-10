package com.ciphervault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Locale;
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

    private static final LruCache<String, Bitmap> uriMemoryCache = new LruCache<String, Bitmap>(Math.max(1024, CACHE_SIZE_KB)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
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

    /**
     * Loads a thumbnail for an already-uploaded remote file in Vault / Recent files.
     */
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
                        Bitmap bitmap = null;
                        try (InputStream is = body.byteStream()) {
                            bitmap = BitmapFactory.decodeStream(is);
                        } catch (Exception ignored) {}

                        final Bitmap finalBitmap = bitmap;
                        mainHandler.post(() -> {
                            Object currentTag = ivThumbnail.getTag();
                            if (currentTag != null && currentTag.equals(fileId)) {
                                if (finalBitmap != null) {
                                    memoryCache.put(fileId, finalBitmap);
                                    ivThumbnail.setImageBitmap(finalBitmap);
                                    ivThumbnail.setVisibility(View.VISIBLE);
                                    if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.GONE);
                                    if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.VISIBLE);
                                } else {
                                    ivThumbnail.setVisibility(View.GONE);
                                    if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
                                    if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);
                                }
                            }
                        });
                    });
                } else {
                    mainHandler.post(() -> {
                        Object currentTag = ivThumbnail.getTag();
                        if (currentTag != null && currentTag.equals(fileId)) {
                            ivThumbnail.setVisibility(View.GONE);
                            if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
                            if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                mainHandler.post(() -> {
                    Object currentTag = ivThumbnail.getTag();
                    if (currentTag != null && currentTag.equals(fileId)) {
                        ivThumbnail.setVisibility(View.GONE);
                        if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
                        if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    /**
     * Loads a thumbnail for a staging file (local URI) with OOM-safe subsampling and
     * file-type specific icons for documents/audio/archives.
     */
    public static void loadStagingThumbnail(@NonNull Context context,
                                            @NonNull Uri uri,
                                            @Nullable String fileName,
                                            @NonNull ImageView ivThumbnail,
                                            @NonNull ImageView ivIcon) {
        String uriKey = uri.toString();
        ivThumbnail.setTag(uriKey);

        String mimeType = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try {
                mimeType = context.getContentResolver().getType(uri);
            } catch (Exception ignored) {}
        }

        String ext = "";
        if (fileName != null) {
            int dot = fileName.lastIndexOf('.');
            if (dot >= 0) {
                ext = fileName.substring(dot + 1).toLowerCase(Locale.US);
            }
        }

        boolean isImage = (mimeType != null && mimeType.startsWith("image/"))
                || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png")
                || ext.equals("webp") || ext.equals("heic") || ext.equals("heif")
                || ext.equals("bmp") || ext.equals("gif");

        boolean isVideo = (mimeType != null && mimeType.startsWith("video/"))
                || ext.equals("mp4") || ext.equals("mkv") || ext.equals("webm")
                || ext.equals("avi") || ext.equals("mov") || ext.equals("3gp")
                || ext.equals("flv") || ext.equals("ts");

        boolean isAudio = (mimeType != null && mimeType.startsWith("audio/"))
                || ext.equals("mp3") || ext.equals("wav") || ext.equals("flac")
                || ext.equals("m4a") || ext.equals("aac") || ext.equals("ogg");

        boolean isPdf = "application/pdf".equalsIgnoreCase(mimeType) || ext.equals("pdf");

        boolean isDoc = ext.equals("doc") || ext.equals("docx") || ext.equals("txt")
                || ext.equals("rtf") || ext.equals("odt") || ext.equals("csv")
                || ext.equals("xls") || ext.equals("xlsx") || ext.equals("ppt")
                || ext.equals("pptx");

        boolean isArchive = ext.equals("zip") || ext.equals("rar") || ext.equals("7z")
                || ext.equals("tar") || ext.equals("gz") || ext.equals("bz2");

        if (isImage) {
            Bitmap cached = uriMemoryCache.get(uriKey);
            if (cached != null) {
                ivThumbnail.setImageBitmap(cached);
                ivThumbnail.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
                return;
            }

            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_image);
            ivIcon.setVisibility(View.VISIBLE);

            decodeExecutor.execute(() -> {
                Bitmap bmp = decodeSampledBitmapFromUri(context, uri, 180, 180);
                mainHandler.post(() -> {
                    if (uriKey.equals(ivThumbnail.getTag())) {
                        if (bmp != null) {
                            uriMemoryCache.put(uriKey, bmp);
                            ivThumbnail.setImageBitmap(bmp);
                            ivThumbnail.setVisibility(View.VISIBLE);
                            ivIcon.setVisibility(View.GONE);
                        } else {
                            ivThumbnail.setVisibility(View.GONE);
                            ivIcon.setImageResource(R.drawable.ic_lucide_image);
                            ivIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
            });
        } else if (isVideo) {
            Bitmap cached = uriMemoryCache.get(uriKey);
            if (cached != null) {
                ivThumbnail.setImageBitmap(cached);
                ivThumbnail.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
                return;
            }

            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_video);
            ivIcon.setVisibility(View.VISIBLE);

            decodeExecutor.execute(() -> {
                Bitmap frame = extractVideoFrame(context, uri, 180, 180);
                mainHandler.post(() -> {
                    if (uriKey.equals(ivThumbnail.getTag())) {
                        if (frame != null) {
                            uriMemoryCache.put(uriKey, frame);
                            ivThumbnail.setImageBitmap(frame);
                            ivThumbnail.setVisibility(View.VISIBLE);
                            ivIcon.setVisibility(View.GONE);
                        } else {
                            ivThumbnail.setVisibility(View.GONE);
                            ivIcon.setImageResource(R.drawable.ic_lucide_video);
                            ivIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
            });
        } else if (isPdf) {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_file_text);
            ivIcon.setVisibility(View.VISIBLE);
        } else if (isDoc) {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_file_text);
            ivIcon.setVisibility(View.VISIBLE);
        } else if (isArchive) {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_archive);
            ivIcon.setVisibility(View.VISIBLE);
        } else if (isAudio) {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_music);
            ivIcon.setVisibility(View.VISIBLE);
        } else {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_file);
            ivIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Decodes a memory-efficient subsampled Bitmap from a Uri and corrects EXIF rotation.
     */
    public static Bitmap decodeSampledBitmapFromUri(Context context, Uri uri, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, options);
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null;
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = null;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    bitmap = BitmapFactory.decodeStream(is, null, options);
                }
            }

            if (bitmap == null) return null;

            int rotation = 0;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    int orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                        rotation = 90;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                        rotation = 180;
                    } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        rotation = 270;
                    }
                }
            } catch (Exception ignored) {}

            if (rotation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }

            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts a video frame thumbnail via MediaMetadataRetriever, safely scaled to thumbnail bounds.
     */
    public static Bitmap extractVideoFrame(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (context == null || uri == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "content".equalsIgnoreCase(uri.getScheme())) {
            try {
                Bitmap thumb = context.getContentResolver().loadThumbnail(uri, new android.util.Size(reqWidth, reqHeight), null);
                if (thumb != null) {
                    return thumb;
                }
            } catch (Throwable ignored) {}
        }
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            Bitmap frame = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                try {
                    frame = retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, reqWidth, reqHeight);
                } catch (Throwable ignored) {}
                if (frame == null) {
                    try {
                        frame = retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST, reqWidth, reqHeight);
                    } catch (Throwable ignored) {}
                }
                if (frame == null) {
                    try {
                        frame = retriever.getScaledFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, reqWidth, reqHeight);
                    } catch (Throwable ignored) {}
                }
            }
            if (frame == null) {
                try {
                    frame = retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                } catch (Throwable ignored) {}
            }
            if (frame == null) {
                try {
                    frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);
                } catch (Throwable ignored) {}
            }
            if (frame == null) {
                try {
                    frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                } catch (Throwable ignored) {}
            }
            if (frame == null) {
                try {
                    frame = retriever.getFrameAtTime();
                } catch (Throwable ignored) {}
            }
            if (frame != null && (frame.getWidth() > reqWidth * 2 || frame.getHeight() > reqHeight * 2)) {
                Bitmap scaled = Bitmap.createScaledBitmap(frame, reqWidth, reqHeight, true);
                if (scaled != frame) {
                    frame.recycle();
                    frame = scaled;
                }
            }
            return frame;
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }

    public static void loadVideoThumbnail(@NonNull Context context,
                                          @NonNull Uri videoUri,
                                          @NonNull ImageView ivThumbnail,
                                          @Nullable ImageView ivPlaceholder) {
        loadStagingThumbnail(context, videoUri, null, ivThumbnail, ivPlaceholder != null ? ivPlaceholder : ivThumbnail);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    public static void remove(@Nullable Long fileId) {
        if (fileId != null) {
            memoryCache.remove(fileId);
        }
    }

    public static void clearCache() {
        memoryCache.evictAll();
        uriMemoryCache.evictAll();
    }
}
