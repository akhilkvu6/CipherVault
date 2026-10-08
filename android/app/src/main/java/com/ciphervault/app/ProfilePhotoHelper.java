package com.ciphervault.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfilePhotoHelper {

    private static final String AVATARS_DIR = "user_avatars";
    private static final String PREF_PENDING_PHOTO = "pref_pending_avatar_upload_";

    private static File getAvatarDir(Context context) {
        File dir = new File(context.getFilesDir(), AVATARS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private static String getSafeEmailKey(String email) {
        if (email == null) return "default";
        return email.trim().toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static File getAvatarFile(Context context, String email) {
        return new File(getAvatarDir(context), "avatar_" + getSafeEmailKey(email) + ".jpg");
    }

    public static synchronized boolean saveProfilePhoto(Context context, String email, Bitmap bitmap) {
        if (bitmap == null) return false;
        try {
            File file = getAvatarFile(context, email);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized boolean saveProfilePhoto(Context context, String email, byte[] data) {
        if (data == null || data.length == 0) return false;
        try {
            File file = getAvatarFile(context, email);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static synchronized Bitmap getProfilePhoto(Context context, String email) {
        File file = getAvatarFile(context, email);
        if (file.exists() && file.length() > 0) {
            try {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static synchronized void deleteProfilePhoto(Context context, String email) {
        File file = getAvatarFile(context, email);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean savePendingSignupPhotoBytes(Context context, String email, byte[] bytes) {
        if (context == null || email == null || bytes == null || bytes.length == 0) return false;
        boolean saved = saveProfilePhoto(context, email, bytes);
        if (saved) {
            context.getSharedPreferences("ciphervault_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_PENDING_PHOTO + getSafeEmailKey(email), true)
                    .apply();
        }
        return saved;
    }

    public static void savePendingSignupPhoto(Context context, String email, Uri photoUri) {
        if (photoUri == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            try (InputStream is = context.getContentResolver().openInputStream(photoUri)) {
                if (is != null) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    byte[] bytes = buffer.toByteArray();
                    if (saveProfilePhoto(context, email, bytes)) {
                        context.getSharedPreferences("ciphervault_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(PREF_PENDING_PHOTO + getSafeEmailKey(email), true)
                                .apply();
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public static void uploadPendingPhotoIfPresent(Context context, String email, ApiService apiService) {
        if (email == null || apiService == null) return;
        boolean hasPending = context.getSharedPreferences("ciphervault_prefs", Context.MODE_PRIVATE)
                .getBoolean(PREF_PENDING_PHOTO + getSafeEmailKey(email), false);
        if (!hasPending) return;

        File file = getAvatarFile(context, email);
        if (!file.exists() || file.length() == 0) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = fis.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                }
                byte[] bytes = buffer.toByteArray();
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/jpeg"), bytes);
                MultipartBody.Part body = MultipartBody.Part.createFormData("photo", "profile_avatar.jpg", reqFile);

                apiService.uploadProfilePhoto(body).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            context.getSharedPreferences("ciphervault_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .remove(PREF_PENDING_PHOTO + getSafeEmailKey(email))
                                    .apply();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
                });
            } catch (Exception ignored) {}
        });
    }
}
