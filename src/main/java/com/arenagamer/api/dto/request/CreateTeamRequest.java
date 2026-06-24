package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTeamRequest {

    @NotBlank @Size(max = 100)
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

    @Schema(description = "PUBLIC, PRIVATE ou PROTECTED", example = "PUBLIC")
    private Visibility visibility = Visibility.PUBLIC;

    @Valid
    @Schema(description = "Ranks por jogo (preset ativo). Um rank por preset.")
    private List<TeamRankRequest> ranks;
}
