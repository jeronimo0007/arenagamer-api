package com.arenagamer.api.service;

import com.arenagamer.api.dto.response.TournamentStandingEntryResponse;
import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.*;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.*;
import com.arenagamer.api.security.AuthenticatedUser;
import com.arenagamer.api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BracketService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final BracketSeedRepository bracketSeedRepository;
    private final GroupStandingRepository groupStandingRepository;
    private final AuditService auditService;
    private final TournamentAccessService tournamentAccessService;

    @Transactional
    public void generateBracket(String slug) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));

        if (tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED
                && tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw BusinessException.badRequest("Status do torneio não permite geração de chaves");
        }

        List<TournamentParticipant> participants = participantRepository
                .findByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);

        if (participants.size() < 2) {
            throw BusinessException.badRequest("Mínimo de 2 participantes para gerar chaves");
        }

        Integer minRequired = tournament.getMinParticipants();
        if (minRequired != null && participants.size() < minRequired) {
            throw BusinessException.badRequest("Número mínimo de participantes não atingido: " + minRequired);
        }

        switch (tournament.getType()) {
            case SINGLE_ELIMINATION -> generateSingleElimination(tournament, participants);
            case ROUND_ROBIN -> generateRoundRobin(tournament, participants);
            case GROUP_STAGE -> generateGroupStage(tournament, participants);
            default -> throw BusinessException.badRequest("Tipo de torneio não suportado ainda: " + tournament.getType());
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(auth -> auditService.recordStaffMessage(auth, "GENERATE_BRACKET", "tournament",
                        tournament.getId(), "Chaves geradas: " + slug));
    }

    @Transactional
    public Match recordResult(Long matchId, Long winnerParticipantId,
                              Integer homeScore, Integer awayScore, String proofUrl, AuthenticatedUser auth) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> BusinessException.notFound("Partida não encontrada"));

        Tournament tournament = match.getRound().getTournament();
        tournamentAccessService.validateCanManage(tournament, auth);

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.CANCELLED) {
            throw BusinessException.badRequest("Partida já finalizada ou cancelada");
        }

        TournamentParticipant home = match.getHomeParticipant();
        TournamentParticipant away = match.getAwayParticipant();
        if (home == null || away == null) {
            throw BusinessException.badRequest("Partida ainda não tem os dois participantes definidos");
        }

        if (homeScore == null || awayScore == null) {
            throw BusinessException.badRequest("Informe o placar da partida");
        }
        if (homeScore < 0 || awayScore < 0) {
            throw BusinessException.badRequest("O placar não pode ser negativo");
        }

        TournamentParticipant winner;
        if (winnerParticipantId != null) {
            // Vencedor manual (override)
            if (home.getId().equals(winnerParticipantId)) {
                winner = home;
            } else if (away.getId().equals(winnerParticipantId)) {
                winner = away;
            } else {
                throw BusinessException.badRequest("O vencedor informado não participa desta partida");
            }
        } else {
            // Vencedor automático pelo maior placar
            if (homeScore > awayScore) {
                winner = home;
            } else if (awayScore > homeScore) {
                winner = away;
            } else {
                throw BusinessException.badRequest("Empate no placar: defina o vencedor manualmente");
            }
        }

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        if (proofUrl != null && !proofUrl.isBlank()) {
            match.setResultProofUrl(proofUrl.trim());
        }
        match.setWinnerParticipant(winner);
        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);

        updateStandings(tournament, match, home, away, winner);

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(staff -> auditService.recordStaffMessage(staff, "MATCH_RESULT", "match",
                        match.getId(), "Resultado registrado no torneio " + tournament.getSlug()));

        return match;
    }

    /**
     * Finaliza o torneio manualmente. Só é permitido quando todas as posições estão definidas,
     * ou seja, todas as partidas concluídas (incluindo final e disputa de 3º lugar).
     */
    @Transactional
    public void finalizeTournament(String slug, AuthenticatedUser auth) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));
        tournamentAccessService.validateCanManage(tournament, auth);

        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw BusinessException.badRequest("Torneio já finalizado");
        }

        List<Match> all = matchRepository.findByTournamentId(tournament.getId());
        if (all.isEmpty()) {
            throw BusinessException.badRequest("Gere as chaves antes de finalizar o torneio");
        }
        if (!allFinished(all)) {
            throw BusinessException.badRequest(
                    "Não é possível finalizar: ainda há partidas sem resultado (posições indefinidas)");
        }

        // Eliminatória e fase de grupos só finalizam após a final do mata-mata estar concluída
        if (tournament.getType() == TournamentType.SINGLE_ELIMINATION
                || tournament.getType() == TournamentType.GROUP_STAGE) {
            boolean finalDecided = all.stream().anyMatch(m -> m.getRound() != null
                    && m.getRound().getType() == RoundType.FINAL
                    && (m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.WALKOVER));
            if (!finalDecided) {
                throw BusinessException.badRequest(
                        "Gere e conclua o mata-mata (final) antes de finalizar o torneio");
            }
        }

        tournament.setStatus(TournamentStatus.COMPLETED);
        tournamentRepository.save(tournament);

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(staff -> auditService.recordStaffMessage(staff, "FINALIZE", "tournament",
                        tournament.getId(), "Torneio finalizado: " + slug));
    }

    /**
     * Gera/preenche a próxima fase da chave eliminatória a partir dos vencedores da fase atual.
     * Só avança quando todos os jogos da fase atual estiverem concluídos (manual, como a 1ª fase).
     */
    @Transactional
    public void advanceToNextRound(String slug, AuthenticatedUser auth) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));
        tournamentAccessService.validateCanManage(tournament, auth);

        if (tournament.getType() != TournamentType.SINGLE_ELIMINATION
                && tournament.getType() != TournamentType.GROUP_STAGE) {
            throw BusinessException.badRequest(
                    "A geração de próxima fase está disponível apenas para mata-mata");
        }

        List<Round> rounds = roundRepository.findByTournamentIdOrderByRoundNumber(tournament.getId());
        if (rounds.isEmpty()) {
            throw BusinessException.badRequest("Gere as chaves antes de avançar de fase");
        }

        for (Round round : rounds) {
            if (round.getType() == RoundType.THIRD_PLACE || round.getType() == RoundType.GROUP_STAGE) {
                continue;
            }
            List<Match> roundMatches = matchRepository.findByRoundId(round.getId());
            if (!isPopulated(roundMatches)) {
                continue;
            }
            if (!allFinished(roundMatches)) {
                throw BusinessException.badRequest(
                        "Conclua todos os jogos da fase atual antes de gerar a próxima fase");
            }

            Round nextRound = rounds.stream()
                    .filter(r -> r.getType() != RoundType.THIRD_PLACE
                            && r.getType() != RoundType.GROUP_STAGE
                            && r.getRoundNumber() == round.getRoundNumber() + 1)
                    .findFirst()
                    .orElse(null);

            if (nextRound == null) {
                throw BusinessException.badRequest(
                        "Não há próxima fase para gerar. Finalize o torneio quando todos os jogos terminarem");
            }

            List<Match> nextMatches = matchRepository.findByRoundId(nextRound.getId());
            if (isPopulated(nextMatches)) {
                continue;
            }

            for (Match match : roundMatches) {
                advanceWinner(match, match.getWinnerParticipant());
            }
            if (round.getType() == RoundType.SEMIFINAL) {
                fillThirdPlaceMatch(tournament, roundMatches);
            }
            round.setStatus(RoundStatus.COMPLETED);
            roundRepository.save(round);
            nextRound.setStatus(RoundStatus.IN_PROGRESS);
            roundRepository.save(nextRound);

            UserPrincipal.tryCurrent()
                    .filter(AuthenticatedUser::isStaff)
                    .ifPresent(staff -> auditService.recordStaffMessage(staff, "ADVANCE_ROUND", "tournament",
                            tournament.getId(), "Fase " + nextRound.getRoundNumber() + " gerada: " + slug));
            return;
        }

        throw BusinessException.badRequest("Não há próxima fase para gerar");
    }

    /**
     * Gera o mata-mata (eliminatória) da fase de grupos a partir dos classificados de cada grupo.
     * Só pode ser gerado quando todos os jogos dos grupos estiverem concluídos e ainda não houver mata-mata.
     */
    @Transactional
    public void generateGroupPlayoffs(String slug, AuthenticatedUser auth) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));
        tournamentAccessService.validateCanManage(tournament, auth);

        if (tournament.getType() != TournamentType.GROUP_STAGE) {
            throw BusinessException.badRequest("A geração de mata-mata está disponível apenas para fase de grupos");
        }

        List<Round> rounds = roundRepository.findByTournamentIdOrderByRoundNumber(tournament.getId());
        boolean hasKnockout = rounds.stream().anyMatch(r -> r.getType() != RoundType.GROUP_STAGE);
        if (hasKnockout) {
            throw BusinessException.badRequest("O mata-mata já foi gerado");
        }

        List<Match> all = matchRepository.findByTournamentId(tournament.getId());
        if (all.isEmpty()) {
            throw BusinessException.badRequest("Gere os jogos da fase de grupos primeiro");
        }
        if (!allFinished(all)) {
            throw BusinessException.badRequest(
                    "Conclua todos os jogos da fase de grupos antes de gerar o mata-mata");
        }

        int advancePerGroup = tournament.getAdvancePerGroup() != null && tournament.getAdvancePerGroup() > 0
                ? tournament.getAdvancePerGroup()
                : 1;

        List<GroupStanding> standings = groupStandingRepository
                .findByTournamentIdOrderByGroupNumberAscPointsDescGoalDifferenceDesc(tournament.getId());

        Map<Integer, List<GroupStanding>> byGroup = new LinkedHashMap<>();
        for (GroupStanding s : standings) {
            byGroup.computeIfAbsent(s.getGroupNumber(), k -> new ArrayList<>()).add(s);
        }

        // Cruzamento: 1ºs de cada grupo, depois 2ºs, etc. (evita confronto entre mesmos grupos na 1ª fase)
        List<TournamentParticipant> qualifiers = new ArrayList<>();
        for (int rank = 0; rank < advancePerGroup; rank++) {
            for (List<GroupStanding> groupStandings : byGroup.values()) {
                if (rank < groupStandings.size()) {
                    qualifiers.add(groupStandings.get(rank).getParticipant());
                }
            }
        }

        if (qualifiers.size() < 2) {
            throw BusinessException.badRequest("Classificados insuficientes para gerar o mata-mata");
        }

        int roundOffset = rounds.stream()
                .map(Round::getRoundNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        buildKnockoutBracket(tournament, qualifiers, roundOffset);

        UserPrincipal.tryCurrent()
                .filter(AuthenticatedUser::isStaff)
                .ifPresent(staff -> auditService.recordStaffMessage(staff, "GENERATE_KNOCKOUT", "tournament",
                        tournament.getId(), "Mata-mata da fase de grupos gerado: " + slug));
    }

    private void buildKnockoutBracket(Tournament tournament, List<TournamentParticipant> qualifiers, int roundOffset) {
        int totalSlots = nextPowerOf2(qualifiers.size());
        int totalRounds = (int) (Math.log(totalSlots) / Math.log(2));

        List<Round> rounds = new ArrayList<>();
        for (int r = 1; r <= totalRounds; r++) {
            RoundType type = r == totalRounds ? RoundType.FINAL :
                    r == totalRounds - 1 ? RoundType.SEMIFINAL :
                    r == totalRounds - 2 ? RoundType.QUARTERFINAL : RoundType.KNOCKOUT;

            rounds.add(roundRepository.save(Round.builder()
                    .tournament(tournament)
                    .roundNumber(roundOffset + r)
                    .type(type)
                    .status(r == 1 ? RoundStatus.IN_PROGRESS : RoundStatus.PENDING)
                    .build()));
        }

        int matchNum = nextMatchNumber(tournament);
        int firstRoundMatches = totalSlots / 2;
        List<Match> allMatches = new ArrayList<>();

        for (int i = 0; i < firstRoundMatches; i++) {
            TournamentParticipant home = i < qualifiers.size() ? qualifiers.get(i) : null;
            int awayIdx = totalSlots - 1 - i;
            TournamentParticipant away = awayIdx < qualifiers.size() ? qualifiers.get(awayIdx) : null;

            Match match = Match.builder()
                    .round(rounds.get(0))
                    .homeParticipant(home)
                    .awayParticipant(away)
                    .matchNumber(matchNum++)
                    .bracketPosition(i)
                    .status(MatchStatus.SCHEDULED)
                    .build();

            if (home != null && away == null) {
                match.setWinnerParticipant(home);
                match.setStatus(MatchStatus.WALKOVER);
            } else if (home == null && away != null) {
                match.setWinnerParticipant(away);
                match.setStatus(MatchStatus.WALKOVER);
            }

            allMatches.add(matchRepository.save(match));
        }

        for (int r = 1; r < totalRounds; r++) {
            int matchesInRound = totalSlots / (int) Math.pow(2, r + 1);
            for (int i = 0; i < matchesInRound; i++) {
                allMatches.add(matchRepository.save(Match.builder()
                        .round(rounds.get(r))
                        .matchNumber(matchNum++)
                        .bracketPosition(i)
                        .status(MatchStatus.SCHEDULED)
                        .build()));
            }
        }

        int offset = 0;
        for (int r = 0; r < totalRounds - 1; r++) {
            int matchesInRound = totalSlots / (int) Math.pow(2, r + 1);
            int nextOffset = offset + matchesInRound;
            for (int i = 0; i < matchesInRound; i++) {
                Match current = allMatches.get(offset + i);
                Match next = allMatches.get(nextOffset + i / 2);
                current.setNextMatchId(next.getId());
                matchRepository.save(current);
            }
            offset = nextOffset;
        }

        if (totalRounds >= 2) {
            Round thirdPlaceRound = roundRepository.save(Round.builder()
                    .tournament(tournament)
                    .roundNumber(roundOffset + totalRounds + 1)
                    .type(RoundType.THIRD_PLACE)
                    .status(RoundStatus.PENDING)
                    .build());

            matchRepository.save(Match.builder()
                    .round(thirdPlaceRound)
                    .matchNumber(matchNum++)
                    .bracketPosition(0)
                    .status(MatchStatus.SCHEDULED)
                    .build());
        }
    }

    private int nextMatchNumber(Tournament tournament) {
        return matchRepository.findByTournamentId(tournament.getId()).stream()
                .map(Match::getMatchNumber)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private void fillThirdPlaceMatch(Tournament tournament, List<Match> semifinalMatches) {
        Round thirdPlaceRound = roundRepository.findByTournamentIdOrderByRoundNumber(tournament.getId()).stream()
                .filter(r -> r.getType() == RoundType.THIRD_PLACE)
                .findFirst()
                .orElse(null);
        if (thirdPlaceRound == null) {
            return;
        }
        List<Match> thirdMatches = matchRepository.findByRoundId(thirdPlaceRound.getId());
        if (thirdMatches.isEmpty()) {
            return;
        }
        Match thirdPlace = thirdMatches.get(0);

        List<TournamentParticipant> losers = new ArrayList<>();
        for (Match semifinal : semifinalMatches) {
            TournamentParticipant winner = semifinal.getWinnerParticipant();
            if (winner == null || semifinal.getHomeParticipant() == null || semifinal.getAwayParticipant() == null) {
                continue;
            }
            TournamentParticipant loser = winner.getId().equals(semifinal.getHomeParticipant().getId())
                    ? semifinal.getAwayParticipant()
                    : semifinal.getHomeParticipant();
            losers.add(loser);
        }

        if (!losers.isEmpty()) {
            thirdPlace.setHomeParticipant(losers.get(0));
        }
        if (losers.size() >= 2) {
            thirdPlace.setAwayParticipant(losers.get(1));
        }
        matchRepository.save(thirdPlace);
        thirdPlaceRound.setStatus(RoundStatus.IN_PROGRESS);
        roundRepository.save(thirdPlaceRound);
    }

    /**
     * Calcula a classificação atual do torneio (posições) sem alterar o estado.
     */
    @Transactional(readOnly = true)
    public List<TournamentStandingEntryResponse> computeStandings(String slug) {
        Tournament tournament = tournamentRepository.findBySlug(slug)
                .orElseThrow(() -> BusinessException.notFound("Torneio não encontrado"));

        return switch (tournament.getType()) {
            case SINGLE_ELIMINATION -> eliminationStandings(tournament);
            case ROUND_ROBIN -> groupStandings(tournament);
            case GROUP_STAGE -> {
                // Após gerar o mata-mata, a classificação final vem dele; antes, mostra a tabela dos grupos
                boolean hasKnockout = roundRepository.findByTournamentIdOrderByRoundNumber(tournament.getId()).stream()
                        .anyMatch(r -> r.getType() != RoundType.GROUP_STAGE);
                yield hasKnockout ? eliminationStandings(tournament) : groupStandings(tournament);
            }
            default -> List.of();
        };
    }

    private List<TournamentStandingEntryResponse> eliminationStandings(Tournament tournament) {
        List<Match> matches = matchRepository.findByTournamentId(tournament.getId());
        List<TournamentParticipant> participants = participantRepository
                .findByTournamentIdAndStatus(tournament.getId(), ParticipantStatus.APPROVED);

        Match finalMatch = findMatchByRoundType(matches, RoundType.FINAL);
        Match thirdMatch = findMatchByRoundType(matches, RoundType.THIRD_PLACE);

        Map<Long, Integer> lostRound = new HashMap<>();
        Map<Integer, RoundType> roundTypes = new HashMap<>();
        for (Match m : matches) {
            if (m.getRound() != null) {
                roundTypes.put(m.getRound().getRoundNumber(), m.getRound().getType());
            }
            if (m.getStatus() == MatchStatus.COMPLETED && m.getWinnerParticipant() != null
                    && m.getHomeParticipant() != null && m.getAwayParticipant() != null) {
                TournamentParticipant loser = m.getWinnerParticipant().getId().equals(m.getHomeParticipant().getId())
                        ? m.getAwayParticipant()
                        : m.getHomeParticipant();
                int rn = m.getRound() != null ? m.getRound().getRoundNumber() : 0;
                lostRound.merge(loser.getId(), rn, Math::max);
            }
        }

        List<TournamentStandingEntryResponse> result = new ArrayList<>();
        Set<Long> placed = new HashSet<>();
        int pos = 1;

        if (isPlayed(finalMatch)) {
            TournamentParticipant champion = finalMatch.getWinnerParticipant();
            TournamentParticipant runnerUp = otherSide(finalMatch, champion);
            result.add(standingEntry(1, champion, "Campeão"));
            result.add(standingEntry(2, runnerUp, "Vice-campeão"));
            placed.add(champion.getId());
            placed.add(runnerUp.getId());
            pos = 3;
        }

        if (isPlayed(thirdMatch)) {
            TournamentParticipant third = thirdMatch.getWinnerParticipant();
            TournamentParticipant fourth = otherSide(thirdMatch, third);
            result.add(standingEntry(Math.max(pos, 3), third, "3º lugar"));
            result.add(standingEntry(Math.max(pos, 3) + 1, fourth, "4º lugar"));
            placed.add(third.getId());
            placed.add(fourth.getId());
            pos = Math.max(pos, 5);
        }

        List<TournamentParticipant> remaining = participants.stream()
                .filter(p -> !placed.contains(p.getId()))
                .sorted(Comparator
                        .comparingInt((TournamentParticipant p) -> lostRound.getOrDefault(p.getId(), 0)).reversed()
                        .thenComparing(p -> p.getSeedNumber() == null ? Integer.MAX_VALUE : p.getSeedNumber()))
                .toList();

        for (TournamentParticipant p : remaining) {
            Integer rn = lostRound.get(p.getId());
            String note = rn != null
                    ? "Eliminado: " + phaseLabel(roundTypes.get(rn), rn)
                    : "Em disputa";
            result.add(standingEntry(pos++, p, note));
        }

        return result;
    }

    private List<TournamentStandingEntryResponse> groupStandings(Tournament tournament) {
        List<GroupStanding> standings = groupStandingRepository
                .findByTournamentIdOrderByGroupNumberAscPointsDescGoalDifferenceDesc(tournament.getId());

        List<TournamentStandingEntryResponse> result = new ArrayList<>();
        Integer currentGroup = null;
        int posInGroup = 0;

        for (GroupStanding s : standings) {
            if (!Objects.equals(currentGroup, s.getGroupNumber())) {
                currentGroup = s.getGroupNumber();
                posInGroup = 0;
            }
            posInGroup++;

            result.add(TournamentStandingEntryResponse.builder()
                    .position(posInGroup)
                    .participantId(s.getParticipant() != null ? s.getParticipant().getId() : null)
                    .participantName(participantName(s.getParticipant()))
                    .groupNumber(s.getGroupNumber())
                    .points(s.getPoints())
                    .wins(s.getWins())
                    .draws(s.getDraws())
                    .losses(s.getLosses())
                    .note(posInGroup == 1 ? "1º do grupo" : null)
                    .build());
        }

        return result;
    }

    private Match findMatchByRoundType(List<Match> matches, RoundType type) {
        return matches.stream()
                .filter(m -> m.getRound() != null && m.getRound().getType() == type)
                .findFirst()
                .orElse(null);
    }

    private boolean isPlayed(Match match) {
        return match != null && match.getStatus() == MatchStatus.COMPLETED
                && match.getWinnerParticipant() != null
                && match.getHomeParticipant() != null && match.getAwayParticipant() != null;
    }

    private TournamentParticipant otherSide(Match match, TournamentParticipant one) {
        return one.getId().equals(match.getHomeParticipant().getId())
                ? match.getAwayParticipant()
                : match.getHomeParticipant();
    }

    private TournamentStandingEntryResponse standingEntry(int position, TournamentParticipant participant, String note) {
        return TournamentStandingEntryResponse.builder()
                .position(position)
                .participantId(participant != null ? participant.getId() : null)
                .participantName(participantName(participant))
                .note(note)
                .build();
    }

    private String phaseLabel(RoundType type, int roundNumber) {
        if (type == null) {
            return "Rodada " + roundNumber;
        }
        return switch (type) {
            case FINAL -> "Final";
            case THIRD_PLACE -> "Disputa de 3º lugar";
            case SEMIFINAL -> "Semifinal";
            case QUARTERFINAL -> "Quartas de final";
            case KNOCKOUT -> "Rodada " + roundNumber;
            case GROUP_STAGE -> "Fase de grupos";
        };
    }

    private String participantName(TournamentParticipant participant) {
        if (participant == null) {
            return null;
        }
        if (participant.getTeam() != null && participant.getTeam().getName() != null) {
            return participant.getTeam().getName();
        }
        Contact contact = participant.getContact();
        if (contact != null) {
            Client client = contact.getClient();
            if (client != null && client.getNickname() != null && !client.getNickname().isBlank()) {
                return client.getNickname();
            }
            String fullName = ((contact.getFirstname() != null ? contact.getFirstname() : "") + " "
                    + (contact.getLastname() != null ? contact.getLastname() : "")).trim();
            if (!fullName.isBlank()) {
                return fullName;
            }
            if (client != null && client.getCompany() != null && !client.getCompany().isBlank()) {
                return client.getCompany();
            }
        }
        return null;
    }

    private boolean isPopulated(List<Match> matches) {
        return matches.stream()
                .anyMatch(m -> m.getHomeParticipant() != null || m.getAwayParticipant() != null);
    }

    private boolean allFinished(List<Match> matches) {
        return matches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED || m.getStatus() == MatchStatus.WALKOVER);
    }

    private void advanceWinner(Match match, TournamentParticipant winner) {
        if (match.getNextMatchId() == null) {
            return;
        }
        Match next = matchRepository.findById(match.getNextMatchId()).orElse(null);
        if (next == null) {
            return;
        }
        int position = match.getBracketPosition() != null ? match.getBracketPosition() : 0;
        if (position % 2 == 0) {
            next.setHomeParticipant(winner);
        } else {
            next.setAwayParticipant(winner);
        }
        matchRepository.save(next);
    }

    private void updateStandings(Tournament tournament, Match match,
                                 TournamentParticipant home, TournamentParticipant away,
                                 TournamentParticipant winner) {
        if (tournament.getType() != TournamentType.ROUND_ROBIN
                && tournament.getType() != TournamentType.GROUP_STAGE) {
            return;
        }
        TournamentParticipant loser = winner.getId().equals(home.getId()) ? away : home;
        applyStanding(tournament, winner, true, match, winner.getId().equals(home.getId()));
        applyStanding(tournament, loser, false, match, loser.getId().equals(home.getId()));
    }

    private void applyStanding(Tournament tournament, TournamentParticipant participant,
                               boolean won, Match match, boolean isHome) {
        groupStandingRepository.findByTournamentIdAndParticipantId(tournament.getId(), participant.getId())
                .ifPresent(standing -> {
                    standing.setGamesPlayed(standing.getGamesPlayed() + 1);
                    if (won) {
                        standing.setWins(standing.getWins() + 1);
                        standing.setPoints(standing.getPoints() + 3);
                    } else {
                        standing.setLosses(standing.getLosses() + 1);
                    }
                    Integer scoreFor = isHome ? match.getHomeScore() : match.getAwayScore();
                    Integer scoreAgainst = isHome ? match.getAwayScore() : match.getHomeScore();
                    if (scoreFor != null && scoreAgainst != null) {
                        standing.setScoreFor(standing.getScoreFor() + scoreFor);
                        standing.setScoreAgainst(standing.getScoreAgainst() + scoreAgainst);
                        standing.setGoalDifference(standing.getScoreFor() - standing.getScoreAgainst());
                    }
                    groupStandingRepository.save(standing);
                });
    }

    private void generateSingleElimination(Tournament tournament, List<TournamentParticipant> participants) {
        Collections.shuffle(participants);

        // Seed participants
        for (int i = 0; i < participants.size(); i++) {
            TournamentParticipant p = participants.get(i);
            p.setSeedNumber(i + 1);
            participantRepository.save(p);

            BracketSeed seed = BracketSeed.builder()
                    .tournament(tournament)
                    .participant(p)
                    .seedNumber(i + 1)
                    .build();
            bracketSeedRepository.save(seed);
        }

        int totalSlots = nextPowerOf2(participants.size());
        int totalRounds = (int) (Math.log(totalSlots) / Math.log(2));

        // Create all rounds
        List<Round> rounds = new ArrayList<>();
        for (int r = 1; r <= totalRounds; r++) {
            RoundType type = r == totalRounds ? RoundType.FINAL :
                    r == totalRounds - 1 ? RoundType.SEMIFINAL :
                    r == totalRounds - 2 ? RoundType.QUARTERFINAL : RoundType.KNOCKOUT;

            Round round = Round.builder()
                    .tournament(tournament)
                    .roundNumber(r)
                    .type(type)
                    .status(r == 1 ? RoundStatus.IN_PROGRESS : RoundStatus.PENDING)
                    .build();
            rounds.add(roundRepository.save(round));
        }

        // Create first round matches
        int firstRoundMatches = totalSlots / 2;
        List<Match> allMatches = new ArrayList<>();
        int matchNum = 1;

        for (int i = 0; i < firstRoundMatches; i++) {
            TournamentParticipant home = i < participants.size() ? participants.get(i) : null;
            // Standard seeding: 1 vs N, 2 vs N-1, etc.
            int awayIdx = totalSlots - 1 - i;
            TournamentParticipant away = awayIdx < participants.size() ? participants.get(awayIdx) : null;

            Match match = Match.builder()
                    .round(rounds.get(0))
                    .homeParticipant(home)
                    .awayParticipant(away)
                    .matchNumber(matchNum++)
                    .bracketPosition(i)
                    .status(MatchStatus.SCHEDULED)
                    .build();

            // Auto-advance BYE matches
            if (home != null && away == null) {
                match.setWinnerParticipant(home);
                match.setStatus(MatchStatus.WALKOVER);
            } else if (home == null && away != null) {
                match.setWinnerParticipant(away);
                match.setStatus(MatchStatus.WALKOVER);
            }

            allMatches.add(matchRepository.save(match));
        }

        // Create subsequent round matches (empty, to be filled as winners advance)
        for (int r = 1; r < totalRounds; r++) {
            int matchesInRound = totalSlots / (int) Math.pow(2, r + 1);
            for (int i = 0; i < matchesInRound; i++) {
                Match match = Match.builder()
                        .round(rounds.get(r))
                        .matchNumber(matchNum++)
                        .bracketPosition(i)
                        .status(MatchStatus.SCHEDULED)
                        .build();
                allMatches.add(matchRepository.save(match));
            }
        }

        // Link matches: each match's winner feeds into the next round
        int offset = 0;
        for (int r = 0; r < totalRounds - 1; r++) {
            int matchesInRound = totalSlots / (int) Math.pow(2, r + 1);
            int nextOffset = offset + matchesInRound;
            for (int i = 0; i < matchesInRound; i++) {
                Match current = allMatches.get(offset + i);
                Match next = allMatches.get(nextOffset + i / 2);
                current.setNextMatchId(next.getId());
                matchRepository.save(current);
            }
            offset = nextOffset;
        }

        // Disputa de 3º lugar (perdedores das semifinais), quando há semifinal
        if (totalRounds >= 2) {
            Round thirdPlaceRound = roundRepository.save(Round.builder()
                    .tournament(tournament)
                    .roundNumber(totalRounds + 1)
                    .type(RoundType.THIRD_PLACE)
                    .status(RoundStatus.PENDING)
                    .build());

            matchRepository.save(Match.builder()
                    .round(thirdPlaceRound)
                    .matchNumber(matchNum++)
                    .bracketPosition(0)
                    .status(MatchStatus.SCHEDULED)
                    .build());
        }
    }

    private void generateRoundRobin(Tournament tournament, List<TournamentParticipant> participants) {
        int n = participants.size();
        int rounds = (n % 2 == 0) ? n - 1 : n;

        List<TournamentParticipant> list = new ArrayList<>(participants);
        if (n % 2 != 0) {
            list.add(null); // BYE
            n++;
        }

        int matchNum = 1;
        for (int r = 0; r < rounds; r++) {
            Round round = Round.builder()
                    .tournament(tournament)
                    .roundNumber(r + 1)
                    .type(RoundType.GROUP_STAGE)
                    .status(r == 0 ? RoundStatus.IN_PROGRESS : RoundStatus.PENDING)
                    .build();
            round = roundRepository.save(round);

            for (int i = 0; i < n / 2; i++) {
                TournamentParticipant home = list.get(i);
                TournamentParticipant away = list.get(n - 1 - i);

                if (home == null || away == null) continue;

                Match match = Match.builder()
                        .round(round)
                        .homeParticipant(home)
                        .awayParticipant(away)
                        .matchNumber(matchNum++)
                        .status(MatchStatus.SCHEDULED)
                        .build();
                matchRepository.save(match);
            }

            // Rotate: fix first, rotate rest
            TournamentParticipant last = list.remove(list.size() - 1);
            list.add(1, last);
        }

        // Create standings
        for (TournamentParticipant p : participants) {
            GroupStanding standing = GroupStanding.builder()
                    .tournament(tournament)
                    .participant(p)
                    .groupNumber(1)
                    .build();
            groupStandingRepository.save(standing);
        }
    }

    private void generateGroupStage(Tournament tournament, List<TournamentParticipant> participants) {
        int groupsCount = tournament.getGroupsCount() != null ? tournament.getGroupsCount() : 2;
        Collections.shuffle(participants);

        // Distribute into groups
        for (int i = 0; i < participants.size(); i++) {
            int groupNum = (i % groupsCount) + 1;
            TournamentParticipant p = participants.get(i);
            p.setGroupNumber(groupNum);
            participantRepository.save(p);

            GroupStanding standing = GroupStanding.builder()
                    .tournament(tournament)
                    .participant(p)
                    .groupNumber(groupNum)
                    .build();
            groupStandingRepository.save(standing);
        }

        // Generate round-robin within each group
        int matchNum = 1;
        for (int g = 1; g <= groupsCount; g++) {
            final int groupNum = g;
            List<TournamentParticipant> groupParticipants = participants.stream()
                    .filter(p -> p.getGroupNumber() != null && p.getGroupNumber() == groupNum)
                    .toList();

            int n = groupParticipants.size();
            if (n < 2) continue;

            List<TournamentParticipant> list = new ArrayList<>(groupParticipants);
            if (n % 2 != 0) {
                list.add(null);
                n = list.size();
            }

            int roundsCount = n - 1;
            for (int r = 0; r < roundsCount; r++) {
                Round round = Round.builder()
                        .tournament(tournament)
                        .roundNumber(r + 1)
                        .type(RoundType.GROUP_STAGE)
                        .groupNumber(groupNum)
                        .status(r == 0 ? RoundStatus.IN_PROGRESS : RoundStatus.PENDING)
                        .build();
                round = roundRepository.save(round);

                for (int i = 0; i < n / 2; i++) {
                    TournamentParticipant home = list.get(i);
                    TournamentParticipant away = list.get(n - 1 - i);
                    if (home == null || away == null) continue;

                    Match match = Match.builder()
                            .round(round)
                            .homeParticipant(home)
                            .awayParticipant(away)
                            .matchNumber(matchNum++)
                            .status(MatchStatus.SCHEDULED)
                            .build();
                    matchRepository.save(match);
                }

                TournamentParticipant last = list.remove(list.size() - 1);
                list.add(1, last);
            }
        }
    }

    private int nextPowerOf2(int n) {
        int power = 1;
        while (power < n) power *= 2;
        return power;
    }
}
