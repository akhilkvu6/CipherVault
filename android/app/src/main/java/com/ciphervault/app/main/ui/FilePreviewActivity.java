package com.ciphervault.app.main.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ciphervault.app.R;
import com.ciphervault.app.core.network.ApiClient;
import com.ciphervault.app.main.api.FileApi;
import com.ciphervault.app.main.model.FileResponse;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FilePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_FILE_ID = "file_id";
    public static final String EXTRA_FILE_NAME = "file_name";
    public static final String EXTRA_FILE_MIME = "file_mime";

    private ImageView ivPreview;
    private VideoView vvPreview;
    private View llAudioPreview;
    private TextView tvAudioFilename;
    private TextView tvAudioStatus;
    private android.widget.SeekBar sbAudioProgress;
    private TextView tvAudioCurrentTime;
    private TextView tvAudioTotalTime;
    private android.widget.ImageButton btnAudioPlayPause;

    private View llPdfPreview;
    private ImageView ivPdfPage;
    private TextView tvPdfPageCount;
    private MaterialButton btnPdfPrev;
    private MaterialButton btnPdfNext;

    private View svTextPreview;
    private TextView tvTextContent;

    private View llArchivePreview;
    private TextView tvArchiveFileList;

    private View llDecryptionLoading;
    private View llError;
    private TextView tvErrorTitle;
    private TextView tvErrorDetail;
    private MaterialButton btnRetryPreview;
    private MaterialButton btnDownloadInstead;

    private long fileId = -1;
    private String fileName = "File";
    private String mimeType = "";

    private File secureTempCacheFile = null;
    private Call<ResponseBody> currentCall = null;

    private MediaPlayer mediaPlayer = null;
    private android.os.Handler audioHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable audioUpdateRunnable = null;

    private android.graphics.pdf.PdfRenderer pdfRenderer = null;
    private android.os.ParcelFileDescriptor pdfFileDescriptor = null;
    private int currentPdfPageIndex = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        androidx.activity.EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_preview);

        fileId = getIntent().getLongExtra(EXTRA_FILE_ID, -1);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        mimeType = getIntent().getStringExtra(EXTRA_FILE_MIME);
        if (fileName == null) fileName = "File";
        if (mimeType == null) mimeType = "application/octet-stream";

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(fileName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.filePreviewRoot), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            int extraSpace = (int) (8 * getResources().getDisplayMetrics().density);
            toolbar.setPadding(0, sb.top + extraSpace, 0, 0);
            return insets;
        });

        ivPreview = findViewById(R.id.ivPreview);
        vvPreview = findViewById(R.id.vvPreview);

        llAudioPreview = findViewById(R.id.llAudioPreview);
        tvAudioFilename = findViewById(R.id.tvAudioFilename);
        tvAudioStatus = findViewById(R.id.tvAudioStatus);
        sbAudioProgress = findViewById(R.id.sbAudioProgress);
        tvAudioCurrentTime = findViewById(R.id.tvAudioCurrentTime);
        tvAudioTotalTime = findViewById(R.id.tvAudioTotalTime);
        btnAudioPlayPause = findViewById(R.id.btnAudioPlayPause);

        llPdfPreview = findViewById(R.id.llPdfPreview);
        ivPdfPage = findViewById(R.id.ivPdfPage);
        tvPdfPageCount = findViewById(R.id.tvPdfPageCount);
        btnPdfPrev = findViewById(R.id.btnPdfPrev);
        btnPdfNext = findViewById(R.id.btnPdfNext);

        svTextPreview = findViewById(R.id.svTextPreview);
        tvTextContent = findViewById(R.id.tvTextContent);

        llArchivePreview = findViewById(R.id.llArchivePreview);
        tvArchiveFileList = findViewById(R.id.tvArchiveFileList);

        llDecryptionLoading = findViewById(R.id.llDecryptionLoading);
        llError = findViewById(R.id.llError);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorDetail = findViewById(R.id.tvErrorDetail);
        btnRetryPreview = findViewById(R.id.btnRetryPreview);
        btnDownloadInstead = findViewById(R.id.btnDownloadInstead);

        btnRetryPreview.setOnClickListener(v -> startDecryptionAndPreview());

        btnDownloadInstead.setOnClickListener(v -> {
            FileResponse dummy = new FileResponse();
            dummy.id = fileId;
            dummy.filename = fileName;
            dummy.contentType = mimeType;
            dummy.encrypted = true;
            FileDownloadManager.showDownloadDialog(this, dummy, null);
        });

        btnPdfPrev.setOnClickListener(v -> {
            if (pdfRenderer != null && currentPdfPageIndex > 0) {
                renderPdfPage(currentPdfPageIndex - 1);
            }
        });

        btnPdfNext.setOnClickListener(v -> {
            if (pdfRenderer != null && currentPdfPageIndex < pdfRenderer.getPageCount() - 1) {
                renderPdfPage(currentPdfPageIndex + 1);
            }
        });

        if (fileId != -1) {
            startDecryptionAndPreview();
        } else {
            showError("Invalid File", "Could not locate file in vault.", false);
        }
    }

    private void startDecryptionAndPreview() {
        releaseAudioResources();
        closePdfRenderer();
        hideAllViewers();
        llError.setVisibility(View.GONE);
        llDecryptionLoading.setVisibility(View.VISIBLE);

        FileApi api = ApiClient.getClient(this).create(FileApi.class);
        currentCall = api.downloadFile(fileId, true);
        currentCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isDestroyed() || isFinishing()) return;
                if (response.isSuccessful() && response.body() != null) {
                    processStream(response.body().byteStream());
                } else {
                    showError("Decryption Failed", "The server returned an error (" + response.code() + ").", true);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isDestroyed() || isFinishing()) return;
                if (!call.isCanceled()) {
                    showError("Network Error", t.getMessage() != null ? t.getMessage() : "Failed to download and decrypt file.", true);
                }
            }
        });
    }

    private void processStream(InputStream inputStream) {
        new Thread(() -> {
            try {
                String ext = "";
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx != -1) {
                    ext = fileName.substring(dotIdx);
                }
                File cacheFile = new File(getCacheDir(), "cv_preview_" + fileId + "_" + System.currentTimeMillis() + ext);
                secureTempCacheFile = cacheFile;

                try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                    byte[] buf = new byte[32768];
                    int r;
                    while ((r = inputStream.read(buf)) != -1) {
                        fos.write(buf, 0, r);
                    }
                    fos.flush();
                }

                String lowerMime = mimeType.toLowerCase(Locale.ROOT);
                String lowerName = fileName.toLowerCase(Locale.ROOT);

                if (lowerMime.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                        || lowerName.endsWith(".png") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif")) {
                    Bitmap bmp = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        if (bmp != null) {
                            ivPreview.setImageBitmap(bmp);
                            ivPreview.setVisibility(View.VISIBLE);
                        } else {
                            showError("Corrupted Image", "Failed to render decrypted image preview.", false);
                        }
                    });
                } else if (lowerMime.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov")) {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        vvPreview.setVisibility(View.VISIBLE);
                        vvPreview.setVideoPath(cacheFile.getAbsolutePath());
                        MediaController mc = new MediaController(this);
                        mc.setAnchorView(vvPreview);
                        vvPreview.setMediaController(mc);
                        vvPreview.start();
                    });
                } else if (lowerMime.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac")) {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        setupAudioPlayback(cacheFile);
                    });
                } else if (lowerMime.contains("pdf") || lowerName.endsWith(".pdf")) {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        setupPdfViewer(cacheFile);
                    });
                } else if (lowerMime.contains("zip") || lowerMime.contains("compressed") || lowerName.endsWith(".zip")) {
                    StringBuilder zipList = new StringBuilder();
                    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(cacheFile))) {
                        java.util.zip.ZipEntry ze;
                        int count = 0;
                        while ((ze = zis.getNextEntry()) != null && count < 500) {
                            double kb = ze.getSize() >= 0 ? ze.getSize() / 1024.0 : 0.0;
                            zipList.append(ze.isDirectory() ? "📁 " : "📄 ")
                                   .append(ze.getName())
                                   .append(ze.getSize() >= 0 ? String.format(Locale.US, " (%.1f KB)\n", kb) : "\n");
                            zis.closeEntry();
                            count++;
                        }
                    } catch (Exception e) {
                        zipList.append("Could not enumerate archive entries: ").append(e.getMessage());
                    }
                    final String displayZip = zipList.length() > 0 ? zipList.toString() : "Archive is empty or unreadable.";
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        tvArchiveFileList.setText(displayZip);
                        llArchivePreview.setVisibility(View.VISIBLE);
                    });
                } else if (lowerMime.startsWith("text/") || lowerMime.contains("json") || lowerMime.contains("xml") || lowerMime.contains("csv")
                        || lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".xml") || lowerName.endsWith(".csv")
                        || lowerName.endsWith(".md") || lowerName.endsWith(".log") || lowerName.endsWith(".py") || lowerName.endsWith(".java")
                        || lowerName.endsWith(".kt") || lowerName.endsWith(".js") || lowerName.endsWith(".html") || lowerName.endsWith(".css")) {
                    StringBuilder textBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)))) {
                        String line;
                        int lines = 0;
                        while ((line = reader.readLine()) != null && lines < 1500) {
                            textBuilder.append(line).append("\n");
                            lines++;
                        }
                    }
                    final String renderedText = textBuilder.toString();
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        llDecryptionLoading.setVisibility(View.GONE);
                        tvTextContent.setText(renderedText);
                        svTextPreview.setVisibility(View.VISIBLE);
                    });
                } else {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        showError("Preview Not Supported", "This file type (" + (mimeType.isEmpty() ? "Unknown" : mimeType) + ") does not support direct in-app display. You can download it securely below.", false);
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    showError("Decryption Failed", e.getMessage() != null ? e.getMessage() : "Error decrypting stream.", true);
                });
            }
        }).start();
    }

    private void setupAudioPlayback(File audioFile) {
        llAudioPreview.setVisibility(View.VISIBLE);
        tvAudioFilename.setText(fileName);
        tvAudioStatus.setText("Decrypted and ready");

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();

            int durationMs = mediaPlayer.getDuration();
            sbAudioProgress.setMax(durationMs);
            tvAudioTotalTime.setText(formatTime(durationMs));
            tvAudioCurrentTime.setText("00:00");

            btnAudioPlayPause.setImageResource(android.R.drawable.ic_media_play);
            btnAudioPlayPause.setOnClickListener(v -> {
                if (mediaPlayer == null) return;
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnAudioPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    tvAudioStatus.setText("Paused");
                } else {
                    mediaPlayer.start();
                    btnAudioPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    tvAudioStatus.setText("Playing");
                    startAudioProgressUpdates();
                }
            });

            sbAudioProgress.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                        tvAudioCurrentTime.setText(formatTime(progress));
                    }
                }
                @Override public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                btnAudioPlayPause.setImageResource(android.R.drawable.ic_media_play);
                tvAudioStatus.setText("Completed");
                sbAudioProgress.setProgress(0);
                tvAudioCurrentTime.setText("00:00");
            });

        } catch (Exception e) {
            showError("Audio Playback Error", "Could not initialize media player: " + e.getMessage(), true);
        }
    }

    private void startAudioProgressUpdates() {
        if (audioUpdateRunnable != null) audioHandler.removeCallbacks(audioUpdateRunnable);
        audioUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int pos = mediaPlayer.getCurrentPosition();
                    sbAudioProgress.setProgress(pos);
                    tvAudioCurrentTime.setText(formatTime(pos));
                    audioHandler.postDelayed(this, 500);
                }
            }
        };
        audioHandler.post(audioUpdateRunnable);
    }

    private void releaseAudioResources() {
        if (audioUpdateRunnable != null) {
            audioHandler.removeCallbacks(audioUpdateRunnable);
            audioUpdateRunnable = null;
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void setupPdfViewer(File pdfFile) {
        try {
            pdfFileDescriptor = android.os.ParcelFileDescriptor.open(pdfFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new android.graphics.pdf.PdfRenderer(pdfFileDescriptor);
            llPdfPreview.setVisibility(View.VISIBLE);
            renderPdfPage(0);
        } catch (Exception e) {
            showError("PDF Render Error", "Could not render PDF document: " + e.getMessage(), true);
        }
    }

    private void renderPdfPage(int pageIndex) {
        if (pdfRenderer == null || pageIndex < 0 || pageIndex >= pdfRenderer.getPageCount()) return;
        currentPdfPageIndex = pageIndex;
        tvPdfPageCount.setText(String.format(Locale.US, "Page %d of %d", currentPdfPageIndex + 1, pdfRenderer.getPageCount()));
        btnPdfPrev.setEnabled(currentPdfPageIndex > 0);
        btnPdfNext.setEnabled(currentPdfPageIndex < pdfRenderer.getPageCount() - 1);

        try (android.graphics.pdf.PdfRenderer.Page page = pdfRenderer.openPage(pageIndex)) {
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            ivPdfPage.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to render PDF page: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void closePdfRenderer() {
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (pdfFileDescriptor != null) {
                pdfFileDescriptor.close();
                pdfFileDescriptor = null;
            }
        } catch (Exception ignored) {}
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void hideAllViewers() {
        ivPreview.setVisibility(View.GONE);
        vvPreview.setVisibility(View.GONE);
        llAudioPreview.setVisibility(View.GONE);
        llPdfPreview.setVisibility(View.GONE);
        svTextPreview.setVisibility(View.GONE);
        llArchivePreview.setVisibility(View.GONE);
    }

    private void showError(String title, String detail, boolean canRetry) {
        hideAllViewers();
        llDecryptionLoading.setVisibility(View.GONE);
        tvErrorTitle.setText(title);
        tvErrorDetail.setText(detail);
        btnRetryPreview.setVisibility(canRetry ? View.VISIBLE : View.GONE);
        llError.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        releaseAudioResources();
        closePdfRenderer();
        super.onDestroy();
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
        if (secureTempCacheFile != null && secureTempCacheFile.exists()) {
            try {
                long len = secureTempCacheFile.length();
                if (len > 0 && len <= 10 * 1024 * 1024) {
                    try (FileOutputStream fos = new FileOutputStream(secureTempCacheFile)) {
                        byte[] zeros = new byte[8192];
                        long written = 0;
                        while (written < len) {
                            int toWrite = (int) Math.min(zeros.length, len - written);
                            fos.write(zeros, 0, toWrite);
                            written += toWrite;
                        }
                        fos.flush();
                    } catch (Exception ignored) {}
                }
                secureTempCacheFile.delete();
            } catch (Exception ignored) {}
        }
    }
}
