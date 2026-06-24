package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Tournament;
import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.enums.TournamentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Torneio ativo em que o time está inscrito")
public class TeamActiveTournamentResponse {

    private Long tournamentId;
    private String slug;
    private String name;
    private String gameName;
    private Long presetId;
    private TournamentStatus status;
    private LocalDateTime registeredAt;

    public static TeamActiveTournamentResponse from(TournamentParticipant participant) {
        Tournament tournament = participant.getTournament();
        String gameName = tournament.getPreset() != null && tournament.getPreset().getGameName() != null
                ? tournament.getPreset().getGameName()
                : tournament.getGameName();
        return TeamActiveTournamentResponse.builder()
                .tournamentId(tournament.getId())
                .slug(tournament.getSlug())
                .name(tournament.getName())
                .gameName(gameName)
                .presetId(tournament.getPreset() != null ? tournament.getPreset().getId() : null)
                .status(tournament.getStatus())
                .registeredAt(participant.getRegisteredAt())
                .build();
    }
}
