package com.unveil.repository;

import com.unveil.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by LinkedIn ID
     */
    Optional<User> findByLinkedInId(String linkedInId);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by LinkedIn ID
     */
    boolean existsByLinkedInId(String linkedInId);

    /**
     * Find active users (for admin purposes)
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    Iterable<User> findActiveUsers();

    /**
     * Count total registered users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    Long countActiveUsers();

    /**
     * Find users who have voted on a specific Case
     */
    @Query("SELECT DISTINCT v.user FROM Vote v WHERE v.caseEntity.id = :caseId")
    Iterable<User> findUsersWhoVotedOnCase(@Param("caseId") Long caseId);

    /**
     * Get user voting statistics
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.user.id = :userId")
    Long countUserVotes(@Param("userId") Long userId);
}