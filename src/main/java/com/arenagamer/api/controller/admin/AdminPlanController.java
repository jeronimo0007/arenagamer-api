package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.PlanRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PlanResponse;
import com.arenagamer.api.entity.Plan;
import com.arenagamer.api.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Planos", description = "Gerenciamento de planos — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminPlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "Listar planos")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> listPlans() {
        List<PlanResponse> plans = planService.listAll().stream().map(PlanResponse::from).toList();
        return ApiResponses.listed(plans);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do plano")
    public ResponseEntity<ApiResponse<PlanResponse>> getPlan(
            @Parameter(description = "ID do plano", example = "1") @PathVariable Long id) {
        return ApiResponses.fetched(PlanResponse.from(planService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Criar plano")
    public ResponseEntity<ApiResponse<PlanResponse>> createPlan(@Valid @RequestBody PlanRequest request) {
        Plan plan = planService.create(request);
        return ApiResponses.created(ApiMessages.PLAN_CREATED, PlanResponse.from(plan));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar plano")
    public ResponseEntity<ApiResponse<PlanResponse>> updatePlan(
            @Parameter(description = "ID do plano", example = "1") @PathVariable Long id,
            @Valid @RequestBody PlanRequest request) {
        Plan plan = planService.update(id, request);
        return ApiResponses.updated(ApiMessages.PLAN_UPDATED, PlanResponse.from(plan));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover plano")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @Parameter(description = "ID do plano", example = "1") @PathVariable Long id) {
        planService.delete(id);
        return ApiResponses.deleted(ApiMessages.PLAN_DELETED);
    }
}
