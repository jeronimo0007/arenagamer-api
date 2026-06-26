package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tblteam_join_bans", indexes = {
        @Index(name = "idx_tjb_client_until", columnList = "client_user_id, banned_until")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamJoinBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", nullable = false)
    private Client client;

    @Column(length = 500)
    private String reason;

    @Column(name = "banned_until", nullable = false)
    private LocalDateTime bannedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_vacancy_id")
    private TeamRosterVacancy rosterVacancy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
