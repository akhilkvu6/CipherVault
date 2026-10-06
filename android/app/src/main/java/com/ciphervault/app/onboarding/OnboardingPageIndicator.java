package com.ciphervault.app.onboarding;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ciphervault.app.R;

/**
 * Animated segmented/dot page indicator matching Samsung One UI design style.
 * Displays smoothly expanding pill for the active page index.
 */
public class OnboardingPageIndicator extends View {

    private int pageCount = 5;
    private int currentPage = 0;
    private float animatedPosition = 0f;

    private int activeColor;
    private int inactiveColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dotRect = new RectF();

    private ValueAnimator positionAnimator;

    public OnboardingPageIndicator(Context context) {
        super(context);
        init();
    }

    public OnboardingPageIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OnboardingPageIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activeColor = ContextCompat.getColor(getContext(), R.color.cv_indicator_active);
        inactiveColor = ContextCompat.getColor(getContext(), R.color.cv_indicator_inactive);
    }

    public void setPageCount(int count) {
        this.pageCount = count;
        invalidate();
    }

    public void setCurrentPage(int page, boolean animate) {
        if (page < 0 || page >= pageCount) return;
        this.currentPage = page;

        if (positionAnimator != null && positionAnimator.isRunning()) {
            positionAnimator.cancel();
        }

        if (animate) {
            positionAnimator = ValueAnimator.ofFloat(animatedPosition, page);
            positionAnimator.setDuration(240);
            positionAnimator.setInterpolator(new DecelerateInterpolator());
            positionAnimator.addUpdateListener(anim -> {
                animatedPosition = (float) anim.getAnimatedValue();
                invalidate();
            });
            positionAnimator.start();
        } else {
            animatedPosition = page;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        float dotH = 6f * density;
        float dotW = 6f * density;
        float activeW = 24f * density;
        float spacing = 6f * density;

        float totalWidth = activeW + (pageCount - 1) * dotW + (pageCount - 1) * spacing + (16f * density);
        float totalHeight = dotH + (12f * density);

        setMeasuredDimension(
                resolveSize((int) totalWidth, widthMeasureSpec),
                resolveSize((int) totalHeight, heightMeasureSpec)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pageCount <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float dotH = 6f * density;
        float dotW = 6f * density;
        float activeW = 24f * density;
        float spacing = 6f * density;

        float totalWidth = activeW + (pageCount - 1) * dotW + (pageCount - 1) * spacing;
        float startX = (getWidth() - totalWidth) / 2f;
        float cy = getHeight() / 2f;

        float currentX = startX;

        for (int i = 0; i < pageCount; i++) {
            // Calculate width based on proximity to animatedPosition
            float distance = Math.abs(animatedPosition - i);
            float factor = Math.max(0f, 1f - distance);
            float w = dotW + (activeW - dotW) * factor;

            dotRect.set(currentX, cy - dotH / 2f, currentX + w, cy + dotH / 2f);

            // Interpolate color
            if (factor > 0.05f) {
                paint.setColor(activeColor);
            } else {
                paint.setColor(inactiveColor);
            }

            canvas.drawRoundRect(dotRect, dotH / 2f, dotH / 2f, paint);
            currentX += w + spacing;
        }
    }
}
