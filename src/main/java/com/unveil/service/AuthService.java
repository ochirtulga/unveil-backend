//package com.unveil.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.unveil.entity.User;
//import com.unveil.repository.UserRepository;
//import com.unveil.repository.VoteRepository;
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//
//import javax.crypto.SecretKey;
//import java.util.*;
//
//@Service
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final VoteRepository voteRepository;
//    private final RestTemplate restTemplate;
//    private final ObjectMapper objectMapper;
//    private final SecretKey jwtSecretKey;
//
//    // LinkedIn OAuth configuration
//    @Value("${linkedin.client.id:your-linkedin-client-id}")
//    private String linkedInClientId;
//
//    @Value("${linkedin.client.secret:your-linkedin-client-secret}")
//    private String linkedInClientSecret;
//
//    // JWT configuration
//    @Value("${jwt.secret:your-very-secure-jwt-secret-key-that-is-at-least-256-bits-long}")
//    private String jwtSecret;
//
//    @Value("${jwt.expiration:86400}") // 24 hours in seconds
//    private Long jwtExpiration;
//
//    // Token blacklist (in production, use Redis or database)
//    private final Set<String> tokenBlacklist = new HashSet<>();
//
//    public AuthService(UserRepository userRepository, VoteRepository voteRepository) {
//        this.userRepository = userRepository;
//        this.voteRepository = voteRepository;
//        this.restTemplate = new RestTemplate();
//        this.objectMapper = new ObjectMapper();
//        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
//    }
//
//    /**
//     * Process LinkedIn OAuth callback
//     */
//    public Map<String, Object> processLinkedInCallback(String code, String redirectUri) throws Exception {
//        // Step 1: Exchange authorization code for access token
//        String accessToken = exchangeCodeForToken(code, redirectUri);
//
//        // Step 2: Get user profile from LinkedIn
//        Map<String, Object> userProfile = getLinkedInUserProfile(accessToken);
//
//        // Step 3: Create or update user in database
//        User user = createOrUpdateUser(userProfile);
//
//        // Step 4: Generate JWT token
//        String jwtToken = generateJwtToken(user);
//
//        // Step 5: Build response
//        Map<String, Object> response = new HashMap<>();
//        response.put("token", jwtToken);
//        response.put("user", buildUserResponse(user));
//        response.put("authenticated", true);
//
//        return response;
//    }
//
//    /**
//     * Exchange authorization code for access token
//     */
//    private String exchangeCodeForToken(String code, String redirectUri) throws Exception {
//        String tokenUrl = "https://www.linkedin.com/oauth/v2/accessToken";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("grant_type", "authorization_code");
//        body.add("code", code);
//        body.add("redirect_uri", redirectUri);
//        body.add("client_id", linkedInClientId);
//        body.add("client_secret", linkedInClientSecret);
//
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
//
//        ResponseEntity<String> response = restTemplate.exchange(
//                tokenUrl, HttpMethod.POST, request, String.class);
//
//        if (response.getStatusCode() != HttpStatus.OK) {
//            throw new RuntimeException("Failed to exchange code for token");
//        }
//
//        JsonNode jsonResponse = objectMapper.readTree(response.getBody());
//        return jsonResponse.get("access_token").asText();
//    }
//
//    /**
//     * Get user profile from LinkedIn
//     */
//    private Map<String, Object> getLinkedInUserProfile(String accessToken) throws Exception {
//        // Get basic profile
//        String profileUrl = "https://api.linkedin.com/v2/people/~?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(accessToken);
//        HttpEntity<String> request = new HttpEntity<>(headers);
//
//        ResponseEntity<String> profileResponse = restTemplate.exchange(
//                profileUrl, HttpMethod.GET, request, String.class);
//
//        if (profileResponse.getStatusCode() != HttpStatus.OK) {
//            throw new RuntimeException("Failed to get user profile from LinkedIn");
//        }
//
//        JsonNode profileData = objectMapper.readTree(profileResponse.getBody());
//
//        // Get email address
//        String emailUrl = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";
//        ResponseEntity<String> emailResponse = restTemplate.exchange(
//                emailUrl, HttpMethod.GET, request, String.class);
//
//        String email = null;
//        if (emailResponse.getStatusCode() == HttpStatus.OK) {
//            JsonNode emailData = objectMapper.readTree(emailResponse.getBody());
//            if (emailData.has("elements") && emailData.get("elements").isArray() &&
//                    emailData.get("elements").size() > 0) {
//                email = emailData.get("elements").get(0)
//                        .get("handle~").get("emailAddress").asText();
//            }
//        }
//
//        // Build user profile map
//        Map<String, Object> userProfile = new HashMap<>();
//        userProfile.put("id", profileData.get("id").asText());
//        userProfile.put("firstName", profileData.get("firstName").get("localized").get("en_US").asText());
//        userProfile.put("lastName", profileData.get("lastName").get("localized").get("en_US").asText());
//        userProfile.put("email", email);
//
//        // Extract profile picture if available
//        if (profileData.has("profilePicture") &&
//                profileData.get("profilePicture").has("displayImage~") &&
//                profileData.get("profilePicture").get("displayImage~").has("elements")) {
//
//            JsonNode pictures = profileData.get("profilePicture").get("displayImage~").get("elements");
//            if (pictures.isArray() && pictures.size() > 0) {
//                String profilePicture = pictures.get(0).get("identifiers").get(0).get("identifier").asText();
//                userProfile.put("profilePicture", profilePicture);
//            }
//        }
//
//        return userProfile;
//    }
//
//    /**
//     * Create or update user in database
//     */
//    private User createOrUpdateUser(Map<String, Object> userProfile) {
//        String linkedInId = (String) userProfile.get("id");
//
//        Optional<User> existingUser = userRepository.findByLinkedInId(linkedInId);
//
//        User user;
//        if (existingUser.isPresent()) {
//            // Update existing user
//            user = existingUser.get();
//            user.updateFromLinkedIn(
//                    (String) userProfile.get("firstName"),
//                    (String) userProfile.get("lastName"),
//                    (String) userProfile.get("email"),
//                    (String) userProfile.get("profilePicture"),
//                    (String) userProfile.get("headline")
//            );
//        } else {
//            // Create new user
//            user = new User();
//            user.setLinkedInId(linkedInId);
//            user.setFirstName((String) userProfile.get("firstName"));
//            user.setLastName((String) userProfile.get("lastName"));
//            user.setEmail((String) userProfile.get("email"));
//            user.setProfilePicture((String) userProfile.get("profilePicture"));
//            user.setHeadline((String) userProfile.get("headline"));
//            user.setIsActive(true);
//            user.updateLoginTime();
//        }
//
//        return userRepository.save(user);
//    }
//
//    /**
//     * Generate JWT token for user
//     */
//    private String generateJwtToken(User user) {
//        Date expirationDate = new Date(System.currentTimeMillis() + jwtExpiration * 1000);
//
//        return Jwts.builder()
//                .setSubject(user.getLinkedInId())
//                .setIssuedAt(new Date())
//                .setExpiration(expirationDate)
//                .claim("userId", user.getId())
//                .claim("email", user.getEmail())
//                .claim("firstName", user.getFirstName())
//                .claim("lastName", user.getLastName())
//                .signWith(jwtSecretKey, SignatureAlgorithm.HS256)
//                .compact();
//    }
//
//    /**
//     * Verify JWT token and return user
//     */
//    public User verifyToken(String token) {
//        try {
//            // Check if token is blacklisted
//            if (tokenBlacklist.contains(token)) {
//                return null;
//            }
//
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(jwtSecretKey)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//
//            String linkedInId = claims.getSubject();
//            return userRepository.findByLinkedInId(linkedInId).orElse(null);
//
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    /**
//     * Invalidate token (logout)
//     */
//    public void invalidateToken(String token) {
//        tokenBlacklist.add(token);
//    }
//
//    /**
//     * Get user statistics
//     */
//    public Map<String, Object> getUserStatistics(Long userId) {
//        Long voteCount = voteRepository.countByUserId(userId);
//
//        Map<String, Object> stats = new HashMap<>();
//        stats.put("totalVotes", voteCount);
//        stats.put("joinDate", userRepository.findById(userId)
//                .map(user -> user.getCreatedAt().toString())
//                .orElse(null));
//
//        return stats;
//    }
//
//    /**
//     * Build user response for API
//     */
//    private Map<String, Object> buildUserResponse(User user) {
//        Map<String, Object> userResponse = new HashMap<>();
//        userResponse.put("id", user.getLinkedInId());
//        userResponse.put("firstName", user.getFirstName());
//        userResponse.put("lastName", user.getLastName());
//        userResponse.put("email", user.getEmail());
//        userResponse.put("profilePicture", user.getProfilePicture());
//        userResponse.put("headline", user.getHeadline());
//        return userResponse;
//    }
//}