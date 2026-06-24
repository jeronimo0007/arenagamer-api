package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cliente membro do time")
public class TeamMemberClientResponse {

    private Integer clientUserId;
    private String clientName;
    private String state;
    private Boolean ownerClient;
    private Boolean captain;
    private LocalDateTime joinedAt;

    public static TeamMemberClientResponse from(TeamMember member, Integer ownerClientUserId) {
        return TeamMemberClientResponse.builder()
                .clientUserId(member.getClient().getUserId())
                .clientName(member.getClient().getCompany())
                .state(member.getClient().getState())
                .ownerClient(ownerClientUserId != null && ownerClientUserId.equals(member.getClient().getUserId()))
                .captain(Boolean.TRUE.equals(member.getIsCaptain()))
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
