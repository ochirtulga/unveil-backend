package com.unveil.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_linkedin_id", columnList = "linkedin_id", unique = true),
        @Index(name = "idx_user_email", columnList = "email")
})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"votes"})
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "linkedin_id", nullable = false, unique = true)
    private String linkedInId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "headline")
    private String headline;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Relationship with votes
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Vote> votes = new HashSet<>();

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void updateLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // Update user info from LinkedIn
    public void updateFromLinkedIn(String firstName, String lastName, String email,
                                   String profilePicture, String headline) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.profilePicture = profilePicture;
        this.headline = headline;
        updateLoginTime();
    }
}