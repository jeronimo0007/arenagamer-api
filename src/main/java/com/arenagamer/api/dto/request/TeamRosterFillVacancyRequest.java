package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TeamRosterFillVacancyRequest {

    @NotNull
    private Integer clientUserId;
}
