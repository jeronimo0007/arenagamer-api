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
@Schema(description = "Entrada do leaderboard de times")
public class TeamLeaderboardEntryResponse {

    private Long position;
    private Long teamId;
    private String teamName;
    private String teamTag;
    private Integer clientUserId;
    private String clientName;
    private String region;
    private Integer rankPoints;
}
