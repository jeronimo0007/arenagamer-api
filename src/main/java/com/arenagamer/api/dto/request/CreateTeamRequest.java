package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTeamRequest {

    @NotBlank @Size(max = 100)
    private String name;

    @Size(max = 20)
    private String tag;

    @Size(max = 500)
    private String logoUrl;

    @Size(max = 500)
    private String youtubeUrl;

    @Size(max = 500)
    private String instagramUrl;

    @Size(max = 500)
    private String twitchUrl;

    @Size(max = 500)
    private String otherSocialUrl;

    private String rulesChange;
}
