package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.TimeWindow;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblmatches", indexes = {
    @Index(name = "idx_matches_round", columnList = "round_id"),
    @Index(name = "idx_matches_scheduled", columnList = "scheduled_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "round_id", nullable = false)
    private Round round;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_participant_id")
    private TournamentParticipant homeParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_participant_id")
    private TournamentParticipant awayParticipant;

    @Column(name = "match_number")
    private Integer matchNumber;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_window", length = 15)
    private TimeWindow timeWindow;

    @Column(name = "home_score")
    private Integer homeScore;

    @Column(name = "away_score")
    private Integer awayScore;

    @Column(name = "result_proof_url", length = 500)
    private String resultProofUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_participant_id")
    private TournamentParticipant winnerParticipant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "next_match_id")
    private Long nextMatchId;

    @Column(name = "bracket_position")
    private Integer bracketPosition;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
