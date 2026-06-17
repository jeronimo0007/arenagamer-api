package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Match;
import com.arenagamer.api.entity.enums.MatchStatus;
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
    private Long winnerParticipantId;
    private LocalDateTime scheduledAt;
    private TimeWindow timeWindow;
    private MatchStatus status;
    private Integer bracketPosition;
    private Long nextMatchId;

    public static MatchResponse from(Match m) {
        var builder = MatchResponse.builder()
                .id(m.getId())
                .matchNumber(m.getMatchNumber())
                .homeScore(m.getHomeScore())
                .awayScore(m.getAwayScore())
                .scheduledAt(m.getScheduledAt())
                .timeWindow(m.getTimeWindow())
                .status(m.getStatus())
                .bracketPosition(m.getBracketPosition())
                .nextMatchId(m.getNextMatchId());

        if (m.getHomeParticipant() != null) {
            builder.homeParticipantId(m.getHomeParticipant().getId());
        }
        if (m.getAwayParticipant() != null) {
            builder.awayParticipantId(m.getAwayParticipant().getId());
        }
        if (m.getWinnerParticipant() != null) {
            builder.winnerParticipantId(m.getWinnerParticipant().getId());
        }

        return builder.build();
    }
}
