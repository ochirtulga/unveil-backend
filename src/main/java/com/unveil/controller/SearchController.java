package com.unveil.controller;

import com.unveil.entity.BadActor;
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

    public  SearchController(SearchService searchService) {
        this.service = searchService;
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
            Page<BadActor> searchResults = service.searchByFilter(filter, value, page, size);

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
        response.put("service", "ScamGuard Search API");
        response.put("version", "v1");
        response.put("timestamp", java.time.LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * Get list of supported filters
     */
    private List<String> getSupportedFiltersMap() {
        return List.of("name", "email", "phone", "company", "actions", "all");
    }

    /**
     * Get examples for each filter
     */
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

    /**
     * Build pagination information
     */
    private Map<String, Object> buildPaginationInfo(Page<BadActor> page) {
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
     * Build search result message
     */
    private String buildSearchMessage(String filter, String value, Page<BadActor> results) {
        long totalResults = results.getTotalElements();

        if (totalResults == 0) {
            return String.format("No results found for %s: %s", filter, value);
        } else if (totalResults == 1) {
            return String.format("1 result found for %s: %s", filter, value);
        } else {
            return String.format("%d results found for %s: %s", totalResults, filter, value);
        }
    }

    /**
     * Get specific BadActor details by ID
     * GET /api/BadActor/123
     */
    @GetMapping("/BadActor/{id}")
    public ResponseEntity<?> getBadActor(@PathVariable Long id) {
        Optional<BadActor> BadActor = service.getBadActorById(id);

        if (BadActor.isPresent()) {
            return ResponseEntity.ok(BadActor.get());
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "BadActor not found");
            errorResponse.put("id", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all available scam types for filtering
     * GET /api/scam-types
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
     * GET /api/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalBadActors = service.getTotalBadActorsCount();
        List<String> actions = service.getAllActions();

        Map<String, Object> response = new HashMap<>();
        response.put("totalBadActors", totalBadActors);
        response.put("totalActions", actions.size());
        response.put("status", "Database operational");

        return ResponseEntity.ok(response);
    }
}