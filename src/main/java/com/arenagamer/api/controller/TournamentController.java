package com.arenagamer.api.controller;

import com.arenagamer.api.dto.request.CreateTournamentRequest;
import com.arenagamer.api.dto.request.JoinTournamentRequest;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.MatchResponse;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Gerenciamento de torneios")
public class TournamentController {

    private final TournamentService tournamentService;
    private final BracketService bracketService;
    private final SchedulingService schedulingService;
    private final MatchRepository matchRepository;

    @PostMapping
    @Operation(summary = "Criar torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> create(@Valid @RequestBody CreateTournamentRequest request) {
        Tournament tournament = tournamentService.create(UserPrincipal.currentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Torneio criado", TournamentResponse.from(tournament)));
    }

    @GetMapping
    @Operation(summary = "Listar torneios públicos")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> listPublic(Pageable pageable) {
        Page<TournamentResponse> page = tournamentService.listPublic(pageable).map(TournamentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/my-created")
    @Operation(summary = "Meus torneios criados")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> myCreated(Pageable pageable) {
        Page<TournamentResponse> page = tournamentService.listMyCreated(UserPrincipal.currentId(), pageable)
                .map(TournamentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/my-joined")
    @Operation(summary = "Torneios que participo")
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> myJoined(Pageable pageable) {
        Page<TournamentResponse> page = tournamentService.listMyJoined(UserPrincipal.currentId(), pageable)
                .map(TournamentResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Detalhes do torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> getBySlug(@PathVariable String slug) {
        Tournament tournament = tournamentService.getBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(TournamentResponse.from(tournament)));
    }

    @PutMapping("/{slug}/status")
    @Operation(summary = "Atualizar status do torneio")
    public ResponseEntity<ApiResponse<TournamentResponse>> updateStatus(
            @PathVariable String slug, @RequestParam TournamentStatus status) {
        Tournament tournament = tournamentService.updateStatus(slug, status, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok(TournamentResponse.from(tournament)));
    }

    @DeleteMapping("/{slug}")
    @Operation(summary = "Cancelar torneio")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String slug) {
        tournamentService.deleteTournament(slug, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok("Torneio cancelado"));
    }

    // --- Participants ---

    @PostMapping("/{slug}/participants")
    @Operation(summary = "Inscrever-se (solo)")
    public ResponseEntity<ApiResponse<Long>> joinSolo(
            @PathVariable String slug, @RequestBody(required = false) JoinTournamentRequest request) {
        if (request == null) request = new JoinTournamentRequest();
        TournamentParticipant p = tournamentService.joinSolo(slug, UserPrincipal.currentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Inscrito com sucesso", p.getId()));
    }

    @PostMapping("/{slug}/participants/team")
    @Operation(summary = "Inscrever time")
    public ResponseEntity<ApiResponse<Long>> joinTeam(
            @PathVariable String slug, @Valid @RequestBody JoinTournamentRequest request) {
        TournamentParticipant p = tournamentService.joinTeam(slug, UserPrincipal.currentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Time inscrito com sucesso", p.getId()));
    }

    @DeleteMapping("/{slug}/participants/{participantId}")
    @Operation(summary = "Expulsar participante")
    public ResponseEntity<ApiResponse<Void>> kick(
            @PathVariable String slug, @PathVariable Long participantId) {
        tournamentService.kickParticipant(slug, participantId, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok("Participante removido"));
    }

    // --- Bracket & Matches ---

    @PostMapping("/{slug}/generate-bracket")
    @Operation(summary = "Gerar chaves do torneio")
    public ResponseEntity<ApiResponse<Void>> generateBracket(@PathVariable String slug) {
        bracketService.generateBracket(slug);
        return ResponseEntity.ok(ApiResponse.ok("Chaves geradas com sucesso"));
    }

    @GetMapping("/{slug}/matches")
    @Operation(summary = "Listar partidas do torneio")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> listMatches(@PathVariable String slug) {
        Tournament tournament = tournamentService.getBySlug(slug);
        List<MatchResponse> matches = matchRepository.findByTournamentId(tournament.getId()).stream()
                .map(MatchResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(matches));
    }

    // --- Scheduling ---

    @PostMapping("/{slug}/schedule")
    @Operation(summary = "Agendar partidas automaticamente")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> schedule(@PathVariable String slug) {
        List<MatchResponse> matches = schedulingService.scheduleMatches(slug).stream()
                .map(MatchResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok("Partidas agendadas", matches));
    }

    @PutMapping("/matches/{matchId}/reschedule")
    @Operation(summary = "Reagendar partida")
    public ResponseEntity<ApiResponse<MatchResponse>> reschedule(
            @PathVariable Long matchId, @RequestParam LocalDateTime newTime) {
        Match match = schedulingService.reschedule(matchId, newTime);
        return ResponseEntity.ok(ApiResponse.ok(MatchResponse.from(match)));
    }
}
