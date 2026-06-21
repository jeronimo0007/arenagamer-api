package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tblgroup_standings", indexes = {
    @Index(name = "idx_gs_tournament_group", columnList = "tournament_id, group_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupStanding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private TournamentParticipant participant;

    @Column(name = "group_number", nullable = false)
    private Integer groupNumber;

    @Builder.Default
    private Integer wins = 0;

    @Builder.Default
    private Integer losses = 0;

    @Builder.Default
    private Integer draws = 0;

    @Builder.Default
    private Integer points = 0;

    @Column(name = "games_played")
    @Builder.Default
    private Integer gamesPlayed = 0;

    @Column(name = "score_for")
    @Builder.Default
    private Integer scoreFor = 0;

    @Column(name = "score_against")
    @Builder.Default
    private Integer scoreAgainst = 0;

    @Column(name = "goal_difference")
    @Builder.Default
    private Integer goalDifference = 0;

    @Column(name = "rank_position")
    private Integer rankPosition;
}
