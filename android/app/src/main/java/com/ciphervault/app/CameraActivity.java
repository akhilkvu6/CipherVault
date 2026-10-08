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
 * - 3:4 / 4:3 sensor capture aspect ratio.
 * - Real hardware flash & torch check via CameraInfo.hasFlashUnit().
 * - Real torch mode toggle via CameraControl.enableTorch().
 * - Photo capture pipeline routing DIRECTLY to staging (never auto-upload).
 * - Full audit event logging.
 */
public class CameraActivity extends BaseActivity {

    private PreviewView viewFinder;
    private ImageView ivCapturedPreview;
    private View layoutCaptureControls;
    private View layoutPostCaptureControls;
    private ImageButton btnFlashToggle;

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

        viewFinder = findViewById(R.id.viewFinder);
        ivCapturedPreview = findViewById(R.id.ivCapturedPreview);
        layoutCaptureControls = findViewById(R.id.layoutCaptureControls);
        layoutPostCaptureControls = findViewById(R.id.layoutPostCaptureControls);
        btnFlashToggle = findViewById(R.id.btnFlashToggle);

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

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        // Use 4:3 / 3:4 aspect ratio centered for high-fidelity photo capture
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
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
