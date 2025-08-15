package com.unveil.repository;

import com.unveil.entity.Vote;
import com.unveil.entity.Vote.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Find existing vote by user and case
     */
    Optional<Vote> findByUserIdAndCaseEntityId(Long userId, Long caseId);

    /**
     * Check if user has already voted on a case
     */
    boolean existsByUserIdAndCaseEntityId(Long userId, Long caseId);

    /**
     * Count votes by case and vote type
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.caseEntity.id = :caseId AND v.voteType = :voteType")
    Long countByCaseIdAndVoteType(@Param("caseId") Long caseId, @Param("voteType") VoteType voteType);

    /**
     * Count total votes for a case
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.caseEntity.id = :caseId")
    Long countByCaseId(@Param("caseId") Long caseId);

    /**
     * Count votes by user
     */
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /**
     * Get user's vote on a specific case
     */
    @Query("SELECT v FROM Vote v WHERE v.user.id = :userId AND v.caseEntity.id = :caseId")
    Optional<Vote> findUserVoteOnCase(@Param("userId") Long userId, @Param("caseId") Long caseId);

    /**
     * Delete user's vote on a case (for vote changes)
     */
    void deleteByUserIdAndCaseEntityId(Long userId, Long caseId);

    /**
     * Get voting statistics
     */
    @Query("SELECT " +
            "COUNT(CASE WHEN v.voteType = 'GUILTY' THEN 1 END) as guiltyCount, " +
            "COUNT(CASE WHEN v.voteType = 'NOT_GUILTY' THEN 1 END) as notGuiltyCount, " +
            "COUNT(v) as totalCount " +
            "FROM Vote v WHERE v.caseEntity.id = :caseId")
    VoteStatistics getVoteStatisticsByCaseId(@Param("caseId") Long caseId);

    // Interface for vote statistics projection
    interface VoteStatistics {
        Long getGuiltyCount();
        Long getNotGuiltyCount();
        Long getTotalCount();
    }
}