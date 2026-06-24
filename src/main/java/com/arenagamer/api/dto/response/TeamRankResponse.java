package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamRank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rank do time em um jogo (preset)")
public class TeamRankResponse {

    private Long presetId;
    private String gameName;
    private String platform;
    private Integer rankPoints;

    public static TeamRankResponse from(TeamRank rank) {
        return TeamRankResponse.builder()
                .presetId(rank.getPreset().getId())
                .gameName(rank.getPreset().getGameName())
                .platform(rank.getPreset().getPlatform())
                .rankPoints(rank.getRankPoints())
                .build();
    }
}
