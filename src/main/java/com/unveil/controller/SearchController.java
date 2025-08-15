package com.unveil.controller;

import com.unveil.entity.Case;
import com.unveil.entity.User;
import com.unveil.service.AuthService;
import com.unveil.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*") // Allow all origins for MVP (configure properly for production)
public class SearchController {

    private final SearchService service;
    private final AuthService authService;

    public SearchController(SearchService searchService, AuthService authService) {
        this.service = searchService;
        this.authService = authService;
    }

    /**
     * Search endpoint with field filtering and pagination
     * GET /api/v1/search?filter=email&value=john@example.com&page=0&size=20
     * GET /api/v1/search?filter=name&value=John&page=0&size=10
     * GET /api/v1/search?filter=phone&value=+1234567890
     * GET /api/v1/search?filter=company&value=Microsoft
     * GET /api/v1/search?filter=all&value=scammer
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam("filter") String filter,
            @RequestParam("value") String value,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        try {
            // Validate input
            if (value == null || value.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Search value cannot be empty");
                errorResponse.put("filter", filter);
                errorResponse.put("supportedFilters", getSupportedFilters());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Limit page size to prevent abuse
            if (size > 100) {
                size = 100;
            }
            if (size < 1) {
                size = 20;
            }

            // Perform search with pagination
            Page<Case> searchResults = service.searchByFilter(filter, value, page, size);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("filter", filter);
            response.put("value", value);
            response.put("results", searchResults.getContent());
            response.put("pagination", buildPaginationInfo(searchResults));
            response.put("found", searchResults.getTotalElements() > 0);
            response.put("message", buildSearchMessage(filter, value, searchResults));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("filter", filter);
            errorResponse.put("value", value);
            errorResponse.put("supportedFilters", getSupportedFilters());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Vote on a Case's guilt (AUTHENTICATION REQUIRED)
     * POST /api/v1/case/{id}/vote
     * Body: {"vote": "guilty"} or {"vote": "not_guilty"}
     * Header: Authorization: Bearer <token>
     */
    @PostMapping("/case/{id}/vote")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable Long id,
            @RequestBody Map<String, String> voteRequest,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Verify authentication
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authentication required");
                errorResponse.put("message", "Please sign in with LinkedIn to vote");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String token = authHeader.substring(7);
            User authenticatedUser = authService.verifyToken(token);

            if (authenticatedUser == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid authentication");
                errorResponse.put("message", "Please sign in again");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String vote = voteRequest.get("vote");

            // Validate vote type
            if (vote == null || (!vote.equals("guilty") && !vote.equals("not_guilty"))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid vote. Must be 'guilty' or 'not_guilty'");
                errorResponse.put("allowedVotes", List.of("guilty", "not_guilty"));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Cast vote (this will handle user authentication and duplicate vote prevention)
            Case updatedCase = service.castAuthenticatedVote(id, vote, authenticatedUser);

            if (updatedCase == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vote cast successfully");
            response.put("caseId", id);
            response.put("vote", vote);
            response.put("verdict", updatedCase.getVerdictSummary());
            response.put("voter", Map.of(
                    "name", authenticatedUser.getFullName(),
                    "id", authenticatedUser.getLinkedInId()
            ));

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // User already voted
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Vote already cast");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to cast vote: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get verdict statistics for a Case
     * GET /api/v1/case/{id}/verdict
     */
    @GetMapping("/case/{id}/verdict")
    public ResponseEntity<Map<String, Object>> getVerdict(@PathVariable Long id) {
        try {
            Optional<Case> caseOpt = service.getCaseById(id);

            if (caseOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            Case caseEntity = caseOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("caseId", id);
            response.put("verdict", caseEntity.getVerdictSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get verdict: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get verdict statistics across all Cases
     * GET /api/v1/verdicts/stats
     */
    @GetMapping("/verdicts/stats")
    public ResponseEntity<Map<String, Object>> getVerdictStats() {
        try {
            Map<String, Object> stats = service.getVerdictStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get verdict statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get most controversial Cases (those with close guilty/not guilty votes)
     * GET /api/v1/cases/controversial?page=0&size=10
     */
    @GetMapping("/cases/controversial")
    public ResponseEntity<Map<String, Object>> getControversialCases(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            if (size > 50) size = 50;
            if (size < 1) size = 10;

            Page<Case> controversialCases = service.getControversialCases(page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("results", controversialCases.getContent());
            response.put("pagination", buildPaginationInfo(controversialCases));
            response.put("message", "Most controversial cases (close votes)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get controversial cases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get Cases by verdict status
     * GET /api/v1/cases/verdict/{status}?page=0&size=20
     * Status can be: guilty, not_guilty, pending
     */
    @GetMapping("/cases/verdict/{status}")
    public ResponseEntity<Map<String, Object>> getCasesByVerdict(
            @PathVariable String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        try {
            // Validate status
            String normalizedStatus = status.toLowerCase().replace("_", " ");
            if (!normalizedStatus.equals("guilty") &&
                    !normalizedStatus.equals("not guilty") &&
                    !normalizedStatus.equals("pending")) {

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid verdict status");
                errorResponse.put("allowedStatuses", List.of("guilty", "not_guilty", "pending"));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (size > 100) size = 100;
            if (size < 1) size = 20;

            Page<Case> cases = service.getCasesByVerdictStatus(normalizedStatus, page, size);

            Map<String, Object> response = new HashMap<>();
            response.put("results", cases.getContent());
            response.put("pagination", buildPaginationInfo(cases));
            response.put("verdictStatus", normalizedStatus);
            response.put("message", String.format("Cases with verdict: %s", normalizedStatus));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get cases by verdict: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get specific Case details by ID
     * GET /api/v1/case/123
     */
    @GetMapping("/case/{id}")
    public ResponseEntity<?> getCase(@PathVariable Long id) {
        Optional<Case> caseEntity = service.getCaseById(id);

        if (caseEntity.isPresent()) {
            return ResponseEntity.ok(caseEntity.get());
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Case not found");
            errorResponse.put("id", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get supported filter types
     * GET /api/v1/search/filters
     */
    @GetMapping("/search/filters")
    public ResponseEntity<Map<String, Object>> getSupportedFilters() {
        Map<String, Object> response = new HashMap<>();
        response.put("supportedFilters", getSupportedFiltersMap());
        response.put("examples", getFilterExamples());
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Unveil Search API");
        response.put("version", "v1");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all available scam types for filtering
     * GET /api/v1/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getActions() {
        List<String> actions = service.getAllActions();

        Map<String, Object> response = new HashMap<>();
        response.put("actions", actions);
        response.put("count", actions.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get database statistics
     * GET /api/v1/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalCases = service.getTotalCasesCount();
        List<String> actions = service.getAllActions();
        Map<String, Object> verdictStats = service.getVerdictStatistics();

        Map<String, Object> response = new HashMap<>();
        response.put("totalCases", totalCases);
        response.put("totalActions", actions.size());
        response.put("verdictStats", verdictStats);
        response.put("status", "Database operational");

        return ResponseEntity.ok(response);
    }

    // Helper methods
    private List<String> getSupportedFiltersMap() {
        return List.of("name", "email", "phone", "company", "actions", "all");
    }

    private Map<String, String> getFilterExamples() {
        Map<String, String> examples = new HashMap<>();
        examples.put("name", "/api/v1/search?filter=name&value=John");
        examples.put("email", "/api/v1/search?filter=email&value=john@scammer.com");
        examples.put("phone", "/api/v1/search?filter=phone&value=+1234567890");
        examples.put("company", "/api/v1/search?filter=company&value=Microsoft");
        examples.put("actions", "/api/v1/search?filter=actions&value=Tech Support");
        examples.put("all", "/api/v1/search?filter=all&value=scammer");
        return examples;
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

    private String buildSearchMessage(String filter, String value, Page<Case> results) {
        long totalResults = results.getTotalElements();

        if (totalResults == 0) {
            return String.format("No results found for %s: %s", filter, value);
        } else if (totalResults == 1) {
            return String.format("1 result found for %s: %s", filter, value);
        } else {
            return String.format("%d results found for %s: %s", totalResults, filter, value);
        }
    }
}