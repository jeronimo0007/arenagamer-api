package com.arenagamer.api.service;

import com.arenagamer.api.entity.*;
import com.arenagamer.api.entity.enums.*;
import com.arenagamer.api.exception.BusinessException;
import com.arenagamer.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BracketService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final RoundRepository roundRepository;
    private final MatchRepository matchRepository;
    private final BracketSeedRepository bracketSeedRepository;
    private final GroupStandingRepository groupStandingRepository;

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
