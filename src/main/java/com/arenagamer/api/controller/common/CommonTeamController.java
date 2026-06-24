package com.arenagamer.api.controller.common;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.dto.request.TeamAvailabilityChangeRequestDto;
import com.arenagamer.api.dto.request.UpdateTeamRequest;
import com.arenagamer.api.dto.response.*;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.security.UserPrincipal;
import com.arenagamer.api.service.TeamAvailabilityChangeService;
import com.arenagamer.api.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Common / Times", description = """
        Gerenciamento de times — JWT (cliente).
        Dono = **cliente** (empresa). Gerenciar: **contato primário** do cliente dono.
        Membros = **clientes** (não contatos). Transferência entre clientes.
        """)
@SecurityRequirement(name = "Bearer")
public class CommonTeamController {

    private final TeamService teamService;
    private final TeamAvailabilityChangeService teamAvailabilityChangeService;

    @PostMapping
    @Operation(summary = "Criar time", description = "Contato primário. Dono = cliente do contato.")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody CreateTeamRequest request) {
        Team team = teamService.create(UserPrincipal.current(), request);
        return ApiResponses.created(ApiMessages.TEAM_CREATED, teamService.toResponse(team, UserPrincipal.current()));
    }

    @GetMapping("/manageable")
    @Operation(summary = "Times que posso gerenciar", description = "Times do meu cliente quando sou contato primário.")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> manageableTeams() {
        return ApiResponses.listed(teamService.listManageableTeams(UserPrincipal.current()));
    }

    @GetMapping("/my")
    @Operation(summary = "Meus times", description = "Times em que meu cliente participa.")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> myTeams() {
        return ApiResponses.listed(teamService.listMyTeams(UserPrincipal.current()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar time", description = "Contato primário do cliente dono.")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @Parameter(description = "ID do time") @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request) {
        Team team = teamService.update(id, UserPrincipal.current(), request);
        return ApiResponses.updated(ApiMessages.TEAM_UPDATED, teamService.toResponse(team, UserPrincipal.current()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir time", description = "Contato primário do cliente dono. Bloqueado se inscrito em torneio.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id, UserPrincipal.current());
        return ApiResponses.deleted(ApiMessages.TEAM_DELETED);
    }

    @GetMapping("/{id}/manage")
    @Operation(summary = "Painel do time", description = "Detalhes, membros (clientes) e flag canManage.")
    public ResponseEntity<ApiResponse<TeamManagementResponse>> manage(@PathVariable Long id) {
        return ApiResponses.fetched(teamService.getTeamManagement(id, UserPrincipal.current()));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Listar clientes membros")
    public ResponseEntity<ApiResponse<List<TeamMemberClientResponse>>> members(@PathVariable Long id) {
        return ApiResponses.listed(teamService.listTeamMembers(id, UserPrincipal.current()));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalhes do time",
            description = """
                    Retorno conforme visibilidade e vínculo:
                    - PUBLIC: detalhes completos para todos
                    - PRIVATE: completos para membros; não vinculados recebem mensagem de time privado
                    - PROTECTED: completos para membros; resumo para não vinculados
                    Rank principal = jogo mais jogado; use presetId para filtrar.""")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getById(
            @Parameter(description = "ID do time") @PathVariable Long id,
            @Parameter(description = "Preset (jogo) para o rank exibido") @RequestParam(required = false) Long presetId) {
        return ApiResponses.fetched(teamService.getTeamDetails(id, UserPrincipal.current(), presetId));
    }

    @PostMapping("/{teamId}/members/clients/{clientUserId}")
    @Operation(summary = "Adicionar cliente ao time")
    public ResponseEntity<ApiResponse<Void>> addMemberClient(
            @PathVariable Long teamId,
            @Parameter(description = "userid do cliente") @PathVariable Integer clientUserId) {
        teamService.addMemberClient(teamId, clientUserId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.MEMBER_ADDED);
    }

    @DeleteMapping("/{teamId}/members/clients/{clientUserId}")
    @Operation(summary = "Remover cliente do time",
            description = "Dono (contato primário) remove qualquer membro, exceto o dono. "
                    + "Membro pode remover a si mesmo (clientUserId = userid do próprio cliente).")
    public ResponseEntity<ApiResponse<Void>> removeMemberClient(
            @PathVariable Long teamId,
            @PathVariable Integer clientUserId) {
        teamService.removeMemberClient(teamId, clientUserId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.MEMBER_REMOVED);
    }

    @PostMapping("/{teamId}/members/clients/{clientUserId}/captain")
    @Operation(summary = "Definir capitão do time", description = "Contato primário do cliente dono. Capitão pode inscrever o time em torneios, mas não gerencia o time.")
    public ResponseEntity<ApiResponse<Void>> setTeamCaptain(
            @PathVariable Long teamId,
            @Parameter(description = "userid do cliente membro") @PathVariable Integer clientUserId) {
        teamService.setTeamCaptain(teamId, clientUserId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.CAPTAIN_SET);
    }

    @PostMapping("/{teamId}/transfer/clients/{newClientUserId}")
    @Operation(summary = "Transferir time para outro cliente", description = "Contato primário do cliente dono atual.")
    public ResponseEntity<ApiResponse<Void>> transferTeam(
            @PathVariable Long teamId,
            @Parameter(description = "userid do novo cliente dono") @PathVariable Integer newClientUserId) {
        teamService.transferTeam(teamId, newClientUserId, UserPrincipal.current());
        return ApiResponses.okMessage(ApiMessages.TEAM_TRANSFERRED);
    }

    @PostMapping("/{teamId}/availability/change-requests")
    @Operation(summary = "Capitão solicita mudança de horários",
            description = "O dono do time aprova ou recusa. Enquanto pendente, bloqueia nova solicitação.")
    public ResponseEntity<ApiResponse<TeamAvailabilityChangeResponse>> requestAvailabilityChange(
            @PathVariable Long teamId,
            @Valid @RequestBody TeamAvailabilityChangeRequestDto request) {
        return ApiResponses.created(
                ApiMessages.AVAILABILITY_CHANGE_REQUESTED,
                teamAvailabilityChangeService.requestChange(teamId, UserPrincipal.current(), request));
    }

    @GetMapping("/{teamId}/availability/change-requests")
    @Operation(summary = "Listar solicitações de horários", description = "Somente dono do time.")
    public ResponseEntity<ApiResponse<List<TeamAvailabilityChangeResponse>>> listAvailabilityChangeRequests(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "false") boolean pendingOnly) {
        return ApiResponses.listed(
                teamAvailabilityChangeService.listRequests(teamId, UserPrincipal.current(), pendingOnly));
    }

    @PostMapping("/{teamId}/availability/change-requests/{requestId}/approve")
    @Operation(summary = "Aprovar horários propostos pelo capitão")
    public ResponseEntity<ApiResponse<TeamAvailabilityChangeResponse>> approveAvailabilityChange(
            @PathVariable Long teamId,
            @PathVariable Long requestId) {
        return ApiResponses.updated(
                ApiMessages.AVAILABILITY_CHANGE_APPROVED,
                teamAvailabilityChangeService.approve(teamId, requestId, UserPrincipal.current()));
    }

    @PostMapping("/{teamId}/availability/change-requests/{requestId}/reject")
    @Operation(summary = "Recusar solicitação de horários do capitão")
    public ResponseEntity<ApiResponse<TeamAvailabilityChangeResponse>> rejectAvailabilityChange(
            @PathVariable Long teamId,
            @PathVariable Long requestId) {
        return ApiResponses.updated(
                ApiMessages.AVAILABILITY_CHANGE_REJECTED,
                teamAvailabilityChangeService.reject(teamId, requestId, UserPrincipal.current()));
    }
}
