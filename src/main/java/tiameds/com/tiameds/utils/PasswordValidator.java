package tiameds.com.tiameds.utils;

import java.util.regex.Pattern;

/**
 * Utility class for password strength validation
 */
public class PasswordValidator {

    // Password requirements:
    // - Minimum 8 characters (configurable, max 12 recommended)
    // - At least one uppercase letter
    // - At least one lowercase letter
    // - At least one digit
    // - At least one special character

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

    /**
     * Validates password strength according to requirements
     *
     * @param password The password to validate
     * @param minLength Minimum password length (default: 8)
     * @param maxLength Maximum password length (default: 128)
     * @return ValidationResult with isValid flag and error message
     */
    public static ValidationResult validate(String password, int minLength, int maxLength) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }

        if (password.length() < minLength) {
            return new ValidationResult(false, 
                String.format("Password must be at least %d characters long", minLength));
        }

        if (password.length() > maxLength) {
            return new ValidationResult(false, 
                String.format("Password must be at most %d characters long", maxLength));
        }

        if (!UPPERCASE_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, "Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, "Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, "Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return new ValidationResult(false, "Password must contain at least one special character (!@#$%^&*()_+-=[]{};':\"|,.<>/?)");
        }

        return new ValidationResult(true, null);
    }

    /**
     * Default validation with min 8, max 128 characters
     */
    public static ValidationResult validate(String password) {
        return validate(password, 8, 128);
    }

    /**
     * Result of password validation
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}



