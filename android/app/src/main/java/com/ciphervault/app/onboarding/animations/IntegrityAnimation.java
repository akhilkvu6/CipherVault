package com.ciphervault.app.onboarding.animations;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Screen 3 Animation: File Integrity & Duplicate Detection
 * Visual Flow:
 * File A ──→ SHA-256 ──→ fingerprint
 *                          │
 * File B ──→ SHA-256 ──→ fingerprint
 *                          │
 *                          ↓
 *                       Duplicate (Matched)
 */
public class IntegrityAnimation extends BaseAnimationView {

    private ValueAnimator stepAnimator;
    private float animProgress = 0f;

    private final RectF fileARect = new RectF();
    private final RectF fileBRect = new RectF();
    private final RectF hasherRect = new RectF();
    private final RectF matchPillRect = new RectF();

    public IntegrityAnimation(Context context) {
        super(context);
    }

    public IntegrityAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IntegrityAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupAnimators() {
        if (isReducedMotion()) {
            animProgress = 1f;
            return;
        }

        stepAnimator = ValueAnimator.ofFloat(0f, 1f);
        stepAnimator.setDuration(3800);
        stepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        stepAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        stepAnimator.addUpdateListener(anim -> {
            animProgress = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void startAnimation() {
        if (isReducedMotion()) {
            animProgress = 1f;
            invalidate();
            return;
        }
        if (stepAnimator != null && !stepAnimator.isRunning()) {
            stepAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (stepAnimator != null && stepAnimator.isRunning()) {
            stepAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float topRowY = height * 0.32f;
        float bottomRowY = height * 0.60f;
        float fileX = width * 0.18f;
        float hasherX = width * 0.52f;
        float fpX = width * 0.82f;

        float fileW = dpToPx(48);
        float fileH = dpToPx(42);

        // Connecting lines File -> Hash
        brandPaint.setStyle(Paint.Style.STROKE);
        brandPaint.setStrokeWidth(dpToPx(1.5f));
        brandPaint.setAlpha(70);
        canvas.drawLine(fileX + fileW / 2f, topRowY, hasherX - dpToPx(38), topRowY, brandPaint);
        canvas.drawLine(fileX + fileW / 2f, bottomRowY, hasherX - dpToPx(38), bottomRowY, brandPaint);

        // Connecting lines Hash -> Fingerprint
        canvas.drawLine(hasherX + dpToPx(38), topRowY, fpX - dpToPx(34), topRowY, brandPaint);
        canvas.drawLine(hasherX + dpToPx(38), bottomRowY, fpX - dpToPx(34), bottomRowY, brandPaint);
        brandPaint.setAlpha(255);

        // Draw File A (Top)
        fileARect.set(fileX - fileW / 2f, topRowY - fileH / 2f, fileX + fileW / 2f, topRowY + fileH / 2f);
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(fileARect, dpToPx(8), dpToPx(8), surfacePaint);
        borderPaint.setColor(colorBorder);
        canvas.drawRoundRect(fileARect, dpToPx(8), dpToPx(8), borderPaint);
        canvas.drawText("FILE A", fileX, topRowY + dpToPx(4), textPaint);

        // Draw File B (Bottom)
        fileBRect.set(fileX - fileW / 2f, bottomRowY - fileH / 2f, fileX + fileW / 2f, bottomRowY + fileH / 2f);
        canvas.drawRoundRect(fileBRect, dpToPx(8), dpToPx(8), surfacePaint);
        canvas.drawRoundRect(fileBRect, dpToPx(8), dpToPx(8), borderPaint);
        canvas.drawText("FILE B", fileX, bottomRowY + dpToPx(4), textPaint);

        // Central SHA-256 Engine
        float hasherH = dpToPx(80);
        float hasherW = dpToPx(76);
        float centerY = (topRowY + bottomRowY) / 2f;
        hasherRect.set(hasherX - hasherW / 2f, centerY - hasherH / 2f,
                hasherX + hasherW / 2f, centerY + hasherH / 2f);

        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawRoundRect(hasherRect, dpToPx(14), dpToPx(14), surfacePaint);
        borderPaint.setColor(colorPrimary);
        borderPaint.setStrokeWidth(dpToPx(2f));
        canvas.drawRoundRect(hasherRect, dpToPx(14), dpToPx(14), borderPaint);

        canvas.drawText("SHA-256", hasherX, centerY - dpToPx(4), textPaint);
        canvas.drawText("HASH", hasherX, centerY + dpToPx(12), secondaryTextPaint);

        // Fingerprint blocks on the right
        float fpW = dpToPx(66);
        float fpH = dpToPx(28);

        // Fingerprint A
        RectF fpARect = new RectF(fpX - fpW / 2f, topRowY - fpH / 2f, fpX + fpW / 2f, topRowY + fpH / 2f);
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(fpARect, dpToPx(6), dpToPx(6), surfacePaint);
        borderPaint.setColor(colorPrimary);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        canvas.drawRoundRect(fpARect, dpToPx(6), dpToPx(6), borderPaint);
        canvas.drawText("# e3b0c44...", fpX, topRowY + dpToPx(4), secondaryTextPaint);

        // Fingerprint B
        RectF fpBRect = new RectF(fpX - fpW / 2f, bottomRowY - fpH / 2f, fpX + fpW / 2f, bottomRowY + fpH / 2f);
        canvas.drawRoundRect(fpBRect, dpToPx(6), dpToPx(6), surfacePaint);
        canvas.drawRoundRect(fpBRect, dpToPx(6), dpToPx(6), borderPaint);
        canvas.drawText("# e3b0c44...", fpX, bottomRowY + dpToPx(4), secondaryTextPaint);

        // Duplicate match connector & badge
        // Animated reveal in the latter half of cycle
        float matchAlpha = isReducedMotion() ? 1f : Math.max(0f, Math.min(1f, (animProgress - 0.45f) / 0.35f));
        if (matchAlpha > 0f) {
            brandPaint.setColor(colorSuccess);
            brandPaint.setStrokeWidth(dpToPx(2f));
            brandPaint.setAlpha((int) (matchAlpha * 255));
            canvas.drawLine(fpX, topRowY + fpH / 2f + dpToPx(4), fpX, bottomRowY - fpH / 2f - dpToPx(4), brandPaint);
            brandPaint.setAlpha(255);

            float pillW = dpToPx(94);
            float pillH = dpToPx(24);
            matchPillRect.set(fpX - pillW / 2f, centerY - pillH / 2f, fpX + pillW / 2f, centerY + pillH / 2f);

            surfacePaint.setColor(colorSurfaceVariant);
            surfacePaint.setAlpha((int) (matchAlpha * 255));
            canvas.drawRoundRect(matchPillRect, dpToPx(12), dpToPx(12), surfacePaint);
            surfacePaint.setAlpha(255);

            accentSuccessPaint.setColor(colorSuccess);
            accentSuccessPaint.setTextAlign(Paint.Align.CENTER);
            accentSuccessPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);
            accentSuccessPaint.setAlpha((int) (matchAlpha * 255));
            canvas.drawText("DUPLICATE MATCH", fpX, centerY + dpToPx(4), accentSuccessPaint);
            accentSuccessPaint.setAlpha(255);
        }
    }
}
