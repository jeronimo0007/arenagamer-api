package com.arenagamer.api.service;

import com.arenagamer.api.dto.request.TeamRosterFillVacancyRequest;
import com.arenagamer.api.dto.request.TeamRosterReallocateRequest;
import com.arenagamer.api.dto.response.TeamRosterVacancyResponse;
import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TeamMember;
import com.arenagamer.api.entity.TeamRosterVacancy;
import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.TournamentParticipantPlayer;
import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.TeamRosterVacancyStatus;
import com.arenagamer.api.entity.enums.TournamentStatus;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.ClientRepository;
import com.arenagamer.api.repository.MatchRepository;
import com.arenagamer.api.repository.TeamMemberRepository;
import com.arenagamer.api.repository.TeamRepository;
import com.arenagamer.api.repository.TeamRosterVacancyRepository;
import com.arenagamer.api.repository.TournamentParticipantPlayerRepository;
import com.arenagamer.api.repository.TournamentParticipantRepository;
import com.arenagamer.api.repository.TournamentRepository;
import com.arenagamer.api.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamRosterService {

    private static final List<TournamentStatus> ACTIVE_TOURNAMENT_STATUSES = List.of(
            TournamentStatus.REGISTRATION_OPEN,
            TournamentStatus.REGISTRATION_CLOSED,
            TournamentStatus.IN_PROGRESS);

    private static final List<MatchStatus> BLOCKING_MATCH_STATUSES = List.of(
            MatchStatus.SCHEDULED,
            MatchStatus.IN_PROGRESS);

    private final TeamRosterVacancyRepository vacancyRepository;
    private final TeamRepository teamRepository;
    private final TournamentParticipantPlayerRepository participantPlayerRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentRepository tournamentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ClientRepository clientRepository;
    private final MatchRepository matchRepository;
    private final TeamJoinBanService teamJoinBanService;
    private final TeamJoinRequestService teamJoinRequestService;
    private final TeamSettingsService teamSettingsService;
    private final IdentityService identityService;

    /**
     * Remove jogador da escalação em torneios ativos e abre vagas para reposição.
     * Membro pode sair a qualquer momento; dono só remove quem não está em partida ativa.
     */
    @Transactional
    public void handleMemberDeparture(Team team, Integer memberClientUserId, boolean selfLeave, Contact requester) {
        List<TournamentParticipantPlayer> rosterEntries = participantPlayerRepository
                .findActiveRosterEntriesByTeamAndClient(
                        team.getId(),
                        memberClientUserId,
                        ParticipantStatus.APPROVED,
                        ACTIVE_TOURNAMENT_STATUSES);

        for (TournamentParticipantPlayer entry : rosterEntries) {
            TournamentParticipant participant = entry.getParticipant();
            if (!selfLeave) {
                requireNotInActiveMatch(participant.getId(),
                        "Não é possível remover jogador que está em uma partida agendada ou em andamento");
            }

            participantPlayerRepository.deleteByParticipantIdAndClient_UserId(
                    participant.getId(), memberClientUserId);

            vacancyRepository.save(TeamRosterVacancy.builder()
                    .team(team)
                    .tournament(participant.getTournament())
                    .participant(participant)
                    .exitedClient(entry.getClient())
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<TeamRosterVacancyResponse> listVacancies(Long teamId, AuthenticatedUser auth, boolean pendingOnly) {
        Contact requester = identityService.requireContact(auth);
        Team team = requireTeam(teamId);
        requireTeamManager(team, requester);

        TeamRosterVacancyStatus filter = pendingOnly ? TeamRosterVacancyStatus.OPEN : null;
        return vacancyRepository.findByTeamIdWithDetails(teamId, filter).stream()
                .map(TeamRosterVacancyResponse::from)
                .toList();
    }

    @Transactional
    public TeamRosterVacancyResponse fillVacancy(
            Long teamId, Long vacancyId, TeamRosterFillVacancyRequest request, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = requireTeam(teamId);
        requireTeamManager(team, requester);

        TeamRosterVacancy vacancy = vacancyRepository.findByIdAndTeam_Id(vacancyId, teamId)
                .orElseThrow(() -> BusinessException.notFound("Vaga de escalação não encontrada"));

        if (vacancy.getStatus() != TeamRosterVacancyStatus.OPEN) {
            throw BusinessException.badRequest("Esta vaga já foi resolvida");
        }

        Client replacement = clientRepository.findById(request.getClientUserId())
                .orElseThrow(() -> BusinessException.notFound("Cliente não encontrado"));

        addToRoster(team, vacancy.getParticipant(), replacement);
        ensureTeamMember(team, replacement);

        vacancy.setReplacementClient(replacement);
        vacancy.setStatus(TeamRosterVacancyStatus.FILLED);
        vacancy.setResolvedAt(LocalDateTime.now());

        return TeamRosterVacancyResponse.from(vacancyRepository.save(vacancy));
    }

    @Transactional
    public TeamRosterVacancyResponse forfeitVacancy(Long teamId, Long vacancyId, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = requireTeam(teamId);
        requireTeamManager(team, requester);

        TeamRosterVacancy vacancy = vacancyRepository.findByIdAndTeam_Id(vacancyId, teamId)
                .orElseThrow(() -> BusinessException.notFound("Vaga de escalação não encontrada"));

        if (vacancy.getStatus() != TeamRosterVacancyStatus.OPEN) {
            throw BusinessException.badRequest("Esta vaga já foi resolvida");
        }

        vacancy.setStatus(TeamRosterVacancyStatus.FORFEITED);
        vacancy.setResolvedAt(LocalDateTime.now());
        vacancyRepository.save(vacancy);

        teamJoinBanService.applyBanForUnreplacedExit(vacancy.getExitedClient(), vacancy);

        return TeamRosterVacancyResponse.from(vacancy);
    }

    @Transactional
    public TeamRosterVacancyResponse reallocate(Long teamId, TeamRosterReallocateRequest request, AuthenticatedUser auth) {
        Contact requester = identityService.requireContact(auth);
        Team team = requireTeam(teamId);
        requireTeamManager(team, requester);

        Tournament tournament = tournamentRepository.findBySlug(request.getTournamentSlug())
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));

        TournamentParticipant participant = participantRepository
                .findByTournamentIdAndTeamId(tournament.getId(), teamId)
                .filter(p -> p.getStatus() == ParticipantStatus.APPROVED)
                .orElseThrow(() -> BusinessException.notFound("Time não inscrito neste torneio"));

        if (!ACTIVE_TOURNAMENT_STATUSES.contains(tournament.getStatus())) {
            throw BusinessException.badRequest("Torneio não está ativo para realocação de escalação");
        }

        if (!participantPlayerRepository.existsByParticipantIdAndClient_UserId(
                participant.getId(), request.getOutClientUserId())) {
            throw BusinessException.badRequest("Jogador de saída não está na escalação deste torneio");
        }

        requireNotInActiveMatch(participant.getId(),
                "Não é possível realocar enquanto o time tem partida agendada ou em andamento");

        Client outClient = clientRepository.findById(request.getOutClientUserId())
                .orElseThrow(() -> BusinessException.notFound("Jogador de saída não encontrado"));
        Client inClient = clientRepository.findById(request.getInClientUserId())
                .orElseThrow(() -> BusinessException.notFound("Jogador de entrada não encontrado"));

        if (request.getOutClientUserId().equals(request.getInClientUserId())) {
            throw BusinessException.badRequest("Informe jogadores diferentes para a realocação");
        }

        participantPlayerRepository.deleteByParticipantIdAndClient_UserId(
                participant.getId(), request.getOutClientUserId());

        addToRoster(team, participant, inClient);
        ensureTeamMember(team, inClient);

        TeamRosterVacancy openVacancy = vacancyRepository
                .findByParticipant_IdAndStatus(participant.getId(), TeamRosterVacancyStatus.OPEN)
                .stream()
                .filter(v -> v.getExitedClient().getUserId().equals(request.getOutClientUserId()))
                .findFirst()
                .orElse(null);

        if (openVacancy != null) {
            openVacancy.setReplacementClient(inClient);
            openVacancy.setStatus(TeamRosterVacancyStatus.FILLED);
            openVacancy.setResolvedAt(LocalDateTime.now());
            return TeamRosterVacancyResponse.from(vacancyRepository.save(openVacancy));
        }

        TeamRosterVacancy vacancy = vacancyRepository.save(TeamRosterVacancy.builder()
                .team(team)
                .tournament(tournament)
                .participant(participant)
                .exitedClient(outClient)
                .replacementClient(inClient)
                .status(TeamRosterVacancyStatus.FILLED)
                .resolvedAt(LocalDateTime.now())
                .build());

        return TeamRosterVacancyResponse.from(vacancy);
    }

    @Transactional
    public void forfeitOpenVacanciesOnTournamentStart(Long tournamentId) {
        List<TeamRosterVacancy> openVacancies = vacancyRepository.findByTournament_IdAndStatus(
                tournamentId, TeamRosterVacancyStatus.OPEN);

        for (TeamRosterVacancy vacancy : openVacancies) {
            vacancy.setStatus(TeamRosterVacancyStatus.FORFEITED);
            vacancy.setResolvedAt(LocalDateTime.now());
            vacancyRepository.save(vacancy);
            teamJoinBanService.applyBanForUnreplacedExit(vacancy.getExitedClient(), vacancy);
        }
    }

    private void addToRoster(Team team, TournamentParticipant participant, Client client) {
        teamJoinBanService.requireCanJoinTeam(client.getUserId());

        if (participantPlayerRepository.existsByParticipantIdAndClient_UserId(
                participant.getId(), client.getUserId())) {
            throw BusinessException.conflict("Jogador já está na escalação deste torneio");
        }

        validateRosterLimits(participant, 1);
        validateNoRosterConflict(participant.getTournament().getId(), team.getId(), Set.of(client.getUserId()));

        if (teamMemberRepository.countByClient_UserId(client.getUserId())
                >= teamSettingsService.getSettings().getMaxParticipatedTeamsPerClient()
                && !teamMemberRepository.existsByTeamIdAndClient_UserId(team.getId(), client.getUserId())) {
            throw BusinessException.conflict("Cliente atingiu o limite de participação em times");
        }

        participantPlayerRepository.save(TournamentParticipantPlayer.builder()
                .participant(participant)
                .tournament(participant.getTournament())
                .client(client)
                .build());
    }

    private void validateRosterLimits(TournamentParticipant participant, int playersToAdd) {
        Tournament tournament = participant.getTournament();
        long current = participantPlayerRepository.findByParticipantIdWithClient(participant.getId()).size();
        long after = current + playersToAdd;

        if (tournament.getMaxPlayersPerTeam() != null && after > tournament.getMaxPlayersPerTeam()) {
            throw BusinessException.badRequest(
                    "A escalação excederia o máximo de "
                            + tournament.getMaxPlayersPerTeam() + " jogadores por equipe");
        }
    }

    private void validateNoRosterConflict(Long tournamentId, Long teamId, Collection<Integer> clientUserIds) {
        List<Object[]> conflicts = participantPlayerRepository.findRosterConflictsInTournament(
                tournamentId, teamId, clientUserIds, ParticipantStatus.APPROVED);
        if (!conflicts.isEmpty()) {
            Object[] row = conflicts.get(0);
            throw BusinessException.conflict(
                    "Jogador já escalado em outra equipe neste torneio: " + row[1] + " (" + row[2] + ")");
        }

        List<Object[]> soloConflicts = participantPlayerRepository.findSoloConflictsInTournament(
                tournamentId, clientUserIds, ParticipantStatus.APPROVED);
        if (!soloConflicts.isEmpty()) {
            throw BusinessException.conflict(
                    "Jogador já inscrito individualmente neste torneio: " + soloConflicts.get(0)[1]);
        }
    }

    private void ensureTeamMember(Team team, Client client) {
        if (client.getUserId().equals(team.getClient().getUserId())) {
            return;
        }
        if (teamMemberRepository.existsByTeamIdAndClient_UserId(team.getId(), client.getUserId())) {
            return;
        }
        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .client(client)
                .build());
        teamJoinRequestService.onMemberAddedDirectly(team.getId(), client.getUserId());
    }

    private void requireNotInActiveMatch(Long participantId, String message) {
        if (matchRepository.existsByParticipantIdAndStatusIn(participantId, BLOCKING_MATCH_STATUSES)) {
            throw BusinessException.badRequest(message);
        }
    }

    private Team requireTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> BusinessException.notFound("Time não encontrado"));
    }

    private void requireTeamManager(Team team, Contact requester) {
        boolean isOwnerManager = requester.getIsPrimary() != null
                && requester.getIsPrimary() == 1
                && team.getClient() != null
                && team.getClient().getUserId().equals(requester.getUserid());
        if (!isOwnerManager) {
            throw BusinessException.forbidden("Apenas o contato primário do cliente dono pode gerenciar a escalação");
        }
    }
}
