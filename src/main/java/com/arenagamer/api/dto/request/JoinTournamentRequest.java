package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.TimeWindow;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class JoinTournamentRequest {

    private Long teamId;

    @Schema(description = """
            Nicknames dos jogadores do time que jogarão neste torneio.
            Obrigatório quando o time tem mais membros que maxPlayersPerTeam.
            Use playerClientUserIds como alternativa.""")
    private List<@Size(max = 50) String> playerNicknames;

    @Schema(description = "IDs dos clientes do time escalados (alternativa aos nicknames)")
    private List<Integer> playerClientUserIds;

    private Set<TimeWindow> availableWindows;

    @Schema(description = "Legado — prefira definir horários no perfil do jogador ou do time")
    private Boolean preferWeekends;
}
