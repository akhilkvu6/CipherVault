package com.ciphervault.app.onboarding.animations;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 4 Animation: Everything in One Place
 * Visual objects:
 * - Photos
 * - Videos
 * - Documents
 * - Audio
 * - Other files
 * Files gently hover in orbit and move gracefully toward CipherVault central hub.
 * Calm, professional, and elegant motion.
 */
public class FilesAnimation extends BaseAnimationView {

    private ValueAnimator floatAnimator;
    private float floatProgress = 0f;

    private static class FileTypeItem {
        final String label;
        final float baseAngle; // radians
        final float radiusFraction;

        FileTypeItem(String label, float baseAngle, float radiusFraction) {
            this.label = label;
            this.baseAngle = baseAngle;
            this.radiusFraction = radiusFraction;
        }
    }

    private List<FileTypeItem> fileTypes;
    private final RectF itemRect = new RectF();
    private final RectF centerHubRect = new RectF();

    public FilesAnimation(Context context) {
        super(context);
    }

    public FilesAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FilesAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupAnimators() {
        if (fileTypes == null) {
            fileTypes = new ArrayList<>();
        }
        fileTypes.clear();
        // 5 distinct category types arranged around center
        fileTypes.add(new FileTypeItem("Photos", (float) (-Math.PI / 2), 0.76f));
        fileTypes.add(new FileTypeItem("Videos", (float) (-Math.PI / 6), 0.78f));
        fileTypes.add(new FileTypeItem("Documents", (float) (Math.PI / 4), 0.75f));
        fileTypes.add(new FileTypeItem("Audio", (float) (3 * Math.PI / 4), 0.75f));
        fileTypes.add(new FileTypeItem("Other files", (float) (7 * Math.PI / 6), 0.78f));

        if (isReducedMotion()) {
            floatProgress = 0f;
            return;
        }

        floatAnimator = ValueAnimator.ofFloat(0f, 1f);
        floatAnimator.setDuration(4500);
        floatAnimator.setRepeatCount(ValueAnimator.INFINITE);
        floatAnimator.setRepeatMode(ValueAnimator.REVERSE);
        floatAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnimator.addUpdateListener(anim -> {
            floatProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void startAnimation() {
        if (isReducedMotion()) {
            floatProgress = 0f;
            invalidate();
            return;
        }
        if (floatAnimator != null && !floatAnimator.isRunning()) {
            floatAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (floatAnimator != null && floatAnimator.isRunning()) {
            floatAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float baseRadius = Math.min(width, height) * 0.38f;

        // Draw Central Hub: CipherVault Unified Storage
        float hubW = dpToPx(90);
        float hubH = dpToPx(42);
        centerHubRect.set(cx - hubW / 2f, cy - hubH / 2f, cx + hubW / 2f, cy + hubH / 2f);

        // Center hub background
        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawRoundRect(centerHubRect, dpToPx(14), dpToPx(14), surfacePaint);
        borderPaint.setColor(colorPrimary);
        borderPaint.setStrokeWidth(dpToPx(2f));
        canvas.drawRoundRect(centerHubRect, dpToPx(14), dpToPx(14), borderPaint);

        canvas.drawText("CipherVault", cx, cy + dpToPx(4), textPaint);

        // Draw orbiting file categories
        float itemW = dpToPx(74);
        float itemH = dpToPx(30);

        for (int i = 0; i < fileTypes.size(); i++) {
            FileTypeItem item = fileTypes.get(i);

            // Subtle gentle floating motion
            float hover = isReducedMotion() ? 0 : (float) Math.sin(floatProgress * Math.PI * 2 + i) * dpToPx(6);
            float currentRadius = baseRadius * item.radiusFraction + hover;

            float x = cx + (float) Math.cos(item.baseAngle) * currentRadius;
            float y = cy + (float) Math.sin(item.baseAngle) * currentRadius;

            // Faint connector line toward hub
            brandPaint.setStyle(Paint.Style.STROKE);
            brandPaint.setStrokeWidth(dpToPx(1.2f));
            brandPaint.setAlpha(60);
            canvas.drawLine(x, y, cx, cy, brandPaint);
            brandPaint.setAlpha(255);

            // Pill item card
            itemRect.set(x - itemW / 2f, y - itemH / 2f, x + itemW / 2f, y + itemH / 2f);
            surfacePaint.setColor(colorSurface);
            canvas.drawRoundRect(itemRect, dpToPx(10), dpToPx(10), surfacePaint);

            borderPaint.setColor(colorBorder);
            borderPaint.setStrokeWidth(dpToPx(1.2f));
            canvas.drawRoundRect(itemRect, dpToPx(10), dpToPx(10), borderPaint);

            canvas.drawText(item.label, x, y + dpToPx(4), secondaryTextPaint);
        }
    }
}
