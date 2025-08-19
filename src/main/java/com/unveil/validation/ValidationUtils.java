// src/main/java/com/unveil/validation/ValidationUtils.java
package com.unveil.validation;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Utility class for common validation patterns and data cleaning
 */
public class ValidationUtils {

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Phone validation pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\d\\s\\-\\(\\)\\+\\.]+$");

    // Common spam/profanity words to filter (expand as needed)
    private static final Set<String> SPAM_WORDS = new HashSet<>(Arrays.asList(
            "viagra", "casino", "lottery", "winner", "congratulations",
            "urgent", "act now", "limited time", "click here", "free money"
            // Add more spam indicators as needed
    ));

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Optional field
        }

        String cleanPhone = phone.trim();

        // Check pattern
        if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
            return false;
        }

        // Check digit count
        String digitsOnly = cleanPhone.replaceAll("\\D", "");
        return digitsOnly.length() >= 10 && digitsOnly.length() <= 15;
    }

    /**
     * Check if text contains obvious spam patterns
     */
    public static boolean containsSpam(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // Check for spam words
        for (String spamWord : SPAM_WORDS) {
            if (lowerText.contains(spamWord)) {
                return true;
            }
        }

        // Check for excessive capitalization (more than 50% caps)
        long upperCount = text.chars().filter(Character::isUpperCase).count();
        long letterCount = text.chars().filter(Character::isLetter).count();

        if (letterCount > 10 && upperCount > letterCount * 0.5) {
            return true;
        }

        // Check for excessive punctuation
        long punctCount = text.chars().filter(ch -> "!@#$%^&*()".indexOf(ch) >= 0).count();
        if (punctCount > text.length() * 0.2) {
            return true;
        }

        return false;
    }

    /**
     * Sanitize input text to remove potentially harmful content
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        return input.trim()
                // Remove script tags and content
                .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                // Remove other HTML tags
                .replaceAll("<[^>]+>", "")
                // Remove excessive whitespace
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ")
                // Remove null characters
                .replaceAll("\\x00", "")
                .trim();
    }

    /**
     * Validate description content
     */
    public static boolean isValidDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }

        String cleanDescription = sanitizeInput(description);

        // Length checks
        if (cleanDescription.length() < 20 || cleanDescription.length() > 2000) {
            return false;
        }

        // Check for repeated characters (spam pattern)
        if (cleanDescription.matches("^(.)\\1{19,}$")) {
            return false;
        }

        // Check for minimum word count (at least 5 words)
        String[] words = cleanDescription.split("\\s+");
        if (words.length < 5) {
            return false;
        }

        // Check for spam content
        if (containsSpam(cleanDescription)) {
            return false;
        }

        return true;
    }

    /**
     * Normalize and format name
     */
    public static String formatName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String cleaned = sanitizeInput(name);

        // Capitalize first letter of each word
        String[] words = cleaned.toLowerCase().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                if (formatted.length() > 0) {
                    formatted.append(" ");
                }
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1));
                }
            }
        }

        return formatted.toString();
    }

    /**
     * Normalize email address
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return sanitizeInput(email).toLowerCase();
    }

    /**
     * Check if the content appears to be legitimate case report
     */
    public static boolean isLegitimateReport(String description, String scamType) {
        if (description == null || scamType == null) {
            return false;
        }

        String lowerDesc = description.toLowerCase();
        String lowerType = scamType.toLowerCase();

        // Check for relevant keywords based on scam type
        boolean hasRelevantKeywords = false;

        if (lowerType.contains("phone")) {
            hasRelevantKeywords = lowerDesc.contains("call") || lowerDesc.contains("phone")
                    || lowerDesc.contains("voicemail") || lowerDesc.contains("robocall");
        } else if (lowerType.contains("email")) {
            hasRelevantKeywords = lowerDesc.contains("email") || lowerDesc.contains("message")
                    || lowerDesc.contains("link") || lowerDesc.contains("attachment");
        } else if (lowerType.contains("investment")) {
            hasRelevantKeywords = lowerDesc.contains("money") || lowerDesc.contains("invest")
                    || lowerDesc.contains("profit") || lowerDesc.contains("return");
        } else if (lowerType.contains("romance")) {
            hasRelevantKeywords = lowerDesc.contains("love") || lowerDesc.contains("relationship")
                    || lowerDesc.contains("dating") || lowerDesc.contains("lonely");
        }

        return hasRelevantKeywords;
    }

    /**
     * Extract domain from email for duplicate checking
     */
    public static String extractEmailDomain(String email) {
        if (email == null || !isValidEmail(email)) {
            return null;
        }

        int atIndex = email.lastIndexOf('@');
        if (atIndex > 0 && atIndex < email.length() - 1) {
            return email.substring(atIndex + 1).toLowerCase();
        }

        return null;
    }

    /**
     * Check if two descriptions are similar (for duplicate detection)
     */
    public static boolean areDescriptionsSimilar(String desc1, String desc2, double threshold) {
        if (desc1 == null || desc2 == null) {
            return false;
        }

        String clean1 = sanitizeInput(desc1).toLowerCase();
        String clean2 = sanitizeInput(desc2).toLowerCase();

        // Simple similarity check based on common words
        String[] words1 = clean1.split("\\s+");
        String[] words2 = clean2.split("\\s+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        // Calculate Jaccard similarity
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return false;
        }

        double similarity = (double) intersection.size() / union.size();
        return similarity >= threshold;
    }
}