package com.unveil.service;

import com.unveil.entity.Case;
import com.unveil.repository.CaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoteService {

    private final CaseRepository repository;

    // Enhanced vote tracking: supports both IP and email-based voting
    // Format: "email:user@example.com" or "ip:192.168.1.1"
    private final Map<String, Set<Long>> voterCaseMap = new ConcurrentHashMap<>();

    public VoteService(CaseRepository caseRepository) {
        this.repository = caseRepository;
    }

    /**
     * Cast a vote on a Case's guilt (with email verification support)
     * @param caseId Case ID to vote on
     * @param vote "guilty" or "not_guilty"
     * @param voterIdentifier Voter identifier (email:xxx or ip:xxx)
     * @param isEmailVerified Whether the voter is email verified
     */
    @Transactional
    public Case castVoteWithVerification(Long caseId, String vote, String voterIdentifier, boolean isEmailVerified) {
        Optional<Case> caseOpt = repository.findById(caseId);

        if (caseOpt.isEmpty()) {
            return null;
        }

        Case caseEntity = caseOpt.get();

        // Enhanced duplicate vote prevention
        if (hasVoterVotedOnCase(voterIdentifier, caseId)) {
            String errorMessage = isEmailVerified
                    ? "You have already voted on this case with this email address."
                    : "This IP address has already voted on this case.";
            throw new IllegalStateException(errorMessage);
        }

        // Validate vote type
        if (!"guilty".equalsIgnoreCase(vote) && !"not_guilty".equalsIgnoreCase(vote)) {
            throw new IllegalArgumentException("Invalid vote type: " + vote);
        }

        // Update Case vote counts
        if ("guilty".equalsIgnoreCase(vote)) {
            caseEntity.addGuiltyVote();
        } else {
            caseEntity.addNotGuiltyVote();
        }

        // Track the vote to prevent duplicates
        recordVote(voterIdentifier, caseId);

        // Save and return updated Case
        return repository.save(caseEntity);
    }

    /**
     * Backward compatibility method for IP-based voting
     */
    @Transactional
    public Case castVote(Long caseId, String vote, String voterIdentifier) {
        return castVoteWithVerification(caseId, vote, "ip:" + voterIdentifier, false);
    }

    /**
     * Check if a voter has already voted on a specific case
     */
    public boolean hasVoterVotedOnCase(String voterIdentifier, Long caseId) {
        Set<Long> votedCases = voterCaseMap.get(voterIdentifier);
        return votedCases != null && votedCases.contains(caseId);
    }

    /**
     * Bulk vote for testing purposes
     */
    @Transactional
    public Case bulkVote(Long caseId, int guiltyVotes, int notGuiltyVotes) {
        Optional<Case> caseOpt = repository.findById(caseId);

        if (caseOpt.isEmpty()) {
            return null;
        }

        Case caseEntity = caseOpt.get();

        // Add the votes
        for (int i = 0; i < guiltyVotes; i++) {
            caseEntity.addGuiltyVote();
        }

        for (int i = 0; i < notGuiltyVotes; i++) {
            caseEntity.addNotGuiltyVote();
        }

        return repository.save(caseEntity);
    }

    /**
     * Reset all votes for a case
     */
    @Transactional
    public Case resetVotes(Long caseId) {
        Optional<Case> caseOpt = repository.findById(caseId);

        if (caseOpt.isEmpty()) {
            return null;
        }

        Case caseEntity = caseOpt.get();

        // Reset all vote counts
        caseEntity.setVerdictScore(0);
        caseEntity.setTotalVotes(0);
        caseEntity.setGuiltyVotes(0);
        caseEntity.setNotGuiltyVotes(0);
        caseEntity.setLastVotedAt(null);

        // Clear vote tracking for this case
        clearCaseVoteTracking(caseId);

        return repository.save(caseEntity);
    }

    /**
     * Get verdict statistics for the entire database
     */
    public Map<String, Object> getVerdictStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // Verdict counts
        Long guiltyCount = repository.countGuiltyCases();
        Long notGuiltyCount = repository.countNotGuiltyCases();
        Long pendingCount = repository.countOnTrialCases();
        Long totalCases = repository.count();

        stats.put("guilty", guiltyCount);
        stats.put("notGuilty", notGuiltyCount);
        stats.put("pending", pendingCount);
        stats.put("total", totalCases);

        // Vote counts
        Long totalVotes = repository.getTotalVotesCount();
        Long totalGuiltyVotes = repository.getTotalGuiltyVotes();
        Long totalNotGuiltyVotes = repository.getTotalNotGuiltyVotes();

        stats.put("totalVotes", totalVotes != null ? totalVotes : 0);
        stats.put("totalGuiltyVotes", totalGuiltyVotes != null ? totalGuiltyVotes : 0);
        stats.put("totalNotGuiltyVotes", totalNotGuiltyVotes != null ? totalNotGuiltyVotes : 0);

        // Percentages
        if (totalCases > 0) {
            stats.put("guiltyPercentage", Math.round((double) guiltyCount / totalCases * 100 * 100) / 100.0);
            stats.put("notGuiltyPercentage", Math.round((double) notGuiltyCount / totalCases * 100 * 100) / 100.0);
            stats.put("pendingPercentage", Math.round((double) pendingCount / totalCases * 100 * 100) / 100.0);
        }

        // Average verdict score
        Double avgScore = repository.getAverageVerdictScore();
        stats.put("averageVerdictScore", avgScore != null ? Math.round(avgScore * 100) / 100.0 : 0.0);

        // Voting engagement metrics
        if (totalCases > 0 && totalVotes != null) {
            stats.put("averageVotesPerCase", Math.round((double) totalVotes / totalCases * 100) / 100.0);
        }

        // Enhanced statistics with email verification info
        long emailVerifiedVoters = voterCaseMap.keySet().stream()
                .mapToLong(key -> key.startsWith("email:") ? 1 : 0)
                .sum();

        long ipBasedVoters = voterCaseMap.keySet().stream()
                .mapToLong(key -> key.startsWith("ip:") ? 1 : 0)
                .sum();

        stats.put("emailVerifiedVoters", emailVerifiedVoters);
        stats.put("ipBasedVoters", ipBasedVoters);
        stats.put("totalUniqueVoters", voterCaseMap.size());

        return stats;
    }

    /**
     * Get Cases with most votes (highest community engagement)
     */
    public Map<String, Object> getTopVotedCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Case> cases = repository.findMostVotedCases(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("results", cases.getContent());
        response.put("pagination", buildPaginationInfo(cases));
        response.put("message", "Cases with most votes (highest community engagement)");

        return response;
    }

    /**
     * Get Cases that need more votes
     */
    public Map<String, Object> getCasesNeedingVotes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Case> cases = repository.findCasesNeedingVotes(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("results", cases.getContent());
        response.put("pagination", buildPaginationInfo(cases));
        response.put("message", "Cases that need more community votes");

        return response;
    }

    /**
     * Get voting activity summary with enhanced verification metrics
     */
    public Map<String, Object> getVotingActivity() {
        Map<String, Object> activity = new HashMap<>();

        // Recently voted cases
        Pageable recentPageable = PageRequest.of(0, 5);
        Page<Case> recentlyVoted = repository.findRecentlyVotedCases(recentPageable);

        // Most controversial cases
        Page<Case> controversial = repository.findControversialCases(recentPageable);

        // Cases needing votes
        Page<Case> needingVotes = repository.findCasesNeedingVotes(recentPageable);

        activity.put("recentlyVoted", recentlyVoted.getContent());
        activity.put("controversial", controversial.getContent());
        activity.put("needingVotes", needingVotes.getContent());

        // Enhanced voter statistics
        long emailVerifiedVoters = voterCaseMap.keySet().stream()
                .mapToLong(key -> key.startsWith("email:") ? 1 : 0)
                .sum();

        long ipBasedVoters = voterCaseMap.keySet().stream()
                .mapToLong(key -> key.startsWith("ip:") ? 1 : 0)
                .sum();

        Map<String, Object> voterStats = new HashMap<>();
        voterStats.put("totalActiveVoters", voterCaseMap.size());
        voterStats.put("emailVerifiedVoters", emailVerifiedVoters);
        voterStats.put("ipBasedVoters", ipBasedVoters);
        voterStats.put("verificationRate", voterCaseMap.size() > 0
                ? Math.round((double) emailVerifiedVoters / voterCaseMap.size() * 100 * 100) / 100.0
                : 0.0);

        activity.put("voterStats", voterStats);

        // Overall stats
        activity.put("stats", getVerdictStatistics());

        return activity;
    }

    /**
     * Get detailed voter information for a case (admin/debugging)
     */
    public Map<String, Object> getCaseVoterInfo(Long caseId) {
        Map<String, Object> info = new HashMap<>();

        long emailVoters = voterCaseMap.entrySet().stream()
                .mapToLong(entry -> entry.getKey().startsWith("email:") && entry.getValue().contains(caseId) ? 1 : 0)
                .sum();

        long ipVoters = voterCaseMap.entrySet().stream()
                .mapToLong(entry -> entry.getKey().startsWith("ip:") && entry.getValue().contains(caseId) ? 1 : 0)
                .sum();

        info.put("caseId", caseId);
        info.put("emailVerifiedVotes", emailVoters);
        info.put("ipBasedVotes", ipVoters);
        info.put("totalUniqueVoters", emailVoters + ipVoters);

        return info;
    }

    /**
     * Get a specific Case by ID
     */
    public Optional<Case> getCaseById(Long id) {
        return repository.findById(id);
    }

    // ============ HELPER METHODS ============

    /**
     * Record that a voter has voted on a case
     */
    private void recordVote(String voterIdentifier, Long caseId) {
        voterCaseMap.computeIfAbsent(voterIdentifier, k -> ConcurrentHashMap.newKeySet()).add(caseId);
    }

    /**
     * Clear vote tracking for a specific case
     */
    private void clearCaseVoteTracking(Long caseId) {
        voterCaseMap.values().forEach(caseSet -> caseSet.remove(caseId));
    }

    /**
     * Build pagination info for responses
     */
    private Map<String, Object> buildPaginationInfo(Page<Case> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("currentPage", page.getNumber());
        pagination.put("pageSize", page.getSize());
        pagination.put("totalPages", page.getTotalPages());
        pagination.put("totalElements", page.getTotalElements());
        pagination.put("hasNext", page.hasNext());
        pagination.put("hasPrevious", page.hasPrevious());
        pagination.put("isFirst", page.isFirst());
        pagination.put("isLast", page.isLast());
        return pagination;
    }

    /**
     * Clean up old vote tracking data (call periodically in production)
     */
    public void cleanupVoteTracking() {
        // In production, implement cleanup logic for old vote tracking data
        // For now, just log the current statistics
        long emailVoters = voterCaseMap.keySet().stream().mapToLong(key -> key.startsWith("email:") ? 1 : 0).sum();
        long ipVoters = voterCaseMap.keySet().stream().mapToLong(key -> key.startsWith("ip:") ? 1 : 0).sum();

        System.out.println("Vote tracking cleanup - Email verified voters: " + emailVoters +
                ", IP-based voters: " + ipVoters +
                ", Total entries: " + voterCaseMap.size());
    }

    /**
     * Get voting method distribution
     */
    public Map<String, Object> getVotingMethodDistribution() {
        Map<String, Object> distribution = new HashMap<>();

        long emailVerifiedVotes = voterCaseMap.entrySet().stream()
                .mapToLong(entry -> entry.getKey().startsWith("email:") ? entry.getValue().size() : 0)
                .sum();

        long ipBasedVotes = voterCaseMap.entrySet().stream()
                .mapToLong(entry -> entry.getKey().startsWith("ip:") ? entry.getValue().size() : 0)
                .sum();

        long totalVotes = emailVerifiedVotes + ipBasedVotes;

        distribution.put("emailVerifiedVotes", emailVerifiedVotes);
        distribution.put("ipBasedVotes", ipBasedVotes);
        distribution.put("totalVotes", totalVotes);

        if (totalVotes > 0) {
            distribution.put("emailVerificationRate", Math.round((double) emailVerifiedVotes / totalVotes * 100 * 100) / 100.0);
        } else {
            distribution.put("emailVerificationRate", 0.0);
        }

        return distribution;
    }
}