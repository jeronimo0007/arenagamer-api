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

    @Column(name = "max_owned_teams_per_contact", nullable = false)
    private Integer maxOwnedTeamsPerContact;

    @Column(name = "max_participated_teams_per_contact", nullable = false)
    private Integer maxParticipatedTeamsPerContact;

    @Column(name = "max_tournaments_per_team")
    private Integer maxTournamentsPerTeam;
}
