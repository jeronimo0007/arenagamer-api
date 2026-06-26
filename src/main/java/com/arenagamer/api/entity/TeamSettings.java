package com.arenagamer.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tblteam_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "max_owned_teams_per_client", nullable = false)
    private Integer maxOwnedTeamsPerClient;

    @Column(name = "max_participated_teams_per_client", nullable = false)
    private Integer maxParticipatedTeamsPerClient;

    @Column(name = "max_tournaments_per_team")
    private Integer maxTournamentsPerTeam;

    @Column(name = "max_tournaments_per_client")
    private Integer maxTournamentsPerClient;

    @Column(name = "team_join_ban_days_after_unreplaced_exit", nullable = false)
    @Builder.Default
    private Integer teamJoinBanDaysAfterUnreplacedExit = 7;
}
