package com.ciphervault.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FileViewerActivity extends BaseActivity {

    public static final String EXTRA_FILE_ID = "extra_file_id";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    public static final String EXTRA_CONTENT_TYPE = "extra_content_type";
    public static final String EXTRA_FILE_SIZE = "extra_file_size";

    private Long fileId;
    private String fileName;
    private String contentType;
    private Long fileSize;

    private View layoutDecrypting;
    private ImageView ivViewerImage;
    private PlayerView playerView;
    private View scrollViewerText;
    private TextView tvViewerTextContent;
    private View layoutViewerGeneric;
    private TextView tvGenericFileName;
    private TextView tvGenericFileSize;
    private Button btnOpenExternal;

    private ExoPlayer exoPlayer;
    private File tempDecryptedFile;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security: Prevent screenshots and screen recording of decrypted vault contents
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_file_viewer);

        fileId = getIntent().getLongExtra(EXTRA_FILE_ID, -1L);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        contentType = getIntent().getStringExtra(EXTRA_CONTENT_TYPE);
        fileSize = getIntent().getLongExtra(EXTRA_FILE_SIZE, 0L);

        if (fileName == null) fileName = "Decrypted File";
        if (contentType == null) contentType = "application/octet-stream";

        MaterialToolbar toolbar = findViewById(R.id.toolbarViewer);
        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> finish());

        layoutDecrypting = findViewById(R.id.layoutDecrypting);
        ivViewerImage = findViewById(R.id.ivViewerImage);
        playerView = findViewById(R.id.playerView);
        scrollViewerText = findViewById(R.id.scrollViewerText);
        tvViewerTextContent = findViewById(R.id.tvViewerTextContent);
        layoutViewerGeneric = findViewById(R.id.layoutViewerGeneric);
        tvGenericFileName = findViewById(R.id.tvGenericFileName);
        tvGenericFileSize = findViewById(R.id.tvGenericFileSize);
        btnOpenExternal = findViewById(R.id.btnOpenExternal);
        btnOpenExternal.setOnClickListener(v -> openExternalFile());

        if (fileId != null && fileId > 0) {
            startStreamingDecryption();
        } else {
            Toast.makeText(this, "Invalid file ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startStreamingDecryption() {
        ApiService apiService = ApiClient.getApiService(this);
        apiService.downloadFile(fileId, true).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveStreamAndDisplay(response.body());
                } else {
                    Toast.makeText(FileViewerActivity.this, "Decryption failed (HTTP " + response.code() + ")", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(FileViewerActivity.this, "Decryption error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void saveStreamAndDisplay(ResponseBody body) {
        diskExecutor.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "decrypted_vault");
                if (!dir.exists()) dir.mkdirs();

                File target = new File(dir, "temp_" + System.currentTimeMillis() + "_" + fileName);
                tempDecryptedFile = target;

                try (InputStream is = body.byteStream();
                     OutputStream os = new FileOutputStream(target)) {
                    byte[] buffer = new byte[16384]; // 16 KB chunked streaming
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                }

                runOnUiThread(() -> displayDecryptedContent(target));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(FileViewerActivity.this, "Error saving decrypted file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayDecryptedContent(File file) {
        if (isFinishing() || isDestroyed()) return;

        layoutDecrypting.setVisibility(View.GONE);

        String mime = contentType.toLowerCase();
        String name = fileName.toLowerCase();

        if (mime.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp")) {
            // Display Image
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                ivViewerImage.setImageBitmap(bitmap);
                ivViewerImage.setVisibility(View.VISIBLE);
                return;
            }
        }

        if (mime.startsWith("video/") || mime.startsWith("audio/")
                || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".mp3")
                || name.endsWith(".wav") || name.endsWith(".aac") || name.endsWith(".m4a")) {
            // Play Media via Media3 ExoPlayer
            playerView.setVisibility(View.VISIBLE);
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);

            MediaItem mediaItem = MediaItem.fromUri(Uri.fromFile(file));
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
            return;
        }

        if (mime.startsWith("text/") || name.endsWith(".txt") || name.endsWith(".json")
                || name.endsWith(".xml") || name.endsWith(".csv") || name.endsWith(".md") || name.endsWith(".log")) {
            // Display Text
            try {
                StringBuilder text = new StringBuilder();
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                int lineCount = 0;
                while ((line = br.readLine()) != null && lineCount < 2000) {
                    text.append(line).append("\n");
                    lineCount++;
                }
                br.close();
                tvViewerTextContent.setText(text.toString());
                scrollViewerText.setVisibility(View.VISIBLE);
                return;
            } catch (Exception ignored) {}
        }

        // Generic File View
        layoutViewerGeneric.setVisibility(View.VISIBLE);
        tvGenericFileName.setText(fileName);
        tvGenericFileSize.setText(FileUtils.formatStorageSize(this, file.length()) + " • " + contentType);
    }

    private void openExternalFile() {
        if (tempDecryptedFile == null || !tempDecryptedFile.exists()) return;

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempDecryptedFile);
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(uri, contentType != null ? contentType : "*/*");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(viewIntent);
        } catch (Exception e) {
            Toast.makeText(this, "No application found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        // Security requirement: Wipe decrypted file on exit
        if (tempDecryptedFile != null && tempDecryptedFile.exists()) {
            tempDecryptedFile.delete();
            tempDecryptedFile = null;
        }
        diskExecutor.shutdown();
    }
}
