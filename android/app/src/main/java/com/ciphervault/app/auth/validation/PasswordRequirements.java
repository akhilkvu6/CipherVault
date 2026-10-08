package com.ciphervault.app.auth.validation;

/**
 * Real-time password requirement checklist state (Section 19).
 */
public class PasswordRequirements {

    private final boolean hasMinLength;
    private final boolean hasUppercase;
    private final boolean hasLowercase;
    private final boolean hasNumber;
    private final boolean hasSpecialChar;

    public PasswordRequirements(
            boolean hasMinLength,
            boolean hasUppercase,
            boolean hasLowercase,
            boolean hasNumber,
            boolean hasSpecialChar
    ) {
        this.hasMinLength = hasMinLength;
        this.hasUppercase = hasUppercase;
        this.hasLowercase = hasLowercase;
        this.hasNumber = hasNumber;
        this.hasSpecialChar = hasSpecialChar;
    }

    public boolean hasMinLength() {
        return hasMinLength;
    }

    public boolean hasUppercase() {
        return hasUppercase;
    }

    public boolean hasLowercase() {
        return hasLowercase;
    }

    public boolean hasNumber() {
        return hasNumber;
    }

    public boolean hasSpecialChar() {
        return hasSpecialChar;
    }

    public boolean isAllMet() {
        return hasMinLength && hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }

    public static PasswordRequirements check(String password) {
        if (password == null) {
            return new PasswordRequirements(false, false, false, false, false);
        }

        boolean minLength = password.length() >= 8;
        boolean uppercase = false;
        boolean lowercase = false;
        boolean number = false;
        boolean special = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isUpperCase(c)) {
                uppercase = true;
            } else if (Character.isLowerCase(c)) {
                lowercase = true;
            } else if (Character.isDigit(c)) {
                number = true;
            } else {
                special = true;
            }
        }

        return new PasswordRequirements(minLength, uppercase, lowercase, number, special);
    }
}
