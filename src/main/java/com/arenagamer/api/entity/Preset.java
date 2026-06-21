package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblpresets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_name", nullable = false, length = 100)
    private String gameName;

    @Column(length = 100)
    private String platform;

    @Column(name = "team_size", nullable = false)
    @Builder.Default
    private Integer teamSize = 1;

    @Column(name = "min_players_per_team", nullable = false)
    @Builder.Default
    private Integer minPlayersPerTeam = 1;

    @Column(name = "max_players_per_team", nullable = false)
    @Builder.Default
    private Integer maxPlayersPerTeam = 1;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "game_image_url", length = 500)
    private String gameImageUrl;

    @Column(name = "rules_template", columnDefinition = "TEXT")
    private String rulesTemplate;

    @Column(name = "scoring_script", columnDefinition = "TEXT")
    private String scoringScript;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
