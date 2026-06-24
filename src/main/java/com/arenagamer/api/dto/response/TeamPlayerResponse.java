package com.arenagamer.api.dto.response;

import com.arenagamer.api.entity.TeamMember;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cliente membro do time (dono também é player)")
public class TeamPlayerResponse {

    private Integer clientUserId;
    private String clientName;
    private Boolean owner;
    private Boolean captain;

    public static TeamPlayerResponse from(TeamMember member, Integer ownerClientUserId) {
        Integer clientUserId = member.getClient().getUserId();
        return TeamPlayerResponse.builder()
                .clientUserId(clientUserId)
                .clientName(member.getClient().getCompany())
                .owner(ownerClientUserId != null && ownerClientUserId.equals(clientUserId))
                .captain(Boolean.TRUE.equals(member.getIsCaptain()))
                .build();
    }
}
