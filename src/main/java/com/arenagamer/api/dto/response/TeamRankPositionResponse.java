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
@Schema(description = "Posição do time em um ranking por jogo")
public class TeamRankPositionResponse {

    private Long teamId;
    private String teamName;
    private String teamTag;
    private Integer clientUserId;
    private String clientName;
    private String region;
    private Long presetId;
    private String gameName;
    private Integer rankPoints;
    private Long globalPosition;
    private Long regionalPosition;
}
