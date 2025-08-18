package com.unveil.repository;

import com.unveil.entity.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CaseRepository extends JpaRepository<Case, Long> {

    /**
     * Search Cases by query string across multiple fields
     * This searches name, email, phone, and company fields
     */
    @Query("SELECT c FROM Case c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.actions) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Case> searchCases(@Param("query") String query);

    /**
     * Search with pagination for better performance
     */
    @Query("SELECT c FROM Case c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.actions) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Case> searchCases(@Param("query") String query, Pageable pageable);

    /**
     * Find by exact email match
     */
    Page<Case> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * Find by exact phone match
     */
    Page<Case> findByPhoneContainingIgnoreCase(String phone, Pageable pageable);

    /**
     * Find by name containing (case insensitive)
     */
    Page<Case> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find by company containing (case insensitive)
     */
    Page<Case> findByCompanyContainingIgnoreCase(String company, Pageable pageable);

    /**
     * Find by scam type
     */
    Page<Case> findByActionsContainingIgnoreCase(String actions, Pageable pageable);

    /**
     * Get all distinct scam types for filtering
     */
    @Query("SELECT DISTINCT c.actions FROM Case c WHERE c.actions IS NOT NULL ORDER BY c.actions")
    List<String> findAllActions();

    // ============ VERDICT-RELATED QUERIES ============

    /**
     * Find Cases with guilty verdict (positive score)
     */
    @Query("SELECT c FROM Case c WHERE c.verdictScore > 0")
    Page<Case> findGuiltyCases(Pageable pageable);

    /**
     * Find Cases with not guilty verdict (negative score)
     */
    @Query("SELECT c FROM Case c WHERE c.verdictScore < 0")
    Page<Case> findNotGuiltyCases(Pageable pageable);

    /**
     * Find Cases on trial (zero score)
     */
    @Query("SELECT c FROM Case c WHERE c.verdictScore = 0")
    Page<Case> findOnTrialCases(Pageable pageable);

    /**
     * Find most controversial Cases (those with many votes but close to 0 score)
     * This finds Cases where the difference between guilty and not guilty votes is small
     * but they have received significant voting activity
     */
    @Query("SELECT c FROM Case c WHERE c.totalVotes >= 5 AND ABS(c.verdictScore) <= 2 ORDER BY c.totalVotes DESC")
    Page<Case> findControversialCases(Pageable pageable);

    /**
     * Find Cases with the most votes (highest community engagement)
     */
    @Query("SELECT c FROM Case c WHERE c.totalVotes > 0 ORDER BY c.totalVotes DESC")
    Page<Case> findMostVotedCases(Pageable pageable);

    /**
     * Find Cases with strongest guilty verdicts (highest positive scores)
     */
    @Query("SELECT c FROM Case c WHERE c.verdictScore > 0 ORDER BY c.verdictScore DESC")
    Page<Case> findStrongestGuiltyCases(Pageable pageable);

    /**
     * Find Cases with strongest not guilty verdicts (most negative scores)
     */
    @Query("SELECT c FROM Case c WHERE c.verdictScore < 0 ORDER BY c.verdictScore ASC")
    Page<Case> findStrongestNotGuiltyCases(Pageable pageable);

    /**
     * Get count of Cases by verdict status
     */
    @Query("SELECT COUNT(c) FROM Case c WHERE c.verdictScore > 0")
    Long countGuiltyCases();

    @Query("SELECT COUNT(c) FROM Case c WHERE c.verdictScore < 0")
    Long countNotGuiltyCases();

    @Query("SELECT COUNT(c) FROM Case c WHERE c.verdictScore = 0")
    Long countOnTrialCases();

    /**
     * Get total vote statistics
     */
    @Query("SELECT SUM(c.totalVotes) FROM Case c")
    Long getTotalVotesCount();

    @Query("SELECT SUM(c.guiltyVotes) FROM Case c")
    Long getTotalGuiltyVotes();

    @Query("SELECT SUM(c.notGuiltyVotes) FROM Case c")
    Long getTotalNotGuiltyVotes();

    /**
     * Get average verdict score
     */
    @Query("SELECT AVG(c.verdictScore) FROM Case c WHERE c.totalVotes > 0")
    Double getAverageVerdictScore();

    /**
     * Find Cases that need more votes (low vote count but potentially important)
     * These are Cases that have been reported but haven't received much community input
     */
    @Query("SELECT c FROM Case c WHERE c.totalVotes < 5 ORDER BY c.createdAt DESC")
    Page<Case> findCasesNeedingVotes(Pageable pageable);

    /**
     * Find recently active Cases (recently voted on)
     */
    @Query("SELECT c FROM Case c WHERE c.lastVotedAt IS NOT NULL ORDER BY c.lastVotedAt DESC")
    Page<Case> findRecentlyVotedCases(Pageable pageable);

    /**
     * Search Cases by verdict status and additional criteria
     */
    @Query("SELECT c FROM Case c WHERE " +
            "c.verdictScore > 0 AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.actions) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Case> searchGuiltyCases(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE " +
            "c.verdictScore < 0 AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.actions) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Case> searchNotGuiltyCases(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE " +
            "c.verdictScore = 0 AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "c.phone LIKE CONCAT('%', :query, '%') OR " +
            "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.actions) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Case> searchPendingCases(@Param("query") String query, Pageable pageable);
}