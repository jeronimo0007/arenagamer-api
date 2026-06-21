package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamSettingsResponse {

    private Integer maxOwnedTeamsPerContact;
    private Integer maxParticipatedTeamsPerContact;
    private Integer maxTournamentsPerTeam;
    private Boolean unlimitedTournamentsPerTeam;

    public static TeamSettingsResponse from(TeamSettings settings) {
        return TeamSettingsResponse.builder()
                .maxOwnedTeamsPerContact(settings.getMaxOwnedTeamsPerContact())
                .maxParticipatedTeamsPerContact(settings.getMaxParticipatedTeamsPerContact())
                .maxTournamentsPerTeam(settings.getMaxTournamentsPerTeam())
                .unlimitedTournamentsPerTeam(settings.getMaxTournamentsPerTeam() == null)
                .build();
    }
}
