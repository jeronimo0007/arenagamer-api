package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Plan;
import com.arenagamer.api.entity.UserSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Plano ativo vinculado ao cliente")
public class UserPlanResponse {

    private Long subscriptionId;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Integer tournamentsUsedThisMonth;
    private Long id;
    private String name;
    private String description;
    private Integer freeTournamentsPerMonth;
    private Integer freeMaxParticipants;
    private Boolean allowsEntryFee;
    private Integer maxTournamentsPerMonth;
    private BigDecimal monthlyPrice;
    private Integer billingPeriodMonths;
    private Boolean cancelAtPeriodEnd;
    private Long pendingPlanId;
    private String pendingPlanName;
    private Boolean scheduledChangeAtPeriodEnd;

    public static UserPlanResponse from(UserSubscription subscription) {
        var plan = subscription.getPlan();
        Plan pendingPlan = subscription.getPendingPlan();
        boolean cancelScheduled = Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd());
        boolean downgradeScheduled = pendingPlan != null;

        return UserPlanResponse.builder()
                .subscriptionId(subscription.getId())
                .startsAt(subscription.getStartsAt())
                .expiresAt(subscription.getExpiresAt())
                .tournamentsUsedThisMonth(subscription.getTournamentsUsedThisMonth())
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .freeTournamentsPerMonth(plan.getFreeTournamentsPerMonth())
                .freeMaxParticipants(plan.getFreeMaxParticipants())
                .allowsEntryFee(plan.getAllowsEntryFee())
                .maxTournamentsPerMonth(plan.getMaxTournamentsPerMonth())
                .monthlyPrice(plan.getMonthlyPrice())
                .billingPeriodMonths(subscription.getBillingPeriodMonths())
                .cancelAtPeriodEnd(cancelScheduled)
                .pendingPlanId(pendingPlan != null ? pendingPlan.getId() : null)
                .pendingPlanName(pendingPlan != null ? pendingPlan.getName() : null)
                .scheduledChangeAtPeriodEnd(cancelScheduled || downgradeScheduled)
                .build();
    }
}
