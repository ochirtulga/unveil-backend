package com.unveil.controller;

import com.unveil.dto.VerificationRequestDto;
import com.unveil.dto.VerificationVerifyDto;
import com.unveil.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification")
@CrossOrigin(origins = "*") // Configure properly for production
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Request email verification code
     * POST /api/v1/verification/request
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestVerificationCode(
            @Valid @RequestBody VerificationRequestDto request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            // Use client IP for rate limiting
            String ipAddress = clientIp != null ? clientIp : "unknown";

            Map<String, Object> result = verificationService.requestVerificationCode(
                    request.getEmail(),
                    ipAddress
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification code sent successfully");
            response.put("expiresIn", result.get("expiresIn"));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("rate limit") || e.getMessage().contains("too many")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to send verification code. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Email service temporarily unavailable. Please try again later.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Verify email code and return token
     * POST /api/v1/verification/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @Valid @RequestBody VerificationVerifyDto request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String ipAddress = clientIp != null ? clientIp : "unknown";

            Map<String, Object> result = verificationService.verifyCode(
                    request.getEmail(),
                    request.getCode(),
                    ipAddress
            );

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            // Check if it's a specific error type
            if (e.getMessage().contains("expired")) {
                errorResponse.put("errorType", "EXPIRED");
            } else if (e.getMessage().contains("invalid")) {
                errorResponse.put("errorType", "INVALID_CODE");
            } else if (e.getMessage().contains("attempts")) {
                errorResponse.put("errorType", "TOO_MANY_ATTEMPTS");
            }

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("rate limit")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Verification failed. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Check verification status
     * GET /api/v1/verification/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getVerificationStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> response = new HashMap<>();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                boolean isValid = verificationService.isTokenValid(token);

                if (isValid) {
                    String email = verificationService.getEmailFromToken(token);
                    response.put("verified", true);
                    response.put("email", email);
                } else {
                    response.put("verified", false);
                }
            } catch (Exception e) {
                response.put("verified", false);
            }
        } else {
            response.put("verified", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Resend verification code
     * POST /api/v1/verification/resend
     */
    @PostMapping("/resend")
    public ResponseEntity<Map<String, Object>> resendVerificationCode(
            @Valid @RequestBody VerificationRequestDto request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        // Same logic as request but with different rate limiting
        return requestVerificationCode(request, clientIp);
    }

    /**
     * Health check for verification service
     * GET /api/v1/verification/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean emailServiceHealthy = verificationService.isEmailServiceHealthy();
            boolean cacheServiceHealthy = verificationService.isCacheServiceHealthy();

            response.put("status", emailServiceHealthy && cacheServiceHealthy ? "UP" : "DEGRADED");
            response.put("emailService", emailServiceHealthy ? "UP" : "DOWN");
            response.put("cacheService", cacheServiceHealthy ? "UP" : "DOWN");
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", "Health check failed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}