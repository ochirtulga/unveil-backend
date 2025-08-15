package com.unveil.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes", indexes = {
        @Index(name = "idx_vote_user_case", columnList = "user_id, case_id", unique = true),
        @Index(name = "idx_vote_case", columnList = "case_id"),
        @Index(name = "idx_vote_user", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case caseEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote_type", nullable = false)
    private VoteType voteType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ip_address")
    private String ipAddress;

    // Vote type enum
    public enum VoteType {
        GUILTY, NOT_GUILTY
    }

    // Helper methods
    public boolean isGuilty() {
        return voteType == VoteType.GUILTY;
    }

    public boolean isNotGuilty() {
        return voteType == VoteType.NOT_GUILTY;
    }

    // Factory methods
    public static Vote createGuiltyVote(User user, Case caseEntity, String ipAddress) {
        Vote vote = new Vote();
        vote.setUser(user);
        vote.setCaseEntity(caseEntity);
        vote.setVoteType(VoteType.GUILTY);
        vote.setIpAddress(ipAddress);
        return vote;
    }

    public static Vote createNotGuiltyVote(User user, Case caseEntity, String ipAddress) {
        Vote vote = new Vote();
        vote.setUser(user);
        vote.setCaseEntity(caseEntity);
        vote.setVoteType(VoteType.NOT_GUILTY);
        vote.setIpAddress(ipAddress);
        return vote;
    }
}