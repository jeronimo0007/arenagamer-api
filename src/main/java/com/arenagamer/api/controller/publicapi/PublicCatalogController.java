package com.arenagamer.api.controller.publicapi;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PresetResponse;
import com.arenagamer.api.dto.response.PublicPlanResponse;
import com.arenagamer.api.dto.response.TeamSettingsResponse;
import com.arenagamer.api.dto.response.TournamentPricingResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.entity.enums.PublicTournamentFilter;
import com.arenagamer.api.service.CatalogService;
import com.arenagamer.api.service.PlanService;
import com.arenagamer.api.service.TeamSettingsService;
import com.arenagamer.api.service.TournamentPricingService;
import com.arenagamer.api.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public / Catálogo", description = "Listagens públicas — HTTP Basic Auth (email e senha)")
@SecurityRequirement(name = "BasicAuth")
public class PublicCatalogController {

    private final PlanService planService;
    private final TournamentService tournamentService;
    private final CatalogService catalogService;
    private final TournamentPricingService tournamentPricingService;
    private final TeamSettingsService teamSettingsService;

    @GetMapping("/plans")
    @Operation(summary = "Listar planos disponíveis")
    public ResponseEntity<ApiResponse<List<PublicPlanResponse>>> listPlans() {
        List<PublicPlanResponse> plans = planService.listPublic().stream()
                .map(PublicPlanResponse::from)
                .toList();
        return ApiResponses.listed(plans);
    }

    @GetMapping("/tournaments")
    @Operation(summary = "Listar torneios públicos")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> listTournaments(
            @RequestParam(required = false) PublicTournamentFilter filter,
            Pageable pageable) {
        return ApiResponses.listed(
                tournamentService.toResponsePage(tournamentService.listPublic(filter, pageable)));
    }

    @GetMapping("/presets")
    @Operation(summary = "Listar presets de jogos")
    public ResponseEntity<ApiResponse<List<PresetResponse>>> listPresets() {
        List<PresetResponse> presets = catalogService.listActivePresets().stream()
                .map(PresetResponse::from)
                .toList();
        return ApiResponses.listed(presets);
    }

    @GetMapping("/tournament-pricing")
    @Operation(summary = "Obter preços para criação de torneios")
    public ResponseEntity<ApiResponse<TournamentPricingResponse>> getTournamentPricing() {
        return ApiResponses.fetched(
                TournamentPricingResponse.from(tournamentPricingService.getSettings()));
    }

    @GetMapping("/team-settings")
    @Operation(summary = "Obter limites de times")
    public ResponseEntity<ApiResponse<TeamSettingsResponse>> getTeamSettings() {
        return ApiResponses.fetched(TeamSettingsResponse.from(teamSettingsService.getSettings()));
    }
}
