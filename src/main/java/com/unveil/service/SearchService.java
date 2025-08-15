package com.unveil.service;

import com.unveil.entity.Case;
import com.unveil.entity.User;
import com.unveil.entity.Vote;
import com.unveil.repository.CaseRepository;
import com.unveil.repository.VoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SearchService {

    private final CaseRepository repository;
    private final VoteRepository voteRepository;

    public SearchService(CaseRepository caseRepository, VoteRepository voteRepository) {
        this.repository = caseRepository;
        this.voteRepository = voteRepository;
    }

    /**
     * Search by specific filter with pagination
     */
    public Page<Case> searchByFilter(String filter, String value, int page, int size) {
        // Validate filter
        String normalizedFilter = filter.toLowerCase().trim();
        if (!isValidFilter(normalizedFilter)) {
            throw new IllegalArgumentException("Unsupported filter: " + filter +
                    ". Supported filters: name, email, phone, company, actions, all");
        }

        // Validate value
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Search value cannot be empty");
        }

        // Create pageable object (sorted by creation date, newest first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Perform search based on filter
        return switch (normalizedFilter) {
            case "name" -> searchByName(value.trim(), pageable);
            case "email" -> searchByEmail(value.trim(), pageable);
            case "phone" -> searchByPhone(value.trim(), pageable);
            case "company" -> searchByCompany(value.trim(), pageable);
            case "actions" -> searchByAction(value.trim(), pageable);
            case "all" -> searchByAll(value.trim(), pageable);
            default -> throw new IllegalArgumentException("Unsupported filter: " + filter);
        };
    }

    // ============ AUTHENTICATED VOTING METHODS ============

    /**
     * Cast an authenticated vote on a Case's guilt
     * Prevents duplicate voting by the same user
     */
    @Transactional
    public Case castAuthenticatedVote(Long caseId, String vote, User user) {
        Optional<Case> caseOpt = repository.findById(caseId);

        if (caseOpt.isEmpty()) {
            return null;
        }

        Case caseEntity = caseOpt.get();

        // Check if user has already voted on this Case
        Optional<Vote> existingVote = voteRepository.findByUserIdAndCaseEntityId(user.getId(), caseId);

        if (existingVote.isPresent()) {
            throw new IllegalStateException("You have already voted on this case. Each user can only vote once per case.");
        }

        Vote.VoteType voteType;
        if ("guilty".equalsIgnoreCase(vote)) {
            voteType = Vote.VoteType.GUILTY;
        } else if ("not_guilty".equalsIgnoreCase(vote)) {
            voteType = Vote.VoteType.NOT_GUILTY;
        } else {
            throw new IllegalArgumentException("Invalid vote type: " + vote);
        }

        // Create new vote record
        Vote newVote = new Vote();
        newVote.setUser(user);
        newVote.setCaseEntity(caseEntity);
        newVote.setVoteType(voteType);
        newVote.setIpAddress("127.0.0.1"); // In production, get real IP address

        // Save vote
        voteRepository.save(newVote);

        // Update Case vote counts
        updateCaseVoteCounts(caseEntity);

        // Save and return updated Case
        return repository.save(caseEntity);
    }

    /**
     * Update Case vote counts based on actual Vote records
     */
    private void updateCaseVoteCounts(Case caseEntity) {
        Long guiltyVotes = voteRepository.countByCaseIdAndVoteType(caseEntity.getId(), Vote.VoteType.GUILTY);
        Long notGuiltyVotes = voteRepository.countByCaseIdAndVoteType(caseEntity.getId(), Vote.VoteType.NOT_GUILTY);

        caseEntity.setGuiltyVotes(guiltyVotes.intValue());
        caseEntity.setNotGuiltyVotes(notGuiltyVotes.intValue());
        caseEntity.setTotalVotes(guiltyVotes.intValue() + notGuiltyVotes.intValue());
        caseEntity.setVerdictScore(guiltyVotes.intValue() - notGuiltyVotes.intValue());
        caseEntity.setLastVotedAt(java.time.LocalDateTime.now());
    }

    /**
     * Check if a user has voted on a specific Case
     */
    public boolean hasUserVoted(Long userId, Long caseId) {
        return voteRepository.existsByUserIdAndCaseEntityId(userId, caseId);
    }

    /**
     * Get user's vote on a specific Case
     */
    public Optional<Vote> getUserVote(Long userId, Long caseId) {
        return voteRepository.findByUserIdAndCaseEntityId(userId, caseId);
    }

    /**
     * Get Cases by verdict status
     */
    public Page<Case> getCasesByVerdictStatus(String verdictStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("verdictScore").descending());

        return switch (verdictStatus.toLowerCase()) {
            case "guilty" -> repository.findGuiltyCases(pageable);
            case "not guilty" -> repository.findNotGuiltyCases(pageable);
            case "pending" -> repository.findOnTrialCases(pageable);
            default -> throw new IllegalArgumentException("Invalid verdict status: " + verdictStatus);
        };
    }

    /**
     * Get controversial Cases (close votes, high engagement)
     */
    public Page<Case> getControversialCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findControversialCases(pageable);
    }

    /**
     * Get Cases that need more votes
     */
    public Page<Case> getCasesNeedingVotes(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findCasesNeedingVotes(pageable);
    }

    /**
     * Get recently voted Cases
     */
    public Page<Case> getRecentlyVotedCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findRecentlyVotedCases(pageable);
    }

    /**
     * Get most voted Cases (highest community engagement)
     */
    public Page<Case> getMostVotedCases(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return repository.findMostVotedCases(pageable);
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

        return stats;
    }

    // ============ EXISTING SEARCH METHODS ============

    /**
     * Search by name (partial match, case insensitive)
     */
    private Page<Case> searchByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Search by email (exact match, case insensitive)
     */
    private Page<Case> searchByEmail(String email, Pageable pageable) {
        // Validate email format
        if (!isValidEmailFormat(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return repository.findByEmailIgnoreCase(email, pageable);
    }

    /**
     * Search by phone (exact match)
     */
    private Page<Case> searchByPhone(String phone, Pageable pageable) {
        return repository.findByPhone(phone, pageable);
    }

    /**
     * Search by company (partial match, case insensitive)
     */
    private Page<Case> searchByCompany(String company, Pageable pageable) {
        return repository.findByCompanyContainingIgnoreCase(company, pageable);
    }

    /**
     * Search by scam type (partial match, case insensitive)
     */
    private Page<Case> searchByAction(String actions, Pageable pageable) {
        return repository.findByActionsContainingIgnoreCase(actions, pageable);
    }

    /**
     * Search across all fields
     */
    private Page<Case> searchByAll(String searchTerm, Pageable pageable) {
        return repository.searchCases(searchTerm, pageable);
    }

    /**
     * Check if filter is valid
     */
    private boolean isValidFilter(String filter) {
        return filter.equals("name") ||
                filter.equals("email") ||
                filter.equals("phone") ||
                filter.equals("company") ||
                filter.equals("actions") ||
                filter.equals("all");
    }

    /**
     * Basic email format validation
     */
    private boolean isValidEmailFormat(String email) {
        return email.contains("@") &&
                email.contains(".") &&
                email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Get total count of cases in database
     */
    public long getTotalCasesCount() {
        return repository.count();
    }

    /**
     * Get a specific Case by ID
     */
    public Optional<Case> getCaseById(Long id) {
        return repository.findById(id);
    }

    /**
     * Get all scam types for frontend filtering
     */
    public List<String> getAllActions() {
        return repository.findAllActions();
    }

    /**
     * Add a new Case (for future user reporting feature)
     */
    public Case addCase(Case caseEntity) {
        // Basic validation
        if (caseEntity.getName() == null && caseEntity.getEmail() == null && caseEntity.getPhone() == null) {
            throw new IllegalArgumentException("At least one of name, email, or phone must be provided");
        }

        // Initialize verdict fields for new Cases
        if (caseEntity.getVerdictScore() == null) {
            caseEntity.setVerdictScore(0);
        }
        if (caseEntity.getTotalVotes() == null) {
            caseEntity.setTotalVotes(0);
        }
        if (caseEntity.getGuiltyVotes() == null) {
            caseEntity.setGuiltyVotes(0);
        }
        if (caseEntity.getNotGuiltyVotes() == null) {
            caseEntity.setNotGuiltyVotes(0);
        }

        return repository.save(caseEntity);
    }
}