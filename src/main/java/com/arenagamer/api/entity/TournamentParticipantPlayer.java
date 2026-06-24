package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbltournament_participant_players", indexes = {
    @Index(name = "idx_tpp_client", columnList = "client_userid")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_participant_player", columnNames = {"participant_id", "client_userid"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentParticipantPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private TournamentParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_userid", nullable = false)
    private Client client;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
