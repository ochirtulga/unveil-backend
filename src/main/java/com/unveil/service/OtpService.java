package com.unveil.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    private SecretKey jwtSecretKey;

    // In-memory storage (use Redis in production)
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> rateLimitMap = new ConcurrentHashMap<>();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.rate-limit-minutes:1}")
    private int rateLimitMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.mail.from:ochronokk@gmail.com}")
    private String fromEmail;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    private void initializeJwtKey() {
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Send OTP to email
     */
    public void sendOtp(String email, String ipAddress) {
        String normalizedEmail = email.toLowerCase().trim();

        // Validate email
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check rate limit
        checkRateLimit(normalizedEmail);

        // Generate OTP
        String otp = generateOtp();

        // Store OTP
        OtpData otpData = new OtpData(otp, LocalDateTime.now().plusMinutes(otpExpiryMinutes), 0);
        otpStorage.put(normalizedEmail, otpData);

        // Send email
        sendOtpEmail(normalizedEmail, otp);

        // Update rate limit
        rateLimitMap.put(normalizedEmail, LocalDateTime.now());

        // Cleanup expired entries
        cleanupExpiredOtps();
    }

    /**
     * Verify OTP and return JWT token
     */
    public String verifyOtp(String email, String otp, String ipAddress) {
        String normalizedEmail = email.toLowerCase().trim();

        OtpData otpData = otpStorage.get(normalizedEmail);

        if (otpData == null) {
            throw new IllegalArgumentException("No OTP found for this email");
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(normalizedEmail);
            throw new IllegalArgumentException("OTP has expired");
        }

        // Check attempts
        if (otpData.attempts >= maxAttempts) {
            otpStorage.remove(normalizedEmail);
            throw new IllegalArgumentException("Too many failed attempts");
        }

        // Verify OTP
        if (!otpData.otp.equals(otp)) {
            otpData.attempts++;
            throw new IllegalArgumentException("Invalid OTP");
        }

        // OTP is valid - remove from storage and generate token
        otpStorage.remove(normalizedEmail);
        return generateToken(normalizedEmail);
    }

    /**
     * Check if JWT token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get email from JWT token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtSecretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }

    // Private helper methods

    private void checkRateLimit(String email) {
        LocalDateTime lastRequest = rateLimitMap.get(email);
        if (lastRequest != null &&
                lastRequest.plusMinutes(rateLimitMinutes).isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Please wait " + rateLimitMinutes + " minute(s) before requesting another OTP");
        }
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String generateToken(String email) {
        Date expirationDate = new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 24 hours

        return Jwts.builder().subject(email).issuedAt(new Date()).expiration(expirationDate)
                .signWith(jwtSecretKey, Jwts.SIG.HS256)
                .compact();
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Your Unveil verification code");
            message.setText(createEmailText(otp));

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    private String createEmailText(String otp) {
        return String.format("""
            Your Unveil verification code is: %s
            
            This code will expire in %d minutes.
            
            If you didn't request this code, please ignore this email.
            
            Best regards,
            Unveil Team
            """, otp, otpExpiryMinutes);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpStorage.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));
        rateLimitMap.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().plusMinutes(rateLimitMinutes * 2L)));
    }

    // Inner class to store OTP data
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;
        int attempts;

        OtpData(String otp, LocalDateTime expiryTime, int attempts) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.attempts = attempts;
        }
    }
}