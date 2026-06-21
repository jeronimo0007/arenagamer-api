package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.AdminSubscriptionAssignRequest;
import com.arenagamer.api.dto.response.AdminSubscriptionResponse;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.UserPlanResponse;
import com.arenagamer.api.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Assinaturas", description = "Gestão manual de planos por cliente — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Listar assinaturas ativas de clientes")
    public ResponseEntity<ApiResponse<Page<AdminSubscriptionResponse>>> listSubscriptions(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<AdminSubscriptionResponse> page = subscriptionService.listActiveSubscriptions(search, pageable);
        return ApiResponses.listed(page);
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "Listar assinantes ativos de um plano")
    public ResponseEntity<ApiResponse<Page<AdminSubscriptionResponse>>> listByPlan(
            @Parameter(description = "ID do plano") @PathVariable Long planId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        Page<AdminSubscriptionResponse> page = subscriptionService.listActiveSubscriptionsByPlan(planId, search, pageable);
        return ApiResponses.listed(page);
    }

    @GetMapping("/client/{clientUserId}")
    @Operation(summary = "Plano ativo de um cliente")
    public ResponseEntity<ApiResponse<AdminSubscriptionResponse>> getByClient(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId) {
        AdminSubscriptionResponse subscription = subscriptionService.getActiveSubscriptionForAdmin(clientUserId);
        return ApiResponses.fetched(subscription);
    }

    @PutMapping("/client/{clientUserId}")
    @Operation(summary = "Atribuir ou trocar plano de um cliente")
    public ResponseEntity<ApiResponse<UserPlanResponse>> assignPlan(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId,
            @Valid @RequestBody AdminSubscriptionAssignRequest request) {
        UserPlanResponse plan = subscriptionService.adminAssignPlan(
                clientUserId,
                request.getPlanId(),
                request.getBillingPeriodMonths());
        return ApiResponses.updated(ApiMessages.SUBSCRIPTION_SUCCESS, plan);
    }

    @DeleteMapping("/client/{clientUserId}")
    @Operation(summary = "Remover plano ativo de um cliente")
    public ResponseEntity<ApiResponse<Void>> removePlan(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId) {
        subscriptionService.adminRemovePlan(clientUserId);
        return ApiResponses.deleted(ApiMessages.SUBSCRIPTION_REMOVED);
    }

    @PostMapping("/client/{clientUserId}/reset-usage")
    @Operation(summary = "Resetar contagem mensal e agendamentos do plano ativo")
    public ResponseEntity<ApiResponse<UserPlanResponse>> resetUsage(
            @Parameter(description = "ID do cliente Perfex (userid)") @PathVariable Integer clientUserId) {
        UserPlanResponse plan = subscriptionService.adminResetSubscriptionUsage(clientUserId);
        return ApiResponses.updated(ApiMessages.SUBSCRIPTION_USAGE_RESET, plan);
    }
}
