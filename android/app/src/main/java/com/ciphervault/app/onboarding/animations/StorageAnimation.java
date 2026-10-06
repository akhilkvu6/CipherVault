package com.ciphervault.app.onboarding.animations;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

/**
 * Screen 5 Animation: Your Private Vault
 * Show:
 * CipherVault
 * 1 GB
 * Private Storage
 *
 * Animation:
 * - Vault card appears
 * - Storage visualization fill bar animates smoothly
 * - Security indicator / badge illuminates
 */
public class StorageAnimation extends BaseAnimationView {

    private ValueAnimator fillAnimator;
    private float fillFraction = 0f;

    private final RectF mainVaultCardRect = new RectF();
    private final RectF progressTrackRect = new RectF();
    private final RectF progressFillRect = new RectF();
    private final RectF securityBadgeRect = new RectF();

    public StorageAnimation(Context context) {
        super(context);
    }

    public StorageAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StorageAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupAnimators() {
        if (isReducedMotion()) {
            fillFraction = 0.65f;
            return;
        }

        fillAnimator = ValueAnimator.ofFloat(0f, 0.65f);
        fillAnimator.setDuration(2200);
        fillAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
        fillAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fillAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fillAnimator.addUpdateListener(anim -> {
            fillFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void startAnimation() {
        if (isReducedMotion()) {
            fillFraction = 0.65f;
            invalidate();
            return;
        }
        if (fillAnimator != null && !fillAnimator.isRunning()) {
            fillAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (fillAnimator != null && fillAnimator.isRunning()) {
            fillAnimator.cancel();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float cx = width / 2f;
        float cy = height * 0.46f;

        // Main Vault Card Container (Samsung Grouped Surface)
        float cardWidth = Math.min(width * 0.86f, dpToPx(280));
        float cardHeight = dpToPx(150);
        mainVaultCardRect.set(cx - cardWidth / 2f, cy - cardHeight / 2f,
                cx + cardWidth / 2f, cy + cardHeight / 2f);

        // Card Surface
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(mainVaultCardRect, dpToPx(18), dpToPx(18), surfacePaint);
        borderPaint.setColor(colorBorder);
        borderPaint.setStrokeWidth(dpToPx(1.5f));
        canvas.drawRoundRect(mainVaultCardRect, dpToPx(18), dpToPx(18), borderPaint);

        // Card Header Title
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(14f * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText("CipherVault", mainVaultCardRect.left + dpToPx(20), mainVaultCardRect.top + dpToPx(32), textPaint);

        // Large 1 GB Capacity text
        textPaint.setTextSize(26f * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText("1 GB", mainVaultCardRect.left + dpToPx(20), mainVaultCardRect.top + dpToPx(66), textPaint);

        // Capacity Subtitle
        secondaryTextPaint.setTextAlign(Paint.Align.LEFT);
        secondaryTextPaint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText("Private Storage Capacity", mainVaultCardRect.left + dpToPx(20), mainVaultCardRect.top + dpToPx(84), secondaryTextPaint);

        // Storage Visualization Bar
        float barX = mainVaultCardRect.left + dpToPx(20);
        float barW = cardWidth - dpToPx(40);
        float barY = mainVaultCardRect.top + dpToPx(104);
        float barH = dpToPx(8);

        progressTrackRect.set(barX, barY, barX + barW, barY + barH);
        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawRoundRect(progressTrackRect, dpToPx(4), dpToPx(4), surfacePaint);

        // Animated progress fill
        float currentFillW = Math.max(dpToPx(8), barW * fillFraction);
        progressFillRect.set(barX, barY, barX + currentFillW, barY + barH);
        brandFillPaint.setColor(colorPrimary);
        canvas.drawRoundRect(progressFillRect, dpToPx(4), dpToPx(4), brandFillPaint);

        // Below card: Security status pill badge
        float badgeW = dpToPx(190);
        float badgeH = dpToPx(32);
        float badgeY = mainVaultCardRect.bottom + dpToPx(18);
        securityBadgeRect.set(cx - badgeW / 2f, badgeY, cx + badgeW / 2f, badgeY + badgeH);

        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawRoundRect(securityBadgeRect, dpToPx(16), dpToPx(16), surfacePaint);

        // Tiny Shield green dot
        accentSuccessPaint.setColor(colorSuccess);
        canvas.drawCircle(securityBadgeRect.left + dpToPx(16), securityBadgeRect.centerY(), dpToPx(4), accentSuccessPaint);

        // Status text
        secondaryTextPaint.setTextAlign(Paint.Align.LEFT);
        secondaryTextPaint.setTextSize(12f * getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText("Private vault secured", securityBadgeRect.left + dpToPx(28),
                securityBadgeRect.centerY() + dpToPx(4), secondaryTextPaint);

        // Reset text aligns
        textPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
    }
}
