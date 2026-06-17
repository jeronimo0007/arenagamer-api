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

    private String logoUrl;
}
