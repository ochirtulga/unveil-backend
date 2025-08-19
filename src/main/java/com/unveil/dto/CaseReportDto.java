package com.unveil.dto;

import com.unveil.validation.AtLeastOneContactMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@AtLeastOneContactMethod
public class CaseReportDto {

    // Scammer Information (at least one required via custom validator)
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phone;

    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String company;

    // Scam Details (required)
    @NotBlank(message = "Scam type is required")
    @Size(max = 300, message = "Scam type must not exceed 300 characters")
    private String actions;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    // Reporter Information (required)
    @NotBlank(message = "Reporter name is required")
    @Size(max = 100, message = "Reporter name must not exceed 100 characters")
    private String reporterName;

    @NotBlank(message = "Reporter email is required")
    @Email(message = "Please provide a valid reporter email address")
    @Size(max = 255, message = "Reporter email must not exceed 255 characters")
    private String reporterEmail;

    // Optional metadata
    private String ipAddress;
    private String userAgent;
    private String referrer;

    // Custom validation method
    public boolean hasAtLeastOneContactMethod() {
        return (name != null && !name.trim().isEmpty()) ||
                (email != null && !email.trim().isEmpty()) ||
                (phone != null && !phone.trim().isEmpty());
    }

    // Helper methods for data cleaning
    public String getCleanName() {
        return sanitizeAndTrim(name);
    }

    public String getCleanEmail() {
        return email != null ? sanitizeAndTrim(email).toLowerCase() : null;
    }

    public String getCleanPhone() {
        return sanitizeAndTrim(phone);
    }

    public String getCleanCompany() {
        return sanitizeAndTrim(company);
    }

    public String getCleanActions() {
        return sanitizeAndTrim(actions);
    }

    public String getCleanDescription() {
        return sanitizeAndTrim(description);
    }

    public String getCleanReporterName() {
        return sanitizeAndTrim(reporterName);
    }

    public String getCleanReporterEmail() {
        return reporterEmail != null ? sanitizeAndTrim(reporterEmail).toLowerCase() : null;
    }

    // Utility method for consistent data cleaning
    private String sanitizeAndTrim(String input) {
        if (input == null) {
            return null;
        }

        String cleaned = input.trim()
                .replaceAll("<script[^>]*>.*?</script>", "") // Remove script tags
                .replaceAll("<[^>]+>", "") // Remove HTML tags
                .replaceAll("[\\r\\n]+", " ") // Replace line breaks with spaces
                .replaceAll("\\s+", " "); // Normalize whitespace

        return cleaned.isEmpty() ? null : cleaned;
    }

    // Validation helper methods
    public boolean isValidForSubmission() {
        return hasAtLeastOneContactMethod() &&
                getCleanActions() != null && !getCleanActions().isEmpty() &&
                getCleanDescription() != null && getCleanDescription().length() >= 20 &&
                getCleanReporterName() != null && !getCleanReporterName().isEmpty() &&
                getCleanReporterEmail() != null && !getCleanReporterEmail().isEmpty();
    }

    public String getFormattedPhone() {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");

        // Format US phone numbers
        if (digits.length() == 10) {
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            return String.format("+1 (%s) %s-%s",
                    digits.substring(1, 4),
                    digits.substring(4, 7),
                    digits.substring(7));
        }

        // Return cleaned version for international numbers
        return getCleanPhone();
    }

    // Generate a summary for logging/debugging
    public String getSummary() {
        StringBuilder summary = new StringBuilder("Case Report: ");

        if (getCleanName() != null) {
            summary.append("Name='").append(getCleanName()).append("' ");
        }
        if (getCleanEmail() != null) {
            summary.append("Email='").append(getCleanEmail()).append("' ");
        }
        if (getCleanPhone() != null) {
            summary.append("Phone='").append(getCleanPhone()).append("' ");
        }
        if (getCleanCompany() != null) {
            summary.append("Company='").append(getCleanCompany()).append("' ");
        }

        summary.append("Type='").append(getCleanActions()).append("' ");
        summary.append("ReportedBy='").append(getCleanReporterEmail()).append("'");

        return summary.toString();
    }
}