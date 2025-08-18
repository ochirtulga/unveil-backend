package com.unveil.controller;

import com.unveil.entity.Case;
import com.unveil.service.VoteService;
import com.unveil.service.VerificationService;
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
    private final VerificationService verificationService;

    public VoteController(VoteService voteService, VerificationService verificationService) {
        this.voteService = voteService;
        this.verificationService = verificationService;
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
                errorResponse.put("allowedVotes", List.of("guilty", "not_guilty"));
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Determine voter identifier
            String voterIdentifier;
            boolean isEmailVerified = false;

            // Check for email verification token
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);

                    if (verificationService.isTokenValid(token)) {
                        String tokenEmail = verificationService.getEmailFromToken(token);

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
                // Fall back to IP-based voting (for backward compatibility)
                String ipAddress = clientIp != null ? clientIp : "unknown";
                voterIdentifier = "ip:" + ipAddress;
            }

            // Cast vote with enhanced voter identifier
            Case updatedCase = voteService.castVoteWithVerification(id, vote, voterIdentifier, isEmailVerified);

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
            response.put("verificationMethod", isEmailVerified ? "email" : "ip");
            response.put("verdict", updatedCase.getVerdictSummary());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            // Duplicate vote or other state issue
            Map<String, Object> errorResponse = new HashMap<>();

            if (e.getMessage().contains("already voted")) {
                errorResponse.put("error", "You have already voted on this case");
                errorResponse.put("errorType", "DUPLICATE_VOTE");
            } else {
                errorResponse.put("error", e.getMessage());
            }

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
     * Check if user has voted on a case
     * GET /api/v1/case/{id}/vote-status
     */
    @GetMapping("/case/{id}/vote-status")
    public ResponseEntity<Map<String, Object>> getVoteStatus(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String voterIdentifier;

            // Determine voter identifier same way as voting
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (verificationService.isTokenValid(token)) {
                        String email = verificationService.getEmailFromToken(token);
                        voterIdentifier = "email:" + email;
                    } else {
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid verification token");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                    }
                } catch (Exception e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid token format");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
                }
            } else {
                String ipAddress = clientIp != null ? clientIp : "unknown";
                voterIdentifier = "ip:" + ipAddress;
            }

            boolean hasVoted = voteService.hasVoterVotedOnCase(voterIdentifier, id);

            Map<String, Object> response = new HashMap<>();
            response.put("caseId", id);
            response.put("hasVoted", hasVoted);
            response.put("voterIdentifier", voterIdentifier.startsWith("email:") ? "email" : "ip");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check vote status: " + e.getMessage());
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
     * Bulk vote endpoint for testing purposes
     * POST /api/v1/case/{id}/bulk-vote
     * Body: {"guiltyVotes": 5, "notGuiltyVotes": 2}
     */
    @PostMapping("/case/{id}/bulk-vote")
    public ResponseEntity<Map<String, Object>> bulkVote(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> voteRequest) {

        try {
            Integer guiltyVotes = voteRequest.get("guiltyVotes");
            Integer notGuiltyVotes = voteRequest.get("notGuiltyVotes");

            if (guiltyVotes == null) guiltyVotes = 0;
            if (notGuiltyVotes == null) notGuiltyVotes = 0;

            if (guiltyVotes < 0 || notGuiltyVotes < 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Vote counts cannot be negative");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (guiltyVotes == 0 && notGuiltyVotes == 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "At least one vote count must be greater than 0");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Case updatedCase = voteService.bulkVote(id, guiltyVotes, notGuiltyVotes);

            if (updatedCase == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Case not found");
                errorResponse.put("id", id);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", String.format("Added %d guilty votes and %d not guilty votes",
                    guiltyVotes, notGuiltyVotes));
            response.put("caseId", id);
            response.put("verdict", updatedCase.getVerdictSummary());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to bulk vote: " + e.getMessage());
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