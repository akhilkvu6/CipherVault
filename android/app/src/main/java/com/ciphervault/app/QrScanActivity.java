package com.ciphervault.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dedicated QR Code Scanner Activity using CameraX and ZXing.
 * Detects CipherVault connection QR codes in real-time and returns the decoded string.
 */
public class QrScanActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_RESULT = "SCAN_RESULT";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private final AtomicBoolean isScanned = new AtomicBoolean(false);
    private final MultiFormatReader qrReader = new MultiFormatReader();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR code", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scan);

        previewView = findViewById(R.id.previewView);
        ImageButton btnClose = findViewById(R.id.btnClose);
        MaterialButton btnEnterManually = findViewById(R.id.btnEnterManually);

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnEnterManually.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Initialize ZXing reader with QR code hint
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        qrReader.setHints(hints);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeFrame(@NonNull ImageProxy imageProxy) {
        if (isScanned.get()) {
            imageProxy.close();
            return;
        }

        try {
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            int rotation = imageProxy.getImageInfo().getRotationDegrees();

            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                    bytes, width, height, 0, 0, width, height, false
            );
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = null;
            try {
                result = qrReader.decodeWithState(bitmap);
            } catch (NotFoundException e) {
                // If not found and frame is rotated (e.g. portrait sensor 90 deg), test rotated buffer
                if (rotation == 90 || rotation == 270) {
                    byte[] rotatedBytes = rotateYuv90(bytes, width, height);
                    PlanarYUVLuminanceSource rotatedSource = new PlanarYUVLuminanceSource(
                            rotatedBytes, height, width, 0, 0, height, width, false
                    );
                    BinaryBitmap rotatedBitmap = new BinaryBitmap(new HybridBinarizer(rotatedSource));
                    try {
                        result = qrReader.decodeWithState(rotatedBitmap);
                    } catch (NotFoundException ignored) {
                        // Not found in rotated frame either
                    }
                }
            } finally {
                qrReader.reset();
            }

            if (result != null && result.getText() != null) {
                String text = result.getText().trim();
                if (!text.isEmpty() && isScanned.compareAndSet(false, true)) {
                    runOnUiThread(() -> onQrCodeDetected(text));
                }
            }

        } catch (Exception ignored) {
            // Frame processing error
        } finally {
            imageProxy.close();
        }
    }

    private byte[] rotateYuv90(byte[] data, int width, int height) {
        byte[] rotated = new byte[width * height];
        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int y = height - 1; y >= 0; y--) {
                rotated[i] = data[y * width + x];
                i++;
            }
        }
        return rotated;
    }

    private void onQrCodeDetected(String rawContent) {
        Intent data = new Intent();
        data.putExtra(EXTRA_SCAN_RESULT, rawContent);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
