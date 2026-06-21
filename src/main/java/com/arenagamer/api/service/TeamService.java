package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.dto.request.UpdateTeamRequest;
import com.arenagamer.api.dto.response.TeamResponse;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final IdentityService identityService;
    private final TeamSettingsService teamSettingsService;

    @Transactional
    public Team create(AuthenticatedUser auth, CreateTeamRequest request) {
        Contact owner = identityService.requireContact(auth);
        TeamSettings settings = teamSettingsService.getSettings();

        if (teamRepository.countByOwner_Id(owner.getId()) >= settings.getMaxOwnedTeamsPerContact()) {
            throw BusinessException.conflict("Você já possui o limite máximo de times como dono.");
        }

        if (teamMemberRepository.countByContact_Id(owner.getId()) >= settings.getMaxParticipatedTeamsPerContact()) {
            throw BusinessException.conflict("Você atingiu o limite de participação em times.");
        }

        Team team = Team.builder()
                .name(request.getName().trim())
                .tag(normalizeOptional(request.getTag()))
                .logoUrl(normalizeUrl(request.getLogoUrl()))
                .youtubeUrl(normalizeUrl(request.getYoutubeUrl()))
                .instagramUrl(normalizeUrl(request.getInstagramUrl()))
                .twitchUrl(normalizeUrl(request.getTwitchUrl()))
                .otherSocialUrl(normalizeUrl(request.getOtherSocialUrl()))
                .rulesChange(normalizeOptional(request.getRulesChange()))
                .owner(owner)
                .build();

        team = teamRepository.save(team);

        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .contact(owner)
                .isCaptain(true)
                .build();
        teamMemberRepository.save(ownerMember);

        return team;
    }

    @Transactional
    public Team update(Long teamId, AuthenticatedUser auth, UpdateTeamRequest request) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);

        if (!team.getOwner().getId().equals(requester.getId())) {
            throw BusinessException.forbidden("Apenas o dono do time pode editar");
        }

        team.setName(request.getName().trim());
        team.setTag(normalizeOptional(request.getTag()));
        team.setLogoUrl(normalizeUrl(request.getLogoUrl()));
        team.setYoutubeUrl(normalizeUrl(request.getYoutubeUrl()));
        team.setInstagramUrl(normalizeUrl(request.getInstagramUrl()));
        team.setTwitchUrl(normalizeUrl(request.getTwitchUrl()));
        team.setOtherSocialUrl(normalizeUrl(request.getOtherSocialUrl()));
        team.setRulesChange(normalizeOptional(request.getRulesChange()));

        return teamRepository.save(team);
    }

    public Team getById(Long teamId) {
        return teamRepository.findByIdWithDetails(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMyTeams(AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        return teamRepository.findByMemberContactIdWithDetails(contact.getId()).stream()
                .map(TeamResponse::from)
                .toList();
    }

    public List<Team> getMyTeams(AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        return teamRepository.findByMemberContactIdWithDetails(contact.getId());
    }

    @Transactional
    public TeamMember addMember(Long teamId, Integer memberContactId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        TeamSettings settings = teamSettingsService.getSettings();

        if (!team.getOwner().getId().equals(requester.getId())) {
            throw BusinessException.forbidden("Apenas o dono do time pode adicionar membros");
        }

        if (teamMemberRepository.existsByTeamIdAndContactId(teamId, memberContactId)) {
            throw BusinessException.conflict("Usuário já é membro do time");
        }

        if (teamMemberRepository.countByContact_Id(memberContactId) >= settings.getMaxParticipatedTeamsPerContact()) {
            throw BusinessException.conflict("Usuário atingiu o limite de participação em times");
        }

        Contact member = identityService.getContactById(memberContactId);

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .contact(member)
                .build();

        return teamMemberRepository.save(teamMember);
    }

    @Transactional
    public void removeMember(Long teamId, Integer memberContactId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        if (!team.getOwner().getId().equals(requester.getId())) {
            throw BusinessException.forbidden("Apenas o dono do time pode remover membros");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndContactId(teamId, memberContactId)
                .orElseThrow(() -> BusinessException.notFound("Membro não encontrado"));

        if (member.getIsCaptain()) {
            throw BusinessException.badRequest("Não é possível remover o capitão");
        }

        teamMemberRepository.delete(member);
    }

    @Transactional
    public void transferOwnership(Long teamId, Integer newOwnerContactId, AuthenticatedUser auth) {
        Contact currentOwner = identityService.requireContact(auth);
        Team team = getById(teamId);
        TeamSettings settings = teamSettingsService.getSettings();

        if (!team.getOwner().getId().equals(currentOwner.getId())) {
            throw BusinessException.forbidden("Apenas o dono pode transferir o time");
        }

        Contact newOwner = identityService.getContactById(newOwnerContactId);

        if (!teamMemberRepository.existsByTeamIdAndContactId(teamId, newOwnerContactId)) {
            throw BusinessException.badRequest("Novo dono deve ser membro do time");
        }

        if (teamRepository.existsByOwner_IdAndIdNot(newOwnerContactId, teamId)) {
            throw BusinessException.conflict("O novo dono já possui outro time");
        }

        if (teamMemberRepository.countByContact_Id(newOwnerContactId) > settings.getMaxParticipatedTeamsPerContact()) {
            throw BusinessException.conflict("O novo dono excede o limite de participação em times");
        }

        teamMemberRepository.findByTeamIdAndContactId(teamId, currentOwner.getId())
                .ifPresent(m -> { m.setIsCaptain(false); teamMemberRepository.save(m); });
        teamMemberRepository.findByTeamIdAndContactId(teamId, newOwnerContactId)
                .ifPresent(m -> { m.setIsCaptain(true); teamMemberRepository.save(m); });

        team.setOwner(newOwner);
        teamRepository.save(team);
    }

    private String normalizeUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
