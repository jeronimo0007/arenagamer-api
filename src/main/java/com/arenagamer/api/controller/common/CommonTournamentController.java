package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.dto.request.UpdateTournamentRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.MatchResponse;
import com.arenagamer.api.dto.response.TournamentManagerResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.entity.Match;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.repository.MatchRepository;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.BracketService;
import com.arenagamer.api.service.SchedulingService;
import com.arenagamer.api.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/common/tournaments")
@RequiredArgsConstructor
@Tag(name = "Common / Torneios", description = "Torneios — JWT (staff ou cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonTournamentController {

    private final TournamentService tournamentService;
    private final BracketService bracketService;
    private final SchedulingService schedulingService;
    private final MatchRepository matchRepository;

    @PostMapping
    @Operation(summary = "Criar torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> create(@Valid @RequestBody CreateTournamentRequest request) {
        Tournament tournament = tournamentService.create(UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TOURNAMENT_CREATED, tournamentService.toResponse(tournament));
    }

    @PutMapping("/{slug}")
    @Operation(summary = "Atualizar torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> update(
            @PathVariable String slug, @Valid @RequestBody UpdateTournamentRequest request) {
        Tournament tournament = tournamentService.update(slug, UserPrincipal.current(), request);
        return ApiResponses.updated(ApiMessages.TOURNAMENT_UPDATED, tournamentService.toResponse(tournament));
    }

    @GetMapping("/my-created")
    @Operation(summary = "Meus torneios criados")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> myCreated(Pageable pageable) {
        return ApiResponses.listed(tournamentService.toResponsePage(
                tournamentService.listMyCreated(UserPrincipal.current(), pageable)));
    }

    @GetMapping("/my-managed")
    @Operation(summary = "Torneios que posso gerenciar")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> myManaged(Pageable pageable) {
        return ApiResponses.listed(tournamentService.toResponsePage(
                tournamentService.listMyManaged(UserPrincipal.current(), pageable)));
    }

    @GetMapping("/my-joined")
    @Operation(summary = "Torneios que participo")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> myJoined(Pageable pageable) {
        return ApiResponses.listed(tournamentService.toResponsePage(
                tournamentService.listMyJoined(UserPrincipal.current(), pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalhes do torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> getBySlug(@PathVariable String slug) {
        return ApiResponses.fetched(tournamentService.toResponse(tournamentService.getBySlug(slug)));
    }

    @PutMapping("/{slug}/status")
    @Operation(summary = "Atualizar status")
    public ResponseEntity<ApiResponse<TournamentResponse>> updateStatus(
            @PathVariable String slug, @RequestParam TournamentStatus status) {
        Tournament tournament = tournamentService.updateStatus(slug, status, UserPrincipal.current());
        return ApiResponses.updated(ApiMessages.TOURNAMENT_UPDATED, tournamentService.toResponse(tournament));
    }

    @DeleteMapping("/{slug}")
    @Operation(summary = "Cancelar torneio")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String slug) {
        tournamentService.deleteTournament(slug, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.TOURNAMENT_CANCELLED);
    }

    @PostMapping("/{slug}/participants")
    @Operation(summary = "Inscrever-se (solo)")
    public ResponseEntity<ApiResponse<Long>> joinSolo(
            @PathVariable String slug, @RequestBody(required = false) JoinTournamentRequest request) {
        if (request == null) request = new JoinTournamentRequest();
        TournamentParticipant p = tournamentService.joinSolo(slug, UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TOURNAMENT_JOINED, p.getId());
    }

    @PostMapping("/{slug}/participants/team")
    @Operation(summary = "Inscrever time")
    public ResponseEntity<ApiResponse<Long>> joinTeam(
            @PathVariable String slug, @Valid @RequestBody JoinTournamentRequest request) {
        TournamentParticipant p = tournamentService.joinTeam(slug, UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TEAM_JOINED, p.getId());
    }

    @DeleteMapping("/{slug}/participants/{participantId}")
    @Operation(summary = "Expulsar participante")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable String slug, @PathVariable Long participantId) {
        tournamentService.kickParticipant(slug, participantId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.PARTICIPANT_REMOVED);
    }

    @PostMapping("/{slug}/generate-bracket")
    @Operation(summary = "Gerar chaves")
    public ResponseEntity<ApiResponse<Void>> generateBracket(@PathVariable String slug) {
        tournamentService.validateOwnership(slug, UserPrincipal.current());
        bracketService.generateBracket(slug);
        return ApiResponses.okMessage(ApiMessages.BRACKET_GENERATED);
    }

    @GetMapping("/{slug}/matches")
    @Operation(summary = "Listar partidas")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> listMatches(@PathVariable String slug) {
        Tournament tournament = tournamentService.getBySlug(slug);
        List<MatchResponse> matches = matchRepository.findByTournamentId(tournament.getId()).stream()
                .map(MatchResponse::from).toList();
        return ApiResponses.listed(matches);
    }

    @PostMapping("/{slug}/schedule")
    @Operation(summary = "Agendar partidas")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> schedule(@PathVariable String slug) {
        tournamentService.validateOwnership(slug, UserPrincipal.current());
        List<MatchResponse> matches = schedulingService.scheduleMatches(slug).stream()
                .map(MatchResponse::from).toList();
        return ApiResponses.ok(ApiMessages.MATCHES_SCHEDULED, matches);
    }

    @PutMapping("/matches/{matchId}/reschedule")
    @Operation(summary = "Reagendar partida")
    public ResponseEntity<ApiResponse<MatchResponse>> reschedule(
            @PathVariable Long matchId, @RequestParam LocalDateTime newTime) {
        schedulingService.validateReschedulePermission(matchId, UserPrincipal.current());
        Match match = schedulingService.reschedule(matchId, newTime);
        return ApiResponses.updated(ApiMessages.MATCH_RESCHEDULED, MatchResponse.from(match));
    }

    @GetMapping("/{slug}/managers")
    @Operation(summary = "Listar contatos com permissão de gestão")
    public ResponseEntity<ApiResponse<List<TournamentManagerResponse>>> listManagers(@PathVariable String slug) {
        return ApiResponses.listed(tournamentService.listManagers(slug, UserPrincipal.current()));
    }

    @PostMapping("/{slug}/managers/{contactId}")
    @Operation(summary = "Conceder permissão de gestão a contato")
    public ResponseEntity<ApiResponse<TournamentManagerResponse>> grantManager(
            @PathVariable String slug, @PathVariable Integer contactId) {
        TournamentManagerResponse manager = tournamentService.grantManager(slug, contactId, UserPrincipal.current());
        return ApiResponses.created("Permissão concedida", manager);
    }

    @DeleteMapping("/{slug}/managers/{contactId}")
    @Operation(summary = "Revogar permissão de gestão de contato")
    public ResponseEntity<ApiResponse<Void>> revokeManager(
            @PathVariable String slug, @PathVariable Integer contactId) {
        tournamentService.revokeManager(slug, contactId, UserPrincipal.current());
        return ApiResponses.okMessage("Permissão revogada");
    }
}
