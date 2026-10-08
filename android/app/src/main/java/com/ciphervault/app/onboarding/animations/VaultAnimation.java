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
 * Screen 1 Animation:
 * - Vault / safe appears
 * - Lock shackle closes smoothly
 * - Subtle encrypted data particles gently move toward the vault
 * - CipherVault emblem pulse
 */
public class VaultAnimation extends BaseAnimationView {

    private ValueAnimator mainAnimator;
    private float animationProgress = 0f;
    private final RectF vaultBodyRect = new RectF();
    private final RectF shackleRect = new RectF();
    private List<Particle> particles;

    private static class Particle {
        float angle;
        float distance;
        float size;
        float speed;
        float alpha;
    }

    public VaultAnimation(Context context) {
        super(context);
    }

    public VaultAnimation(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VaultAnimation(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void setupAnimators() {
        // Prepare 12 subtle orbit/inflow data particles
        if (particles == null) {
            particles = new ArrayList<>();
        }
        particles.clear();
        for (int i = 0; i < 12; i++) {
            Particle p = new Particle();
            p.angle = (float) (i * (2 * Math.PI / 12));
            p.distance = 0.5f + (i % 3) * 0.2f; // fraction of orbit
            p.size = dpToPx(3.5f + (i % 3));
            p.speed = 0.8f + (i % 4) * 0.15f;
            p.alpha = 0.4f + (i % 3) * 0.25f;
            particles.add(p);
        }

        if (isReducedMotion()) {
            animationProgress = 1f;
            return;
        }

        mainAnimator = ValueAnimator.ofFloat(0f, 1f);
        mainAnimator.setDuration(3600);
        mainAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mainAnimator.setRepeatMode(ValueAnimator.RESTART);
        mainAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mainAnimator.addUpdateListener(animator -> {
            animationProgress = (float) animator.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    public void startAnimation() {
        if (isReducedMotion()) {
            animationProgress = 1f;
            invalidate();
            return;
        }
        if (mainAnimator != null && !mainAnimator.isRunning()) {
            mainAnimator.start();
        }
    }

    @Override
    public void stopAnimation() {
        if (mainAnimator != null && mainAnimator.isRunning()) {
            mainAnimator.cancel();
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

        // Draw inward floating data particle dots
        float maxRadius = Math.min(width, height) * 0.42f;
        float minRadius = dpToPx(52);

        for (Particle p : particles) {
            float cycle = (animationProgress * p.speed + p.distance) % 1.0f;
            float currentDist = maxRadius - cycle * (maxRadius - minRadius);
            float px = cx + (float) Math.cos(p.angle + animationProgress * 0.5f) * currentDist;
            float py = cy + (float) Math.sin(p.angle + animationProgress * 0.5f) * currentDist;

            int alpha = (int) (255 * p.alpha * (1f - Math.abs(cycle - 0.5f) * 1.5f));
            if (alpha < 0) alpha = 0;
            if (alpha > 255) alpha = 255;

            brandFillPaint.setAlpha(alpha);
            canvas.drawCircle(px, py, p.size / 2f, brandFillPaint);
        }
        brandFillPaint.setAlpha(255);

        // Outer subtle safe border ring
        float ringRadius = dpToPx(56);
        brandPaint.setAlpha(40);
        canvas.drawCircle(cx, cy, ringRadius + dpToPx(6), brandPaint);
        brandPaint.setAlpha(255);

        // Vault Safe Body: Rounded Rect
        float boxWidth = dpToPx(76);
        float boxHeight = dpToPx(66);
        vaultBodyRect.set(cx - boxWidth / 2f, cy - boxHeight / 2f + dpToPx(10),
                cx + boxWidth / 2f, cy + boxHeight / 2f + dpToPx(10));

        // Background of vault
        surfacePaint.setColor(colorSurface);
        canvas.drawRoundRect(vaultBodyRect, dpToPx(14), dpToPx(14), surfacePaint);
        borderPaint.setColor(colorPrimary);
        borderPaint.setStrokeWidth(dpToPx(2.5f));
        canvas.drawRoundRect(vaultBodyRect, dpToPx(14), dpToPx(14), borderPaint);

        // Lock Shackle (top arch)
        float shackleWidth = dpToPx(38);
        float shackleHeight = dpToPx(36);
        // Lock close effect: shackle moves down into the body
        float shackleOffset = isReducedMotion() ? 0 : (float) Math.sin(animationProgress * Math.PI) * dpToPx(4);
        float shackleTop = vaultBodyRect.top - shackleHeight + dpToPx(8) + shackleOffset;
        shackleRect.set(cx - shackleWidth / 2f, shackleTop, cx + shackleWidth / 2f, shackleTop + shackleHeight);

        brandPaint.setStyle(Paint.Style.STROKE);
        brandPaint.setStrokeWidth(dpToPx(3.5f));
        canvas.drawArc(shackleRect, 180, 180, false, brandPaint);

        // Center Vault Dial / Keyhole emblem
        float dialRadius = dpToPx(14);
        surfacePaint.setColor(colorSurfaceVariant);
        canvas.drawCircle(cx, vaultBodyRect.centerY(), dialRadius, surfacePaint);

        // Inner keyhole / core lock
        brandFillPaint.setColor(colorPrimary);
        canvas.drawCircle(cx, vaultBodyRect.centerY() - dpToPx(2), dpToPx(3.5f), brandFillPaint);
        canvas.drawRect(cx - dpToPx(2), vaultBodyRect.centerY() - dpToPx(1),
                cx + dpToPx(2), vaultBodyRect.centerY() + dpToPx(6), brandFillPaint);

        // Dial tick marks
        brandPaint.setStrokeWidth(dpToPx(1.5f));
        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI / 2);
            float tx1 = cx + (float) Math.cos(angle) * (dialRadius - dpToPx(3));
            float ty1 = vaultBodyRect.centerY() + (float) Math.sin(angle) * (dialRadius - dpToPx(3));
            float tx2 = cx + (float) Math.cos(angle) * dialRadius;
            float ty2 = vaultBodyRect.centerY() + (float) Math.sin(angle) * dialRadius;
            canvas.drawLine(tx1, ty1, tx2, ty2, brandPaint);
        }
    }
}
