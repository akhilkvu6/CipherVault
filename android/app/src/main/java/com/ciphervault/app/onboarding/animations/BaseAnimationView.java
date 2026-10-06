package com.ciphervault.app.onboarding.animations;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ciphervault.app.R;

/**
 * Base custom animation view providing common styling, reduced-motion detection,
 * and lifecycle management for smooth 60fps animations.
 */
public abstract class BaseAnimationView extends View {

    protected Paint brandPaint;
    protected Paint brandFillPaint;
    protected Paint surfacePaint;
    protected Paint borderPaint;
    protected Paint textPaint;
    protected Paint secondaryTextPaint;
    protected Paint accentSuccessPaint;

    protected int colorPrimary;
    protected int colorSurface;
    protected int colorSurfaceVariant;
    protected int colorBorder;
    protected int colorTextPrimary;
    protected int colorTextSecondary;
    protected int colorSuccess;

    private boolean isReducedMotion = false;
    private boolean isAttached = false;

    public BaseAnimationView(Context context) {
        super(context);
        init();
    }

    public BaseAnimationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        resolveColors();
        checkReducedMotion();
        initPaints();
        setupAnimators();
    }

    protected void resolveColors() {
        Context context = getContext();
        colorPrimary = ContextCompat.getColor(context, R.color.cv_primary);
        colorSurface = ContextCompat.getColor(context, R.color.cv_surface);
        colorSurfaceVariant = ContextCompat.getColor(context, R.color.cv_surface_variant);
        colorBorder = ContextCompat.getColor(context, R.color.cv_border);
        colorTextPrimary = ContextCompat.getColor(context, R.color.cv_text_primary);
        colorTextSecondary = ContextCompat.getColor(context, R.color.cv_text_secondary);
        colorSuccess = ContextCompat.getColor(context, R.color.cv_security_success);
    }

    protected void initPaints() {
        float density = getResources().getDisplayMetrics().density;

        brandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brandPaint.setColor(colorPrimary);
        brandPaint.setStyle(Paint.Style.STROKE);
        brandPaint.setStrokeWidth(2.5f * density);

        brandFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        brandFillPaint.setColor(colorPrimary);
        brandFillPaint.setStyle(Paint.Style.FILL);

        surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        surfacePaint.setColor(colorSurface);
        surfacePaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(colorBorder);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * density);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(colorTextPrimary);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(13f * getResources().getDisplayMetrics().scaledDensity);

        secondaryTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        secondaryTextPaint.setColor(colorTextSecondary);
        secondaryTextPaint.setTextAlign(Paint.Align.CENTER);
        secondaryTextPaint.setTextSize(11f * getResources().getDisplayMetrics().scaledDensity);

        accentSuccessPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        accentSuccessPaint.setColor(colorSuccess);
        accentSuccessPaint.setStyle(Paint.Style.FILL);
    }

    private void checkReducedMotion() {
        try {
            float durationScale = Settings.Global.getFloat(
                    getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            isReducedMotion = (durationScale == 0f);
        } catch (Exception e) {
            isReducedMotion = false;
        }
    }

    public boolean isReducedMotion() {
        return isReducedMotion;
    }

    protected abstract void setupAnimators();
    public abstract void startAnimation();
    public abstract void stopAnimation();

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttached = true;
        resolveColors();
        initPaints();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttached = false;
        stopAnimation();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && isAttached) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    protected float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
