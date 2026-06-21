package com.arenagamer.api.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TeamSettingsRequest {

    @Min(1)
    private Integer maxOwnedTeamsPerContact;

    @Min(1)
    private Integer maxParticipatedTeamsPerContact;

    @Min(1)
    private Integer maxTournamentsPerTeam;
}
