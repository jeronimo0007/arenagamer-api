package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Team;
import com.arenagamer.api.entity.TournamentParticipantPlayer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Time inscrito no torneio")
public class TournamentParticipantTeamResponse {

    private Long id;
    private String name;
    private String tag;
    private String logoUrl;
    @Schema(description = "Jogadores escalados para este torneio")
    private List<TournamentParticipantPlayerResponse> players;

    public static TournamentParticipantTeamResponse from(Team team, List<TournamentParticipantPlayer> roster) {
        List<TournamentParticipantPlayerResponse> players = roster.stream()
                .map(TournamentParticipantPlayer::getClient)
                .map(TournamentParticipantPlayerResponse::from)
                .toList();
        return TournamentParticipantTeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .tag(team.getTag())
                .logoUrl(team.getLogoUrl())
                .players(players.isEmpty() ? null : players)
                .build();
    }
}
