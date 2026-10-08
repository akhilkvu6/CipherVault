package com.ciphervault.app.motion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

/**
 * Unit tests verifying Motion Foundation timing tokens, tab direction, and reduced-motion decisions (C).
 */
public class GlobalMotionTest {

    @After
    public void tearDown() {
        ReducedMotionHelper.setTestOverride(null);
    }

    @Test
    public void testTimingTokens() {
        assertEquals("MICRO duration must be 100ms", 100L, MotionTokens.MICRO);
        assertEquals("FAST duration must be 160ms", 160L, MotionTokens.FAST);
        assertEquals("STANDARD duration must be 220ms", 220L, MotionTokens.STANDARD);
        assertEquals("EMPHASIS duration must be 280ms", 280L, MotionTokens.EMPHASIS);
        assertEquals("Standard translation must be 24dp", 24, MotionTokens.TRANSLATION_DP_STANDARD);
    }

    @Test
    public void testDirectionalLogicDeterministic() {
        // Higher index -> FORWARD (B4)
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(0, 1));
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(0, 2));
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(0, 3));
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(1, 2));
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(1, 3));
        assertEquals(TabDirection.FORWARD, NavigationMotionManager.calculateDirection(2, 3));

        // Lower index -> BACKWARD (B4)
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(1, 0));
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(2, 0));
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(3, 0));
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(2, 1));
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(3, 1));
        assertEquals(TabDirection.BACKWARD, NavigationMotionManager.calculateDirection(3, 2));

        // Same index -> SAME (reselection)
        assertEquals(TabDirection.SAME, NavigationMotionManager.calculateDirection(0, 0));
        assertEquals(TabDirection.SAME, NavigationMotionManager.calculateDirection(1, 1));
        assertEquals(TabDirection.SAME, NavigationMotionManager.calculateDirection(2, 2));
        assertEquals(TabDirection.SAME, NavigationMotionManager.calculateDirection(3, 3));
    }

    @Test
    public void testReducedMotionDecisionLogic() {
        // Pure mathematical check: 0.0 scale triggers reduced motion
        assertTrue("0.0f duration scale should reduce motion", ReducedMotionHelper.shouldReduceMotion(0.0f));
        assertFalse("0.5f duration scale should not reduce motion", ReducedMotionHelper.shouldReduceMotion(0.5f));
        assertFalse("1.0f duration scale should not reduce motion", ReducedMotionHelper.shouldReduceMotion(1.0f));

        // Null context safe fallback
        assertFalse("Null context should default to false safely", ReducedMotionHelper.isReducedMotionEnabled(null));

        // Deterministic override check
        ReducedMotionHelper.setTestOverride(true);
        assertTrue(ReducedMotionHelper.isReducedMotionEnabled(null));

        ReducedMotionHelper.setTestOverride(false);
        assertFalse(ReducedMotionHelper.isReducedMotionEnabled(null));
    }

    @Test
    public void testMotionManagerTabIndexState() {
        NavigationMotionManager manager = new NavigationMotionManager();
        assertEquals(NavigationMotionManager.TAB_INDEX_HOME, manager.getCurrentTabIndex());

        manager.setCurrentTabIndex(NavigationMotionManager.TAB_INDEX_ACTIVITY);
        assertEquals(NavigationMotionManager.TAB_INDEX_ACTIVITY, manager.getCurrentTabIndex());
    }
}
