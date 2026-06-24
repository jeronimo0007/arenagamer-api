package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateTeamRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String tag;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String bannerUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String twitchUrl;

    @Size(max = 500)
    private String otherSocialUrl;

    private String description;

    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED")
    private Visibility visibility;

    @Valid
    @Schema(description = "Substitui todos os ranks do time. Somente presets ativos.")
    private List<TeamRankRequest> ranks;

    @Valid
    @Schema(description = "Substitui os horários do time (somente dono). Capitão deve usar solicitação de mudança.")
    private AvailabilityScheduleRequest availability;
}
