package com.arenagamer.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PresetRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nome do jogo")
    private String gameName;

    @Size(max = 100)
    private String platform;

    @NotNull
    @Min(1)
    private Integer teamSize;

    @NotNull
    @Min(1)
    private Integer minPlayersPerTeam;

    @NotNull
    @Min(1)
    private Integer maxPlayersPerTeam;

    @Size(max = 500)
    private String iconUrl;

    @Size(max = 500)
    private String gameImageUrl;

    private String rulesTemplate;

    private String scoringScript;

    private Boolean active;
}
