package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblplans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "free_tournaments_per_month", nullable = false)
    @Builder.Default
    private Integer freeTournamentsPerMonth = 0;

    @Column(name = "free_max_participants", nullable = false)
    @Builder.Default
    private Integer freeMaxParticipants = 0;

    @Column(name = "allows_entry_fee", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean allowsEntryFee = false;

    @Column(name = "max_tournaments_per_month")
    private Integer maxTournamentsPerMonth;

    @Column(name = "monthly_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean hidden = false;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
