package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamJoinBan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamJoinBanResponse {

    private Integer clientUserId;
    private String reason;
    private LocalDateTime bannedUntil;
    private boolean active;

    public static TeamJoinBanResponse from(TeamJoinBan ban) {
        return TeamJoinBanResponse.builder()
                .clientUserId(ban.getClient().getUserId())
                .reason(ban.getReason())
                .bannedUntil(ban.getBannedUntil())
                .active(ban.getBannedUntil().isAfter(LocalDateTime.now()))
                .build();
    }
}
