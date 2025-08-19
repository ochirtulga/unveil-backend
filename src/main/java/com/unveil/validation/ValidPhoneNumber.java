// src/main/java/com/unveil/validation/ValidPhoneNumber.java
package com.unveil.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.regex.Pattern;

/**
 * Custom validation annotation for phone numbers
 */
@Documented
@Constraint(validatedBy = ValidPhoneNumber.ValidPhoneNumberValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhoneNumber {
    String message() default "Invalid phone number format. Must be 10-15 digits and may include spaces, hyphens, parentheses, or plus sign.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

        // Pattern for valid phone number characters
        private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\d\\s\\-\\(\\)\\+\\.]+$");

        @Override
        public void initialize(ValidPhoneNumber constraintAnnotation) {
            // No initialization needed
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Allow null/empty since this is an optional field
            if (value == null || value.trim().isEmpty()) {
                return true;
            }

            String phone = value.trim();

            // Check if it contains only valid characters
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                return false;
            }

            // Remove all non-digit characters for digit count validation
            String digitsOnly = phone.replaceAll("\\D", "");

            // Must have between 10 and 15 digits (international standard)
            if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
                return false;
            }

            // Additional validation: if it starts with +, it should have country code
            if (phone.startsWith("+")) {
                // Country code should be 1-4 digits, so total should be 11-19 characters with country code
                return digitsOnly.length() >= 11;
            }

            return true;
        }

        /**
         * Utility method to format phone number
         */
        public static String formatPhoneNumber(String phone) {
            if (phone == null || phone.trim().isEmpty()) {
                return null;
            }

            String digits = phone.replaceAll("\\D", "");

            if (digits.length() == 10) {
                // US format: (123) 456-7890
                return String.format("(%s) %s-%s",
                        digits.substring(0, 3),
                        digits.substring(3, 6),
                        digits.substring(6));
            } else if (digits.length() == 11 && digits.startsWith("1")) {
                // US with country code: +1 (123) 456-7890
                return String.format("+1 (%s) %s-%s",
                        digits.substring(1, 4),
                        digits.substring(4, 7),
                        digits.substring(7));
            }

            // Return original for international numbers
            return phone.trim();
        }
    }
}