package com.ciphervault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.media.ExifInterface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    private static File getThumbnailDir(Context context) {
        File dir = new File(context.getCacheDir(), "thumbnails");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    public static File getDiskThumbnailFile(Context context, @Nullable Long fileId, @Nullable String sha256) {
        if (context == null) return null;
        File dir = getThumbnailDir(context);
        if (fileId != null) {
            File byId = new File(dir, "thumb_" + fileId + ".jpg");
            if (byId.exists() && byId.length() > 0) return byId;
        }
        if (sha256 != null && !sha256.trim().isEmpty()) {
            File byHash = new File(dir, "thumb_sha_" + sha256.trim() + ".jpg");
            if (byHash.exists() && byHash.length() > 0) return byHash;
        }
        return null;
    }

    public static boolean hasCachedThumbnail(Context context, @Nullable Long fileId, @Nullable String sha256) {
        if (fileId != null && memoryCache.get(fileId) != null) {
            return true;
        }
        File diskFile = getDiskThumbnailFile(context, fileId, sha256);
        return diskFile != null && diskFile.exists() && diskFile.length() > 0;
    }

    public static void saveThumbnailToDisk(Context context, Bitmap bitmap, @Nullable Long fileId, @Nullable String sha256) {
        if (context == null || bitmap == null) return;
        decodeExecutor.execute(() -> {
            try {
                File dir = getThumbnailDir(context);
                if (fileId != null) {
                    File file = new File(dir, "thumb_" + fileId + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    }
                }
                if (sha256 != null && !sha256.trim().isEmpty()) {
                    File file = new File(dir, "thumb_sha_" + sha256.trim() + ".jpg");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    /**
     * Persists a staged thumbnail into the local persistent cache under the newly assigned fileId and sha256.
     */
    public static void associateStagingThumbnail(@NonNull Context context,
                                                @NonNull Uri uri,
                                                @NonNull Long fileId,
                                                @Nullable String sha256) {
        String uriKey = uri.toString();
        Bitmap cached = uriMemoryCache.get(uriKey);
        if (cached != null) {
            memoryCache.put(fileId, cached);
            saveThumbnailToDisk(context, cached, fileId, sha256);
            return;
        }

        decodeExecutor.execute(() -> {
            Bitmap bmp = null;
            String mime = null;
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                try {
                    mime = context.getContentResolver().getType(uri);
                } catch (Exception ignored) {}
            }
            String path = uri.getPath();
            String name = (path != null) ? path.toLowerCase(Locale.US) : "";

            if ((mime != null && mime.startsWith("video/")) || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".avi")) {
                bmp = extractVideoFrame(context, uri, 384, 384);
            } else if ((mime != null && mime.startsWith("image/")) || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".webp") || name.endsWith(".heic")) {
                bmp = decodeSampledBitmapFromUri(context, uri, 384, 384);
            } else if ("application/pdf".equalsIgnoreCase(mime) || name.endsWith(".pdf")) {
                bmp = renderPdfFirstPage(context, uri, 384, 384);
            } else if ((mime != null && mime.startsWith("audio/")) || name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".m4a") || name.endsWith(".flac")) {
                bmp = extractAudioAlbumArt(context, uri, 384, 384);
            }

            if (bmp != null) {
                final Bitmap finalBmp = bmp;
                mainHandler.post(() -> {
                    uriMemoryCache.put(uriKey, finalBmp);
                    memoryCache.put(fileId, finalBmp);
                });
                saveThumbnailToDisk(context, bmp, fileId, sha256);
            }
        });
    }

    /**
     * Corrects image orientation according to all 8 standard EXIF orientation states.
     */
    public static Bitmap applyExifOrientation(@Nullable Bitmap bitmap, int orientation) {
        if (bitmap == null) return null;
        Matrix matrix = new Matrix();
        boolean needsTransform = false;

        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: // 2
                matrix.postScale(-1f, 1f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180: // 3
                matrix.postRotate(180f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: // 4
                matrix.postScale(1f, -1f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE: // 5
                matrix.postRotate(90f);
                matrix.postScale(-1f, 1f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90: // 6
                matrix.postRotate(90f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE: // 7
                matrix.postRotate(270f);
                matrix.postScale(-1f, 1f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270: // 8
                matrix.postRotate(270f);
                needsTransform = true;
                break;
            case ExifInterface.ORIENTATION_NORMAL: // 1
            default:
                break;
        }

        if (!needsTransform) {
            return bitmap;
        }

        try {
            Bitmap transformed = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true
            );
            if (transformed != bitmap) {
                bitmap.recycle();
            }
            return transformed;
        } catch (Throwable t) {
            return bitmap;
        }
    }

    /**
     * Loads a thumbnail for an already-uploaded remote file in Vault / Recent files.
     */
    public static void loadThumbnail(@NonNull Context context,
                                     @NonNull Long fileId,
                                     @NonNull ImageView ivThumbnail,
                                     @Nullable ImageView ivPlaceholder,
                                     @Nullable ImageView ivVideoBadge) {
        loadThumbnail(context, fileId, null, true, ivThumbnail, ivPlaceholder, ivVideoBadge);
    }

    public static void loadThumbnail(@NonNull Context context,
                                     @NonNull Long fileId,
                                     @Nullable String sha256,
                                     boolean hasServerPreview,
                                     @NonNull ImageView ivThumbnail,
                                     @Nullable ImageView ivPlaceholder,
                                     @Nullable ImageView ivVideoBadge) {
        ivThumbnail.setTag(fileId);

        // 1. Check in-memory cache
        Bitmap cached = memoryCache.get(fileId);
        if (cached != null) {
            ivThumbnail.setImageBitmap(cached);
            ivThumbnail.setVisibility(View.VISIBLE);
            if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.GONE);
            if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.VISIBLE);
            return;
        }

        // 2. Check persistent disk cache
        File diskFile = getDiskThumbnailFile(context, fileId, sha256);
        if (diskFile != null && diskFile.exists() && diskFile.length() > 0) {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
            if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);

            decodeExecutor.execute(() -> {
                Bitmap bmp = BitmapFactory.decodeFile(diskFile.getAbsolutePath());
                if (bmp != null) {
                    mainHandler.post(() -> {
                        Object currentTag = ivThumbnail.getTag();
                        if (currentTag != null && currentTag.equals(fileId)) {
                            memoryCache.put(fileId, bmp);
                            ivThumbnail.setImageBitmap(bmp);
                            ivThumbnail.setVisibility(View.VISIBLE);
                            if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.GONE);
                            if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
            return;
        }

        // 3. Check server preview
        ivThumbnail.setImageDrawable(null);
        ivThumbnail.setVisibility(View.GONE);
        if (ivPlaceholder != null) ivPlaceholder.setVisibility(View.VISIBLE);
        if (ivVideoBadge != null) ivVideoBadge.setVisibility(View.GONE);

        if (!hasServerPreview) {
            return;
        }

        ApiService apiService = ApiClient.getApiService(context);
        apiService.getFilePreview(fileId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    final ResponseBody body = response.body();
                    decodeExecutor.execute(() -> {
                        Bitmap bitmap = null;
                        try (InputStream is = body.byteStream()) {
                            byte[] bytes = is.readAllBytes();
                            Bitmap raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            int orient = ExifInterface.ORIENTATION_NORMAL;
                            try {
                                ExifInterface exif = new ExifInterface(new ByteArrayInputStream(bytes));
                                orient = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            } catch (Exception ignored) {}
                            bitmap = applyExifOrientation(raw, orient);
                        } catch (Exception ignored) {}

                        final Bitmap finalBitmap = bitmap;
                        if (finalBitmap != null) {
                            saveThumbnailToDisk(context, finalBitmap, fileId, sha256);
                        }
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
     * Loads a thumbnail for a staging file (local URI) with OOM-safe subsampling,
     * PDF first-page rendering, audio album art extraction, and file-type specific icons.
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
            Bitmap cached = uriMemoryCache.get(uriKey);
            if (cached != null) {
                ivThumbnail.setImageBitmap(cached);
                ivThumbnail.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
                return;
            }

            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_file_text);
            ivIcon.setVisibility(View.VISIBLE);

            decodeExecutor.execute(() -> {
                Bitmap page = renderPdfFirstPage(context, uri, 180, 180);
                mainHandler.post(() -> {
                    if (uriKey.equals(ivThumbnail.getTag())) {
                        if (page != null) {
                            uriMemoryCache.put(uriKey, page);
                            ivThumbnail.setImageBitmap(page);
                            ivThumbnail.setVisibility(View.VISIBLE);
                            ivIcon.setVisibility(View.GONE);
                        } else {
                            ivThumbnail.setVisibility(View.GONE);
                            ivIcon.setImageResource(R.drawable.ic_lucide_file_text);
                            ivIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
            });
        } else if (isAudio) {
            Bitmap cached = uriMemoryCache.get(uriKey);
            if (cached != null) {
                ivThumbnail.setImageBitmap(cached);
                ivThumbnail.setVisibility(View.VISIBLE);
                ivIcon.setVisibility(View.GONE);
                return;
            }

            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_music);
            ivIcon.setVisibility(View.VISIBLE);

            decodeExecutor.execute(() -> {
                Bitmap art = extractAudioAlbumArt(context, uri, 180, 180);
                mainHandler.post(() -> {
                    if (uriKey.equals(ivThumbnail.getTag())) {
                        if (art != null) {
                            uriMemoryCache.put(uriKey, art);
                            ivThumbnail.setImageBitmap(art);
                            ivThumbnail.setVisibility(View.VISIBLE);
                            ivIcon.setVisibility(View.GONE);
                        } else {
                            ivThumbnail.setVisibility(View.GONE);
                            ivIcon.setImageResource(R.drawable.ic_lucide_music);
                            ivIcon.setVisibility(View.VISIBLE);
                        }
                    }
                });
            });
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
        } else {
            ivThumbnail.setImageDrawable(null);
            ivThumbnail.setVisibility(View.GONE);
            ivIcon.setImageResource(R.drawable.ic_lucide_file);
            ivIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Decodes a memory-efficient subsampled Bitmap from a Uri and corrects EXIF rotation across all 8 states.
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

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is != null) {
                    ExifInterface exif = new ExifInterface(is);
                    orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);
                }
            } catch (Exception ignored) {}

            return applyExifOrientation(bitmap, orientation);
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

    /**
     * Renders first page of a PDF file using Android's native PdfRenderer.
     */
    @Nullable
    public static Bitmap renderPdfFirstPage(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (context == null || uri == null) return null;
        try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
            if (pfd == null) return null;
            try (PdfRenderer renderer = new PdfRenderer(pfd)) {
                if (renderer.getPageCount() <= 0) return null;
                try (PdfRenderer.Page page = renderer.openPage(0)) {
                    int w = page.getWidth();
                    int h = page.getHeight();
                    if (w <= 0 || h <= 0) return null;
                    float scale = Math.min((float) reqWidth / w, (float) reqHeight / h);
                    if (scale <= 0) scale = 1.0f;
                    int targetW = Math.max(1, (int) (w * scale));
                    int targetH = Math.max(1, (int) (h * scale));
                    Bitmap bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    return bitmap;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Extracts embedded album art from an audio file using MediaMetadataRetriever.
     */
    @Nullable
    public static Bitmap extractAudioAlbumArt(Context context, Uri uri, int reqWidth, int reqHeight) {
        if (context == null || uri == null) return null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null && art.length > 0) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(art, 0, art.length, options);
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                return BitmapFactory.decodeByteArray(art, 0, art.length, options);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }
        return null;
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
