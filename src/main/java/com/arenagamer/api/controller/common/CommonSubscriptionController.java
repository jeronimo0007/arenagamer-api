package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.SubscribePlanRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/common/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Common / Assinaturas", description = "Contratação e troca de planos — JWT (cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Contratar, fazer upgrade ou agendar downgrade")
    public ResponseEntity<ApiResponse<UserPlanResponse>> subscribe(@Valid @RequestBody SubscribePlanRequest request) {
        requireContact();
        UserPlanResponse plan = subscriptionService.subscribe(
                UserPrincipal.currentContactId(),
                request.getPlanId(),
                request.getBillingPeriodMonths());
        String message = resolveSubscribeMessage(plan);
        return ApiResponses.ok(message, plan);
    }

    @PostMapping("/with-credits")
    @Operation(summary = "Contratar ou fazer upgrade debitando créditos da carteira")
    public ResponseEntity<ApiResponse<UserPlanResponse>> subscribeWithCredits(
            @Valid @RequestBody SubscribePlanRequest request) {
        requireContact();
        UserPlanResponse plan = subscriptionService.subscribeWithCredits(
                UserPrincipal.currentContactId(),
                request.getPlanId(),
                request.getBillingPeriodMonths());
        String message = resolveSubscribeMessage(plan);
        if (!Boolean.TRUE.equals(plan.getScheduledChangeAtPeriodEnd())) {
            message = ApiMessages.SUBSCRIPTION_PAID_WITH_CREDITS;
        }
        return ApiResponses.ok(message, plan);
    }

    @DeleteMapping
    @Operation(summary = "Agendar cancelamento do plano pago")
    public ResponseEntity<ApiResponse<UserPlanResponse>> cancel() {
        requireContact();
        UserPlanResponse plan = subscriptionService.scheduleCancel(UserPrincipal.currentContactId());
        return ApiResponses.ok(ApiMessages.SUBSCRIPTION_CANCEL_SCHEDULED, plan);
    }

    private String resolveSubscribeMessage(UserPlanResponse plan) {
        if (Boolean.TRUE.equals(plan.getScheduledChangeAtPeriodEnd())) {
            if (plan.getPendingPlanId() != null) {
                return ApiMessages.SUBSCRIPTION_DOWNGRADE_SCHEDULED;
            }
            return ApiMessages.SUBSCRIPTION_CANCEL_SCHEDULED;
        }

        return ApiMessages.SUBSCRIPTION_SUCCESS;
    }

    private void requireContact() {
        if (!UserPrincipal.current().isContact()) {
            throw BusinessException.forbidden("Assinaturas disponíveis apenas para clientes");
        }
    }
}
