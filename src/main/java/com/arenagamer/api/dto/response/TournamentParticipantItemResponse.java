package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TournamentParticipant;
import com.arenagamer.api.entity.TournamentParticipantPlayer;
import com.arenagamer.api.entity.enums.ParticipantStatus;
import com.arenagamer.api.entity.enums.TournamentFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Participante do torneio — team ou player conforme o format do torneio")
public class TournamentParticipantItemResponse {

    private Long id;
    private ParticipantStatus status;
    private Integer seedNumber;
    private Integer groupNumber;
    private LocalDateTime registeredAt;
    @Schema(description = "Preenchido quando format = SOLO")
    private TournamentParticipantSoloResponse player;
    @Schema(description = "Preenchido quando format = TEAM")
    private TournamentParticipantTeamResponse team;

    public static TournamentParticipantItemResponse from(
            TournamentParticipant participant,
            TournamentFormat format,
            Map<Long, List<TournamentParticipantPlayer>> rosterByParticipantId) {
        TournamentParticipantItemResponse.TournamentParticipantItemResponseBuilder builder =
                TournamentParticipantItemResponse.builder()
                        .id(participant.getId())
                        .status(participant.getStatus())
                        .seedNumber(participant.getSeedNumber())
                        .groupNumber(participant.getGroupNumber())
                        .registeredAt(participant.getRegisteredAt());

        if (format == TournamentFormat.SOLO && participant.getContact() != null) {
            builder.player(TournamentParticipantSoloResponse.from(participant.getContact()));
        }
        if (format == TournamentFormat.TEAM && participant.getTeam() != null) {
            List<TournamentParticipantPlayer> roster =
                    rosterByParticipantId.getOrDefault(participant.getId(), List.of());
            builder.team(TournamentParticipantTeamResponse.from(participant.getTeam(), roster));
        }
        return builder.build();
    }
}
