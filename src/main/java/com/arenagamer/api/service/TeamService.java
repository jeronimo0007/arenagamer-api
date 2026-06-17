package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.User;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public Team create(Long ownerId, CreateTeamRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));

        Team team = Team.builder()
                .name(request.getName())
                .tag(request.getTag())
                .logoUrl(request.getLogoUrl())
                .owner(owner)
                .build();

        team = teamRepository.save(team);

        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .user(owner)
                .isCaptain(true)
                .build();
        teamMemberRepository.save(ownerMember);

        return team;
    }

    public Team getById(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
    }

    public List<Team> getMyTeams(Long userId) {
        return teamRepository.findByMembersUserId(userId);
    }

    @Transactional
    public TeamMember addMember(Long teamId, Long userId, Long requesterId) {
        Team team = getById(teamId);
        if (!team.getOwner().getId().equals(requesterId)) {
            throw BusinessException.forbidden("Apenas o dono do time pode adicionar membros");
        }

        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw BusinessException.conflict("Usuário já é membro do time");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Usuário não encontrado"));

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .build();

        return teamMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long teamId, Long userId, Long requesterId) {
        Team team = getById(teamId);
        if (!team.getOwner().getId().equals(requesterId)) {
            throw BusinessException.forbidden("Apenas o dono do time pode remover membros");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> BusinessException.notFound("Membro não encontrado"));

        if (member.getIsCaptain()) {
            throw BusinessException.badRequest("Não é possível remover o capitão");
        }

        teamMemberRepository.delete(member);
    }

    @Transactional
    public void transferOwnership(Long teamId, Long newOwnerId, Long currentOwnerId) {
        Team team = getById(teamId);
        if (!team.getOwner().getId().equals(currentOwnerId)) {
            throw BusinessException.forbidden("Apenas o dono pode transferir o time");
        }

        User newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> BusinessException.notFound("Novo dono não encontrado"));

        if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, newOwnerId)) {
            throw BusinessException.badRequest("Novo dono deve ser membro do time");
        }

        // Update captain flags
        teamMemberRepository.findByTeamIdAndUserId(teamId, currentOwnerId)
                .ifPresent(m -> { m.setIsCaptain(false); teamMemberRepository.save(m); });
        teamMemberRepository.findByTeamIdAndUserId(teamId, newOwnerId)
                .ifPresent(m -> { m.setIsCaptain(true); teamMemberRepository.save(m); });

        team.setOwner(newOwner);
        teamRepository.save(team);
    }
}
