// src/main/java/com/unveil/validation/ValidScamType.java
package com.unveil.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Custom validation annotation for scam type
 */
@Documented
@Constraint(validatedBy = ValidScamType.ValidScamTypeValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidScamType {
    String message() default "Invalid scam type. Must be one of: Phone Scam, Email Fraud, Investment Scam, Romance Scam, Tech Support Scam, Identity Theft, Online Shopping Fraud, Phishing, Cryptocurrency Scam, Job Interview Scam, Tax Refund Scam, Charity Scam, Other";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class ValidScamTypeValidator implements ConstraintValidator<ValidScamType, String> {

        private static final Set<String> VALID_SCAM_TYPES = new HashSet<>(Arrays.asList(
                "Phone Scam", "Email Fraud", "Investment Scam", "Romance Scam",
                "Tech Support Scam", "Identity Theft", "Online Shopping Fraud",
                "Phishing", "Cryptocurrency Scam", "Job Interview Scam",
                "Tax Refund Scam", "Charity Scam", "Other"
        ));

        @Override
        public void initialize(ValidScamType constraintAnnotation) {
            // No initialization needed
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // Allow null/empty since @NotBlank handles that separately
            if (value == null || value.trim().isEmpty()) {
                return true;
            }

            String normalizedValue = value.trim();

            // Case-insensitive check
            return VALID_SCAM_TYPES.stream()
                    .anyMatch(validType -> validType.equalsIgnoreCase(normalizedValue));
        }

        /**
         * Get all valid scam types for reference
         */
        public static Set<String> getValidScamTypes() {
            return new HashSet<>(VALID_SCAM_TYPES);
        }
    }
}