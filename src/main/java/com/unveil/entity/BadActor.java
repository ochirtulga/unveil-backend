package com.unveil.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "bad_actors", indexes = {
        @Index(name = "idx_badactor_name", columnList = "name"),
        @Index(name = "idx_badactor_email", columnList = "email"),
        @Index(name = "idx_badactor_phone", columnList = "phone")
})
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Getter
@Setter
public class BadActor {

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}