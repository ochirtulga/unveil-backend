// src/main/java/com/unveil/validation/AtLeastOneContactMethod.java
package com.unveil.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import com.unveil.dto.CaseReportDto;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure at least one contact method is provided
 */
@Documented
@Constraint(validatedBy = AtLeastOneContactMethod.AtLeastOneContactMethodValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneContactMethod {
    String message() default "At least one contact method (name, email, or phone) is required";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class AtLeastOneContactMethodValidator implements ConstraintValidator<AtLeastOneContactMethod, CaseReportDto> {

        @Override
        public void initialize(AtLeastOneContactMethod constraintAnnotation) {
            // No initialization needed
        }

        @Override
        public boolean isValid(CaseReportDto dto, ConstraintValidatorContext context) {
            if (dto == null) {
                return false;
            }

            boolean hasName = dto.getName() != null && !dto.getName().trim().isEmpty();
            boolean hasEmail = dto.getEmail() != null && !dto.getEmail().trim().isEmpty();
            boolean hasPhone = dto.getPhone() != null && !dto.getPhone().trim().isEmpty();

            if (!(hasName || hasEmail || hasPhone)) {
                // Customize the error message location
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                        .addPropertyNode("name")
                        .addConstraintViolation();
                return false;
            }

            return true;
        }
    }
}