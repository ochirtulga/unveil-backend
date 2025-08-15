package com.unveil.controller;

import com.unveil.entity.User;
import com.unveil.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Configure properly for production
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handle LinkedIn OAuth callback
     * POST /api/v1/auth/linkedin/callback
     */
    @PostMapping("/linkedin/callback")
    public ResponseEntity<Map<String, Object>> handleLinkedInCallback(
            @RequestBody Map<String, String> request) {

        try {
            String code = request.get("code");
            String redirectUri = request.get("redirectUri");

            if (code == null || code.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authorization code is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (redirectUri == null || redirectUri.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Redirect URI is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Process LinkedIn OAuth
            Map<String, Object> authResult = authService.processLinkedInCallback(code, redirectUri);

            return ResponseEntity.ok(authResult);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Verify authentication token
     * GET /api/v1/auth/verify
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid authorization header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            User user = authService.verifyToken(token);

            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Return user data
            Map<String, Object> response = new HashMap<>();
            response.put("user", buildUserResponse(user));
            response.put("authenticated", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Token verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * Logout user
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                authService.invalidateToken(token);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logout successful");
            response.put("authenticated", false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logout completed");
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get current user profile
     * GET /api/v1/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestHeader("Authorization") String authHeader) {

        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authentication required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String token = authHeader.substring(7);
            User user = authService.verifyToken(token);

            if (user == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid authentication");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("user", buildUserResponse(user));

            // Add user statistics
            Map<String, Object> stats = authService.getUserStatistics(user.getId());
            response.put("statistics", stats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get user profile");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get authentication status
     * GET /api/v1/auth/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> response = new HashMap<>();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                User user = authService.verifyToken(token);

                if (user != null) {
                    response.put("authenticated", true);
                    response.put("user", buildUserResponse(user));
                } else {
                    response.put("authenticated", false);
                }
            } catch (Exception e) {
                response.put("authenticated", false);
            }
        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to build user response
     */
    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getLinkedInId());
        userResponse.put("firstName", user.getFirstName());
        userResponse.put("lastName", user.getLastName());
        userResponse.put("email", user.getEmail());
        userResponse.put("profilePicture", user.getProfilePicture());
        userResponse.put("headline", user.getHeadline());
        return userResponse;
    }
}