package com.arenagamer.api.controller;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.TeamResponse;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Gerenciamento de times")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Criar time")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody CreateTeamRequest request) {
        Team team = teamService.create(UserPrincipal.currentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Time criado", TeamResponse.from(team)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do time")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable Long id) {
        Team team = teamService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(TeamResponse.from(team)));
    }

    @GetMapping("/my")
    @Operation(summary = "Meus times")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> myTeams() {
        List<TeamResponse> teams = teamService.getMyTeams(UserPrincipal.currentId()).stream()
                .map(TeamResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }

    @PostMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Adicionar membro ao time")
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.addMember(teamId, userId, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok("Membro adicionado"));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Remover membro do time")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long teamId, @PathVariable Long userId) {
        teamService.removeMember(teamId, userId, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok("Membro removido"));
    }

    @PostMapping("/{teamId}/transfer/{newOwnerId}")
    @Operation(summary = "Transferir liderança do time")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable Long teamId, @PathVariable Long newOwnerId) {
        teamService.transferOwnership(teamId, newOwnerId, UserPrincipal.currentId());
        return ResponseEntity.ok(ApiResponse.ok("Liderança transferida"));
    }
}
