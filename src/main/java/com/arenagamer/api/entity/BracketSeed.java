package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tblbracket_seeds", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bracket_seed", columnNames = {"tournament_id", "seed_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BracketSeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private TournamentParticipant participant;

    @Column(name = "seed_number", nullable = false)
    private Integer seedNumber;
}
