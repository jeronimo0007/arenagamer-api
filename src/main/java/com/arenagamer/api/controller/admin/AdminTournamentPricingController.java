package com.arenagamer.api.controller.admin;

import com.arenagamer.api.dto.request.TournamentPricingRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.TournamentPricingResponse;
import com.arenagamer.api.entity.TournamentPricingSettings;
import com.arenagamer.api.service.TournamentPricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/tournament-pricing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
@Tag(name = "Admin / Preços de torneio", description = "Configuração de preços para criação de torneios — JWT (staff)")
@SecurityRequirement(name = "Bearer")
public class AdminTournamentPricingController {

    private final TournamentPricingService tournamentPricingService;

    @GetMapping
    @Operation(summary = "Obter configuração de preços de torneio")
    public ResponseEntity<ApiResponse<TournamentPricingResponse>> get() {
        TournamentPricingSettings settings = tournamentPricingService.getSettings();
        return ApiResponses.fetched(TournamentPricingResponse.from(settings));
    }

    @PutMapping
    @Operation(summary = "Atualizar configuração de preços de torneio")
    public ResponseEntity<ApiResponse<TournamentPricingResponse>> update(
            @Valid @RequestBody TournamentPricingRequest request) {
        TournamentPricingSettings settings = tournamentPricingService.update(request);
        return ApiResponses.updated(ApiMessages.TOURNAMENT_PRICING_UPDATED, TournamentPricingResponse.from(settings));
    }
}
