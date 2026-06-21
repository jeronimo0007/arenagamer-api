package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.CreditTierRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.CreditTierResponse;
import com.arenagamer.api.entity.CreditTier;
import com.arenagamer.api.service.CreditTierService;
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
@RequestMapping("/api/v1/admin/credit-tiers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Tiers de créditos", description = "Gerenciamento de tiers de créditos — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminCreditTierController {

    private final CreditTierService creditTierService;

    @GetMapping
    @Operation(summary = "Listar tiers de créditos")
    public ResponseEntity<ApiResponse<List<CreditTierResponse>>> list() {
        List<CreditTierResponse> tiers = creditTierService.listAll().stream()
                .map(CreditTierResponse::from)
                .toList();
        return ApiResponses.listed(tiers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do tier")
    public ResponseEntity<ApiResponse<CreditTierResponse>> getById(
            @Parameter(description = "ID do tier", example = "1") @PathVariable Long id) {
        return ApiResponses.fetched(CreditTierResponse.from(creditTierService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Criar tier de créditos")
    public ResponseEntity<ApiResponse<CreditTierResponse>> create(@Valid @RequestBody CreditTierRequest request) {
        CreditTier tier = creditTierService.create(request);
        return ApiResponses.created(ApiMessages.CREDIT_TIER_CREATED, CreditTierResponse.from(tier));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tier de créditos")
    public ResponseEntity<ApiResponse<CreditTierResponse>> update(
            @Parameter(description = "ID do tier", example = "1") @PathVariable Long id,
            @Valid @RequestBody CreditTierRequest request) {
        CreditTier tier = creditTierService.update(id, request);
        return ApiResponses.updated(ApiMessages.CREDIT_TIER_UPDATED, CreditTierResponse.from(tier));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover tier de créditos")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID do tier", example = "1") @PathVariable Long id) {
        creditTierService.delete(id);
        return ApiResponses.deleted(ApiMessages.CREDIT_TIER_DELETED);
    }
}
