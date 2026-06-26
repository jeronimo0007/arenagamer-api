package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.TeamRosterVacancyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblteam_roster_vacancies", indexes = {
        @Index(name = "idx_trv_team_status", columnList = "team_id, status"),
        @Index(name = "idx_trv_participant_status", columnList = "participant_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRosterVacancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private TournamentParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exited_client_user_id", nullable = false)
    private Client exitedClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replacement_client_user_id")
    private Client replacementClient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TeamRosterVacancyStatus status = TeamRosterVacancyStatus.OPEN;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        openedAt = LocalDateTime.now();
    }
}
