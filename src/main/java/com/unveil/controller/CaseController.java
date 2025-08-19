package com.unveil.controller;

import com.unveil.dto.CaseReportDto;
import com.unveil.entity.Case;
import com.unveil.service.CaseService;
import com.unveil.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/case")
@CrossOrigin(origins = "*") // Configure properly for production
public class CaseController {

    private final CaseService caseService;
    private final OtpService otpService;

    public CaseController(CaseService caseService, OtpService otpService) {
        this.caseService = caseService;
        this.otpService = otpService;
    }

    /**
     * Submit a new case
     * POST /api/v1/case/submit
     */
    @PostMapping("/submit")
        public ResponseEntity<Map<String, Object>> submitCase(
            @Valid @RequestBody CaseReportDto request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            // Verify email if token provided
            boolean isEmailVerified = false;
            String verifiedEmail = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (otpService.isTokenValid(token)) {
                        verifiedEmail = otpService.getEmailFromToken(token);
                        isEmailVerified = true;

                        // Verify the email in the request matches the verified email
                        if (!request.getReporterEmail().equalsIgnoreCase(verifiedEmail)) {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("error", "Reporter email does not match verified email");
                            return ResponseEntity.badRequest().body(errorResponse);
                        }
                    }
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid verification token");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
            }

            // Email verification is required for case submission
            if (!isEmailVerified) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email verification required to submit case reports");
                errorResponse.put("requiresVerification", true);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Submit the case
            String ipAddress = clientIp != null ? clientIp : "unknown";
            Case newCase = caseService.submitCase(request, verifiedEmail, ipAddress);

            // Build success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Case submitted successfully");
            response.put("caseId", newCase.getId());
            response.put("submittedBy", verifiedEmail);
            response.put("case", buildCaseResponse(newCase));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("duplicate") || e.getMessage().contains("already exists")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "A similar case already exists in our database");
                errorResponse.put("errorType", "DUPLICATE_CASE");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            if (e.getMessage().contains("rate limit")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to submit case: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred while submitting the case");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get specific Case details by ID
     * GET /api/v1/case/123
     */
    @GetMapping("/case/{id}")
    public ResponseEntity<?> getCase(@PathVariable Long id) {
        Optional<Case> caseEntity = caseService.getCaseById(id);

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
     * Update a case (for admin/moderation purposes)
     * PUT /api/v1/case/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseReportDto request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // For now, allow updates without strict admin verification
            // In production, implement proper admin role checking

            Optional<Case> caseOpt = caseService.getCaseById(id);

            if (caseOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            Case updatedCase = caseService.updateCase(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Case updated successfully");
            response.put("case", buildCaseResponse(updatedCase));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update case: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a case (admin only)
     * DELETE /api/v1/case/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCase(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // For now, allow deletion without strict admin verification
            // In production, implement proper admin role checking

            boolean deleted = caseService.deleteCase(id);

            if (!deleted) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Case deleted successfully");
            response.put("id", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete case: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get case submission statistics
     * GET /api/v1/case/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCaseStats() {
        try {
            Map<String, Object> stats = caseService.getCaseStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get case statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get recent case submissions
     * GET /api/v1/case/recent?page=0&size=10
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentCases(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            if (size > 50) size = 50; // Limit page size
            if (size < 1) size = 10;

            Map<String, Object> result = caseService.getRecentCases(page, size);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get recent cases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Search cases by reporter email (admin feature)
     * GET /api/v1/case/by-reporter?email=example@email.com
     */
    @GetMapping("/by-reporter")
    public ResponseEntity<Map<String, Object>> getCasesByReporter(
            @RequestParam String email,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            if (size > 50) size = 50;
            if (size < 1) size = 10;

            Map<String, Object> result = caseService.getCasesByReporter(email, page, size);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get cases by reporter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Flag a case for review (community moderation)
     * POST /api/v1/case/{id}/flag
     */
    @PostMapping("/{id}/flag")
    public ResponseEntity<Map<String, Object>> flagCase(
            @PathVariable Long id,
            @RequestBody Map<String, String> flagRequest,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String reason = flagRequest.get("reason");
            String description = flagRequest.get("description");

            if (reason == null || reason.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Flag reason is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String ipAddress = clientIp != null ? clientIp : "unknown";
            boolean flagged = caseService.flagCase(id, reason, description, ipAddress);

            if (!flagged) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found or already flagged");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Case flagged for review");
            response.put("caseId", id);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to flag case: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate case data before submission
     * POST /api/v1/case/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCase(
            @Valid @RequestBody CaseReportDto request) {

        try {
            Map<String, Object> validation = caseService.validateCaseData(request);
            return ResponseEntity.ok(validation);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Validation failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    // Helper method to build case response
    private Map<String, Object> buildCaseResponse(Case caseEntity) {
        Map<String, Object> caseResponse = new HashMap<>();
        caseResponse.put("id", caseEntity.getId());
        caseResponse.put("name", caseEntity.getName());
        caseResponse.put("email", caseEntity.getEmail());
        caseResponse.put("phone", caseEntity.getPhone());
        caseResponse.put("company", caseEntity.getCompany());
        caseResponse.put("description", caseEntity.getDescription());
        caseResponse.put("actions", caseEntity.getActions());
        caseResponse.put("reportedBy", caseEntity.getReportedBy());
        caseResponse.put("createdAt", caseEntity.getCreatedAt().toString());

        // Verdict information
        caseResponse.put("verdictScore", caseEntity.getVerdictScore());
        caseResponse.put("totalVotes", caseEntity.getTotalVotes());
        caseResponse.put("guiltyVotes", caseEntity.getGuiltyVotes());
        caseResponse.put("notGuiltyVotes", caseEntity.getNotGuiltyVotes());
        caseResponse.put("lastVotedAt", caseEntity.getLastVotedAt() != null ?
                caseEntity.getLastVotedAt().toString() : null);
        caseResponse.put("verdictSummary", caseEntity.getVerdictSummary());

        return caseResponse;
    }
}