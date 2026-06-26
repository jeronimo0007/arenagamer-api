package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamRosterReallocateRequest {

    @NotNull
    private String tournamentSlug;

    @NotNull
    private Integer outClientUserId;

    @NotNull
    private Integer inClientUserId;
}
