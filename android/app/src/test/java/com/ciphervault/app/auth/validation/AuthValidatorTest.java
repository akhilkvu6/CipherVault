package com.ciphervault.app.auth.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests verifying authentication input validation rules (Section 43).
 */
public class AuthValidatorTest {

    @Test
    public void testValidateName() {
        assertNotNull("Blank name should fail", AuthValidator.validateName(""));
        assertNotNull("Whitespace name should fail", AuthValidator.validateName("   "));
        assertNotNull("Null name should fail", AuthValidator.validateName(null));

        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            longName.append("a");
        }
        assertNotNull("Name > 255 chars should fail", AuthValidator.validateName(longName.toString()));

        assertNull("Valid name should pass", AuthValidator.validateName("Akhil Krishna"));
    }

    @Test
    public void testValidateUsername() {
        assertNotNull("Blank username should fail", AuthValidator.validateUsername(""));
        assertNotNull("Whitespace username should fail", AuthValidator.validateUsername("   "));
        assertNotNull("Null username should fail", AuthValidator.validateUsername(null));
        assertNull("Valid username should pass", AuthValidator.validateUsername("akhil"));
    }

    @Test
    public void testValidateEmail() {
        assertNotNull("Blank email should fail", AuthValidator.validateEmail(""));
        assertNotNull("Null email should fail", AuthValidator.validateEmail(null));
        assertNotNull("Invalid email format (no @) should fail", AuthValidator.validateEmail("notanemail"));
        assertNotNull("Invalid email format (no domain) should fail", AuthValidator.validateEmail("test@"));
        assertNotNull("Invalid email format (no TLD) should fail", AuthValidator.validateEmail("test@domain"));

        assertNull("Valid email should pass", AuthValidator.validateEmail("akhil@example.com"));
        assertNull("Valid email with plus should pass", AuthValidator.validateEmail("user+tag@domain.co.uk"));
    }

    @Test
    public void testValidatePasswordRequirements() {
        assertNotNull("Blank password should fail", AuthValidator.validatePassword(""));
        assertNotNull("Null password should fail", AuthValidator.validatePassword(null));

        // Password < 8 characters
        assertNotNull("Password < 8 chars should fail", AuthValidator.validatePassword("Pass1!"));
        assertEquals("Password must be at least 8 characters", AuthValidator.validatePassword("Pass1!"));

        // Missing uppercase
        assertNotNull("Missing uppercase should fail", AuthValidator.validatePassword("password123!"));
        assertEquals("Password must contain an uppercase letter", AuthValidator.validatePassword("password123!"));

        // Missing lowercase
        assertNotNull("Missing lowercase should fail", AuthValidator.validatePassword("PASSWORD123!"));
        assertEquals("Password must contain a lowercase letter", AuthValidator.validatePassword("PASSWORD123!"));

        // Missing number
        assertNotNull("Missing number should fail", AuthValidator.validatePassword("Password!!!!"));
        assertEquals("Password must contain a number", AuthValidator.validatePassword("Password!!!!"));

        // Missing special character
        assertNotNull("Missing special character should fail", AuthValidator.validatePassword("Password123"));
        assertEquals("Password must contain a special character", AuthValidator.validatePassword("Password123"));

        // All requirements met
        assertNull("Valid password should pass", AuthValidator.validatePassword("Password123!"));
    }

    @Test
    public void testPasswordRequirementsChecklistModel() {
        PasswordRequirements empty = PasswordRequirements.check("");
        assertFalse(empty.hasMinLength());
        assertFalse(empty.hasUppercase());
        assertFalse(empty.hasLowercase());
        assertFalse(empty.hasNumber());
        assertFalse(empty.hasSpecialChar());
        assertFalse(empty.isAllMet());

        PasswordRequirements partial = PasswordRequirements.check("Pass");
        assertFalse(partial.hasMinLength());
        assertTrue(partial.hasUppercase());
        assertTrue(partial.hasLowercase());
        assertFalse(partial.hasNumber());
        assertFalse(partial.hasSpecialChar());

        PasswordRequirements complete = PasswordRequirements.check("Password123!");
        assertTrue(complete.hasMinLength());
        assertTrue(complete.hasUppercase());
        assertTrue(complete.hasLowercase());
        assertTrue(complete.hasNumber());
        assertTrue(complete.hasSpecialChar());
        assertTrue(complete.isAllMet());
    }

    @Test
    public void testValidateConfirmPassword() {
        assertNotNull("Null confirm password should fail", AuthValidator.validateConfirmPassword("Password123!", null));
        assertNotNull("Empty confirm password should fail", AuthValidator.validateConfirmPassword("Password123!", ""));
        assertNotNull("Mismatched confirm password should fail", AuthValidator.validateConfirmPassword("Password123!", "Password456!"));
        assertEquals("Passwords do not match", AuthValidator.validateConfirmPassword("Password123!", "Password456!"));

        assertNull("Matching confirm password should pass", AuthValidator.validateConfirmPassword("Password123!", "Password123!"));
    }
}
