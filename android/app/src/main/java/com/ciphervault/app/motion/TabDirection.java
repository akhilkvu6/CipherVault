package com.ciphervault.app.motion;

/**
 * Directional values for tab transitions (B4).
 */
public enum TabDirection {
    /**
     * Navigating to a higher tab index (current slides LEFT, target enters from RIGHT).
     */
    FORWARD,

    /**
     * Navigating to a lower tab index (current slides RIGHT, target enters from LEFT).
     */
    BACKWARD,

    /**
     * Reselecting the currently active tab.
     */
    SAME
}
