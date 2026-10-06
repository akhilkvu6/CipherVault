package com.ciphervault.app.onboarding;

import com.ciphervault.app.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provider class creating the 5 onboarding pages.
 */
public final class OnboardingPage {

    private OnboardingPage() {}

    public static List<OnboardingPageData> getPages() {
        List<OnboardingPageData> pages = new ArrayList<>();

        // Screen 1: Welcome to CipherVault
        pages.add(new OnboardingPageData(
                0,
                R.string.onboarding_badge_1,
                R.string.onboarding_title_1,
                R.string.onboarding_desc_1,
                Arrays.asList(
                        R.string.onboarding_pill_1_1,
                        R.string.onboarding_pill_1_2,
                        R.string.onboarding_pill_1_3
                ),
                0,
                0
        ));

        // Screen 2: Encrypted Storage
        pages.add(new OnboardingPageData(
                1,
                R.string.onboarding_badge_2,
                R.string.onboarding_title_2,
                R.string.onboarding_desc_2,
                null,
                R.string.onboarding_expandable_header_2,
                R.string.onboarding_desc_2
        ));

        // Screen 3: File Integrity & Duplicate Detection
        pages.add(new OnboardingPageData(
                2,
                R.string.onboarding_badge_3,
                R.string.onboarding_title_3,
                R.string.onboarding_desc_3,
                null,
                R.string.onboarding_expandable_header_3,
                R.string.onboarding_expandable_content_3
        ));

        // Screen 4: Everything in One Place
        pages.add(new OnboardingPageData(
                3,
                R.string.onboarding_badge_4,
                R.string.onboarding_title_4,
                R.string.onboarding_desc_4,
                null,
                0,
                0
        ));

        // Screen 5: Your Private Vault
        pages.add(new OnboardingPageData(
                4,
                R.string.onboarding_badge_5,
                R.string.onboarding_title_5,
                R.string.onboarding_desc_5,
                null,
                0,
                0
        ));

        return pages;
    }
}
