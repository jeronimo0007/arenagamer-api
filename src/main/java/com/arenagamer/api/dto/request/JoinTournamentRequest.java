package com.arenagamer.api.dto.request;

import com.arenagamer.api.entity.enums.TimeWindow;
import lombok.Data;

import java.util.Set;

@Data
public class JoinTournamentRequest {

    private Long teamId;

    private Set<TimeWindow> availableWindows;

    private Boolean preferWeekends;
}
