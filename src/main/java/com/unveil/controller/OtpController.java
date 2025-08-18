package com.unveil.controller;

import com.unveil.service.OtpService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/otp")
@CrossOrigin(origins = "*")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Send OTP to email
     * POST /api/v1/otp/send
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String ipAddress = clientIp != null ? clientIp : "unknown";

            otpService.sendOtp(request.getEmail(), ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP sent successfully");
            response.put("email", request.getEmail());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("rate limit") || e.getMessage().contains("too many")) {
                return buildErrorResponse(e.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
            }
            System.out.println(e.getMessage());
            return buildErrorResponse("Failed to send OTP", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify OTP
     * POST /api/v1/otp/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String clientIp) {

        try {
            String ipAddress = clientIp != null ? clientIp : "unknown";

            String token = otpService.verifyOtp(request.getEmail(), request.getOtp(), ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "OTP verified successfully");
            response.put("token", token);
            response.put("email", request.getEmail());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return buildErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return buildErrorResponse("OTP verification failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Validate token
     * GET /api/v1/otp/validate?token=xxx
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestParam String token) {

        try {
            boolean isValid = otpService.isTokenValid(token);
            String email = isValid ? otpService.getEmailFromToken(token) : null;

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            if (isValid) {
                response.put("email", email);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            return ResponseEntity.ok(response);
        }
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @Data
    public static class SendOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        private String otp;
    }
}