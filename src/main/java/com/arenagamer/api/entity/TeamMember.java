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
    @Index(name = "idx_team_members_client", columnList = "client_userid")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_member_client", columnNames = {"team_id", "client_userid"})
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
    @JoinColumn(name = "client_userid", nullable = false)
    private Client client;

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
