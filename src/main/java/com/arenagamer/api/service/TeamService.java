package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.CreateTeamRequest;
import com.arenagamer.api.dto.request.TeamRankRequest;
import com.arenagamer.api.dto.request.UpdateTeamRequest;
import com.arenagamer.api.dto.response.TeamActiveTournamentResponse;
import com.arenagamer.api.dto.response.TeamDetailAccess;
import com.arenagamer.api.dto.response.TeamDetailResponse;
import com.arenagamer.api.dto.response.TeamJoinRequestResponse;
import com.arenagamer.api.dto.response.TeamManagementResponse;
import com.arenagamer.api.dto.response.TeamMemberClientResponse;
import com.arenagamer.api.dto.response.TeamOwnerResponse;
import com.arenagamer.api.dto.response.TeamPerformanceResponse;
import com.arenagamer.api.dto.response.TeamPlayerResponse;
import com.arenagamer.api.dto.response.TeamRankSummaryResponse;
import com.arenagamer.api.dto.response.TeamResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Preset;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.TeamRank;
import com.arenagamer.api.entity.TeamSettings;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.TeamStatus;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.entity.enums.Visibility;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.AvailabilityProfileRepository;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.PresetRepository;
import com.arenagamer.api.repository.TeamAvailabilityChangeRequestRepository;
import com.arenagamer.api.repository.TeamJoinRequestRepository;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRankRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.repository.TournamentParticipantRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.util.TeamVisibilityRules;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRankRepository teamRankRepository;
    private final PresetRepository presetRepository;
    private final ClientRepository clientRepository;
    private final TournamentParticipantRepository tournamentParticipantRepository;
    private final TeamJoinRequestRepository teamJoinRequestRepository;
    private final TeamAvailabilityChangeRequestRepository teamAvailabilityChangeRequestRepository;
    private final AvailabilityProfileRepository availabilityProfileRepository;
    private final IdentityService identityService;
    private final TeamSettingsService teamSettingsService;
    private final TeamRankService teamRankService;
    private final AvailabilityService availabilityService;
    private final TeamJoinRequestService teamJoinRequestService;
    private final TeamRosterService teamRosterService;
    private final TournamentService tournamentService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamRankRepository teamRankRepository,
            PresetRepository presetRepository,
            ClientRepository clientRepository,
            TournamentParticipantRepository tournamentParticipantRepository,
            TeamJoinRequestRepository teamJoinRequestRepository,
            TeamAvailabilityChangeRequestRepository teamAvailabilityChangeRequestRepository,
            AvailabilityProfileRepository availabilityProfileRepository,
            IdentityService identityService,
            TeamSettingsService teamSettingsService,
            TeamRankService teamRankService,
            AvailabilityService availabilityService,
            TeamJoinRequestService teamJoinRequestService,
            TeamRosterService teamRosterService,
            @Lazy TournamentService tournamentService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRankRepository = teamRankRepository;
        this.presetRepository = presetRepository;
        this.clientRepository = clientRepository;
        this.tournamentParticipantRepository = tournamentParticipantRepository;
        this.teamJoinRequestRepository = teamJoinRequestRepository;
        this.teamAvailabilityChangeRequestRepository = teamAvailabilityChangeRequestRepository;
        this.availabilityProfileRepository = availabilityProfileRepository;
        this.identityService = identityService;
        this.teamSettingsService = teamSettingsService;
        this.teamRankService = teamRankService;
        this.availabilityService = availabilityService;
        this.teamJoinRequestService = teamJoinRequestService;
        this.teamRosterService = teamRosterService;
        this.tournamentService = tournamentService;
    }

    private static final List<TournamentStatus> ACTIVE_TOURNAMENT_STATUSES = List.of(
            TournamentStatus.REGISTRATION_OPEN,
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentStatus.IN_PROGRESS);

    @Transactional
    public Team create(AuthenticatedUser auth, CreateTeamRequest request) {
        Contact requester = identityService.requireContact(auth);
        requirePrimaryContact(requester);
        Client client = clientRepository.findById(requester.getUserid())
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));
        TeamSettings settings = teamSettingsService.getSettings();

        if (teamRepository.countByClient_UserId(client.getUserId()) >= settings.getMaxOwnedTeamsPerClient()) {
            throw BusinessException.conflict("Este cliente já possui o limite máximo de times.");
        }

        if (teamMemberRepository.countByClient_UserId(client.getUserId()) >= settings.getMaxParticipatedTeamsPerClient()) {
            throw BusinessException.conflict("Seu cliente atingiu o limite de participação em times.");
        }

        Team team = Team.builder()
                .name(request.getName().trim())
                .tag(normalizeOptional(request.getTag()))
                .logoUrl(normalizeUrl(request.getLogoUrl()))
                .bannerUrl(normalizeUrl(request.getBannerUrl()))
                .youtubeUrl(normalizeUrl(request.getYoutubeUrl()))
                .instagramUrl(normalizeUrl(request.getInstagramUrl()))
                .twitchUrl(normalizeUrl(request.getTwitchUrl()))
                .otherSocialUrl(normalizeUrl(request.getOtherSocialUrl()))
                .description(normalizeOptional(request.getDescription()))
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
                .client(client)
                .build();

        team = teamRepository.save(team);

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .client(client)
                .isCaptain(true)
                .build());

        syncRanks(team, request.getRanks());

        return team;
    }

    @Transactional
    public Team update(Long teamId, AuthenticatedUser auth, UpdateTeamRequest request) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        requireTeamManager(team, requester, "editar");

        team.setName(request.getName().trim());
        team.setTag(normalizeOptional(request.getTag()));
        team.setLogoUrl(normalizeUrl(request.getLogoUrl()));
        team.setBannerUrl(normalizeUrl(request.getBannerUrl()));
        team.setYoutubeUrl(normalizeUrl(request.getYoutubeUrl()));
        team.setInstagramUrl(normalizeUrl(request.getInstagramUrl()));
        team.setTwitchUrl(normalizeUrl(request.getTwitchUrl()));
        team.setOtherSocialUrl(normalizeUrl(request.getOtherSocialUrl()));
        team.setDescription(normalizeOptional(request.getDescription()));

        if (request.getVisibility() != null) {
            team.setVisibility(request.getVisibility());
        }

        team = teamRepository.save(team);

        if (request.getRanks() != null) {
            syncRanks(team, request.getRanks());
        }

        if (request.getAvailability() != null && request.getAvailability().getWeeklySlots() != null) {
            availabilityService.syncTeamSchedule(team, request.getAvailability().getWeeklySlots());
        }

        return team;
    }

    @Transactional
    public void delete(Long teamId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        requireTeamManager(team, requester, "excluir");

        tournamentService.releaseTeamForDeletion(teamId);
        cleanupTeamDependencies(teamId);

        teamRepository.delete(team);
    }

    private void cleanupTeamDependencies(Long teamId) {
        teamJoinRequestRepository.deleteByTeam_Id(teamId);
        teamAvailabilityChangeRequestRepository.deleteByTeam_Id(teamId);
        availabilityProfileRepository.findByTeam_Id(teamId)
                .ifPresent(availabilityProfileRepository::delete);
    }

    @Transactional
    public void transferTeam(Long teamId, Integer newClientUserId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        requireTeamManager(team, requester, "transferir");

        if (team.getClient().getUserId().equals(newClientUserId)) {
            throw BusinessException.badRequest("O time já pertence a este cliente");
        }

        if (teamRepository.existsByClient_UserIdAndIdNot(newClientUserId, teamId)) {
            throw BusinessException.conflict("O cliente destino já possui um time");
        }

        Client newOwner = clientRepository.findById(newClientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente destino não encontrado"));

        Integer oldClientUserId = team.getClient().getUserId();
        detachMemberFromTeam(team, oldClientUserId);
        teamMemberRepository.deleteByTeamIdAndClient_UserId(teamId, oldClientUserId);

        team.setClient(newOwner);
        teamRepository.save(team);

        teamMemberRepository.findByTeamIdAndClient_UserId(teamId, newClientUserId)
                .ifPresentOrElse(
                        member -> {
                            member.setIsCaptain(true);
                            teamMemberRepository.save(member);
                        },
                        () -> teamMemberRepository.save(TeamMember.builder()
                                .team(team)
                                .client(newOwner)
                                .isCaptain(true)
                                .build()));
    }

    @Transactional
    public TeamJoinRequestResponse addMemberClient(Long teamId, Integer memberClientUserId, AuthenticatedUser auth) {
        return teamJoinRequestService.inviteMember(teamId, memberClientUserId, auth);
    }

    @Transactional
    public void removeMemberClient(Long teamId, Integer memberClientUserId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);

        if (memberClientUserId.equals(team.getClient().getUserId())) {
            throw BusinessException.badRequest("Não é possível remover o cliente dono do time");
        }

        if (!teamMemberRepository.existsByTeamIdAndClient_UserId(teamId, memberClientUserId)) {
            throw BusinessException.notFound("Cliente membro não encontrado");
        }

        boolean selfLeave = requester.getUserid() != null
                && requester.getUserid().equals(memberClientUserId);
        if (!selfLeave) {
            requireTeamManager(team, requester, "remover clientes");
        }

        teamRosterService.handleMemberDeparture(team, memberClientUserId, selfLeave, requester);
        teamJoinRequestService.onMemberLeftTeam(teamId, memberClientUserId);

        detachMemberFromTeam(team, memberClientUserId);
        teamMemberRepository.deleteByTeamIdAndClient_UserId(teamId, memberClientUserId);
    }

    public Team getById(Long teamId) {
        return teamRepository.findByIdWithDetails(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getTeamDetails(Long teamId, AuthenticatedUser auth, Long presetId) {
        Team team = getById(teamId);
        validateOptionalPreset(presetId);

        boolean isMember = isMemberClient(team, auth);
        TeamDetailAccess access = resolveDetailAccess(team.getVisibility(), isMember);

        if (access == TeamDetailAccess.PRIVATE_RESTRICTED) {
            return TeamDetailResponse.privateRestricted();
        }
        if (access == TeamDetailAccess.PROTECTED_SUMMARY) {
            return buildProtectedSummary(team, presetId);
        }
        return buildFullDetails(team, presetId);
    }

    @Transactional(readOnly = true)
    public TeamDetailResponse getDiscoverableTeamDetails(Long teamId, AuthenticatedUser auth, Long presetId) {
        Team team = teamRepository.findDiscoverableByIdWithClient(teamId, TeamVisibilityRules.DISCOVERABLE)
                .orElseGet(() -> {
                    Team existing = teamRepository.findById(teamId).orElse(null);
                    if (existing != null && existing.getVisibility() == Visibility.PRIVATE) {
                        return null;
                    }
                    throw BusinessException.notFound("Time não encontrado");
                });

        if (team == null) {
            return TeamDetailResponse.privateRestricted();
        }

        validateOptionalPreset(presetId);
        boolean isMember = isMemberClient(team, auth);
        TeamDetailAccess access = resolveDetailAccess(team.getVisibility(), isMember);

        if (access == TeamDetailAccess.PROTECTED_SUMMARY) {
            return buildProtectedSummary(team, presetId);
        }
        return buildFullDetails(team, presetId);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamForViewer(Long teamId, AuthenticatedUser auth) {
        Team team = getById(teamId);
        requireTeamViewAccess(team, auth);
        return buildResponse(team, authContext(auth, team));
    }

    @Transactional(readOnly = true)
    public TeamManagementResponse getTeamManagement(Long teamId, AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        Team team = getById(teamId);
        requireTeamViewAccess(team, auth);

        List<TeamMemberClientResponse> members = listMemberClients(team);
        boolean canManage = canManageTeam(team, contact);

        return TeamManagementResponse.builder()
                .team(buildResponse(team, authContext(auth, team)))
                .members(members)
                .canManage(canManage)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberClientResponse> listTeamMembers(Long teamId, AuthenticatedUser auth) {
        Team team = getById(teamId);
        requireTeamViewAccess(team, auth);
        return listMemberClients(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listManageableTeams(AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        if (!isPrimaryContact(contact)) {
            return List.of();
        }
        return teamRepository.findOwnedByClientUserIdWithDetails(contact.getUserid()).stream()
                .map(team -> buildResponse(team, authContext(auth, team)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TeamResponse> listPublicTeams(Pageable pageable) {
        return teamRepository.findDiscoverableWithClient(TeamVisibilityRules.DISCOVERABLE, pageable)
                .map(team -> buildResponse(team, authContext(null, team)));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMyTeams(AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        return teamRepository.findByMemberClientUserIdWithDetails(contact.getUserid()).stream()
                .map(team -> {
                    TeamResponse response = toResponse(team, auth);
                    response.setCanRegisterInTournament(canRegisterTeamInTournament(contact, team));
                    return response;
                })
                .toList();
    }

    public List<Team> getMyTeams(AuthenticatedUser auth) {
        Contact contact = identityService.requireContact(auth);
        return teamRepository.findByMemberClientUserIdWithDetails(contact.getUserid());
    }

    @Transactional(readOnly = true)
    public TeamResponse toResponse(Team team, AuthenticatedUser auth) {
        return buildResponse(team, authContext(auth, team));
    }

    public boolean isPrimaryManagerOfTeam(Contact contact, Team team) {
        return canManageTeam(team, contact);
    }

    /**
     * Pode inscrever o time em torneio: contato primário do cliente dono ou capitão do time.
     */
    public boolean canRegisterTeamInTournament(Contact contact, Team team) {
        if (canManageTeam(team, contact)) {
            return true;
        }
        if (contact.getUserid() == null) {
            return false;
        }
        return teamMemberRepository.existsByTeamIdAndClient_UserIdAndIsCaptainTrue(
                team.getId(), contact.getUserid());
    }

    @Transactional
    public void setTeamCaptain(Long teamId, Integer clientUserId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = getById(teamId);
        requireTeamManager(team, requester, "definir capitão");

        if (clientUserId.equals(team.getClient().getUserId())) {
            throw BusinessException.badRequest("O cliente dono não precisa ser capitão — use a gestão do time");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndClient_UserId(teamId, clientUserId)
                .orElseThrow(() -> BusinessException.notFound("Cliente membro não encontrado no time"));

        teamMemberRepository.findByTeamIdWithClient(teamId).forEach(existing -> {
            if (Boolean.TRUE.equals(existing.getIsCaptain())) {
                existing.setIsCaptain(false);
                teamMemberRepository.save(existing);
            }
        });

        member.setIsCaptain(true);
        teamMemberRepository.save(member);
    }

    private List<TeamMemberClientResponse> listMemberClients(Team team) {
        return teamMemberRepository.findByTeamIdWithClient(team.getId()).stream()
                .map(member -> TeamMemberClientResponse.from(member, team.getClient().getUserId()))
                .toList();
    }

    /**
     * Evita conflito com orphanRemoval quando o time foi carregado com {@code members} (JOIN FETCH).
     */
    private void detachMemberFromTeam(Team team, Integer memberClientUserId) {
        team.getMembers().removeIf(member ->
                member.getClient() != null && memberClientUserId.equals(member.getClient().getUserId()));
    }

    private boolean canManageTeam(Team team, Contact contact) {
        return isPrimaryContact(contact)
                && team.getClient() != null
                && team.getClient().getUserId().equals(contact.getUserid());
    }

    private TeamResponse buildResponse(Team team, ViewerContext context) {
        List<TeamRank> ranks = context.includeRanks()
                ? teamRankRepository.findByTeamIdWithPreset(team.getId())
                : null;
        TeamResponse response = TeamResponse.from(team, ranks, context.includeRanks());
        response.setMemberCount((int) teamMemberRepository.countByTeam_Id(team.getId()));
        return response;
    }

    private ViewerContext authContext(AuthenticatedUser auth, Team team) {
        boolean isMember = isMemberClient(team, auth);
        return new ViewerContext(isMember, shouldExposeRanks(team, isMember));
    }

    private record ViewerContext(boolean isMember, boolean includeRanks) {}

    private void requireTeamViewAccess(Team team, AuthenticatedUser auth) {
        if (team.getVisibility() == Visibility.PUBLIC) {
            return;
        }
        if (auth == null || !auth.isContact()) {
            throw BusinessException.forbidden("Acesso restrito aos membros do time");
        }
        if (!isMemberClient(team, auth)) {
            throw BusinessException.forbidden("Acesso restrito aos membros do time");
        }
    }

    private TeamDetailAccess resolveDetailAccess(Visibility visibility, boolean isMember) {
        return switch (visibility) {
            case PUBLIC -> TeamDetailAccess.FULL;
            case PRIVATE -> isMember ? TeamDetailAccess.FULL : TeamDetailAccess.PRIVATE_RESTRICTED;
            case PROTECTED -> isMember ? TeamDetailAccess.FULL : TeamDetailAccess.PROTECTED_SUMMARY;
        };
    }

    private TeamDetailResponse buildFullDetails(Team team, Long presetId) {
        Integer ownerClientUserId = team.getClient().getUserId();
        List<TeamMember> members = teamMemberRepository.findByTeamIdWithClient(team.getId());
        List<TeamPlayerResponse> players = members.stream()
                .map(member -> TeamPlayerResponse.from(member, ownerClientUserId))
                .toList();

        List<TeamRank> ranks = teamRankRepository.findByTeamIdWithPreset(team.getId());
        Optional<TeamRank> displayRank = resolveDisplayRank(team.getId(), presetId, ranks);
        List<TeamPerformanceResponse> performance = ranks.stream()
                .map(teamRankService::toPerformance)
                .toList();

        List<TeamActiveTournamentResponse> activeTournaments = tournamentParticipantRepository
                .findActiveByTeamId(team.getId(), ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES)
                .stream()
                .map(TeamActiveTournamentResponse::from)
                .toList();

        return TeamDetailResponse.builder()
                .access(TeamDetailAccess.FULL)
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .privacy(team.getVisibility())
                .logoUrl(team.getLogoUrl())
                .bannerUrl(team.getBannerUrl())
                .youtubeUrl(team.getYoutubeUrl())
                .twitchUrl(team.getTwitchUrl())
                .description(team.getDescription())
                .owner(TeamOwnerResponse.from(team.getClient()))
                .players(players)
                .rank(displayRank.map(TeamRankSummaryResponse::from).orElse(null))
                .performance(performance.isEmpty() ? null : performance)
                .activeTournaments(activeTournaments.isEmpty() ? null : activeTournaments)
                .availability(availabilityService.getTeamSchedule(team.getId()))
                .status(resolveTeamStatus(team, !activeTournaments.isEmpty()))
                .build();
    }

    private TeamDetailResponse buildProtectedSummary(Team team, Long presetId) {
        List<TeamRank> ranks = teamRankRepository.findByTeamIdWithPreset(team.getId());
        Optional<TeamRank> displayRank = resolveDisplayRank(team.getId(), presetId, ranks);
        boolean inTournament = !tournamentParticipantRepository
                .findActiveByTeamId(team.getId(), ParticipantStatus.APPROVED, ACTIVE_TOURNAMENT_STATUSES)
                .isEmpty();

        return TeamDetailResponse.builder()
                .access(TeamDetailAccess.PROTECTED_SUMMARY)
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .privacy(team.getVisibility())
                .logoUrl(team.getLogoUrl())
                .description(team.getDescription())
                .rank(displayRank.map(TeamRankSummaryResponse::from).orElse(null))
                .status(resolveTeamStatus(team, inTournament))
                .build();
    }

    private Optional<TeamRank> resolveDisplayRank(Long teamId, Long presetId, List<TeamRank> ranks) {
        if (presetId != null) {
            return teamRankRepository.findByTeamIdAndPresetIdWithDetails(teamId, presetId);
        }
        Long mostPlayedPresetId = resolveMostPlayedPresetId(teamId);
        if (mostPlayedPresetId != null) {
            Optional<TeamRank> mostPlayed = teamRankRepository.findByTeamIdAndPresetIdWithDetails(
                    teamId, mostPlayedPresetId);
            if (mostPlayed.isPresent()) {
                return mostPlayed;
            }
        }
        return ranks.stream()
                .max(Comparator.comparing(TeamRank::getRankPoints)
                        .thenComparing(rank -> rank.getPreset().getId()));
    }

    private Long resolveMostPlayedPresetId(Long teamId) {
        List<Object[]> rows = tournamentParticipantRepository.countApprovedParticipationsByPreset(
                teamId, ParticipantStatus.APPROVED);
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return null;
        }
        Object presetIdValue = rows.get(0)[0];
        if (presetIdValue instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private TeamStatus resolveTeamStatus(Team team, boolean inActiveTournament) {
        if (!Boolean.TRUE.equals(team.getActive())) {
            return TeamStatus.INACTIVE;
        }
        if (inActiveTournament) {
            return TeamStatus.IN_TOURNAMENT;
        }
        return TeamStatus.ACTIVE;
    }

    private void validateOptionalPreset(Long presetId) {
        if (presetId == null) {
            return;
        }
        presetRepository.findByIdAndActiveTrue(presetId)
                .orElseThrow(() -> BusinessException.badRequest("Preset inválido ou inativo: " + presetId));
    }

    private boolean isMemberClient(Team team, AuthenticatedUser auth) {
        if (auth == null || !auth.isContact() || auth.getClientUserId() == null) {
            return false;
        }
        return teamMemberRepository.existsByTeamIdAndClient_UserId(team.getId(), auth.getClientUserId());
    }

    private boolean shouldExposeRanks(Team team, boolean isMember) {
        if (team.getVisibility() == Visibility.PRIVATE) {
            return isMember;
        }
        return true;
    }

    private void syncRanks(Team team, List<TeamRankRequest> requests) {
        if (requests == null) {
            return;
        }

        if (requests.isEmpty()) {
            teamRankRepository.deleteByTeam_Id(team.getId());
            return;
        }

        Set<Long> presetIds = new HashSet<>();
        for (TeamRankRequest request : requests) {
            if (!presetIds.add(request.getPresetId())) {
                throw BusinessException.badRequest("Cada jogo (preset) pode aparecer apenas uma vez nos ranks");
            }

            Preset preset = presetRepository.findByIdAndActiveTrue(request.getPresetId())
                    .orElseThrow(() -> BusinessException.badRequest(
                            "Preset inválido ou inativo: " + request.getPresetId()));

            TeamRank rank = teamRankRepository.findByTeam_IdAndPreset_Id(team.getId(), preset.getId())
                    .orElseGet(() -> TeamRank.builder().team(team).preset(preset).build());
            rank.setRankPoints(request.getRankPoints());
            teamRankRepository.save(rank);
        }

        teamRankRepository.deleteByTeam_IdAndPreset_IdNotIn(team.getId(), presetIds);
    }

    private void requireTeamManager(Team team, Contact requester, String action) {
        if (!canManageTeam(team, requester)) {
            if (!isPrimaryContact(requester)) {
                throw BusinessException.forbidden("Apenas o contato primário do cliente dono pode " + action);
            }
            throw BusinessException.forbidden("Apenas o cliente dono do time pode " + action);
        }
    }

    private void requirePrimaryContact(Contact contact) {
        if (!isPrimaryContact(contact)) {
            throw BusinessException.forbidden("Apenas o contato primário pode gerenciar o time");
        }
    }

    private boolean isPrimaryContact(Contact contact) {
        return contact.getIsPrimary() != null && contact.getIsPrimary() == 1;
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
