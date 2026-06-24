package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.TeamAvailabilityChangeRequestDto;
import com.arenagamer.api.dto.request.WeeklyAvailabilitySlotRequest;
import com.arenagamer.api.dto.response.TeamAvailabilityChangeResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamAvailabilityChangeRequest;
import com.arenagamer.api.entity.TeamAvailabilityRequestSlot;
import com.arenagamer.api.entity.enums.AvailabilityChangeStatus;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TeamAvailabilityChangeRequestRepository;
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
public class TeamAvailabilityChangeService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamAvailabilityChangeRequestRepository changeRequestRepository;
    private final AvailabilityService availabilityService;
    private final IdentityService identityService;

    @Transactional
    public TeamAvailabilityChangeResponse requestChange(
            Long teamId, AuthenticatedUser auth, TeamAvailabilityChangeRequestDto request) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireCaptain(requester, team);

        availabilityService.validateWeeklySlots(request.getWeeklySlots());

        if (changeRequestRepository.existsByTeam_IdAndStatus(teamId, AvailabilityChangeStatus.PENDING)) {
            throw BusinessException.conflict("Já existe uma solicitação de horários pendente para este time");
        }

        TeamAvailabilityChangeRequest changeRequest = TeamAvailabilityChangeRequest.builder()
                .team(team)
                .requestedBy(requester)
                .message(request.getMessage())
                .build();
        for (WeeklyAvailabilitySlotRequest slot : request.getWeeklySlots()) {
            changeRequest.getSlots().add(TeamAvailabilityRequestSlot.builder()
                    .request(changeRequest)
                    .dayOfWeek(slot.getDayOfWeek())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .build());
        }
        return TeamAvailabilityChangeResponse.from(changeRequestRepository.save(changeRequest));
    }

    @Transactional(readOnly = true)
    public List<TeamAvailabilityChangeResponse> listRequests(Long teamId, AuthenticatedUser auth, boolean pendingOnly) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireOwnerManager(requester, team);

        List<TeamAvailabilityChangeRequest> requests = pendingOnly
                ? changeRequestRepository.findByTeam_IdAndStatusOrderByCreatedAtDesc(
                        teamId, AvailabilityChangeStatus.PENDING)
                : changeRequestRepository.findByTeam_IdOrderByCreatedAtDesc(teamId);
        return requests.stream().map(TeamAvailabilityChangeResponse::from).toList();
    }

    @Transactional
    public TeamAvailabilityChangeResponse approve(Long teamId, Long requestId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireOwnerManager(requester, team);

        TeamAvailabilityChangeRequest changeRequest = changeRequestRepository.findByIdAndTeam_Id(requestId, teamId)
                .orElseThrow(() -> BusinessException.notFound("Solicitação não encontrada"));
        if (changeRequest.getStatus() != AvailabilityChangeStatus.PENDING) {
            throw BusinessException.badRequest("Solicitação já foi resolvida");
        }

        List<WeeklyAvailabilitySlotRequest> slots = changeRequest.getSlots().stream()
                .map(slot -> {
                    WeeklyAvailabilitySlotRequest dto = new WeeklyAvailabilitySlotRequest();
                    dto.setDayOfWeek(slot.getDayOfWeek());
                    dto.setStartTime(slot.getStartTime());
                    dto.setEndTime(slot.getEndTime());
                    return dto;
                })
                .toList();
        availabilityService.syncTeamSchedule(team, slots);

        changeRequest.setStatus(AvailabilityChangeStatus.APPROVED);
        changeRequest.setResolvedAt(LocalDateTime.now());
        return TeamAvailabilityChangeResponse.from(changeRequestRepository.save(changeRequest));
    }

    @Transactional
    public TeamAvailabilityChangeResponse reject(Long teamId, Long requestId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
        requireOwnerManager(requester, team);

        TeamAvailabilityChangeRequest changeRequest = changeRequestRepository.findByIdAndTeam_Id(requestId, teamId)
                .orElseThrow(() -> BusinessException.notFound("Solicitação não encontrada"));
        if (changeRequest.getStatus() != AvailabilityChangeStatus.PENDING) {
            throw BusinessException.badRequest("Solicitação já foi resolvida");
        }

        changeRequest.setStatus(AvailabilityChangeStatus.REJECTED);
        changeRequest.setResolvedAt(LocalDateTime.now());
        return TeamAvailabilityChangeResponse.from(changeRequestRepository.save(changeRequest));
    }

    private void requireCaptain(Contact requester, Team team) {
        if (requester.getUserid() == null) {
            throw BusinessException.forbidden("Apenas o capitão pode solicitar alteração de horários");
        }
        boolean isCaptain = teamMemberRepository.existsByTeamIdAndClient_UserIdAndIsCaptainTrue(
                team.getId(), requester.getUserid());
        if (!isCaptain) {
            throw BusinessException.forbidden("Apenas o capitão pode solicitar alteração de horários");
        }
    }

    private void requireOwnerManager(Contact requester, Team team) {
        boolean isOwnerManager = requester.getIsPrimary() != null
                && requester.getIsPrimary() == 1
                && team.getClient() != null
                && team.getClient().getUserId().equals(requester.getUserid());
        if (!isOwnerManager) {
            throw BusinessException.forbidden("Apenas o dono do time pode gerenciar solicitações de horários");
        }
    }
}
