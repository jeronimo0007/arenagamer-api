package com.arenagamer.api.controller.publicapi;

import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.PlayerDetailResponse;
import com.arenagamer.api.dto.response.PresetResponse;
import com.arenagamer.api.dto.response.PublicPlanResponse;
import com.arenagamer.api.dto.response.TeamDetailResponse;
import com.arenagamer.api.dto.response.TeamLeaderboardEntryResponse;
import com.arenagamer.api.dto.response.TeamResponse;
import com.arenagamer.api.dto.response.TeamSettingsResponse;
import com.arenagamer.api.dto.response.TournamentPricingResponse;
import com.arenagamer.api.dto.response.TournamentParticipantsResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.PublicTournamentFilter;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.CatalogService;
import com.arenagamer.api.service.PlanService;
import com.arenagamer.api.service.PlayerProfileService;
import com.arenagamer.api.service.TeamRankService;
import com.arenagamer.api.service.TeamService;
import com.arenagamer.api.service.TeamSettingsService;
import com.arenagamer.api.service.TournamentPricingService;
import com.arenagamer.api.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final TeamService teamService;
    private final TeamRankService teamRankService;
    private final PlayerProfileService playerProfileService;

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

    @GetMapping("/tournaments/{slug}/participants")
    @Operation(summary = "Listar inscritos no torneio (público)",
            description = "Torneio PUBLIC ou PROTECTED. SOLO → player; TEAM → team + escalação.")
    public ResponseEntity<ApiResponse<TournamentParticipantsResponse>> listTournamentParticipants(
            @PathVariable String slug) {
        AuthenticatedUser auth = UserPrincipal.tryCurrent().orElse(null);
        return ApiResponses.listed(
                tournamentService.listParticipants(slug, auth, ParticipantStatus.APPROVED));
    }

    @GetMapping("/presets")
    @Operation(summary = "Listar ou pesquisar presets (jogos)",
            description = """
                    Sem parâmetro: todos os presets ativos, ordenados por nome.
                    Com q: filtra por nome do jogo ou plataforma (contém, case insensitive).""")
    public ResponseEntity<ApiResponse<List<PresetResponse>>> listPresets(
            @Parameter(description = "Texto para buscar no nome do jogo ou plataforma")
            @RequestParam(required = false)
            @Size(max = 100)
            String q) {
        List<PresetResponse> presets = catalogService.searchPresets(q, true).stream()
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

    @GetMapping("/teams")
    @Operation(
            summary = "Listar times públicos e protegidos",
            description = "Times PUBLIC e PROTECTED ativos. PRIVATE não aparecem.")
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> listPublicTeams(Pageable pageable) {
        return ApiResponses.listed(teamService.listPublicTeams(pageable));
    }

    @GetMapping("/teams/{id}")
    @Operation(
            summary = "Detalhes do time",
            description = """
                    PUBLIC/PROTECTED ativos. PRIVATE retorna mensagem de acesso restrito.
                    PROTECTED: resumo para não vinculados. presetId opcional para rank.""")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getPublicTeam(
            @Parameter(description = "ID do time", example = "1") @PathVariable Long id,
            @Parameter(description = "Preset (jogo) para o rank exibido") @RequestParam(required = false) Long presetId) {
        AuthenticatedUser auth = UserPrincipal.tryCurrent().orElse(null);
        return ApiResponses.fetched(teamService.getDiscoverableTeamDetails(id, auth, presetId));
    }

    @GetMapping("/players/{clientUserId}")
    @Operation(
            summary = "Detalhes do jogador",
            description = """
                    PUBLIC/PROTECTED ativos. PRIVATE retorna mensagem de acesso restrito.
                    PROTECTED: resumo (nickname + rank). presetId opcional.""")
    public ResponseEntity<ApiResponse<PlayerDetailResponse>> getPlayer(
            @Parameter(description = "ID do jogador (clientUserId)", example = "1") @PathVariable Integer clientUserId,
            @Parameter(description = "Preset (jogo) para o rank exibido") @RequestParam(required = false) Long presetId) {
        AuthenticatedUser auth = UserPrincipal.tryCurrent().orElse(null);
        return ApiResponses.fetched(
                playerProfileService.getDiscoverablePlayerDetails(clientUserId, auth, presetId));
    }

    @GetMapping("/teams/ranks/global")
    @Operation(summary = "Ranking global por jogo", description = "Times públicos. Ordenado por rankPoints DESC.")
    public ResponseEntity<ApiResponse<Page<TeamLeaderboardEntryResponse>>> globalRanks(
            @Parameter(description = "ID do preset (jogo)", required = true) @RequestParam Long presetId,
            Pageable pageable) {
        return ApiResponses.listed(teamRankService.getGlobalLeaderboard(presetId, pageable));
    }

    @GetMapping("/teams/ranks/regional")
    @Operation(summary = "Ranking regional por jogo", description = "Times públicos filtrados por estado do cliente.")
    public ResponseEntity<ApiResponse<Page<TeamLeaderboardEntryResponse>>> regionalRanks(
            @Parameter(description = "ID do preset (jogo)", required = true) @RequestParam Long presetId,
            @Parameter(description = "Estado/região (ex.: SP)", required = true) @RequestParam String state,
            Pageable pageable) {
        return ApiResponses.listed(teamRankService.getRegionalLeaderboard(presetId, state, pageable));
    }
}
