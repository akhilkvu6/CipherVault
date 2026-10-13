package com.ciphervault.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.view.Gravity;
import android.widget.FrameLayout;
import java.io.FileOutputStream;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CameraActivity for encrypted in-app captures.
 *
 * Implements:
 * - 3:4 / 4:3 and 9:16 aspect ratio selection with letterboxed preview container.
 * - Sensor capture aspect ratio enforcement with center crop.
 * - Real hardware flash & torch check via CameraInfo.hasFlashUnit().
 * - Real torch mode toggle via CameraControl.enableTorch().
 * - Photo capture pipeline routing DIRECTLY to staging (never auto-upload).
 * - Full audit event logging.
 */
public class CameraActivity extends BaseActivity {

    private View previewContainer;
    private PreviewView viewFinder;
    private ImageView ivCapturedPreview;
    private View layoutCaptureControls;
    private View layoutPostCaptureControls;
    private ImageButton btnFlashToggle;
    private com.google.android.material.button.MaterialButtonToggleGroup toggleGroupRatio;
    private int currentAspectRatio = AspectRatio.RATIO_4_3;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTorchOn = false;
    private boolean hasFlash = false;

    private File currentCapturedFile = null;
    private ExecutorService cameraExecutor;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to capture photos", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        cameraExecutor = Executors.newSingleThreadExecutor();

        previewContainer = findViewById(R.id.previewContainer);
        viewFinder = findViewById(R.id.viewFinder);
        ivCapturedPreview = findViewById(R.id.ivCapturedPreview);
        layoutCaptureControls = findViewById(R.id.layoutCaptureControls);
        layoutPostCaptureControls = findViewById(R.id.layoutPostCaptureControls);
        btnFlashToggle = findViewById(R.id.btnFlashToggle);
        toggleGroupRatio = findViewById(R.id.toggleGroupRatio);

        updatePreviewDimensions();

        if (toggleGroupRatio != null) {
            toggleGroupRatio.check(R.id.btnRatio34);
            toggleGroupRatio.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                if (checkedId == R.id.btnRatio34) {
                    currentAspectRatio = AspectRatio.RATIO_4_3;
                } else if (checkedId == R.id.btnRatio169) {
                    currentAspectRatio = AspectRatio.RATIO_16_9;
                }
                updatePreviewDimensions();
                bindCameraUseCases();
            });
        }

        findViewById(R.id.btnCloseCamera).setOnClickListener(v -> finish());
        findViewById(R.id.btnSwitchCamera).setOnClickListener(v -> switchCameraLens());
        btnFlashToggle.setOnClickListener(v -> toggleFlashMode());

        findViewById(R.id.btnCapturePhoto).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btnRetake).setOnClickListener(v -> retakePhoto());
        findViewById(R.id.btnKeepPhoto).setOnClickListener(v -> keepAndReturnPhoto());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void updatePreviewDimensions() {
        if (previewContainer == null) return;
        previewContainer.post(() -> {
            View root = (View) previewContainer.getParent();
            if (root == null) return;
            int rootW = root.getWidth();
            int rootH = root.getHeight();
            if (rootW <= 0 || rootH <= 0) return;

            int targetW;
            int targetH;

            if (currentAspectRatio == AspectRatio.RATIO_4_3) {
                targetW = rootW;
                targetH = (int) Math.round((rootW * 4.0) / 3.0);
                if (targetH > rootH) {
                    targetH = rootH;
                    targetW = (int) Math.round((rootH * 3.0) / 4.0);
                }
            } else {
                targetW = rootW;
                targetH = (int) Math.round((rootW * 16.0) / 9.0);
                if (targetH > rootH) {
                    targetH = rootH;
                    targetW = (int) Math.round((rootH * 9.0) / 16.0);
                }
            }

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(targetW, targetH);
            lp.gravity = Gravity.CENTER;
            previewContainer.setLayoutParams(lp);
            viewFinder.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        updatePreviewDimensions();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        // Use selected aspect ratio (3:4 or 9:16) for both cameras
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(currentAspectRatio)
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(currentAspectRatio)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(isTorchOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            // Check if current camera lens has flash capability
            hasFlash = camera.getCameraInfo().hasFlashUnit();
            if (!hasFlash) {
                btnFlashToggle.setEnabled(false);
                btnFlashToggle.setAlpha(0.35f);
                btnFlashToggle.setContentDescription("Flash unavailable on this lens");
                isTorchOn = false;
            } else {
                btnFlashToggle.setEnabled(true);
                btnFlashToggle.setAlpha(1.0f);
                updateFlashIcon();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Camera binding failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCameraLens() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        isTorchOn = false;
        bindCameraUseCases();
    }

    private void toggleFlashMode() {
        if (!hasFlash || camera == null) {
            Toast.makeText(this, "Flash unavailable on this camera", Toast.LENGTH_SHORT).show();
            return;
        }

        isTorchOn = !isTorchOn;
        try {
            camera.getCameraControl().enableTorch(isTorchOn);
            if (imageCapture != null) {
                imageCapture.setFlashMode(isTorchOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
            }
            updateFlashIcon();
            Toast.makeText(this, isTorchOn ? "Flash / Torch: ON" : "Flash / Torch: OFF", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to toggle flash: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFlashIcon() {
        if (btnFlashToggle == null) return;
        if (isTorchOn) {
            btnFlashToggle.setColorFilter(ContextCompat.getColor(this, R.color.cv_primary));
            btnFlashToggle.setContentDescription("Flash ON");
        } else {
            btnFlashToggle.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
            btnFlashToggle.setContentDescription("Flash OFF");
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File photoFile = new File(getCacheDir(), "vault_capture_" + timeStamp + ".jpg");
        currentCapturedFile = photoFile;

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                cropCapturedImageToSelectedRatio(photoFile);
                AuditLogger.log(CameraActivity.this, "Camera Capture", "SUCCESS", "Captured " + photoFile.getName());
                showPhotoPreview(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                AuditLogger.log(CameraActivity.this, "Camera Capture", "FAILED", exception.getMessage());
                Toast.makeText(CameraActivity.this, "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cropCapturedImageToSelectedRatio(File file) {
        if (file == null || !file.exists()) return;
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return;

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try {
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            } catch (Exception ignored) {}

            int effectiveW = opts.outWidth;
            int effectiveH = opts.outHeight;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
                    || orientation == ExifInterface.ORIENTATION_TRANSPOSE || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
                effectiveW = opts.outHeight;
                effectiveH = opts.outWidth;
            }

            float currentRatio = (float) effectiveW / (float) effectiveH;
            float targetRatio = (currentAspectRatio == AspectRatio.RATIO_4_3) ? (3.0f / 4.0f) : (9.0f / 16.0f);

            if (Math.abs(currentRatio - targetRatio) < 0.02f) {
                return;
            }

            Bitmap raw = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (raw == null) return;
            Bitmap oriented = ThumbnailLoader.applyExifOrientation(raw, orientation);
            if (oriented == null) return;

            int bmpW = oriented.getWidth();
            int bmpH = oriented.getHeight();

            int cropW;
            int cropH;
            int startX;
            int startY;

            if ((float) bmpW / (float) bmpH > targetRatio) {
                cropH = bmpH;
                cropW = Math.round(bmpH * targetRatio);
                startX = Math.max(0, (bmpW - cropW) / 2);
                startY = 0;
            } else {
                cropW = bmpW;
                cropH = Math.round(bmpW / targetRatio);
                startX = 0;
                startY = Math.max(0, (bmpH - cropH) / 2);
            }

            cropW = Math.min(cropW, bmpW - startX);
            cropH = Math.min(cropH, bmpH - startY);

            Bitmap cropped = Bitmap.createBitmap(oriented, startX, startY, cropW, cropH);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                cropped.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }

            if (cropped != oriented) cropped.recycle();
            if (oriented != raw) oriented.recycle();
            raw.recycle();

            try {
                ExifInterface newExif = new ExifInterface(file.getAbsolutePath());
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_NORMAL));
                newExif.saveAttributes();
            } catch (Exception ignored) {}

        } catch (Throwable ignored) {}
    }

    private void showPhotoPreview(File file) {
        ivCapturedPreview.setImageURI(Uri.fromFile(file));
        ivCapturedPreview.setVisibility(View.VISIBLE);
        layoutCaptureControls.setVisibility(View.GONE);
        layoutPostCaptureControls.setVisibility(View.VISIBLE);
    }

    private void retakePhoto() {
        if (currentCapturedFile != null && currentCapturedFile.exists()) {
            currentCapturedFile.delete();
            currentCapturedFile = null;
        }
        ivCapturedPreview.setImageDrawable(null);
        ivCapturedPreview.setVisibility(View.GONE);
        layoutCaptureControls.setVisibility(View.VISIBLE);
        layoutPostCaptureControls.setVisibility(View.GONE);
    }

    private void keepAndReturnPhoto() {
        if (currentCapturedFile == null || !currentCapturedFile.exists()) {
            finish();
            return;
        }

        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", currentCapturedFile);
        AuditLogger.log(this, "File Staged", "SUCCESS", "Staged captured photo: " + currentCapturedFile.getName());

        if (getCallingActivity() != null) {
            Intent resultIntent = new Intent();
            resultIntent.setData(contentUri);
            resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Opened directly (e.g. from Home quick camera) -> Route directly into Staging
            Intent stagingIntent = new Intent(this, UploadStagingActivity.class);
            stagingIntent.setData(contentUri);
            stagingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(stagingIntent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null && isTorchOn) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (Exception ignored) {}
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
