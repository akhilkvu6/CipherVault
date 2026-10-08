package com.ciphervault.app.onboarding;

import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.List;

/**
 * Immutable model holding data for an onboarding screen.
 */
public final class OnboardingPageData {

    private final int pageIndex;
    @StringRes
    private final int badgeTextResId;
    @StringRes
    private final int titleResId;
    @StringRes
    private final int descriptionResId;
    private final List<Integer> pillTextResIds;
    @StringRes
    private final int expandableTitleResId;
    @StringRes
    private final int expandableContentResId;

    public OnboardingPageData(
            int pageIndex,
            @StringRes int badgeTextResId,
            @StringRes int titleResId,
            @StringRes int descriptionResId,
            List<Integer> pillTextResIds,
            @StringRes int expandableTitleResId,
            @StringRes int expandableContentResId) {
        this.pageIndex = pageIndex;
        this.badgeTextResId = badgeTextResId;
        this.titleResId = titleResId;
        this.descriptionResId = descriptionResId;
        this.pillTextResIds = pillTextResIds != null ? pillTextResIds : Collections.emptyList();
        this.expandableTitleResId = expandableTitleResId;
        this.expandableContentResId = expandableContentResId;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getBadgeTextResId() {
        return badgeTextResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getDescriptionResId() {
        return descriptionResId;
    }

    public List<Integer> getPillTextResIds() {
        return pillTextResIds;
    }

    public int getExpandableTitleResId() {
        return expandableTitleResId;
    }

    public int getExpandableContentResId() {
        return expandableContentResId;
    }

    public boolean hasPills() {
        return !pillTextResIds.isEmpty();
    }

    public boolean hasExpandable() {
        return expandableTitleResId != 0;
    }
}
