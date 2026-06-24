package com.arenagamer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Performance do time em um jogo (posições global e regional)")
public class TeamPerformanceResponse {

    private Long presetId;
    private String gameName;
    private String platform;
    private Integer rankPoints;
    private Long globalPosition;
    private Long regionalPosition;
}
