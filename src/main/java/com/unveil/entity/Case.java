package com.unveil.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_case_name", columnList = "name"),
        @Index(name = "idx_case_email", columnList = "email"),
        @Index(name = "idx_case_phone", columnList = "phone"),
        @Index(name = "idx_case_verdict", columnList = "verdict_score")
})
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String name;

    @Email
    @Column
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String reportedBy;

    @Column(length = 300)
    private String actions;

    // Verdict System Fields
    @Column(name = "verdict_score", nullable = false)
    private Integer verdictScore = 0;

    @Column(name = "total_votes", nullable = false)
    private Integer totalVotes = 0;

    @Column(name = "guilty_votes", nullable = false)
    private Integer guiltyVotes = 0;

    @Column(name = "not_guilty_votes", nullable = false)
    private Integer notGuiltyVotes = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_voted_at")
    private LocalDateTime lastVotedAt;

    // Computed verdict status based on score
    @Transient
    public String getVerdictStatus() {
        if (verdictScore > 0) {
            return "Guilty";
        } else if (verdictScore < 0) {
            return "Not Guilty";
        } else {
            return "Pending";
        }
    }

    // Get verdict confidence level (percentage)
    @Transient
    public Double getVerdictConfidence() {
        if (totalVotes == 0) {
            return 0.0;
        }

        int dominantVotes = Math.max(guiltyVotes, notGuiltyVotes);
        return (double) dominantVotes / totalVotes * 100;
    }

    // Helper method to add a guilty vote (for manual updates)
    public void addGuiltyVote() {
        this.verdictScore++;
        this.guiltyVotes++;
        this.totalVotes++;
        this.lastVotedAt = LocalDateTime.now();
    }

    // Helper method to add a not guilty vote (for manual updates)
    public void addNotGuiltyVote() {
        this.verdictScore--;
        this.notGuiltyVotes++;
        this.totalVotes++;
        this.lastVotedAt = LocalDateTime.now();
    }

    // Get verdict summary for API responses
    @Transient
    public VerdictSummary getVerdictSummary() {
        return new VerdictSummary(
                getVerdictStatus(),
                verdictScore,
                totalVotes,
                guiltyVotes,
                notGuiltyVotes,
                getVerdictConfidence()
        );
    }

    // Inner class for verdict summary
    @Getter
    @AllArgsConstructor
    public static class VerdictSummary {
        private String status;
        private Integer score;
        private Integer totalVotes;
        private Integer guiltyVotes;
        private Integer notGuiltyVotes;
        private Double confidence;
    }
}