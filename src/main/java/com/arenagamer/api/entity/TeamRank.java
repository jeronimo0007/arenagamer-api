package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblteam_ranks", indexes = {
    @Index(name = "idx_team_ranks_team", columnList = "team_id"),
    @Index(name = "idx_team_ranks_preset", columnList = "preset_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_rank_preset", columnNames = {"team_id", "preset_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preset_id", nullable = false)
    private Preset preset;

    @Column(name = "rank_points", nullable = false)
    @Builder.Default
    private Integer rankPoints = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
