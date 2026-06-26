package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.dto.request.UpdateTournamentRequest;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.MatchResponse;
import com.arenagamer.api.dto.response.TournamentManagerResponse;
import com.arenagamer.api.dto.response.TournamentParticipantsResponse;
import com.arenagamer.api.dto.response.TournamentResponse;
import com.arenagamer.api.dto.response.TournamentRevenueResponse;
import com.arenagamer.api.dto.response.TournamentStandingEntryResponse;
import com.arenagamer.api.entity.Match;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.ParticipantStatus;
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

    @GetMapping("/{slug}/entry-fees/revenue")
    @Operation(summary = "Arrecadação de taxas de entrada",
            description = """
                    Total arrecadado e detalhamento por inscrição.
                    Cada item indica quem pagou (payerClientUserId) e o time ou jogador inscrito.
                    Somente organizadores do torneio. Reembolsos reduzem o total arrecadado.""")
    public ResponseEntity<ApiResponse<TournamentRevenueResponse>> getEntryFeeRevenue(
            @PathVariable String slug,
            @RequestParam(required = false) Integer clientUserId) {
        return ApiResponses.fetched(
                tournamentService.getEntryFeeRevenue(slug, UserPrincipal.current(), clientUserId));
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

    @GetMapping("/{slug}/participants")
    @Operation(summary = "Listar inscritos no torneio",
            description = """
                    format = SOLO: cada item traz player (jogador).
                    format = TEAM: cada item traz team (nome, tag, logo, escalação).
                    Por padrão retorna status APPROVED. Organizador pode filtrar com ?status=.""")
    public ResponseEntity<ApiResponse<TournamentParticipantsResponse>> listParticipants(
            @PathVariable String slug,
            @RequestParam(required = false) ParticipantStatus status) {
        return ApiResponses.listed(
                tournamentService.listParticipants(slug, UserPrincipal.current(), status));
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
    @Operation(summary = "Inscrever time", description = """
            Escale os jogadores com playerNicknames (ou playerClientUserIds).
            Conflito só entre jogadores escalados — ser membro de outro time não bloqueia.
            Sem escalação explícita, todos os membros do time são considerados escalados.""")
    public ResponseEntity<ApiResponse<Long>> joinTeam(
            @PathVariable String slug, @Valid @RequestBody JoinTournamentRequest request) {
        TournamentParticipant p = tournamentService.joinTeam(slug, UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TEAM_JOINED, p.getId());
    }

    @DeleteMapping("/{slug}/participants/{participantId}")
    @Operation(summary = "Expulsar participante", description = "Somente organizador do torneio.")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable String slug, @PathVariable Long participantId) {
        tournamentService.kickParticipant(slug, participantId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.PARTICIPANT_REMOVED);
    }

    @DeleteMapping("/{slug}/registration")
    @Operation(summary = "Desinscrever-se do torneio",
            description = """
                    Se o torneio tiver startDate, permitido até o dia anterior ao início.
                    Sem data de início, pode desinscrever enquanto o torneio não tiver começado.
                    Solo: sem parâmetros. Time: informe teamId.
                    Devolve a taxa de inscrição retida, se houver.""")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable String slug,
            @RequestParam(required = false) Long teamId) {
        tournamentService.withdrawFromTournament(slug, UserPrincipal.current(), teamId);
        return ApiResponses.okMessage(ApiMessages.TOURNAMENT_WITHDRAWN);
    }

    @PostMapping("/{slug}/generate-bracket")
    @Operation(summary = "Gerar chaves")
    public ResponseEntity<ApiResponse<Void>> generateBracket(@PathVariable String slug) {
        tournamentService.validateOwnership(slug, UserPrincipal.current());
        bracketService.generateBracket(slug);
        schedulingService.scheduleMatches(slug);
        return ApiResponses.okMessage(ApiMessages.BRACKET_GENERATED);
    }

    @PostMapping("/{slug}/advance-round")
    @Operation(summary = "Gerar próxima fase da chave")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> advanceRound(@PathVariable String slug) {
        bracketService.advanceToNextRound(slug, UserPrincipal.current());
        List<MatchResponse> matches = schedulingService.scheduleMatches(slug).stream()
                .map(MatchResponse::from).toList();
        return ApiResponses.ok(ApiMessages.ROUND_ADVANCED, matches);
    }

    @PostMapping("/{slug}/generate-knockout")
    @Operation(summary = "Gerar mata-mata da fase de grupos")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> generateKnockout(@PathVariable String slug) {
        bracketService.generateGroupPlayoffs(slug, UserPrincipal.current());
        List<MatchResponse> matches = schedulingService.scheduleMatches(slug).stream()
                .map(MatchResponse::from).toList();
        return ApiResponses.ok(ApiMessages.KNOCKOUT_GENERATED, matches);
    }

    @GetMapping("/{slug}/matches")
    @Operation(summary = "Listar partidas")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> listMatches(@PathVariable String slug) {
        Tournament tournament = tournamentService.getBySlug(slug);
        List<MatchResponse> matches = matchRepository.findByTournamentIdWithParticipants(tournament.getId()).stream()
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

    @GetMapping("/{slug}/standings")
    @Operation(summary = "Classificação / posições do torneio")
    public ResponseEntity<ApiResponse<List<TournamentStandingEntryResponse>>> standings(@PathVariable String slug) {
        return ApiResponses.listed(bracketService.computeStandings(slug));
    }

    @PostMapping("/{slug}/finalize")
    @Operation(summary = "Finalizar torneio")
    public ResponseEntity<ApiResponse<Void>> finalizeTournament(@PathVariable String slug) {
        bracketService.finalizeTournament(slug, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.TOURNAMENT_FINALIZED);
    }

    @PostMapping("/matches/{matchId}/result")
    @Operation(summary = "Registrar vencedor da partida")
    public ResponseEntity<ApiResponse<MatchResponse>> recordResult(
            @PathVariable Long matchId,
            @RequestParam(required = false) Long winnerParticipantId,
            @RequestParam Integer homeScore,
            @RequestParam Integer awayScore,
            @RequestParam(required = false) String proofUrl) {
        bracketService.recordResult(
                matchId, winnerParticipantId, homeScore, awayScore, proofUrl, UserPrincipal.current());
        Match match = matchRepository.findByIdWithParticipants(matchId)
                .orElseThrow(() -> BusinessException.notFound("Partida não encontrada"));
        return ApiResponses.updated(ApiMessages.MATCH_RESULT_RECORDED, MatchResponse.from(match));
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
