package com.ciphervault.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PillNavHelper {

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    public static void setup(Activity activity, OnTabSelectedListener listener) {
        LinearLayout tabHome = activity.findViewById(R.id.tabHome);
        LinearLayout tabFiles = activity.findViewById(R.id.tabFiles);
        LinearLayout tabUpload = activity.findViewById(R.id.tabUpload);
        LinearLayout tabSettings = activity.findViewById(R.id.tabSettings);

        if (tabHome == null || tabFiles == null || tabUpload == null || tabSettings == null) {
            return;
        }

        LinearLayout[] tabs = {tabHome, tabFiles, tabUpload, tabSettings};

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> {
                ViewGroup root = (ViewGroup) tabs[index].getRootView();
                AutoTransition transition = new AutoTransition();
                transition.setDuration(220L);
                TransitionManager.beginDelayedTransition(root, transition);

                updateActiveTab(activity, tabs, index);
                if (listener != null) {
                    listener.onTabSelected(index);
                }
            });
        }

        // Default to Home tab
        updateActiveTab(activity, tabs, 0);
    }

    public static void selectTab(Activity activity, int index) {
        LinearLayout tabHome = activity.findViewById(R.id.tabHome);
        LinearLayout tabFiles = activity.findViewById(R.id.tabFiles);
        LinearLayout tabUpload = activity.findViewById(R.id.tabUpload);
        LinearLayout tabSettings = activity.findViewById(R.id.tabSettings);

        if (tabHome == null || tabFiles == null || tabUpload == null || tabSettings == null) {
            return;
        }

        LinearLayout[] tabs = {tabHome, tabFiles, tabUpload, tabSettings};
        if (index >= 0 && index < tabs.length) {
            ViewGroup root = (ViewGroup) tabs[index].getRootView();
            AutoTransition transition = new AutoTransition();
            transition.setDuration(220L);
            TransitionManager.beginDelayedTransition(root, transition);

            updateActiveTab(activity, tabs, index);
        }
    }

    private static void updateActiveTab(Context context, LinearLayout[] tabs, int activeIndex) {
        int activeIconColor = resolveColorAttr(context, com.google.android.material.R.attr.colorOnPrimaryContainer);
        int inactiveIconColor = resolveColorAttr(context, com.google.android.material.R.attr.colorOnSurfaceVariant);

        for (int i = 0; i < tabs.length; i++) {
            LinearLayout tab = tabs[i];
            ImageView icon = (ImageView) tab.getChildAt(0);
            TextView text = (TextView) tab.getChildAt(1);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tab.getLayoutParams();

            if (i == activeIndex) {
                tab.setBackgroundResource(R.drawable.bg_pill_active);
                params.weight = 1.6f;
                if (icon != null) {
                    icon.setImageTintList(ColorStateList.valueOf(activeIconColor));
                }
                if (text != null) {
                    text.setVisibility(View.VISIBLE);
                }
            } else {
                tab.setBackground(null);
                params.weight = 1.0f;
                if (icon != null) {
                    icon.setImageTintList(ColorStateList.valueOf(inactiveIconColor));
                }
                if (text != null) {
                    text.setVisibility(View.GONE);
                }
            }
            tab.setLayoutParams(params);
        }
    }

    private static int resolveColorAttr(Context context, int attrRes) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }
}