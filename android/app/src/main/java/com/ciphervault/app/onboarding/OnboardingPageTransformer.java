package com.ciphervault.app.onboarding;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * Subtle One UI page transformer with restrained fade and scale
 * to create a calm, fluid transition without aggressive sliding or bounce.
 */
public class OnboardingPageTransformer implements ViewPager2.PageTransformer {

    private static final float MIN_SCALE = 0.94f;
    private static final float MIN_ALPHA = 0.3f;

    @Override
    public void transformPage(@NonNull View page, float position) {
        int pageWidth = page.getWidth();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            page.setAlpha(0f);
        } else if (position <= 1) { // [-1,1]
            // Counteract default slide slightly for a more layered feel
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position) * 0.06f);
            float alphaFactor = Math.max(MIN_ALPHA, 1 - Math.abs(position) * 0.7f);

            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
            page.setAlpha(alphaFactor);

            // Subtle parallax on inner visual container if present
            View visualContainer = page.findViewById(com.ciphervault.app.R.id.visualContainer);
            if (visualContainer != null) {
                visualContainer.setTranslationX(-position * (pageWidth * 0.15f));
            }
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            page.setAlpha(0f);
        }
    }
}
