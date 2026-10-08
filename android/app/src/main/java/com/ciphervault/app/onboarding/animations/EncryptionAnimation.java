package com.ciphervault.app.onboarding.animations;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Screen 2 Animation: Encrypted Storage
 * Visual Flow:
 * File -> Encryption (AES-256-GCM) -> Encrypted data blocks -> Private storage
 * - File document appears
 * - Moves toward AES-256 cipher chamber
 * - Transforms into encrypted blocks
 * - Securely deposited into vault storage
 */
public class EncryptionAnimation extends BaseAnimationView {

    private ValueAnimator loopAnimator;
    private float flowProgress = 0f;
    private final RectF fileRect = new RectF();
    private final RectF chamberRect = new RectF();
    private final RectF vaultRect = new RectF();
    private final RectF blockRect = new RectF();
    private final Path arrowPath = new Path();

    public EncryptionAnimation(Context context) {
        super(context);
    }

    public EncryptionAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EncryptionAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupAnimators() {
        if (isReducedMotion()) {
            flowProgress = 0.5f;
            return;
        }

        loopAnimator = ValueAnimator.ofFloat(0f, 1f);
        loopAnimator.setDuration(4000);
        loopAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loopAnimator.setInterpolator(new LinearInterpolator());
        loopAnimator.addUpdateListener(animation -> {
            flowProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void startAnimation() {
        if (isReducedMotion()) {
            flowProgress = 0.5f;
            invalidate();
            return;
        }
        if (loopAnimator != null && !loopAnimator.isRunning()) {
            loopAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (loopAnimator != null && loopAnimator.isRunning()) {
            loopAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cy = height * 0.45f;
        float nodeWidth = dpToPx(56);
        float nodeHeight = dpToPx(68);

        // Three nodes along horizontal axis:
        // Left: Plain File
        // Center: AES-256 Cipher Lock
        // Right: Private Vault Box
        float leftX = width * 0.18f;
        float centerX = width * 0.50f;
        float rightX = width * 0.82f;

        // Draw connecting dotted line between left and center, center and right
        brandPaint.setStyle(Paint.Style.STROKE);
        brandPaint.setStrokeWidth(dpToPx(1.5f));
        brandPaint.setAlpha(80);
        canvas.drawLine(leftX + nodeWidth / 2f + dpToPx(4), cy,
                centerX - nodeWidth / 2f - dpToPx(4), cy, brandPaint);
        canvas.drawLine(centerX + nodeWidth / 2f + dpToPx(4), cy,
                rightX - nodeWidth / 2f - dpToPx(4), cy, brandPaint);
        brandPaint.setAlpha(255);

        // 1. Plain File Node (Left)
        fileRect.set(leftX - nodeWidth / 2f, cy - nodeHeight / 2f,
                leftX + nodeWidth / 2f, cy + nodeHeight / 2f);
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(fileRect, dpToPx(10), dpToPx(10), surfacePaint);
        borderPaint.setColor(colorBorder);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        canvas.drawRoundRect(fileRect, dpToPx(10), dpToPx(10), borderPaint);

        // File inner document lines
        borderPaint.setColor(colorBorder);
        float lineX1 = fileRect.left + dpToPx(10);
        float lineX2 = fileRect.right - dpToPx(10);
        canvas.drawLine(lineX1, fileRect.top + dpToPx(18), lineX2, fileRect.top + dpToPx(18), borderPaint);
        canvas.drawLine(lineX1, fileRect.top + dpToPx(28), lineX2, fileRect.top + dpToPx(28), borderPaint);
        canvas.drawLine(lineX1, fileRect.top + dpToPx(38), lineX1 + dpToPx(16), fileRect.top + dpToPx(38), borderPaint);

        canvas.drawText("FILE", leftX, fileRect.bottom + dpToPx(18), secondaryTextPaint);

        // 2. Encryption Engine / Lock Node (Center)
        chamberRect.set(centerX - nodeWidth / 2f, cy - nodeHeight / 2f,
                centerX + nodeWidth / 2f, cy + nodeHeight / 2f);
        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawRoundRect(chamberRect, dpToPx(14), dpToPx(14), surfacePaint);
        borderPaint.setColor(colorPrimary);
        borderPaint.setStrokeWidth(dpToPx(2f));
        canvas.drawRoundRect(chamberRect, dpToPx(14), dpToPx(14), borderPaint);

        // Lock icon in center
        float lockW = dpToPx(22);
        float lockH = dpToPx(18);
        RectF lockBody = new RectF(centerX - lockW / 2f, cy - dpToPx(3), centerX + lockW / 2f, cy + lockH - dpToPx(3));
        brandFillPaint.setColor(colorPrimary);
        canvas.drawRoundRect(lockBody, dpToPx(4), dpToPx(4), brandFillPaint);

        // Lock shackle
        RectF shackle = new RectF(centerX - dpToPx(8), cy - dpToPx(15), centerX + dpToPx(8), cy - dpToPx(1));
        brandPaint.setStyle(Paint.Style.STROKE);
        brandPaint.setStrokeWidth(dpToPx(2.5f));
        canvas.drawArc(shackle, 180, 180, false, brandPaint);

        canvas.drawText("AES-256", centerX, chamberRect.bottom + dpToPx(18), textPaint);

        // 3. Vault Destination Node (Right)
        vaultRect.set(rightX - nodeWidth / 2f, cy - nodeHeight / 2f,
                rightX + nodeWidth / 2f, cy + nodeHeight / 2f);
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(vaultRect, dpToPx(12), dpToPx(12), surfacePaint);
        borderPaint.setColor(colorBorder);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        canvas.drawRoundRect(vaultRect, dpToPx(12), dpToPx(12), borderPaint);

        // Shield in Vault
        accentSuccessPaint.setColor(colorSuccess);
        canvas.drawCircle(rightX, cy, dpToPx(12), accentSuccessPaint);
        surfacePaint.setColor(colorSurface);
        canvas.drawCircle(rightX, cy, dpToPx(6), surfacePaint);

        canvas.drawText("VAULT", rightX, vaultRect.bottom + dpToPx(18), secondaryTextPaint);

        // 4. Moving Particles / Blocks
        // In the first half (0 -> 0.5), a plain packet moves Left -> Center
        // In the second half (0.5 -> 1.0), an encrypted cipher block moves Center -> Right
        if (!isReducedMotion()) {
            if (flowProgress < 0.5f) {
                float seg = flowProgress / 0.5f;
                float px = leftX + seg * (centerX - leftX);
                brandFillPaint.setColor(colorTextSecondary);
                canvas.drawCircle(px, cy, dpToPx(4.5f), brandFillPaint);
            } else {
                float seg = (flowProgress - 0.5f) / 0.5f;
                float px = centerX + seg * (rightX - centerX);
                blockRect.set(px - dpToPx(6), cy - dpToPx(6), px + dpToPx(6), cy + dpToPx(6));
                brandFillPaint.setColor(colorPrimary);
                canvas.drawRoundRect(blockRect, dpToPx(2), dpToPx(2), brandFillPaint);
            }
        }
    }
}
