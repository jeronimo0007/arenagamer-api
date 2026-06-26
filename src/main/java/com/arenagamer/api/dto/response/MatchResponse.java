package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Client;
import com.arenagamer.api.entity.Contact;
import com.arenagamer.api.entity.Match;
import com.arenagamer.api.entity.Round;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.MatchStatus;
import com.arenagamer.api.entity.enums.RoundType;
import com.arenagamer.api.entity.enums.TimeWindow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponse {

    private Long id;
    private Integer matchNumber;
    private Long homeParticipantId;
    private Long awayParticipantId;
    private String homeParticipantName;
    private String awayParticipantName;
    private Integer homeScore;
    private Integer awayScore;
    private String resultProofUrl;
    private Long winnerParticipantId;
    private LocalDateTime scheduledAt;
    private TimeWindow timeWindow;
    private MatchStatus status;
    private Integer bracketPosition;
    private Long nextMatchId;
    private Integer roundNumber;
    private RoundType roundType;
    private Integer groupNumber;
    private String phaseLabel;

    public static MatchResponse from(Match m) {
        var builder = MatchResponse.builder()
                .id(m.getId())
                .matchNumber(m.getMatchNumber())
                .homeScore(m.getHomeScore())
                .awayScore(m.getAwayScore())
                .resultProofUrl(m.getResultProofUrl())
                .scheduledAt(m.getScheduledAt())
                .timeWindow(m.getTimeWindow())
                .status(m.getStatus())
                .bracketPosition(m.getBracketPosition())
                .nextMatchId(m.getNextMatchId());

        Round round = m.getRound();
        if (round != null) {
            builder.roundNumber(round.getRoundNumber());
            builder.roundType(round.getType());
            builder.groupNumber(round.getGroupNumber());
            builder.phaseLabel(resolvePhaseLabel(round));
        }

        if (m.getHomeParticipant() != null) {
            builder.homeParticipantId(m.getHomeParticipant().getId());
            builder.homeParticipantName(resolveName(m.getHomeParticipant()));
        }
        if (m.getAwayParticipant() != null) {
            builder.awayParticipantId(m.getAwayParticipant().getId());
            builder.awayParticipantName(resolveName(m.getAwayParticipant()));
        }
        if (m.getWinnerParticipant() != null) {
            builder.winnerParticipantId(m.getWinnerParticipant().getId());
        }

        return builder.build();
    }

    private static String resolvePhaseLabel(Round round) {
        RoundType type = round.getType();
        if (type == null) {
            return null;
        }
        return switch (type) {
            case FINAL -> "Final (1º lugar)";
            case THIRD_PLACE -> "Disputa de 3º lugar";
            case SEMIFINAL -> "Semifinal";
            case QUARTERFINAL -> "Quartas de final";
            case KNOCKOUT -> "Eliminatória — Rodada " + round.getRoundNumber();
            case GROUP_STAGE -> round.getGroupNumber() != null
                    ? "Fase de grupos — Grupo " + round.getGroupNumber()
                    : "Fase de grupos";
        };
    }

    private static String resolveName(TournamentParticipant participant) {
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
}
