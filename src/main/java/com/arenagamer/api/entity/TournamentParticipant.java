package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.ParticipantStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbltournament_participants", indexes = {
    @Index(name = "idx_tp_tournament", columnList = "tournament_id"),
    @Index(name = "idx_tp_contact", columnList = "contact_id"),
    @Index(name = "idx_tp_team", columnList = "team_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ParticipantStatus status = ParticipantStatus.PENDING;

    @Column(name = "seed_number")
    private Integer seedNumber;

    @Column(name = "group_number")
    private Integer groupNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_profile_id")
    private AvailabilityProfile availabilityProfile;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
