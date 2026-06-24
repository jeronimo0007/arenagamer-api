package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Rank do time em um jogo (preset)")
public class TeamRankRequest {

    @NotNull
    @Schema(description = "ID do preset do jogo", example = "1")
    private Long presetId;

    @NotNull
    @Min(0)
    @Schema(description = "Pontuação do rank", example = "1500")
    private Integer rankPoints;
}
