package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamSettings;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamSettingsResponse {

    private Integer maxOwnedTeamsPerClient;
    private Integer maxParticipatedTeamsPerClient;
    private Integer maxTournamentsPerTeam;
    private Integer maxTournamentsPerClient;
    private Integer teamJoinBanDaysAfterUnreplacedExit;
    private Boolean unlimitedTournamentsPerTeam;
    private Boolean unlimitedTournamentsPerClient;

    /** @deprecated use maxOwnedTeamsPerClient */
    @JsonProperty("maxOwnedTeamsPerContact")
    public Integer getMaxOwnedTeamsPerContact() {
        return maxOwnedTeamsPerClient;
    }

    /** @deprecated use maxParticipatedTeamsPerClient */
    @JsonProperty("maxParticipatedTeamsPerContact")
    public Integer getMaxParticipatedTeamsPerContact() {
        return maxParticipatedTeamsPerClient;
    }

    public static TeamSettingsResponse from(TeamSettings settings) {
        return TeamSettingsResponse.builder()
                .maxOwnedTeamsPerClient(settings.getMaxOwnedTeamsPerClient())
                .maxParticipatedTeamsPerClient(settings.getMaxParticipatedTeamsPerClient())
                .maxTournamentsPerTeam(settings.getMaxTournamentsPerTeam())
                .maxTournamentsPerClient(settings.getMaxTournamentsPerClient())
                .teamJoinBanDaysAfterUnreplacedExit(settings.getTeamJoinBanDaysAfterUnreplacedExit())
                .unlimitedTournamentsPerTeam(settings.getMaxTournamentsPerTeam() == null)
                .unlimitedTournamentsPerClient(settings.getMaxTournamentsPerClient() == null)
                .build();
    }
}
