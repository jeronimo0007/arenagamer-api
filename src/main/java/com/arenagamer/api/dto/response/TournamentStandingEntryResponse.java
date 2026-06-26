package com.arenagamer.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentStandingEntryResponse {

    private Integer position;
    private Long participantId;
    private String participantName;
    private Integer groupNumber;
    private Integer points;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private String note;
}
