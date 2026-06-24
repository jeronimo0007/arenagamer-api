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
@Schema(description = "Detalhes do time conforme visibilidade e vínculo do viewer")
public class TeamDetailResponse {

    @Schema(description = "Nível de acesso ao payload")
    private TeamDetailAccess access;

    @Schema(description = "true quando o time é PRIVATE e o viewer não é membro")
    private Boolean restricted;

    @Schema(description = "Mensagem quando restricted=true")
    private String message;

    private Long id;
    private String name;
    private String tag;
    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility privacy;
    private String logoUrl;
    private String bannerUrl;
    private String youtubeUrl;
    private String twitchUrl;
    private String description;
    private TeamOwnerResponse owner;
    private List<TeamPlayerResponse> players;
    @Schema(description = "Rank principal (jogo mais jogado ou preset informado)")
    private TeamRankSummaryResponse rank;
    @Schema(description = "Performance por jogo — somente acesso FULL")
    private List<TeamPerformanceResponse> performance;
    @Schema(description = "Torneios ativos — somente acesso FULL")
    private List<TeamActiveTournamentResponse> activeTournaments;
    @Schema(description = "Horários para jogar — somente acesso FULL")
    private AvailabilityScheduleResponse availability;
    private TeamStatus status;

    public static TeamDetailResponse privateRestricted() {
        return TeamDetailResponse.builder()
                .access(TeamDetailAccess.PRIVATE_RESTRICTED)
                .restricted(true)
                .message("Este time é privado. Apenas membros podem ver os detalhes.")
                .privacy(Visibility.PRIVATE)
                .build();
    }
}
