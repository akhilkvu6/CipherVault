package com.ciphervault.app.motion;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.navigation.NavOptions;

import com.ciphervault.app.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized motion controller for Navigation Component tab switching and animations (B2, B3, B4, B6).
 */
public class NavigationMotionManager {

    public static final int TAB_INDEX_HOME = 0;
    public static final int TAB_INDEX_FILES = 1;
    public static final int TAB_INDEX_ACTIVITY = 2;
    public static final int TAB_INDEX_PROFILE = 3;

    private static final Map<Integer, Integer> TAB_ORDER_MAP;

    static {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(R.id.nav_home, TAB_INDEX_HOME);
        map.put(R.id.nav_files, TAB_INDEX_FILES);
        map.put(R.id.nav_activity, TAB_INDEX_ACTIVITY);
        map.put(R.id.nav_profile, TAB_INDEX_PROFILE);
        TAB_ORDER_MAP = Collections.unmodifiableMap(map);
    }

    private int currentTabIndex = TAB_INDEX_HOME;

    public NavigationMotionManager() {
    }

    public static int getTabIndexForMenuItemId(int menuItemId) {
        Integer index = TAB_ORDER_MAP.get(menuItemId);
        return index != null ? index : -1;
    }

    public static boolean isTopLevelTab(int menuItemId) {
        return TAB_ORDER_MAP.containsKey(menuItemId);
    }

    public static Map<Integer, Integer> getTabOrderMap() {
        return TAB_ORDER_MAP;
    }

    /**
     * Determines whether the given destination ID is one of the four top-level roots:
     * Home, Files, Activity, or Profile.
     */
    public static boolean isTopLevelDestination(int destinationId) {
        return destinationId == R.id.dest_home
                || destinationId == R.id.dest_files
                || destinationId == R.id.dest_activity
                || destinationId == R.id.dest_profile;
    }

    /**
     * Determines whether Back on the given destination should exit the application (Correction 1).
     * All four top-level roots exit the application.
     */
    public static boolean shouldExitOnBack(int destinationId) {
        return isTopLevelDestination(destinationId);
    }

    /**
     * Computes the tab direction deterministically (B4):
     * - target > current: FORWARD (current -> LEFT, target enters from RIGHT)
     * - target < current: BACKWARD (current -> RIGHT, target enters from LEFT)
     * - target == current: SAME
     */
    public static TabDirection calculateDirection(int currentIndex, int targetIndex) {
        if (targetIndex > currentIndex) {
            return TabDirection.FORWARD;
        } else if (targetIndex < currentIndex) {
            return TabDirection.BACKWARD;
        } else {
            return TabDirection.SAME;
        }
    }

    public int getCurrentTabIndex() {
        return currentTabIndex;
    }

    public void setCurrentTabIndex(int index) {
        this.currentTabIndex = index;
    }

    /**
     * Creates NavOptions for tab transition respecting tab direction and reduced-motion preference (A13, B4, B6).
     */
    public NavOptions createTabNavOptions(
            @Nullable Context context,
            int currentGraphStartDestId,
            int targetTabIndex
    ) {
        TabDirection direction = calculateDirection(currentTabIndex, targetTabIndex);
        boolean reducedMotion = ReducedMotionHelper.isReducedMotionEnabled(context);

        NavOptions.Builder builder = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true);

        if (currentGraphStartDestId != 0) {
            builder.setPopUpTo(currentGraphStartDestId, false, true);
        }

        if (reducedMotion) {
            builder.setEnterAnim(R.animator.nav_fade_enter)
                    .setExitAnim(R.animator.nav_fade_exit);
        } else if (direction == TabDirection.FORWARD) {
            builder.setEnterAnim(R.animator.nav_forward_enter)
                    .setExitAnim(R.animator.nav_forward_exit);
        } else if (direction == TabDirection.BACKWARD) {
            builder.setEnterAnim(R.animator.nav_back_enter)
                    .setExitAnim(R.animator.nav_back_exit);
        }

        return builder.build();
    }
}
