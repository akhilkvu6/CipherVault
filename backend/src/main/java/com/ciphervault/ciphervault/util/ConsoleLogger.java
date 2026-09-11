package com.ciphervault.ciphervault.util;

public final class ConsoleLogger {

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";

    private ConsoleLogger() {
        // Prevent object creation
    }

    public static void info(String message) {
        print(CYAN, "[INFO]    ", message);
    }

    public static void success(String message) {
        print(GREEN, "[SUCCESS] ", message);
    }

    public static void warn(String message) {
        print(YELLOW, "[WARN]    ", message);
    }

    public static void error(String message) {
        print(RED, "[ERROR]   ", message);
    }

    private static void print(String color, String level, String message) {
        System.out.println(color + level + message + RESET);
    }
}