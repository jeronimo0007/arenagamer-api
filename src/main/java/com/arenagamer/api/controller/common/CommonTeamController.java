package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.dto.request.UpdateTeamRequest;
import com.arenagamer.api.dto.response.ApiMessages;
import com.arenagamer.api.dto.response.ApiResponse;
import com.arenagamer.api.dto.response.ApiResponses;
import com.arenagamer.api.dto.response.TeamResponse;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/common/teams")
@RequiredArgsConstructor
@Tag(name = "Common / Times", description = "Gerenciamento de times — JWT (staff ou cliente)")
@SecurityRequirement(name = "Bearer")
public class CommonTeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Criar time")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody CreateTeamRequest request) {
        Team team = teamService.create(UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TEAM_CREATED, TeamResponse.from(team));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar time")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request) {
        Team team = teamService.update(id, UserPrincipal.current(), request);
        return ApiResponses.updated(ApiMessages.TEAM_UPDATED, TeamResponse.from(team));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes do time")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable Long id) {
        return ApiResponses.fetched(TeamResponse.from(teamService.getById(id)));
    }

    @GetMapping("/my")
    @Operation(summary = "Meus times")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> myTeams() {
        return ApiResponses.listed(teamService.listMyTeams(UserPrincipal.current()));
    }

    @PostMapping("/{teamId}/members/{contactId}")
    @Operation(summary = "Adicionar membro")
    public ResponseEntity<ApiResponse<Void>> addMember(@PathVariable Long teamId, @PathVariable Integer contactId) {
        teamService.addMember(teamId, contactId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.MEMBER_ADDED);
    }

    @DeleteMapping("/{teamId}/members/{contactId}")
    @Operation(summary = "Remover membro")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long teamId, @PathVariable Integer contactId) {
        teamService.removeMember(teamId, contactId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.MEMBER_REMOVED);
    }

    @PostMapping("/{teamId}/transfer/{newOwnerContactId}")
    @Operation(summary = "Transferir liderança")
    public ResponseEntity<ApiResponse<Void>> transferOwnership(
            @PathVariable Long teamId, @PathVariable Integer newOwnerContactId) {
        teamService.transferOwnership(teamId, newOwnerContactId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.OWNERSHIP_TRANSFERRED);
    }
}
