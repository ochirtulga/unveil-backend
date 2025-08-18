// src/main/java/com/unveil/repository/VerificationCodeRepository.java
package com.unveil.repository;

import com.unveil.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    /**
     * Find active (non-expired, non-verified) verification code by email hash
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.emailHash = :emailHash " +
            "AND vc.expiresAt > :now AND vc.verified = false " +
            "ORDER BY vc.createdAt DESC")
    Optional<VerificationCode> findActiveCodeByEmailHash(
            @Param("emailHash") String emailHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Find active verification code by email hash (using current time)
     */
    default Optional<VerificationCode> findActiveCodeByEmailHash(String emailHash) {
        return findActiveCodeByEmailHash(emailHash, LocalDateTime.now());
    }

    /**
     * Find verification code by email hash and code
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.emailHash = :emailHash " +
            "AND vc.code = :code AND vc.expiresAt > :now")
    Optional<VerificationCode> findByEmailHashAndCode(
            @Param("emailHash") String emailHash,
            @Param("code") String code,
            @Param("now") LocalDateTime now
    );

    /**
     * Check if email hash has recent verification attempts
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc WHERE vc.emailHash = :emailHash " +
            "AND vc.createdAt > :since")
    Long countRecentAttemptsByEmailHash(
            @Param("emailHash") String emailHash,
            @Param("since") LocalDateTime since
    );

    /**
     * Check if IP has recent verification attempts
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc WHERE vc.ipAddress = :ipAddress " +
            "AND vc.createdAt > :since")
    Long countRecentAttemptsByIp(
            @Param("ipAddress") String ipAddress,
            @Param("since") LocalDateTime since
    );

    /**
     * Find all expired verification codes
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.expiresAt <= :now")
    Iterable<VerificationCode> findExpiredCodes(@Param("now") LocalDateTime now);

    /**
     * Delete expired verification codes
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCode vc WHERE vc.expiresAt <= :now")
    int deleteExpiredCodes(@Param("now") LocalDateTime now);

    /**
     * Delete expired codes for specific email hash
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationCode vc WHERE vc.emailHash = :emailHash " +
            "AND vc.expiresAt <= :now")
    int deleteExpiredCodesForEmail(
            @Param("emailHash") String emailHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Delete expired codes for specific email hash (using current time)
     */
    @Modifying
    @Transactional
    default int deleteExpiredCodesForEmail(String emailHash) {
        return deleteExpiredCodesForEmail(emailHash, LocalDateTime.now());
    }

    /**
     * Delete all codes for specific email hash (cleanup)
     */
    @Modifying
    @Transactional
    void deleteByEmailHash(String emailHash);

    /**
     * Find verification codes by IP address (for abuse detection)
     */
    @Query("SELECT vc FROM VerificationCode vc WHERE vc.ipAddress = :ipAddress " +
            "ORDER BY vc.createdAt DESC")
    Iterable<VerificationCode> findByIpAddress(@Param("ipAddress") String ipAddress);

    /**
     * Count verified codes for statistics
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc WHERE vc.verified = true")
    Long countVerifiedCodes();

    /**
     * Count total verification attempts in time period
     */
    @Query("SELECT COUNT(vc) FROM VerificationCode vc WHERE vc.createdAt >= :since")
    Long countAttemptsSince(@Param("since") LocalDateTime since);

    /**
     * Get verification statistics
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN vc.verified = true THEN 1 END) as verifiedCount, " +
            "COUNT(CASE WHEN vc.verified = false AND vc.expiresAt > :now THEN 1 END) as pendingCount, " +
            "COUNT(CASE WHEN vc.expiresAt <= :now THEN 1 END) as expiredCount, " +
            "COUNT(vc) as totalCount " +
            "FROM VerificationCode vc")
    VerificationStatistics getVerificationStatistics(@Param("now") LocalDateTime now);

    /**
     * Interface for verification statistics projection
     */
    interface VerificationStatistics {
        Long getVerifiedCount();
        Long getPendingCount();
        Long getExpiredCount();
        Long getTotalCount();
    }
}