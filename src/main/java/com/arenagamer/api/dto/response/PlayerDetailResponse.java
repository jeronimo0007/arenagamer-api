package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.enums.TeamStatus;
import com.arenagamer.api.entity.enums.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Perfil público do jogador (cliente)")
public class PlayerDetailResponse {

    private TeamDetailAccess access;
    private Boolean restricted;
    private String message;

    @Schema(description = "ID do jogador (clientUserId)")
    private Integer clientUserId;
    private String nickname;
    private String profileImageUrl;
    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility privacy;
    @Schema(description = "Rank principal (jogo mais jogado ou preset informado)")
    private TeamRankSummaryResponse rank;
    @Schema(description = "Performance por jogo — somente acesso FULL")
    private List<TeamPerformanceResponse> performance;
    @Schema(description = "Torneios ativos — somente acesso FULL")
    private List<TeamActiveTournamentResponse> activeTournaments;
    @Schema(description = "Horários para jogar — somente acesso FULL")
    private AvailabilityScheduleResponse availability;
    private TeamStatus status;

    public static PlayerDetailResponse privateRestricted() {
        return PlayerDetailResponse.builder()
                .access(TeamDetailAccess.PRIVATE_RESTRICTED)
                .restricted(true)
                .message("Este perfil é privado. Apenas o próprio jogador pode ver os detalhes.")
                .privacy(Visibility.PRIVATE)
                .build();
    }
}
