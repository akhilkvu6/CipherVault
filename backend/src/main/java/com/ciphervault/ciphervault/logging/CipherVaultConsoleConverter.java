package com.ciphervault.ciphervault.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback converter that applies the CipherVault Backend Colour-Coded Terminal Logging Standard.
 *
 * Colour scheme:
 * - GREEN: successful operations, [OK], [SUCCESS], [APP]
 * - CYAN: HTTP, endpoints, [HTTP], [API], [METADATA], [PREVIEW], [BOOT], [SERVER]
 * - YELLOW: warnings, validation rejections, duplicate files, [WARN], [DUP], [CRYPTO]
 * - RED: application errors, exceptions, [ERROR], [FAIL]
 * - BLUE: database, storage, files, search, [DB], [DATABASE], [STORAGE], [FILE], [SEARCH], [FACETS], [TRANSFERS], [ACTIVITY]
 * - MAGENTA: authentication, security, quota, [AUTH], [SECURITY], [QUOTA]
 * - GREY: debug details, timings (e.g. "24 ms"), sizes (e.g. "2.50 MB")
 * - WHITE/DEFAULT: standard text
 */
public class CipherVaultConsoleConverter extends ClassicConverter {

    private static final Pattern CATEGORY_TAG_PATTERN = Pattern.compile(
            "\\[(OK|SUCCESS|HTTP|API|WARN|DUP|ERROR|FAIL|DB|DATABASE|STORAGE|FILE|SEARCH|FACETS|TRANSFERS|ACTIVITY|AUTH|SECURITY|QUOTA|CRYPTO|METADATA|PREVIEW|APP|BOOT|SERVER)\\]"
    );

    private static final Pattern TIMING_PATTERN = Pattern.compile("(\\b\\d+(?:\\.\\d+)?\\s*(?:ms|s|KB|MB|GB)\\b)");

    @Override
    public String convert(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        if (msg == null || msg.isEmpty()) {
            return "";
        }

        // If ANSI output is disabled, return clean message without escape codes
        if (AnsiOutput.getEnabled() == AnsiOutput.Enabled.NEVER) {
            return msg;
        }

        // Replace category tags with colour-coded counterparts
        StringBuilder sb = new StringBuilder();
        Matcher matcher = CATEGORY_TAG_PATTERN.matcher(msg);
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(msg, lastEnd, matcher.start());
            String tag = matcher.group(1);
            sb.append(colorizeTag(tag));
            lastEnd = matcher.end();
        }
        sb.append(msg.substring(lastEnd));

        String result = sb.toString();

        // Colorize timings and sizes in grey (bright black / faint)
        Matcher timeMatcher = TIMING_PATTERN.matcher(result);
        sb = new StringBuilder();
        lastEnd = 0;
        while (timeMatcher.find()) {
            sb.append(result, lastEnd, timeMatcher.start());
            sb.append(AnsiOutput.toString(AnsiColor.BRIGHT_BLACK, timeMatcher.group(1), AnsiColor.DEFAULT));
            lastEnd = timeMatcher.end();
        }
        sb.append(result.substring(lastEnd));

        return sb.toString();
    }

    private String colorizeTag(String tag) {
        String fullTag = "[" + tag + "]";
        return switch (tag) {
            case "OK", "SUCCESS", "APP" ->
                AnsiOutput.toString(AnsiColor.GREEN, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            case "HTTP", "API", "METADATA", "PREVIEW", "BOOT", "SERVER" ->
                AnsiOutput.toString(AnsiColor.CYAN, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            case "WARN", "DUP", "CRYPTO" ->
                AnsiOutput.toString(AnsiColor.YELLOW, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            case "ERROR", "FAIL" ->
                AnsiOutput.toString(AnsiColor.RED, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            case "DB", "DATABASE", "STORAGE", "FILE", "SEARCH", "FACETS", "TRANSFERS", "ACTIVITY" ->
                AnsiOutput.toString(AnsiColor.BLUE, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            case "AUTH", "SECURITY", "QUOTA" ->
                AnsiOutput.toString(AnsiColor.MAGENTA, AnsiStyle.BOLD, fullTag, AnsiStyle.NORMAL, AnsiColor.DEFAULT);
            default -> fullTag;
        };
    }
}
