package com.arenagamer.api.dto.response;

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
@Schema(description = "Rank do meu cliente em um ou mais jogos")
public class MyTeamRankResponse {

    private Integer clientUserId;
    private String clientName;
    private String region;
    private List<TeamRankPositionResponse> ranks;
}
