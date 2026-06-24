package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.Preset;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Preset de configuração de jogo — gameName é o nome do jogo")
public class PresetResponse {

    private Long id;
    @Schema(description = "Nome do jogo")
    private String gameName;
    private String platform;
    private Integer teamSize;
    private Integer minPlayersPerTeam;
    private Integer maxPlayersPerTeam;
    private String iconUrl;
    private String gameImageUrl;
    private String rulesTemplate;
    private Boolean active;

    public static PresetResponse from(Preset preset) {
        return PresetResponse.builder()
                .id(preset.getId())
                .gameName(preset.getGameName())
                .platform(preset.getPlatform())
                .teamSize(preset.getTeamSize())
                .minPlayersPerTeam(preset.getMinPlayersPerTeam())
                .maxPlayersPerTeam(preset.getMaxPlayersPerTeam())
                .iconUrl(preset.getIconUrl())
                .gameImageUrl(preset.getGameImageUrl())
                .rulesTemplate(preset.getRulesTemplate())
                .active(preset.getActive())
                .build();
    }
}
