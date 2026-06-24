package com.arenagamer.api.entity;

import com.arenagamer.api.entity.enums.EntryFeeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbltournament_entry_fees", indexes = {
        @Index(name = "idx_tef_tournament_status", columnList = "tournament_id, status"),
        @Index(name = "idx_tef_client_tournament", columnList = "client_user_id, tournament_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentEntryFee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false, unique = true)
    private TournamentParticipant participant;

    @Column(name = "client_user_id", nullable = false)
    private Integer clientUserId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EntryFeeStatus status = EntryFeeStatus.HELD;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
