package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblteam_members", indexes = {
    @Index(name = "idx_team_members_team", columnList = "team_id"),
    @Index(name = "idx_team_members_contact", columnList = "contact_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_member", columnNames = {"team_id", "contact_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Column(length = 50)
    private String position;

    @Column(name = "is_captain", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isCaptain = false;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
    }
}
