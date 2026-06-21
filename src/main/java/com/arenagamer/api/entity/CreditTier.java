package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tblcredit_tiers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "min_participants", nullable = false)
    private Integer minParticipants;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    @Column(name = "credit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal creditCost;
}
