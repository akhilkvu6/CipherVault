package com.ciphervault.app.auth.validation;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Authentication input validation rules (Sections 17, 18).
 */
public final class AuthValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private AuthValidator() {
    }

    @Nullable
    public static String validateName(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Full Name is required";
        }
        if (name.trim().length() > 255) {
            return "Name must not exceed 255 characters";
        }
        return null;
    }

    @Nullable
    public static String validateUsername(@Nullable String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required";
        }
        return null;
    }

    @Nullable
    public static String validateEmail(@Nullable String email) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Please enter a valid email address";
        }
        return null;
    }

    @Nullable
    public static String validatePassword(@Nullable String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        PasswordRequirements reqs = PasswordRequirements.check(password);
        if (!reqs.hasMinLength()) {
            return "Password must be at least 8 characters";
        }
        if (!reqs.hasUppercase()) {
            return "Password must contain an uppercase letter";
        }
        if (!reqs.hasLowercase()) {
            return "Password must contain a lowercase letter";
        }
        if (!reqs.hasNumber()) {
            return "Password must contain a number";
        }
        if (!reqs.hasSpecialChar()) {
            return "Password must contain a special character";
        }
        return null;
    }

    @Nullable
    public static String validateConfirmPassword(
            @Nullable String password,
            @Nullable String confirmPassword
    ) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return "Please confirm your password";
        }
        if (password == null || !password.equals(confirmPassword)) {
            return "Passwords do not match";
        }
        return null;
    }
}
