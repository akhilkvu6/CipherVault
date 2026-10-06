package com.ciphervault.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ciphervault.app.onboarding.OnboardingPage;
import com.ciphervault.app.onboarding.OnboardingPageData;

import org.junit.Test;

import java.util.List;

public class OnboardingPageDataTest {

    @Test
    public void testOnboardingPagesCount() {
        List<OnboardingPageData> pages = OnboardingPage.getPages();
        assertNotNull(pages);
        assertEquals(5, pages.size());
    }

    @Test
    public void testPageProperties() {
        List<OnboardingPageData> pages = OnboardingPage.getPages();

        // Screen 1: Welcome (has supporting concepts)
        OnboardingPageData page1 = pages.get(0);
        assertEquals(0, page1.getPageIndex());
        assertEquals(R.string.onboarding_title_1, page1.getTitleResId());
        assertTrue(page1.hasPills());
        assertEquals(3, page1.getPillTextResIds().size());
        assertFalse(page1.hasExpandable());

        // Screen 2: Encrypted Storage (has expandable)
        OnboardingPageData page2 = pages.get(1);
        assertEquals(1, page2.getPageIndex());
        assertEquals(R.string.onboarding_title_2, page2.getTitleResId());
        assertTrue(page2.hasExpandable());

        // Screen 3: File Integrity (has expandable)
        OnboardingPageData page3 = pages.get(2);
        assertEquals(2, page3.getPageIndex());
        assertEquals(R.string.onboarding_title_3, page3.getTitleResId());
        assertTrue(page3.hasExpandable());

        // Screen 4: Everything in One Place
        OnboardingPageData page4 = pages.get(3);
        assertEquals(3, page4.getPageIndex());
        assertEquals(R.string.onboarding_title_4, page4.getTitleResId());
        assertFalse(page4.hasPills());
        assertFalse(page4.hasExpandable());

        // Screen 5: Private Vault
        OnboardingPageData page5 = pages.get(4);
        assertEquals(4, page5.getPageIndex());
        assertEquals(R.string.onboarding_title_5, page5.getTitleResId());
        assertFalse(page5.hasPills());
        assertFalse(page5.hasExpandable());
    }
}
