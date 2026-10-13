package com.ciphervault.ciphervault.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.ansi.AnsiOutput;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CipherVaultConsoleConverterTest {

    private CipherVaultConsoleConverter converter;

    @BeforeEach
    void setUp() {
        converter = new CipherVaultConsoleConverter();
    }

    @AfterEach
    void tearDown() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @Test
    void testAnsiDisabledReturnsCleanString() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        String input = "[HTTP] GET /api/files | 200 OK | 24 ms";
        when(event.getFormattedMessage()).thenReturn(input);

        String result = converter.convert(event);
        assertEquals(input, result);
        assertFalse(result.contains("\u001B"));
    }

    @Test
    void testAnsiEnabledColorizesTags() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);

        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        String input = "[UPLOAD] Upload completed | [OK] 201 Created | 150 ms";
        when(event.getFormattedMessage()).thenReturn(input);

        String result = converter.convert(event);
        assertNotNull(result);
        assertTrue(result.contains("\u001B[")); // Contains ANSI escape sequence
        assertTrue(result.contains("Upload completed"));
    }

    @Test
    void testNullOrEmptyMessageHandledGracefully() {
        ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(null);
        assertEquals("", converter.convert(event));

        when(event.getFormattedMessage()).thenReturn("");
        assertEquals("", converter.convert(event));
    }
}
