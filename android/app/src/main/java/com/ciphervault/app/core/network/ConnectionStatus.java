package com.ciphervault.app.core.network;

/**
 * Server connection states (Section 26).
 */
public enum ConnectionStatus {
    CONNECTED,
    NOT_CONFIGURED,
    CHECKING,
    UNAVAILABLE
}
