package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.UserSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Assinatura ativa de um cliente")
public class AdminSubscriptionResponse {

    private Long subscriptionId;
    private Integer clientUserId;
    private String clientCompany;
    private UserPlanResponse plan;

    public static AdminSubscriptionResponse from(UserSubscription subscription) {
        return AdminSubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .clientUserId(subscription.getClient().getUserId())
                .clientCompany(subscription.getClient().getCompany())
                .plan(UserPlanResponse.from(subscription))
                .build();
    }
}
