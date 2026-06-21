package com.arenagamer.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbluser_subscriptions", indexes = {
    @Index(name = "idx_subscriptions_client", columnList = "client_user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_user_id", referencedColumnName = "userid", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_plan_id")
    private Plan pendingPlan;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "tournaments_used_this_month")
    @Builder.Default
    private Integer tournamentsUsedThisMonth = 0;

    @Column(name = "tournaments_usage_month", length = 7)
    private String tournamentsUsageMonth;

    @Column(name = "tournaments_usage_baseline")
    @Builder.Default
    private Integer tournamentsUsageBaseline = 0;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "cancel_at_period_end", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "billing_period_months", nullable = false)
    @Builder.Default
    private Integer billingPeriodMonths = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
