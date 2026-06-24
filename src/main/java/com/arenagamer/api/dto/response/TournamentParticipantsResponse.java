package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.TournamentParticipantPlayer;
import com.arenagamer.api.entity.enums.TournamentFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Lista de inscritos no torneio")
public class TournamentParticipantsResponse {

    private Long tournamentId;
    private String slug;
    private String tournamentName;
    @Schema(description = "SOLO ou TEAM — define se cada item traz player ou team")
    private TournamentFormat format;
    private List<TournamentParticipantItemResponse> participants;

    public static TournamentParticipantsResponse from(
            Tournament tournament,
            List<TournamentParticipant> participants,
            List<TournamentParticipantPlayer> rosterPlayers) {
        Map<Long, List<TournamentParticipantPlayer>> rosterByParticipantId = rosterPlayers.stream()
                .collect(Collectors.groupingBy(p -> p.getParticipant().getId()));

        List<TournamentParticipantItemResponse> items = participants.stream()
                .map(p -> TournamentParticipantItemResponse.from(
                        p, tournament.getFormat(), rosterByParticipantId))
                .toList();

        return TournamentParticipantsResponse.builder()
                .tournamentId(tournament.getId())
                .slug(tournament.getSlug())
                .tournamentName(tournament.getName())
                .format(tournament.getFormat())
                .participants(items)
                .build();
    }
}
