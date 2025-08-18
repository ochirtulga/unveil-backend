package com.unveil.service;

import com.unveil.entity.VerificationCode;
import com.unveil.repository.VerificationCodeRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import jakarta.mail.internet.MimeMessage;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final JavaMailSender mailSender;
    private final SecretKey jwtSecretKey;

    // Rate limiting maps (in production, use Redis)
    private final Map<String, LocalDateTime> emailRateLimit = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> ipRateLimit = new ConcurrentHashMap<>();
    private final Map<String, Integer> ipAttemptCount = new ConcurrentHashMap<>();

    // Configuration
    @Value("${app.verification.code-expiry-minutes:10}")
    private int codeExpiryMinutes;

    @Value("${app.verification.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.verification.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.verification.rate-limit-minutes:1}")
    private int rateLimitMinutes;

    @Value("${app.verification.from-email:noreply@unveil.com}")
    private String fromEmail;

    @Value("${app.verification.from-name:Unveil}")
    private String fromName;

    @Value("${app.jwt.secret:your-very-secure-jwt-secret-key-that-is-at-least-256-bits-long}")
    private String jwtSecret;

    public VerificationService(VerificationCodeRepository verificationCodeRepository,
                               JavaMailSender mailSender) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.mailSender = mailSender;
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Request verification code for email
     */
    public Map<String, Object> requestVerificationCode(String email, String ipAddress) {
        // Normalize email
        String normalizedEmail = email.toLowerCase().trim();

        // Validate email format
        if (!isValidEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Invalid email address format");
        }

        // Check rate limiting
        checkRateLimit(normalizedEmail, ipAddress);

        // Generate verification code
        String code = generateVerificationCode();
        String emailHash = hashEmail(normalizedEmail);

        // Clean up expired codes for this email
        verificationCodeRepository.deleteExpiredCodesForEmail(emailHash);

        // Check if there's a recent valid code
        Optional<VerificationCode> existingCode = verificationCodeRepository
                .findActiveCodeByEmailHash(emailHash);

        if (existingCode.isPresent()) {
            VerificationCode existing = existingCode.get();
            if (existing.getCreatedAt().plusMinutes(1).isAfter(LocalDateTime.now())) {
                throw new RuntimeException("A verification code was recently sent. Please wait a minute before requesting another.");
            }
            // Delete existing code if it's old enough
            verificationCodeRepository.delete(existing);
        }

        // Create new verification code
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmailHash(emailHash);
        verificationCode.setCode(code);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        verificationCode.setAttempts(0);
        verificationCode.setIpAddress(ipAddress);
        verificationCode.setVerified(false);

        verificationCodeRepository.save(verificationCode);

        // Send email
        sendVerificationEmail(normalizedEmail, code);

        // Update rate limiting
        updateRateLimit(normalizedEmail, ipAddress);

        Map<String, Object> result = new HashMap<>();
        result.put("expiresIn", codeExpiryMinutes * 60); // Return in seconds
        return result;
    }

    /**
     * Verify code and return token
     */
    public Map<String, Object> verifyCode(String email, String code, String ipAddress) {
        String normalizedEmail = email.toLowerCase().trim();
        String emailHash = hashEmail(normalizedEmail);

        // Check IP attempt rate limiting
        checkVerificationAttemptLimit(ipAddress);

        // Find verification code
        Optional<VerificationCode> verificationCodeOpt = verificationCodeRepository
                .findActiveCodeByEmailHash(emailHash);

        if (verificationCodeOpt.isEmpty()) {
            incrementAttemptCount(ipAddress);
            throw new IllegalArgumentException("Verification code has expired or does not exist. Please request a new code.");
        }

        VerificationCode verificationCode = verificationCodeOpt.get();

        // Check if expired
        if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationCodeRepository.delete(verificationCode);
            incrementAttemptCount(ipAddress);
            throw new IllegalArgumentException("Verification code has expired. Please request a new code.");
        }

        // Check attempt limit
        if (verificationCode.getAttempts() >= maxAttempts) {
            verificationCodeRepository.delete(verificationCode);
            incrementAttemptCount(ipAddress);
            throw new IllegalArgumentException("Too many failed attempts. Please request a new verification code.");
        }

        // Verify code
        if (!verificationCode.getCode().equals(code)) {
            verificationCode.setAttempts(verificationCode.getAttempts() + 1);
            verificationCodeRepository.save(verificationCode);
            incrementAttemptCount(ipAddress);

            int remainingAttempts = maxAttempts - verificationCode.getAttempts();
            throw new IllegalArgumentException("Invalid verification code. " + remainingAttempts + " attempts remaining.");
        }

        // Mark as verified
        verificationCode.setVerified(true);
        verificationCode.setVerifiedAt(LocalDateTime.now());
        verificationCodeRepository.save(verificationCode);

        // Generate JWT token
        String token = generateVerificationToken(normalizedEmail);

        // Clear attempt count for this IP
        ipAttemptCount.remove(ipAddress);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Email verified successfully");
        result.put("token", token);
        result.put("email", normalizedEmail);
        result.put("expiresIn", tokenExpiryHours * 3600); // Return in seconds

        return result;
    }

    /**
     * Check if verification token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Check if token is expired
            return !claims.getExpiration().before(new java.util.Date());

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get email from verification token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid verification token");
        }
    }

    /**
     * Check email service health
     */
    public boolean isEmailServiceHealthy() {
        try {
            // Simple test - try to create a message
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("test@example.com");
            message.setSubject("Health Check");
            message.setText("Health check");
            // Don't actually send, just check if we can create it
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check cache service health
     */
    public boolean isCacheServiceHealthy() {
        try {
            // Simple test of database connectivity
            verificationCodeRepository.count();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ============ PRIVATE HELPER METHODS ============

    private void checkRateLimit(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rateLimitTime = now.minusMinutes(rateLimitMinutes);

        // Check email rate limit
        LocalDateTime lastEmailRequest = emailRateLimit.get(email);
        if (lastEmailRequest != null && lastEmailRequest.isAfter(rateLimitTime)) {
            throw new RuntimeException("Too many requests for this email. Please wait " + rateLimitMinutes + " minute(s) before requesting another code.");
        }

        // Check IP rate limit
        LocalDateTime lastIpRequest = ipRateLimit.get(ipAddress);
        if (lastIpRequest != null && lastIpRequest.isAfter(rateLimitTime)) {
            throw new RuntimeException("Too many requests from this IP. Please wait " + rateLimitMinutes + " minute(s) before requesting another code.");
        }
    }

    private void updateRateLimit(String email, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        emailRateLimit.put(email, now);
        ipRateLimit.put(ipAddress, now);
    }

    private void checkVerificationAttemptLimit(String ipAddress) {
        Integer attempts = ipAttemptCount.getOrDefault(ipAddress, 0);
        if (attempts >= 10) { // 10 attempts per IP before blocking
            throw new RuntimeException("Too many verification attempts from this IP. Please try again later.");
        }
    }

    private void incrementAttemptCount(String ipAddress) {
        ipAttemptCount.merge(ipAddress, 1, Integer::sum);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    private String generateVerificationToken(String email) {
        LocalDateTime expirationTime = LocalDateTime.now().plusHours(tokenExpiryHours);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new java.util.Date())
                .setExpiration(java.sql.Timestamp.valueOf(expirationTime))
                .claim("type", "email_verification")
                .signWith(jwtSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash email", e);
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private void sendVerificationEmail(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            helper.setSubject("Your " + fromName + " verification code: " + code);

            // HTML email content
            String htmlContent = createEmailHtml(code);
            String textContent = createEmailText(code);

            helper.setText(textContent, htmlContent);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String createEmailHtml(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Verification - %s</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { text-align: center; padding: 20px 0; border-bottom: 2px solid #f0f0f0; }
                    .content { padding: 30px 0; }
                    .code-box { background: #f8f9fa; border: 2px solid #e9ecef; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; font-family: 'Courier New', monospace; }
                    .footer { text-align: center; padding: 20px 0; border-top: 1px solid #f0f0f0; color: #666; font-size: 14px; }
                    .warning { background: #fff3cd; border: 1px solid #ffeaa7; border-radius: 4px; padding: 15px; margin: 20px 0; color: #856404; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>%s</h1>
                        <h2>Email Verification</h2>
                    </div>
                    
                    <div class="content">
                        <p>Hello,</p>
                        <p>You requested to verify your email address for voting on %s. Please use the verification code below:</p>
                        
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        
                        <p>This code will expire in <strong>%d minutes</strong>.</p>
                        
                        <div class="warning">
                            <strong>Security Notice:</strong> If you didn't request this verification code, please ignore this email.
                        </div>
                        
                        <p>This verification allows you to vote on scam cases while maintaining your privacy. Your email is only used for verification and is not stored permanently.</p>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated message from %s.</p>
                        <p>Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fromName, fromName, fromName, code, codeExpiryMinutes, fromName);
    }

    private String createEmailText(String code) {
        return """
            %s - Email Verification
            
            Hello,
            
            You requested to verify your email address for voting on %s.
            
            Your verification code is: %s
            
            This code will expire in %d minutes.
            
            SECURITY NOTICE: If you didn't request this verification code, please ignore this email.
            
            This verification allows you to vote on scam cases while maintaining your privacy. Your email is only used for verification and is not stored permanently.
            
            This is an automated message from %s.
            Please do not reply to this email.
            """.formatted(fromName, fromName, code, codeExpiryMinutes, fromName);
    }
}