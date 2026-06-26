package com.arenagamer.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TeamSettingsRequest {

    @Min(1)
    @JsonAlias("maxOwnedTeamsPerContact")
    private Integer maxOwnedTeamsPerClient;

    @Min(1)
    @JsonAlias("maxParticipatedTeamsPerContact")
    private Integer maxParticipatedTeamsPerClient;

    @Min(1)
    private Integer maxTournamentsPerTeam;

    @Min(1)
    private Integer maxTournamentsPerClient;

    @Min(1)
    private Integer teamJoinBanDaysAfterUnreplacedExit;
}
