package com.unveil.service;

import com.unveil.dto.CaseReportDto;
import com.unveil.entity.Case;
import com.unveil.repository.CaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class CaseService {

    private final CaseRepository caseRepository;

    // Rate limiting for case submissions (use Redis in production)
    private final Map<String, LocalDateTime> emailSubmissionLimit = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> ipSubmissionLimit = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailySubmissionCount = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_SUBMISSIONS_PER_EMAIL_PER_DAY = 5;
    private static final int MAX_SUBMISSIONS_PER_IP_PER_HOUR = 3;
    private static final int MIN_MINUTES_BETWEEN_SUBMISSIONS = 5;

    public CaseService(CaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    /**
     * Submit a new case report
     */
    public Case submitCase(CaseReportDto request, String verifiedEmail, String ipAddress) {
        // Validate the case data
        validateCaseSubmission(request, verifiedEmail, ipAddress);

        // Check for duplicate cases
        checkForDuplicates(request);

        // Create new case entity
        Case newCase = createCaseFromDto(request, verifiedEmail);

        // Save the case
        Case savedCase = caseRepository.save(newCase);

        // Update rate limiting
        updateSubmissionLimits(verifiedEmail, ipAddress);

        return savedCase;
    }

    /**
     * Get a case by ID
     */
    public Optional<Case> getCaseById(Long id) {
        return caseRepository.findById(id);
    }

    /**
     * Update an existing case
     */
    public Case updateCase(Long id, CaseReportDto request) {
        Optional<Case> caseOpt = caseRepository.findById(id);

        if (caseOpt.isEmpty()) {
            throw new IllegalArgumentException("Case not found with ID: " + id);
        }

        Case existingCase = caseOpt.get();

        // Validate the update data
        if (!request.hasAtLeastOneContactMethod()) {
            throw new IllegalArgumentException("At least one contact method (name, email, or phone) is required");
        }

        // Update case fields
        existingCase.setName(request.getCleanName());
        existingCase.setEmail(request.getCleanEmail());
        existingCase.setPhone(request.getCleanPhone());
        existingCase.setCompany(request.getCleanCompany());
        existingCase.setActions(request.getCleanActions());
        existingCase.setDescription(request.getCleanDescription());

        return caseRepository.save(existingCase);
    }

    /**
     * Delete a case
     */
    public boolean deleteCase(Long id) {
        if (caseRepository.existsById(id)) {
            caseRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get case statistics
     */
    public Map<String, Object> getCaseStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        long totalCases = caseRepository.count();
        long guiltyCount = caseRepository.countGuiltyCases();
        long notGuiltyCount = caseRepository.countNotGuiltyCases();
        long pendingCount = caseRepository.countOnTrialCases();

        stats.put("totalCases", totalCases);
        stats.put("guiltyCount", guiltyCount);
        stats.put("notGuiltyCount", notGuiltyCount);
        stats.put("pendingCount", pendingCount);

        // Recent submissions (last 24 hours, 7 days, 30 days)
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime monthAgo = LocalDateTime.now().minusDays(30);

        // Note: These would need custom repository queries in production
        stats.put("submissionsLast24h", 0); // Placeholder
        stats.put("submissionsLast7d", 0);  // Placeholder
        stats.put("submissionsLast30d", 0); // Placeholder

        // Top scam types
        List<String> topScamTypes = caseRepository.findAllActions();
        stats.put("topScamTypes", topScamTypes.size() > 10 ?
                topScamTypes.subList(0, 10) : topScamTypes);

        // Verification stats
        Long totalVotes = caseRepository.getTotalVotesCount();
        stats.put("totalVotes", totalVotes != null ? totalVotes : 0);

        return stats;
    }

    /**
     * Get recent case submissions
     */
    public Map<String, Object> getRecentCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Case> cases = caseRepository.findAll(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("results", cases.getContent());
        response.put("pagination", buildPaginationInfo(cases));
        response.put("message", "Recent case submissions");

        return response;
    }

    /**
     * Get cases by reporter email
     */
    public Map<String, Object> getCasesByReporter(String email, int page, int size) {
        // Note: This would need a custom repository query to search by reportedBy field
        // For now, return empty results
        Pageable pageable = PageRequest.of(page, size);
        Page<Case> emptyCases = Page.empty(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("results", emptyCases.getContent());
        response.put("pagination", buildPaginationInfo(emptyCases));
        response.put("message", "Cases reported by: " + email);
        response.put("reporterEmail", email);

        return response;
    }

    /**
     * Flag a case for review
     */
    public boolean flagCase(Long id, String reason, String description, String ipAddress) {
        Optional<Case> caseOpt = caseRepository.findById(id);

        if (caseOpt.isEmpty()) {
            return false;
        }

        // In production, this would create a CaseFlag entity
        // For now, just log the flag
        System.out.println("Case " + id + " flagged for: " + reason + " from IP: " + ipAddress);

        return true;
    }

    /**
     * Validate case data
     */
    public Map<String, Object> validateCaseData(CaseReportDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (!request.hasAtLeastOneContactMethod()) {
            errors.add("At least one contact method (name, email, or phone) is required");
        }

        if (request.getCleanActions() == null || request.getCleanActions().isEmpty()) {
            errors.add("Scam type is required");
        }

        if (request.getCleanDescription() == null || request.getCleanDescription().length() < 20) {
            errors.add("Description must be at least 20 characters long");
        }

        if (request.getCleanReporterName() == null || request.getCleanReporterName().isEmpty()) {
            errors.add("Reporter name is required");
        }

        if (request.getCleanReporterEmail() == null || request.getCleanReporterEmail().isEmpty()) {
            errors.add("Reporter email is required");
        }

        // Validate email formats
        if (request.getEmail() != null && !isValidEmail(request.getEmail())) {
            errors.add("Invalid scammer email format");
        }

        if (request.getReporterEmail() != null && !isValidEmail(request.getReporterEmail())) {
            errors.add("Invalid reporter email format");
        }

        // Check for potential duplicates
        if (errors.isEmpty()) {
            boolean isDuplicate = checkForDuplicates(request, false);
            if (isDuplicate) {
                errors.add("A similar case may already exist in the database");
            }
        }

        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);

        if (errors.isEmpty()) {
            validation.put("message", "Case data is valid and ready for submission");
        }

        return validation;
    }

    // Private helper methods

    private void validateCaseSubmission(CaseReportDto request, String verifiedEmail, String ipAddress) {
        // Validate required fields
        if (!request.hasAtLeastOneContactMethod()) {
            throw new IllegalArgumentException("At least one contact method (name, email, or phone) is required");
        }

        if (request.getCleanActions() == null || request.getCleanActions().isEmpty()) {
            throw new IllegalArgumentException("Scam type is required");
        }

        if (request.getCleanDescription() == null || request.getCleanDescription().length() < 20) {
            throw new IllegalArgumentException("Description must be at least 20 characters long");
        }

        if (request.getCleanReporterEmail() == null || request.getCleanReporterEmail().isEmpty()) {
            throw new IllegalArgumentException("Reporter email is required");
        }

        // Validate email formats
        if (request.getEmail() != null && !isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid scammer email format");
        }

        if (!isValidEmail(request.getReporterEmail())) {
            throw new IllegalArgumentException("Invalid reporter email format");
        }

        // Check rate limits
        checkRateLimits(verifiedEmail, ipAddress);
    }

    private void checkRateLimits(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();

        // Check email submission limit
        LocalDateTime lastEmailSubmission = emailSubmissionLimit.get(email);
        if (lastEmailSubmission != null &&
                lastEmailSubmission.plusMinutes(MIN_MINUTES_BETWEEN_SUBMISSIONS).isAfter(now)) {
            throw new RuntimeException("Please wait " + MIN_MINUTES_BETWEEN_SUBMISSIONS +
                    " minutes between case submissions");
        }

        // Check IP submission limit
        LocalDateTime lastIpSubmission = ipSubmissionLimit.get(ipAddress);
        if (lastIpSubmission != null &&
                lastIpSubmission.plusHours(1).isAfter(now)) {

            String key = ipAddress + ":" + now.toLocalDate().toString();
            int todayCount = dailySubmissionCount.getOrDefault(key, 0);

            if (todayCount >= MAX_SUBMISSIONS_PER_IP_PER_HOUR) {
                throw new RuntimeException("Too many submissions from this IP address. Please try again later.");
            }
        }

        // Check daily email limit
        String emailKey = email + ":" + now.toLocalDate().toString();
        int emailTodayCount = dailySubmissionCount.getOrDefault(emailKey, 0);
        if (emailTodayCount >= MAX_SUBMISSIONS_PER_EMAIL_PER_DAY) {
            throw new RuntimeException("Daily submission limit reached for this email address");
        }
    }

    private void updateSubmissionLimits(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();

        // Update email submission time
        emailSubmissionLimit.put(email, now);

        // Update IP submission time
        ipSubmissionLimit.put(ipAddress, now);

        // Update daily counters
        String emailKey = email + ":" + now.toLocalDate().toString();
        String ipKey = ipAddress + ":" + now.toLocalDate().toString();

        dailySubmissionCount.merge(emailKey, 1, Integer::sum);
        dailySubmissionCount.merge(ipKey, 1, Integer::sum);

        // Cleanup old entries (keep only last 2 days)
        cleanupOldLimitEntries();
    }

    private void checkForDuplicates(CaseReportDto request) {
        if (checkForDuplicates(request, true)) {
            throw new RuntimeException("A similar case already exists in our database");
        }
    }

    private boolean checkForDuplicates(CaseReportDto request, boolean throwException) {
        // Check for exact email match
        if (request.getCleanEmail() != null) {
            Page<Case> emailMatches = caseRepository.findByEmailContainingIgnoreCase(
                    request.getCleanEmail(), PageRequest.of(0, 1));
            if (!emailMatches.isEmpty()) {
                return true;
            }
        }

        // Check for exact phone match
        if (request.getCleanPhone() != null) {
            Page<Case> phoneMatches = caseRepository.findByPhoneContainingIgnoreCase(
                    request.getCleanPhone(), PageRequest.of(0, 1));
            if (!phoneMatches.isEmpty()) {
                return true;
            }
        }

        // Check for similar name and company combination
        if (request.getCleanName() != null && request.getCleanCompany() != null) {
            // This would need a custom query in production
            // For now, just check name
            Page<Case> nameMatches = caseRepository.findByNameContainingIgnoreCase(
                    request.getCleanName(), PageRequest.of(0, 5));

            for (Case existingCase : nameMatches.getContent()) {
                if (existingCase.getCompany() != null &&
                        existingCase.getCompany().equalsIgnoreCase(request.getCleanCompany())) {
                    return true;
                }
            }
        }

        return false;
    }

    private Case createCaseFromDto(CaseReportDto request, String verifiedEmail) {
        Case newCase = new Case();

        // Set scammer information
        newCase.setName(request.getCleanName());
        newCase.setEmail(request.getCleanEmail());
        newCase.setPhone(request.getCleanPhone());
        newCase.setCompany(request.getCleanCompany());

        // Set scam details
        newCase.setActions(request.getCleanActions());
        newCase.setDescription(request.getCleanDescription());

        // Set reporter information
        newCase.setReportedBy(verifiedEmail);

        // Initialize verdict fields
        newCase.setVerdictScore(0);
        newCase.setTotalVotes(0);
        newCase.setGuiltyVotes(0);
        newCase.setNotGuiltyVotes(0);

        return newCase;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private void cleanupOldLimitEntries() {
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        String cutoffDate = twoDaysAgo.toLocalDate().toString();

        // Remove old email submission limits
        emailSubmissionLimit.entrySet().removeIf(entry ->
                entry.getValue().isBefore(twoDaysAgo));

        // Remove old IP submission limits
        ipSubmissionLimit.entrySet().removeIf(entry ->
                entry.getValue().isBefore(twoDaysAgo));

        // Remove old daily submission counts
        dailySubmissionCount.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split(":");
            return parts.length > 1 && parts[1].compareTo(cutoffDate) < 0;
        });
    }

    private Map<String, Object> buildPaginationInfo(Page<Case> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page.getNumber());
        pagination.put("pageSize", page.getSize());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());
        return pagination;
    }

    /**
     * Get submission rate statistics for monitoring
     */
    public Map<String, Object> getSubmissionRateStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime now = LocalDateTime.now();
        String today = now.toLocalDate().toString();

        // Count submissions today
        long submissionsToday = dailySubmissionCount.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(":" + today))
                .mapToLong(Map.Entry::getValue)
                .sum();

        // Count unique submitters today
        long uniqueSubmittersToday = dailySubmissionCount.entrySet().stream()
                .filter(entry -> entry.getKey().endsWith(":" + today))
                .filter(entry -> !entry.getKey().startsWith("192.168.") &&
                        !entry.getKey().startsWith("10.") &&
                        !entry.getKey().startsWith("172."))
                .count();

        stats.put("submissionsToday", submissionsToday);
        stats.put("uniqueSubmittersToday", uniqueSubmittersToday);
        stats.put("averageSubmissionsPerUser", uniqueSubmittersToday > 0 ?
                (double) submissionsToday / uniqueSubmittersToday : 0.0);

        // Rate limiting stats
        stats.put("emailsInCooldown", emailSubmissionLimit.size());
        stats.put("ipsInCooldown", ipSubmissionLimit.size());

        return stats;
    }

    /**
     * Admin function to reset rate limits for a user
     */
    public boolean resetRateLimitsForEmail(String email) {
        boolean hadLimits = false;

        // Remove email from submission limits
        if (emailSubmissionLimit.remove(email) != null) {
            hadLimits = true;
        }

        // Remove daily submission counts for this email
        String today = LocalDateTime.now().toLocalDate().toString();
        String emailKey = email + ":" + today;
        if (dailySubmissionCount.remove(emailKey) != null) {
            hadLimits = true;
        }

        return hadLimits;
    }

    /**
     * Check if a user can submit a case (without actually validating the case)
     */
    public Map<String, Object> checkSubmissionEligibility(String email, String ipAddress) {
        Map<String, Object> eligibility = new HashMap<>();

        try {
            checkRateLimits(email, ipAddress);
            eligibility.put("canSubmit", true);
            eligibility.put("message", "Ready to submit case");
        } catch (RuntimeException e) {
            eligibility.put("canSubmit", false);
            eligibility.put("message", e.getMessage());

            // Calculate wait time if possible
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastSubmission = emailSubmissionLimit.get(email);
            if (lastSubmission != null) {
                LocalDateTime nextAllowed = lastSubmission.plusMinutes(MIN_MINUTES_BETWEEN_SUBMISSIONS);
                if (nextAllowed.isAfter(now)) {
                    long minutesToWait = java.time.Duration.between(now, nextAllowed).toMinutes();
                    eligibility.put("waitTimeMinutes", minutesToWait);
                }
            }
        }

        return eligibility;
    }

    /**
     * Get case submission trends for admin dashboard
     */
    public Map<String, Object> getSubmissionTrends() {
        Map<String, Object> trends = new HashMap<>();

        // This would typically involve more complex queries
        // For now, return basic information
        trends.put("totalCases", caseRepository.count());
        trends.put("submissionRateStats", getSubmissionRateStats());

        // Recent activity
        Pageable recentPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Case> recentCases = caseRepository.findAll(recentPageable);
        trends.put("recentSubmissions", recentCases.getContent().size());

        return trends;
    }
}