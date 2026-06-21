package com.arenagamer.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tbltournament_pricing_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentPricingSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "base_tournament_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseTournamentPrice;

    @Column(name = "extra_participant_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraParticipantPrice;

    @Column(name = "included_participants", nullable = false)
    private Integer includedParticipants;
}
