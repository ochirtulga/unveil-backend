package com.unveil.controller;

import com.unveil.entity.Case;
import com.unveil.service.OtpService;
import com.unveil.service.VoteService;
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
public class VoteController {

    private final VoteService voteService;
    private final OtpService otpService;

    public VoteController(VoteService voteService, OtpService otpService) {
        this.voteService = voteService;
        this.otpService = otpService;
    }

    /**
     * Cast a vote on a Case's guilt (WITH EMAIL VERIFICATION SUPPORT)
     * POST /api/v1/case/{id}/vote
     * Body: {"vote": "guilty", "email": "user@example.com"}
     * Headers: Authorization: Bearer <verification-token> (optional, but recommended)
     */

    @PostMapping("/case/{id}/vote")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable Long id,
            @RequestBody Map<String, String> voteRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String vote = voteRequest.get("vote");
            String email = voteRequest.get("email");

            // Validate vote type
            if (vote == null || (!vote.equals("guilty") && !vote.equals("not_guilty"))) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid vote. Must be 'guilty' or 'not_guilty'");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Determine voter identifier
            String voterIdentifier;
            boolean isEmailVerified = false;

            // Check for OTP verification token
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);

                    if (otpService.isTokenValid(token)) {  // Use simplified OTP service
                        String tokenEmail = otpService.getEmailFromToken(token);

                        // If email provided in body, verify it matches token
                        if (email != null && !email.equalsIgnoreCase(tokenEmail)) {
                            Map<String, Object> errorResponse = new HashMap<>();
                            errorResponse.put("error", "Email in request does not match verified email");
                            return ResponseEntity.badRequest().body(errorResponse);
                        }

                        voterIdentifier = "email:" + tokenEmail;
                        isEmailVerified = true;
                    } else {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid or expired verification token. Please verify your email again.");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                    }
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid verification token format");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
            } else if (email != null && !email.trim().isEmpty()) {
                // Email provided but no verification token
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Email verification required. Please verify your email address to vote.");
                errorResponse.put("requiresVerification", true);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            } else {
                // Fall back to IP-based voting
                String ipAddress = clientIp != null ? clientIp : "unknown";
                voterIdentifier = "ip:" + ipAddress;
            }

            // Cast vote
            Case updatedCase = voteService.castVoteWithVerification(id, vote, voterIdentifier, isEmailVerified);

            if (updatedCase == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                return ResponseEntity.notFound().build();
            }

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vote cast successfully");
            response.put("caseId", id);
            response.put("vote", vote);
            response.put("verificationMethod", isEmailVerified ? "email" : "ip");
            response.put("verdict", updatedCase.getVerdictSummary());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Duplicate vote
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "You have already voted on this case");
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
            Optional<Case> caseOpt = voteService.getCaseById(id);

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
            Map<String, Object> stats = voteService.getVerdictStatistics();
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get verdict statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Remove votes from a case (for testing/admin purposes)
     * POST /api/v1/case/{id}/reset-votes
     */
    @PostMapping("/case/{id}/reset-votes")
    public ResponseEntity<Map<String, Object>> resetVotes(@PathVariable Long id) {
        try {
            Case updatedCase = voteService.resetVotes(id);

            if (updatedCase == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All votes reset for case");
            response.put("caseId", id);
            response.put("verdict", updatedCase.getVerdictSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to reset votes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get top voted cases (most community engagement)
     * GET /api/v1/cases/top-voted?page=0&size=10
     */
    @GetMapping("/cases/top-voted")
    public ResponseEntity<Map<String, Object>> getTopVotedCases(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            if (size > 50) size = 50;
            if (size < 1) size = 10;

            Map<String, Object> result = voteService.getTopVotedCases(page, size);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get top voted cases: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get cases needing more votes
     * GET /api/v1/cases/needs-votes?page=0&size=10
     */
    @GetMapping("/cases/needs-votes")
    public ResponseEntity<Map<String, Object>> getCasesNeedingVotes(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        try {
            if (size > 50) size = 50;
            if (size < 1) size = 10;

            Map<String, Object> result = voteService.getCasesNeedingVotes(page, size);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get cases needing votes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get voting activity summary
     * GET /api/v1/votes/activity
     */
    @GetMapping("/votes/activity")
    public ResponseEntity<Map<String, Object>> getVotingActivity() {
        try {
            Map<String, Object> activity = voteService.getVotingActivity();
            return ResponseEntity.ok(activity);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get voting activity: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}