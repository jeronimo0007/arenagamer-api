package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.TeamJoinRequestResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamJoinRequest;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.entity.enums.TeamJoinRequestStatus;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.TeamJoinRequestRepository;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamJoinRequestService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamJoinRequestRepository joinRequestRepository;
    private final ClientRepository clientRepository;
    private final TeamJoinBanService teamJoinBanService;
    private final TeamSettingsService teamSettingsService;
    private final IdentityService identityService;

    @Transactional
    public TeamJoinRequestResponse inviteMember(Long teamId, Integer memberClientUserId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireTeamManager(team, requester);

        validateMembershipEligibility(team, memberClientUserId);
        teamJoinBanService.requireCanJoinTeam(memberClientUserId);

        cancelPendingInvites(team.getId(), memberClientUserId);

        TeamJoinRequest request = joinRequestRepository.save(TeamJoinRequest.builder()
                .team(team)
                .client(clientRepository.findById(memberClientUserId)
                        .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado")))
                .invitedBy(requester)
                .build());

        return TeamJoinRequestResponse.from(request);
    }

    @Transactional(readOnly = true)
    public List<TeamJoinRequestResponse> listForTeam(Long teamId, AuthenticatedUser auth, boolean pendingOnly) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireTeamManager(team, requester);

        TeamJoinRequestStatus statusFilter = pendingOnly ? TeamJoinRequestStatus.PENDING : null;
        return joinRequestRepository.findByTeamIdWithDetails(teamId, statusFilter).stream()
                .map(TeamJoinRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamJoinRequestResponse> listReceived(AuthenticatedUser auth, boolean pendingOnly) {
        Contact requester = identityService.requireContact(auth);
        if (requester.getUserid() == null) {
            throw BusinessException.badRequest("Contato sem cliente vinculado");
        }

        TeamJoinRequestStatus statusFilter = pendingOnly ? TeamJoinRequestStatus.PENDING : null;
        return joinRequestRepository.findByClientUserIdWithDetails(requester.getUserid(), statusFilter).stream()
                .map(TeamJoinRequestResponse::from)
                .toList();
    }

    @Transactional
    public TeamJoinRequestResponse accept(Long requestId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        if (requester.getUserid() == null) {
            throw BusinessException.badRequest("Contato sem cliente vinculado");
        }

        TeamJoinRequest request = joinRequestRepository.findByIdAndClient_UserIdWithDetails(requestId, requester.getUserid())
                .orElseThrow(() -> BusinessException.notFound("Convite não encontrado"));

        if (request.getStatus() != TeamJoinRequestStatus.PENDING) {
            throw BusinessException.badRequest("Convite já foi resolvido");
        }

        teamJoinBanService.requireCanJoinTeam(requester.getUserid());

        Team team = request.getTeam();
        validateMembershipEligibility(team, requester.getUserid());

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .client(request.getClient())
                .build());

        request.setStatus(TeamJoinRequestStatus.ACCEPTED);
        request.setResolvedAt(LocalDateTime.now());
        return TeamJoinRequestResponse.from(joinRequestRepository.save(request));
    }

    @Transactional
    public void cancelPendingInvites(Long teamId, Integer clientUserId) {
        joinRequestRepository.updateStatusByTeamAndClient(
                teamId, clientUserId, TeamJoinRequestStatus.PENDING, TeamJoinRequestStatus.CANCELLED);
    }

    @Transactional
    public void onMemberAddedDirectly(Long teamId, Integer clientUserId) {
        cancelPendingInvites(teamId, clientUserId);
    }

    @Transactional
    public void onMemberLeftTeam(Long teamId, Integer clientUserId) {
        cancelPendingInvites(teamId, clientUserId);
    }

    private void validateMembershipEligibility(Team team, Integer memberClientUserId) {
        TeamSettings settings = teamSettingsService.getSettings();

        if (memberClientUserId.equals(team.getClient().getUserId())) {
            throw BusinessException.badRequest("O cliente dono já é membro do time");
        }

        if (teamMemberRepository.existsByTeamIdAndClient_UserId(team.getId(), memberClientUserId)) {
            throw BusinessException.conflict("Cliente já é membro do time");
        }

        if (teamMemberRepository.countByClient_UserId(memberClientUserId) >= settings.getMaxParticipatedTeamsPerClient()) {
            throw BusinessException.conflict("Cliente atingiu o limite de participação em times");
        }

        clientRepository.findById(memberClientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
    }

    private void requireTeamManager(Team team, Contact requester) {
        boolean isOwnerManager = requester.getIsPrimary() != null
                && requester.getIsPrimary() == 1
                && team.getClient() != null
                && team.getClient().getUserId().equals(requester.getUserid());
        if (!isOwnerManager) {
            throw BusinessException.forbidden("Apenas o contato primário do cliente dono pode gerenciar convites");
        }
    }
}
